/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.service;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponents.commons.keymanagement.KeysManager;
import ch.post.it.evoting.controlcomponents.returncodes.domain.CombinedCorrectnessInformationExtended;
import ch.post.it.evoting.controlcomponents.returncodes.domain.CombinedCorrectnessInformationExtendedRepository;
import ch.post.it.evoting.controlcomponents.returncodes.domain.ComputedVerificationCard;
import ch.post.it.evoting.controlcomponents.returncodes.domain.ComputedVerificationCardRepository;
import ch.post.it.evoting.controlcomponents.returncodes.domain.ControlComponentContext;
import ch.post.it.evoting.controlcomponents.returncodes.domain.LongChoiceReturnCodesShareExponentiationProof;
import ch.post.it.evoting.controlcomponents.returncodes.domain.LongVoteCastReturnCodesShareExponentiationProof;
import ch.post.it.evoting.controlcomponents.returncodes.domain.ReturnCodesMessage;
import ch.post.it.evoting.controlcomponents.returncodes.domain.ReturnCodesMessageFactory;
import ch.post.it.evoting.controlcomponents.returncodes.domain.primarykey.CombinedCorrectnessInformationExtendedPrimaryKey;
import ch.post.it.evoting.controlcomponents.returncodes.domain.primarykey.ComputedVerificationCardPrimaryKey;
import ch.post.it.evoting.controlcomponents.returncodes.service.exception.AlreadyExponentiatedComputeVerificationCardException;
import ch.post.it.evoting.controlcomponents.returncodes.service.exception.MissingCombinedCorrectnessInformationExtendedException;
import ch.post.it.evoting.controlcomponents.returncodes.service.exception.MissingComputeVerificationCardException;
import ch.post.it.evoting.controlcomponents.returncodes.service.exception.NotExponentiatedComputeVerificationCardException;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.exponentiation.ExponentiatedElementsAndProof;
import ch.post.it.evoting.cryptolib.elgamal.exponentiation.ExponentiationService;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptoprimitives.hashing.HashService;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.CombinedCorrectnessInformation;
import ch.post.it.evoting.domain.election.model.confirmation.TraceableConfirmationMessage;
import ch.post.it.evoting.domain.election.model.messaging.PayloadSignatureException;
import ch.post.it.evoting.domain.election.model.messaging.SafeStreamDeserializationException;
import ch.post.it.evoting.domain.election.payload.sign.PayloadSigner;
import ch.post.it.evoting.domain.returncodes.ReturnCodeComputationDTO;
import ch.post.it.evoting.domain.returncodes.ReturnCodesExponentiationResponsePayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodesInput;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectReader;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectReaderImpl;

@Service
public class ReturnCodesExponentiationConsumer {
	private static final Logger LOGGER = LoggerFactory.getLogger(ReturnCodesExponentiationConsumer.class);
	private static final org.apache.logging.log4j.Logger SECURE_LOGGER = LogManager.getLogger("SecureLog");

	private static final int MAX_CONFIRMATION_ATTEMPTS = 5;
	private static final String CONFIRM_STRING_PADDING = "confirm";

	private final ExponentiationService exponentiationService;
	private final RabbitTemplate rabbitTemplate;
	private final VoterReturnCodeGenerationKeyDerivationService voterReturnCodeGenerationKeyDerivationService;
	private final ReturnCodesKeyRepository returnCodesKeyRepository;
	private final PayloadSigner payloadSigner;
	private final KeysManager keysManager;
	private final HashService hashService;
	private final ComputedVerificationCardRepository computedVerificationCardRepository;
	private final CombinedCorrectnessInformationExtendedRepository combinedCorrectnessInformationExtendedRepository;

	private final String controlComponentId;
	private final String computationOutputQueue;

	private final ReturnCodesMessageFactory returnCodesMessageFactory;

	@Autowired
	public ReturnCodesExponentiationConsumer(final RabbitTemplate rabbitTemplate, final ExponentiationService exponentiationService,
			final VoterReturnCodeGenerationKeyDerivationService voterReturnCodeGenerationKeyDerivationService,
			final ReturnCodesKeyRepository returnCodesKeyRepository,
			final PayloadSigner payloadSigner, final KeysManager keysManager, final HashService hashService,
			final ComputedVerificationCardRepository computedVerificationCardRepository,
			final CombinedCorrectnessInformationExtendedRepository combinedCorrectnessInformationExtendedRepository,
			@Value("${keys.nodeId:defCcxId}")
			final String controlComponentId,
			@Value("${verification.computation.response.queue}")
			final String computationOutputQueue, final ReturnCodesMessageFactory returnCodesMessageFactory) {

		this.rabbitTemplate = rabbitTemplate;
		this.exponentiationService = exponentiationService;
		this.voterReturnCodeGenerationKeyDerivationService = voterReturnCodeGenerationKeyDerivationService;
		this.returnCodesKeyRepository = returnCodesKeyRepository;
		this.payloadSigner = payloadSigner;
		this.keysManager = keysManager;
		this.hashService = hashService;
		this.computedVerificationCardRepository = computedVerificationCardRepository;
		this.combinedCorrectnessInformationExtendedRepository = combinedCorrectnessInformationExtendedRepository;

		this.controlComponentId = controlComponentId;
		this.computationOutputQueue = computationOutputQueue;

		this.returnCodesMessageFactory = returnCodesMessageFactory;
	}

	private static boolean isChoiceReturnCodesComputation(final ReturnCodeComputationDTO<ReturnCodesInput> data) {
		return data.getPayload().getConfirmationKeyVerificationInput() == null;
	}

	private static ZpGroupElement derivePublicKey(final ZpSubgroup group, final Exponent exponent) throws GeneralCryptoLibException {
		return group.getGenerator().exponentiate(exponent);
	}

	@RabbitListener(queues = "${verification.computation.request.queue}", autoStartup = "false")
	public void onMessage(Message message) throws SafeStreamDeserializationException {

		StreamSerializableObjectReader<ReturnCodeComputationDTO<ReturnCodesInput>> reader = new StreamSerializableObjectReaderImpl<>();
		ReturnCodeComputationDTO<ReturnCodesInput> data = reader.read(message.getBody(), 1, message.getBody().length);

		validateParameters(data);
		if (isValid(data)) {
			compute(data);
		}
	}

	private void validateParameters(final ReturnCodeComputationDTO<ReturnCodesInput> data) {
		List<String> errors = new ArrayList<>();
		if (isBlank(data.getRequestId())) {
			errors.add("Request Id");
			LOGGER.error("Request Id is empty and this field is mandatory");
		}
		if (isBlank(data.getVerificationCardId())) {
			errors.add("Verification card Id");
			LOGGER.error("Verification card Id is empty and this field is mandatory");
		}
		if (isBlank(data.getVerificationCardSetId())) {
			errors.add("Verification card set Id");
			LOGGER.error("Verification card set Id is empty and this field is mandatory");
		}
		if (isBlank(data.getElectionEventId())) {
			errors.add("Election Event Id");
			LOGGER.error("Election Event Id is empty and this field is mandatory");
		}
		if (data.getPayload() == null) {
			errors.add("Payload");
			LOGGER.error("Payload is null and this field is mandatory");
		}
		if (!errors.isEmpty()) {
			throw new IllegalArgumentException("The following fields presents validation errors: " + errors);
		}
	}

	/**
	 * Checks if the input data is valid
	 */
	private boolean isValid(final ReturnCodeComputationDTO<ReturnCodesInput> data) {

		final String electionEventId = data.getElectionEventId();
		final String verificationCardSetId = data.getVerificationCardSetId();
		final String verificationCardId = data.getVerificationCardId();

		// Get the mathematicalGroup
		final ZpSubgroup mathematicalGroup;
		try {
			mathematicalGroup = returnCodesKeyRepository.getMathematicalGroup(electionEventId, verificationCardSetId);

			if (mathematicalGroup == null) {
				LOGGER.error("Unexpected scenario - Return Codes mathematicalGroup is null.");
				return false;
			}
		} catch (KeyManagementException e) {
			LOGGER.error("Unexpected error getting the group for election Event Id {} and VerificationCardSetId{}", electionEventId,
					verificationCardSetId);
			return false;
		}

		// The control components check that the verification card is in the list L_decPCC,j, i.e. that it already decrypted the encrypted partial
		// Choice Return Codes for this verification card id.
		ComputedVerificationCard computedVerificationCard = getComputedVerificationCard(data);

		if (isChoiceReturnCodesComputation(data)) {
			// Generate the long Choice Return Codes share

			// The control components check that the verification card IS NOT in the list L_sentVotes,j, i.e. that it did not process partial Choice
			// Return Codes for this verification card ID before.
			if (computedVerificationCard.isExponentiationComputed()) {
				LOGGER.error(
						"Cannot generate the CCR_j long Choice Return Code shares since the CCR_j already generated them in a previous attempt for "
								+ "electionEventId {}, verificationCardSetId {} and verificationCardId {}", electionEventId, verificationCardSetId,
						verificationCardId);
				throw new AlreadyExponentiatedComputeVerificationCardException(electionEventId, verificationCardId);
			}

			return checkPartialChoiceReturnCodes(data, mathematicalGroup);

		} else {
			// Generate the long Vote Cast Return Codes share

			// The control components check that the verification card IS in the list L_sentVotes,j, i.e. that it processed the partial Choice Return
			// Codes for this verification card ID.
			if (!computedVerificationCard.isExponentiationComputed()) {
				LOGGER.error("Cannot generate the CCR_j long Vote Cast Return Code shares since the CCR_j did not execute the createLCCShare_j for  "
								+ "electionEventId {}, verificationCardSetId {} and verificationCardId {}", electionEventId, verificationCardSetId,
						verificationCardId);
				throw new NotExponentiatedComputeVerificationCardException(electionEventId, verificationCardId);
			}

			if (!checkConfirmationAttempts(data)) {
				return false;
			}
			if (!checkConfirmationKey(data, mathematicalGroup)) {
				return false;
			}

			LOGGER.info("Confirmation key is valid for election event id {}, verification card set id {}, verification card id {}", electionEventId,
					verificationCardSetId, verificationCardId);

			return true;
		}
	}

	/**
	 * Checks the validity of the partial Choice Return Codes pCC
	 *
	 * @param data              The voting server's data object, containing the partial Choice Return Codes
	 * @param mathematicalGroup The group parameters (p,q,g) of the partial Choice Return Codes' expected mathematical group
	 */
	private boolean checkPartialChoiceReturnCodes(final ReturnCodeComputationDTO<ReturnCodesInput> data, final ZpSubgroup mathematicalGroup) {

		final List<BigInteger> partialChoiceReturnCodes = data.getPayload().getReturnCodesInputElements();

		if (partialChoiceReturnCodes == null) {
			LOGGER.error("{} the partial Choice Return Codes field in the payload was null",
					ReturnCodesVerificationServiceConstants.UNEXPECTED_SCENARIO_PROCESSING_CC);
			return false;
		}

		final CombinedCorrectnessInformation combinedCorrectnessInformation = getCombinedCorrectnessInformation(data.getElectionEventId(),
				data.getVerificationCardSetId());

		// check that the number of partial choice return codes received matches the totalNumberOfSelections.
		if (partialChoiceReturnCodes.size() != combinedCorrectnessInformation.getTotalNumberOfSelections()) {
			LOGGER.error("The size of the partial choice return code list ({}) does not match the expected size ({}).",
					partialChoiceReturnCodes.size(), combinedCorrectnessInformation.getTotalNumberOfSelections());
			return false;
		}

		//  check that all partial choice return codes are distinct.
		if (!partialChoiceReturnCodes.stream().allMatch(new HashSet<>()::add)) {
			LOGGER.error("Duplicate element in the partial choice return code list.");
			return false;
		}

		// Validate the group membership of each partial choice return code.
		for (final BigInteger partialChoiceReturnCode : partialChoiceReturnCodes) {
			try {
				if (!validateGroupMembership(partialChoiceReturnCode, mathematicalGroup)) {
					LOGGER.error(
							"Unexpected scenario when processing the partial Choice Return Codes, the partial Choice Return Code is not a member of the group.");

					return false;
				}
			} catch (GeneralCryptoLibException e) {
				LOGGER.error(ReturnCodesVerificationServiceConstants.UNEXPECTED_ERROR_CHECKING_CC_IS_MEMBER_OF_GROUP, e);
				return false;
			}
		}

		return true;
	}

	private CombinedCorrectnessInformation getCombinedCorrectnessInformation(final String electionEventId, final String verificationCardSetId) {
		final CombinedCorrectnessInformationExtendedPrimaryKey combinedCorrectnessInformationExtendedPrimaryKey = new CombinedCorrectnessInformationExtendedPrimaryKey(
				electionEventId, verificationCardSetId);

		final Optional<CombinedCorrectnessInformationExtended> optionalCombinedCorrectnessExtendedInformation = combinedCorrectnessInformationExtendedRepository
				.findById(combinedCorrectnessInformationExtendedPrimaryKey);

		if (!optionalCombinedCorrectnessExtendedInformation.isPresent()) {
			throw new MissingCombinedCorrectnessInformationExtendedException(electionEventId, verificationCardSetId);
		}

		return optionalCombinedCorrectnessExtendedInformation.get().getCombinedCorrectnessInformation();
	}

	private boolean checkConfirmationAttempts(final ReturnCodeComputationDTO<ReturnCodesInput> data) {
		final int attempts = getConfirmationAttempts(data);
		if (attempts >= MAX_CONFIRMATION_ATTEMPTS) {
			LOGGER.error("Confirmation attempts exceeded. Attempts: {}", attempts);
			return false;
		} else {
			LOGGER.info("Confirmation attempts not exceeded. Attempts: {}", attempts);
			return true;
		}
	}

	private ComputedVerificationCard getComputedVerificationCard(final ReturnCodeComputationDTO<ReturnCodesInput> data) {
		final Optional<ComputedVerificationCard> optionalComputedVerificationCard = computedVerificationCardRepository
				.findById(new ComputedVerificationCardPrimaryKey(data.getElectionEventId(), data.getVerificationCardId()));

		if (!optionalComputedVerificationCard.isPresent()) {
			LOGGER.error(
					"Cannot generate CCR_j long Choice Return Code shares for this verification card ID since the CCR_j did not register a corresponding partial decryption of the partial Choice Return Codes for electionEventId {}, verificationCardSetId {} and verificationCardId {}",
					data.getElectionEventId(), data.getVerificationCardSetId(), data.getVerificationCardId());
			throw new MissingComputeVerificationCardException(data.getElectionEventId(), data.getVerificationCardId());
		}
		return optionalComputedVerificationCard.get();
	}

	private Integer getConfirmationAttempts(final ReturnCodeComputationDTO<ReturnCodesInput> data) {
		final ComputedVerificationCard computedVerificationCard = getComputedVerificationCard(data);
		return computedVerificationCard.getConfirmationAttempts();
	}

	private void incrementConfirmationAttempts(final ReturnCodeComputationDTO<ReturnCodesInput> data) {
		final ComputedVerificationCard computedVerificationCard = getComputedVerificationCard(data);
		computedVerificationCard.setConfirmationAttempts(computedVerificationCard.getConfirmationAttempts() + 1);
		computedVerificationCardRepository.save(computedVerificationCard);
	}

	/**
	 * Checks the Confirmation Key CK.
	 *
	 * @param data              The voting server's data object, containing the Confirmation Key.
	 * @param mathematicalGroup The group parameters (p,q,g) of the partial Choice Return Codes' expected mathematical group
	 */
	private boolean checkConfirmationKey(final ReturnCodeComputationDTO<ReturnCodesInput> data, final ZpSubgroup mathematicalGroup) {
		try {
			final List<BigInteger> confirmationKey = data.getPayload().getReturnCodesInputElements();

			if (confirmationKey == null) {
				LOGGER.error("{} the confirmation key in the payload was null",
						ReturnCodesVerificationServiceConstants.UNEXPECTED_SCENARIO_PROCESSING_CC);
				return false;
			}

			if (confirmationKey.size() != 1) {
				LOGGER.error("Unexpected scenario: more than one confirmation key: {}", confirmationKey);
				return false;
			}
			final TraceableConfirmationMessage votingClientConfirmationMessage = ObjectMappers
					.fromJson(data.getPayload().getConfirmationKeyVerificationInput().getConfirmationMessage(), TraceableConfirmationMessage.class);
			final String votingClientConfirmationKeyString = new String(
					Base64.getDecoder().decode(votingClientConfirmationMessage.getConfirmationKey()), StandardCharsets.UTF_8);

			final BigInteger votingClientConfirmationKey = new BigInteger(votingClientConfirmationKeyString);
			final BigInteger votingServerConfirmationKey = confirmationKey.get(0);

			// Check that the voting client's confirmation key is not null
			if (votingServerConfirmationKey == null) {
				LOGGER.error("Unexpected scenario when checking the confirmation key: the voting client's confirmation key in the payload is null");
				return false;
			}
			if (!validateGroupMembership(votingServerConfirmationKey, mathematicalGroup)) {
				LOGGER.error("Unexpected scenario when checking the confirmation key: the confirmation key {} is not a member of the group {}.",
						votingServerConfirmationKey, mathematicalGroup);
				return false;
			}

			return votingServerConfirmationKey.equals(votingClientConfirmationKey);
		} catch (IOException e) {
			LOGGER.error("Unexpected error checking the group membership of the confirmation key and its consistency:", e);
			return false;
		} catch (GeneralCryptoLibException e) {
			LOGGER.error("Unexpected error checking the group membership of the confirmation key:", e);
			return false;
		}
	}

	/**
	 * Validates the group membership of the partial Choice Return Codes or the Confirmation Key
	 *
	 * @param partialChoiceReturnCodeOrConfirmationKey The partial Choice Return Codes or the Confirmation Key as a BigInteger
	 * @param mathematicalGroup                        ZpSubgroup for the group
	 */
	private boolean validateGroupMembership(final BigInteger partialChoiceReturnCodeOrConfirmationKey, final ZpSubgroup mathematicalGroup)
			throws GeneralCryptoLibException {
		ZpGroupElement groupElement = new ZpGroupElement(partialChoiceReturnCodeOrConfirmationKey, mathematicalGroup);
		return mathematicalGroup.isGroupMember(groupElement);

	}

	private void compute(final ReturnCodeComputationDTO<ReturnCodesInput> data) {

		try {
			final ElGamalPrivateKey ccrjReturnCodesGenerationSecretKey = getCcrjReturnCodesGenerationSecretKey(data);
			final ZpSubgroup zpSubgroup = ccrjReturnCodesGenerationSecretKey.getGroup();

			// List containing either the partial Choice Return Codes or the Confirmation Key.
			final List<BigInteger> partialChoiceReturnCodesOrConfirmationKey = data.getPayload().getReturnCodesInputElements();

			// List containing either the hashed, squared partial Choice Return Codes or the hashed, squared Confirmation Key
			final List<BigInteger> hashedSquaredPccOrCk = hashAndSquare(partialChoiceReturnCodesOrConfirmationKey, zpSubgroup);

			final ControlComponentContext context = new ControlComponentContext(data.getElectionEventId(), data.getVerificationCardSetId(),
					controlComponentId);

			if (isChoiceReturnCodesComputation(data)) {
				// Long Choice Return Codes share computation
				final Exponent voterChoiceReturnCodeGenerationSecretKey = deriveVoterChoiceReturnCodeGenerationSecretKey(data,
						ccrjReturnCodesGenerationSecretKey);
				final ExponentiatedElementsAndProof<BigInteger> longChoiceReturnCodesShareAndProof = computeLongReturnCodesShareAndProof(data,
						zpSubgroup, hashedSquaredPccOrCk, voterChoiceReturnCodeGenerationSecretKey);

				final LongChoiceReturnCodesShareExponentiationProof exponentiationProof = new LongChoiceReturnCodesShareExponentiationProof(
						data.getVerificationCardId(), hashedSquaredPccOrCk, longChoiceReturnCodesShareAndProof.exponentiatedElements(),
						longChoiceReturnCodesShareAndProof.exponentiationProof());

				// The control component stores the fact that it created the Long Choice Return Codes share for this specific verification card id.
				final ComputedVerificationCard computedVerificationCard = getComputedVerificationCard(data);
				computedVerificationCard.setExponentiationComputed(true);
				computedVerificationCardRepository.save(computedVerificationCard);

				final ReturnCodesMessage message = returnCodesMessageFactory
						.buildLongChoiceReturnCodesShareExponentiationProofLogMessage(context, exponentiationProof);
				SECURE_LOGGER.info(message);

				final ZpGroupElement voterChoiceReturnCodeGenerationPublicKey = derivePublicKey(zpSubgroup, voterChoiceReturnCodeGenerationSecretKey);
				sendResponse(data, longChoiceReturnCodesShareAndProof, voterChoiceReturnCodeGenerationPublicKey, null);
			} else {
				// Long Vote Cast Return Code share computation
				final Exponent voterVoteCastReturnCodeGenerationSecretKey = deriveVoterVoteCastReturnCodeGenerationSecretKey(data,
						ccrjReturnCodesGenerationSecretKey);
				final ExponentiatedElementsAndProof<BigInteger> longVoteCastReturnCodeShareAndProof = computeLongReturnCodesShareAndProof(data,
						zpSubgroup, hashedSquaredPccOrCk, voterVoteCastReturnCodeGenerationSecretKey);

				final LongVoteCastReturnCodesShareExponentiationProof exponentiationProof = new LongVoteCastReturnCodesShareExponentiationProof(
						data.getVerificationCardId(), getConfirmationAttempts(data), hashedSquaredPccOrCk.get(0),
						longVoteCastReturnCodeShareAndProof.exponentiatedElements().get(0),
						longVoteCastReturnCodeShareAndProof.exponentiationProof());

				final ReturnCodesMessage message = returnCodesMessageFactory
						.buildLongVoteCastReturnCodesShareExponentiationProofLogMessage(context, exponentiationProof);
				SECURE_LOGGER.info(message);

				final ZpGroupElement voterVoteCastReturnCodeGenerationPublicKey = derivePublicKey(zpSubgroup,
						voterVoteCastReturnCodeGenerationSecretKey);
				incrementConfirmationAttempts(data);
				sendResponse(data, longVoteCastReturnCodeShareAndProof, null, voterVoteCastReturnCodeGenerationPublicKey);
			}
		} catch (GeneralCryptoLibException | KeyManagementException | MessagingException | PayloadSignatureException e) {
			LOGGER.error("Failed to handle Return Codes Exponentiation request.", e);
		}
	}

	private ElGamalPrivateKey getCcrjReturnCodesGenerationSecretKey(final ReturnCodeComputationDTO<ReturnCodesInput> data)
			throws KeyManagementException {
		final String electionEventId = data.getElectionEventId();
		final String verificationCardSetId = data.getVerificationCardSetId();
		return returnCodesKeyRepository.getCcrjReturnCodesGenerationSecretKey(electionEventId, verificationCardSetId);
	}

	private Exponent deriveVoterChoiceReturnCodeGenerationSecretKey(final ReturnCodeComputationDTO<ReturnCodesInput> data,
			ElGamalPrivateKey ccrjReturnCodesGenerationSecretKey) throws GeneralCryptoLibException {

		final Exponent ccrjReturnCodesGenerationSecretKeyExponent = ccrjReturnCodesGenerationSecretKey.getKeys().get(0);
		final String seed = data.getVerificationCardId();
		final BigInteger q = ccrjReturnCodesGenerationSecretKey.getGroup().getQ();
		Exponent voterChoiceReturnCodeGenerationSecretKey;

		try {
			voterChoiceReturnCodeGenerationSecretKey = voterReturnCodeGenerationKeyDerivationService
					.deriveVoterReturnCodeGenerationPrivateKey(ccrjReturnCodesGenerationSecretKeyExponent, seed, q);
			LOGGER.info(
					"Voter Choice Return Code Generation secret key successfully derived for electionEventId {},  verificationCardSetId {} and verificationCardId {}",
					data.getElectionEventId(), data.getVerificationCardSetId(), data.getVerificationCardId());
		} catch (GeneralCryptoLibException e) {
			LOGGER.error("The derivation of the private key has failed for electionEventId {},  verificationCardSetId {} and verificationCardId {}",
					data.getElectionEventId(), data.getVerificationCardSetId(), data.getVerificationCardId());
			throw e;
		}

		return voterChoiceReturnCodeGenerationSecretKey;
	}

	private Exponent deriveVoterVoteCastReturnCodeGenerationSecretKey(final ReturnCodeComputationDTO<ReturnCodesInput> data,
			ElGamalPrivateKey ccrjReturnCodesGenerationSecretKey) throws GeneralCryptoLibException {

		final Exponent ccrjReturnCodesGenerationSecretKeyExponent = ccrjReturnCodesGenerationSecretKey.getKeys().get(0);
		final String seed = data.getVerificationCardId() + CONFIRM_STRING_PADDING;
		final BigInteger q = ccrjReturnCodesGenerationSecretKey.getGroup().getQ();
		Exponent voterVoteCastReturnCodeGenerationSecretKey;

		try {
			voterVoteCastReturnCodeGenerationSecretKey = voterReturnCodeGenerationKeyDerivationService
					.deriveVoterReturnCodeGenerationPrivateKey(ccrjReturnCodesGenerationSecretKeyExponent, seed, q);
			LOGGER.info(
					"Voter Vote Cast Return Code Generation secret key successfully derived for electionEventId {}, verificationCardSetId {} and verificationCardId {}",
					data.getElectionEventId(), data.getVerificationCardSetId(), data.getVerificationCardId());
		} catch (GeneralCryptoLibException e) {
			LOGGER.error("The derivation of the private key has failed for electionEventId {}, verificationCardSetId {} and verificationCardId {}",
					data.getElectionEventId(), data.getVerificationCardSetId(), data.getVerificationCardId());
			throw e;
		}

		return voterVoteCastReturnCodeGenerationSecretKey;
	}

	private List<BigInteger> hashAndSquare(final List<BigInteger> partialChoiceReturnCodesOrConfirmationKey, final ZpSubgroup group) {
		// Create GqGroup.
		final BigInteger p = group.getP();
		final BigInteger q = group.getQ();
		final BigInteger g = group.getG();
		final GqGroup gqGroup = new GqGroup(p, q, g);

		// Break the homomorphic properties of the partial Choice Return Codes or the confirmation key by hashing and squaring them.
		return partialChoiceReturnCodesOrConfirmationKey.stream().map(code -> GqElement.create(code, gqGroup))
				.map(gqElement -> gqElement.hashAndSquare(hashService)).map(GqElement::getValue).collect(Collectors.toList());
	}

	/**
	 * This method either exponentiates the hashed squared partial Choice Return Codes to the Voter Choice Return Codes Generation secret key or
	 * exponentiates the hashed, squared Confirmation Key to the Voter Vote Cast Return Code Generation secret key. In addition, the method proves
	 * correct exponentiation.
	 */
	private ExponentiatedElementsAndProof<BigInteger> computeLongReturnCodesShareAndProof(final ReturnCodeComputationDTO<ReturnCodesInput> data,
			final ZpSubgroup group, final List<BigInteger> hashedSquaredPccOrCk, final Exponent voterReturnCodesGenerationSecretKey)
			throws GeneralCryptoLibException {

		ExponentiatedElementsAndProof<BigInteger> longReturnCodeSharesAndProof;

		try {
			longReturnCodeSharesAndProof = exponentiationService
					.exponentiateCleartexts(hashedSquaredPccOrCk, voterReturnCodesGenerationSecretKey, group);
		} catch (GeneralCryptoLibException e) {
			LOGGER.error(
					"Error while computing the CCR_j long Return Codes Share (either the CCR_j long Choice Return Codes shares or the CCR_j long Vote Cast Return Code share).");
			throw e;
		}

		LOGGER.info(
				"Successfully computed the CCR_j long Return Codes Share (either the CCR_j long Choice Return Codes shares or the CCR_j long Vote Cast Return Code share) for electionEventId {}, verificationCardSetId {} and verificationCardId {}",
				data.getElectionEventId(), data.getVerificationCardSetId(), data.getVerificationCardId());

		return longReturnCodeSharesAndProof;
	}

	private void sendResponse(final ReturnCodeComputationDTO<ReturnCodesInput> data,
			final ExponentiatedElementsAndProof<BigInteger> longReturnCodesShareAndProof,
			final ZpGroupElement voterChoiceReturnCodeGenerationPublicKey, final ZpGroupElement voterVoteCastReturnCodeGenerationPublicKey)
			throws MessagingException, GeneralCryptoLibException, KeyManagementException, PayloadSignatureException {

		final ReturnCodesExponentiationResponsePayload payload = new ReturnCodesExponentiationResponsePayload();

		// Contains either the partial Choice Return Codes or the Confirmation Key.
		final List<BigInteger> partialChoiceReturnCodesOrConfirmationKey = data.getPayload().getReturnCodesInputElements();

		// Maps the partial Choice Return Codes (or the Confirmation Key) to the long Return Codes share
		final Map<BigInteger, BigInteger> pccOrCkToLongReturnCodeShare = new LinkedHashMap<>();

		for (int i = 0; i < partialChoiceReturnCodesOrConfirmationKey.size(); i++) {
			pccOrCkToLongReturnCodeShare
					.put(partialChoiceReturnCodesOrConfirmationKey.get(i), longReturnCodesShareAndProof.exponentiatedElements().get(i));
		}
		payload.setPccOrCkToLongReturnCodeShare(pccOrCkToLongReturnCodeShare);
		payload.setExponentiationProofJson(longReturnCodesShareAndProof.exponentiationProof().toJson());
		payload.setVoterChoiceReturnCodeGenerationPublicKeyJson(voterChoiceReturnCodeGenerationPublicKey != null ?
				Base64.getEncoder().encodeToString(voterChoiceReturnCodeGenerationPublicKey.toJson().getBytes(StandardCharsets.UTF_8)) :
				null);
		payload.setVoterVoteCastReturnCodeGenerationPublicKeyJson(voterVoteCastReturnCodeGenerationPublicKey != null ?
				Base64.getEncoder().encodeToString(voterVoteCastReturnCodeGenerationPublicKey.toJson().getBytes(StandardCharsets.UTF_8)) :
				null);

		final ReturnCodeComputationDTO<ReturnCodesExponentiationResponsePayload> result = new ReturnCodeComputationDTO<>(data.getCorrelationId(),
				data.getRequestId(), data.getElectionEventId(), data.getVerificationCardSetId(), data.getVerificationCardId(), payload);
		try {
			payload.setSignature(payloadSigner.sign(payload, keysManager.getElectionSigningPrivateKey(data.getElectionEventId()),
					keysManager.getElectionSigningCertificateChain(data.getElectionEventId())));
			LOGGER.info(
					"Successfully signed the CCR_j long Return Codes Share (either the CCR_j long Choice Return Codes shares or the CCR_j long Vote Cast Return Code share) and the corresponding exponentiation proof for electionEventId {}, verificationCardSetId {} and verificationCardId {}",
					data.getElectionEventId(), data.getVerificationCardSetId(), data.getVerificationCardId());
		} catch (PayloadSignatureException e) {
			LOGGER.error(
					"Error while signing the CCR_j long Return Codes Share (either the CCR_j long Choice Return Codes shares or the CCR_j long Vote Cast Return Code share) and the corresponding exponentiation proof for electionEventId {}, verificationCardSetId {} and verificationCardId {}",
					data.getElectionEventId(), data.getVerificationCardSetId(), data.getVerificationCardId());
			throw e;
		}

		final Message amqpMessage = MessageSerialisation.getMessage(result);
		rabbitTemplate.send(computationOutputQueue, amqpMessage);

	}

}
