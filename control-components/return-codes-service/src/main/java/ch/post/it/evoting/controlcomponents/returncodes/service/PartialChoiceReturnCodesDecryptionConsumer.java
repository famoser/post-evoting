/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.service;

import static ch.post.it.evoting.cryptolib.commons.validations.Validate.validateUUID;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyManagementException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponents.commons.keymanagement.KeysManager;
import ch.post.it.evoting.controlcomponents.returncodes.domain.CombinedCorrectnessInformationExtended;
import ch.post.it.evoting.controlcomponents.returncodes.domain.CombinedCorrectnessInformationExtendedRepository;
import ch.post.it.evoting.controlcomponents.returncodes.domain.ComputedVerificationCard;
import ch.post.it.evoting.controlcomponents.returncodes.domain.ComputedVerificationCardRepository;
import ch.post.it.evoting.controlcomponents.returncodes.domain.ControlComponentContext;
import ch.post.it.evoting.controlcomponents.returncodes.domain.PartialDecryptPccExponentiationProof;
import ch.post.it.evoting.controlcomponents.returncodes.domain.ReturnCodesMessage;
import ch.post.it.evoting.controlcomponents.returncodes.domain.ReturnCodesMessageFactory;
import ch.post.it.evoting.controlcomponents.returncodes.domain.primarykey.CombinedCorrectnessInformationExtendedPrimaryKey;
import ch.post.it.evoting.controlcomponents.returncodes.domain.primarykey.ComputedVerificationCardPrimaryKey;
import ch.post.it.evoting.controlcomponents.returncodes.service.exception.MissingCombinedCorrectnessInformationExtendedException;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.proofs.ProofsServiceAPI;
import ch.post.it.evoting.cryptolib.certificates.utils.CertificateChainValidationException;
import ch.post.it.evoting.cryptolib.certificates.utils.PayloadSigningCertificateValidator;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.WitnessImpl;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Witness;
import ch.post.it.evoting.cryptolib.mathematical.groups.activity.ExponentCompressor;
import ch.post.it.evoting.cryptolib.mathematical.groups.activity.GroupElementsCompressor;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;
import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.CombinedCorrectnessInformation;
import ch.post.it.evoting.domain.election.model.messaging.PayloadSignature;
import ch.post.it.evoting.domain.election.model.messaging.PayloadSignatureException;
import ch.post.it.evoting.domain.election.model.messaging.SafeStreamDeserializationException;
import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.payload.sign.PayloadSigner;
import ch.post.it.evoting.domain.returncodes.ChoiceCodesVerificationDecryptResPayload;
import ch.post.it.evoting.domain.returncodes.PartialChoiceReturnCodesVerificationInput;
import ch.post.it.evoting.domain.returncodes.ReturnCodeComputationDTO;
import ch.post.it.evoting.domain.returncodes.ReturnCodesInput;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectReader;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectReaderImpl;

/**
 * Decrypts the encrypted Partial Choice Return Codes and implements part of the VerifyBallotCCR and PartialDecryptPCC algorithms.
 */
@Service
public class PartialChoiceReturnCodesDecryptionConsumer {

	public static final String SEPARATOR_ENCRYPTED_OPTIONS = ";";
	public static final String TENANT_CERT_NAME = "tenantCA";
	public static final String AB_CERT_NAME = "adminBoard";

	private static final String CCR_J_CHOICE_RETURN_CODES_ENCRYPTION_PRIVATE_KEY_DID_NOT_CONTAIN_ANY_EXPONENT = "CCR_j Choice Return Codes Encryption private key did not contain any exponent.";

	private static final String FAILED_TO_OBTAIN_THE_DECRYPTION_INPUT_PARAMETERS = "Failed to obtain the decryption input parameters.";
	private static final String FAILED_FAILED_TO_GET_THE_CCR_J_CHOICE_RETURN_CODES_ENCRYPTION_PRIVATE_KEY_FROM_THE_KEY_REPOSITORY = "Failed to get the CCR_j Choice Return Codes Encryption private key from the key repository.";

	private static final String FAILED_TO_SIGN_PARTIAL_CHOICE_RETURN_CODES_DECRYPTION_RESULT = "Failed to sign partial Choice Return Codes decryption result.";
	private static final String THE_PARTIAL_CHOICE_RETURN_CODE_IS_NOT_A_MEMBER_OF_THE_GROUP = "the encrypted partial Choice Return Code is not a member of the group.";
	private static final String THE_CIPHER_TEXT_EXPONENTIATION_IS_NOT_A_MEMBER_OF_THE_GROUP = "the cipherTextExponentiation is not a member of the group.";

	private static final String THE_CIPHER_TEXT_IS_NOT_A_MEMBER_OF_THE_GROUP = "the cipherText is not a member of the group.";
	private static final String UNEXPECTED_ERROR_CHECKING_IF_THE_CIPHER_TEXT_EXPONENTIATION_IS_A_MEMBER_OF_THE_GROUP = "Unexpected error checking if the cipherTextExponentiation is a member of the group.";

	private static final String UNEXPECTED_ERROR_CHECKING_IF_THE_CIPHER_TEXT_IS_A_MEMBER_OF_THE_GROUP = "Unexpected error checking if the cipherText is a member of the group.";

	private static final Logger LOGGER = LoggerFactory.getLogger(PartialChoiceReturnCodesDecryptionConsumer.class);
	private static final org.apache.logging.log4j.Logger SECURE_LOGGER = LogManager.getLogger("SecureLog");
	private static final int NUMBER_OF_CIPHERTEXT_ELEMENTS = 2;

	private final ReturnCodesKeyRepository returnCodesKeyRepository;
	private final GroupElementsCompressor<ZpGroupElement> groupElementsCompressor;
	private final ProofsServiceAPI proofsService;
	private final PayloadSigningCertificateValidator certificateValidator;
	private final ComputedVerificationCardRepository computedVerificationCardRepository;
	private final PayloadSigner payloadSigner;
	private final CombinedCorrectnessInformationExtendedRepository combinedCorrectnessInformationExtendedRepository;
	private final VotingClientProofsValidator votingClientProofsValidator;
	private final KeysManager keysManager;
	private final RabbitTemplate rabbitTemplate;

	private final String controlComponentId;
	private final String decryptionOutputQueue;

	private final ReturnCodesMessageFactory returnCodesMessageFactory;

	@Autowired
	public PartialChoiceReturnCodesDecryptionConsumer(final ReturnCodesKeyRepository returnCodesKeyRepository,
			final GroupElementsCompressor<ZpGroupElement> groupElementsCompressor, final ProofsServiceAPI proofsService,
			final PayloadSigningCertificateValidator certificateValidator,
			final ComputedVerificationCardRepository computedVerificationCardRepository, final PayloadSigner payloadSigner,
			final CombinedCorrectnessInformationExtendedRepository combinedCorrectnessInformationExtendedRepository,
			final VotingClientProofsValidator votingClientProofsValidator, final KeysManager keysManager, final RabbitTemplate rabbitTemplate,
			@Value("${keys.nodeId:defCcxId}")
			final String controlComponentId,
			@Value("${verification.decryption.response.queue}")
			final String decryptionOutputQueue, final ReturnCodesMessageFactory returnCodesMessageFactory) {

		this.returnCodesKeyRepository = returnCodesKeyRepository;
		this.groupElementsCompressor = groupElementsCompressor;
		this.proofsService = proofsService;
		this.certificateValidator = certificateValidator;
		this.computedVerificationCardRepository = computedVerificationCardRepository;
		this.payloadSigner = payloadSigner;
		this.combinedCorrectnessInformationExtendedRepository = combinedCorrectnessInformationExtendedRepository;
		this.votingClientProofsValidator = votingClientProofsValidator;
		this.keysManager = keysManager;
		this.rabbitTemplate = rabbitTemplate;

		this.controlComponentId = controlComponentId;
		this.decryptionOutputQueue = decryptionOutputQueue;
		this.returnCodesMessageFactory = returnCodesMessageFactory;
	}

	private static boolean isPartialChoiceReturnCodesComputation(final ReturnCodeComputationDTO<ReturnCodesInput> data) {
		return data.getPayload().getPartialChoiceReturnCodesVerificationInput() != null;
	}

	private static boolean validateGroupMembershipOfElements(final ZpSubgroup mathematicalGroup, final List<BigInteger> elements,
			final String notAGroupMemberLogWarnMessage, final String logMessageInCaseOfException) {
		for (final BigInteger code : elements) {
			try {
				if (!checkGroupMembership(code, mathematicalGroup)) {
					LOGGER.warn("{} {}", ReturnCodesVerificationServiceConstants.UNEXPECTED_SCENARIO_PROCESSING_CC, notAGroupMemberLogWarnMessage);

					return false;
				}
			} catch (GeneralCryptoLibException e) {
				LOGGER.error(logMessageInCaseOfException, e);
				return false;
			}
		}
		return true;
	}

	/**
	 * Validates the code group membership.
	 *
	 * @param code              BigInteger to check the membership
	 * @param mathematicalGroup ZpSubgroup for the group
	 */
	private static boolean checkGroupMembership(final BigInteger code, final ZpSubgroup mathematicalGroup) throws GeneralCryptoLibException {
		final ZpGroupElement groupElement = new ZpGroupElement(code, mathematicalGroup);
		return mathematicalGroup.isGroupMember(groupElement);
	}

	public static String[] getNCiphertextElementsFromEncryptedOptions(final String encryptedOptions, final int n) {
		final String[] ciphers = encryptedOptions.split(SEPARATOR_ENCRYPTED_OPTIONS);
		if (ciphers.length != n) {
			throw new IllegalArgumentException("Unexpected number of elements in encrypted options, found " + ciphers.length + " should be " + n);
		}
		return ciphers;
	}

	@RabbitListener(queues = "${verification.decryption.request.queue}", autoStartup = "false")
	public void onMessage(final Message message) throws SafeStreamDeserializationException {

		final StreamSerializableObjectReader<ReturnCodeComputationDTO<ReturnCodesInput>> reader = new StreamSerializableObjectReaderImpl<>();
		final ReturnCodeComputationDTO<ReturnCodesInput> data = reader.read(message.getBody(), 1, message.getBody().length);

		validateParameters(data);
		if (isValid(data)) {
			decryptMessageAndSendToOutputQueue(data);
		}
	}

	private void validateParameters(final ReturnCodeComputationDTO<ReturnCodesInput> data) {
		final List<String> errors = new ArrayList<>();
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
			throw new IllegalArgumentException("The following fields present validation errors: " + String.join(" | ", errors));
		}
	}

	/**
	 * Checks if the input data is valid.
	 */
	private boolean isValid(final ReturnCodeComputationDTO<ReturnCodesInput> data) {

		final String electionEventId = validateUUID(data.getElectionEventId());
		final String verificationCardSetId = validateUUID(data.getVerificationCardSetId());
		final String verificationCardId = validateUUID(data.getVerificationCardId());

		// Get the mathematicalGroup
		final ZpSubgroup mathematicalGroup;
		try {
			mathematicalGroup = returnCodesKeyRepository.getMathematicalGroup(electionEventId, verificationCardSetId);

			if (mathematicalGroup == null) {
				LOGGER.error("Unexpected scenario - encrypted partial Choice Return Codes' mathematicalGroup is null.");
				return false;
			}

		} catch (KeyManagementException e) {
			LOGGER.error("Unexpected error getting the group for election event id {} and verification card set id {}.", electionEventId,
					verificationCardSetId);
			return false;
		}

		if (!isPartialChoiceReturnCodesComputation(data)) {
			LOGGER.error("Unexpected scenario - Necessary input to decrypt the encrypted partial Choice Return Codes is empty.");
			return false;
		}

		if (data.getPayload().getConfirmationKeyVerificationInput() != null) {
			LOGGER.error("Unexpected scenario - Confirmation Key verification input must be empty.");
			return false;
		}

		// We abort the process if the control component already decrypted the partial Choice Return Codes for this verificationCardId
		if (computedVerificationCardRepository.existsById(new ComputedVerificationCardPrimaryKey(electionEventId, verificationCardId))) {
			LOGGER.error("Verification card has already been computed for electionEventId {} and verificationCardId {}", data.getElectionEventId(),
					data.getVerificationCardId());
			return false;
		}

		LOGGER.info("Verification card is yet to be computed for electionEventId {} and verificationCardId {}", data.getElectionEventId(),
				data.getVerificationCardId());

		if (!checkInputDataConsistency(data, mathematicalGroup)) {
			return false;
		}

		final PartialChoiceReturnCodesVerificationInput partialChoiceReturnCodesVerificationInput = data.getPayload()
				.getPartialChoiceReturnCodesVerificationInput();

		// verify and obtain admin board public key
		final PublicKey adminBoardPublicKey;
		try {
			adminBoardPublicKey = verifyChainAndGetAdminBoardPublicKey(data.getPayload().getCertificates());
		} catch (GeneralCryptoLibException e) {
			LOGGER.error("Could not read certificates from json.");
			return false;
		} catch (CertificateChainValidationException e) {
			LOGGER.error("Invalid certificate chain: {}", String.join(" | ", certificateValidator.getErrors()));
			return false;
		}

		return votingClientProofsValidator.validateVoteAndProofs(mathematicalGroup, partialChoiceReturnCodesVerificationInput, adminBoardPublicKey);
	}

	/**
	 * Checks the consistency and group membership of the ciphertext (the encrypted vote), the exponentiated ciphertext (the encrypted vote
	 * exponentiated to the verification card secret key), and the encrypted partial choice return codes.
	 *
	 * @param data              The voting server's data object, containing the encrypted partial Choice Return Codes and the voting client's vote
	 *                          object.
	 * @param mathematicalGroup The mathematical group against which we want to check the group membership.
	 */
	private boolean checkInputDataConsistency(final ReturnCodeComputationDTO<ReturnCodesInput> data, final ZpSubgroup mathematicalGroup) {

		// Contains the voting client's vote object.
		final Vote vote;
		try {
			vote = ObjectMappers.fromJson(data.getPayload().getPartialChoiceReturnCodesVerificationInput().getVote(), Vote.class);
		} catch (IOException e) {
			LOGGER.error("Unexpected error checking the group membership and the vote consistency: ", e);
			return false;
		}

		final String[] encryptedPartialChoiceReturnCodes = vote.getEncryptedPartialChoiceCodes().split(";");

		// Contains the voting client's list of encrypted partial Choice Return Codes.
		final List<BigInteger> votingClientEncryptedPCC = Stream.of(encryptedPartialChoiceReturnCodes).map(BigInteger::new)
				.collect(Collectors.toList());

		// Contains the voting server's list of encrypted partial Choice Return Codes.
		final List<BigInteger> votingServerEncryptedPCC = data.getPayload().getReturnCodesInputElements();

		if (votingServerEncryptedPCC == null) {
			LOGGER.warn("{}the voting server's list of encrypted partial Choice Return Codes in the payload was null.",
					ReturnCodesVerificationServiceConstants.UNEXPECTED_SCENARIO_PROCESSING_CC);
			return false;
		}

		final CombinedCorrectnessInformation combinedCorrectnessInformation = getCombinedCorrectnessInformation(data.getElectionEventId(),
				data.getVerificationCardSetId());

		final int psi = combinedCorrectnessInformation.getTotalNumberOfSelections();

		// check that the number of voting clients's encrypted partial Choice Return Codes received matches psi + 1.
		if (votingClientEncryptedPCC.size() != psi + 1) {
			LOGGER.error("The size of the voting client's list of encrypted partial Choice Return Codes ({}) does not match the expected size ({}).",
					votingClientEncryptedPCC.size(), psi + 1);
			return false;
		}

		// Validate the group membership of each encrypted partial choice return code (as it is stored in the vote object).
		if (!validateGroupMembershipOfElements(mathematicalGroup, votingClientEncryptedPCC,
				THE_PARTIAL_CHOICE_RETURN_CODE_IS_NOT_A_MEMBER_OF_THE_GROUP,
				ReturnCodesVerificationServiceConstants.UNEXPECTED_ERROR_CHECKING_CC_IS_MEMBER_OF_GROUP)) {
			return false;
		}

		// Validate the group membership of each encrypted partial choice return code (as it is stored in the payload).
		if (!validateGroupMembershipOfElements(mathematicalGroup, votingServerEncryptedPCC,
				THE_PARTIAL_CHOICE_RETURN_CODE_IS_NOT_A_MEMBER_OF_THE_GROUP,
				ReturnCodesVerificationServiceConstants.UNEXPECTED_ERROR_CHECKING_CC_IS_MEMBER_OF_GROUP)) {
			return false;
		}

		/*
		 * Validate the number of elements and group membership of the exponentiated ciphertext (the encrypted vote exponentiated to the verification
		 * card secret key).
		 */
		final String[] cipherTextExponentiations = getNCiphertextElementsFromEncryptedOptions(vote.getCipherTextExponentiations(),
				NUMBER_OF_CIPHERTEXT_ELEMENTS);
		final List<BigInteger> cipherTextExponentiationsElements = Stream.of(cipherTextExponentiations).map(BigInteger::new)
				.collect(Collectors.toList());
		if (!validateGroupMembershipOfElements(mathematicalGroup, cipherTextExponentiationsElements,
				THE_CIPHER_TEXT_EXPONENTIATION_IS_NOT_A_MEMBER_OF_THE_GROUP,
				UNEXPECTED_ERROR_CHECKING_IF_THE_CIPHER_TEXT_EXPONENTIATION_IS_A_MEMBER_OF_THE_GROUP)) {
			return false;
		}

		// Validate the number of elements and group membership of the vote ciphertext (the encrypted product of voting options).
		final String[] cipherTexts = getNCiphertextElementsFromEncryptedOptions(vote.getEncryptedOptions(), NUMBER_OF_CIPHERTEXT_ELEMENTS);
		final List<BigInteger> cipherTextsElements = Stream.of(cipherTexts).map(BigInteger::new).collect(Collectors.toList());
		if (!validateGroupMembershipOfElements(mathematicalGroup, cipherTextsElements, THE_CIPHER_TEXT_IS_NOT_A_MEMBER_OF_THE_GROUP,
				UNEXPECTED_ERROR_CHECKING_IF_THE_CIPHER_TEXT_IS_A_MEMBER_OF_THE_GROUP)) {
			return false;
		}

		return votingServerEncryptedPCC.get(0).equals(votingClientEncryptedPCC.get(0));

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

	private PublicKey verifyChainAndGetAdminBoardPublicKey(final String certificates)
			throws GeneralCryptoLibException, CertificateChainValidationException {
		JsonObject jsonCertificates = ObjectMappers.getJsonObject(certificates);

		// get tenant certificate.
		final String tenantCACert = jsonCertificates.getString(TENANT_CERT_NAME);
		final X509Certificate tenantCA = (X509Certificate) PemUtils.certificateFromPem(tenantCACert);

		// get admin board certificate.
		final String adminBoardCert = jsonCertificates.getString(AB_CERT_NAME);
		final X509Certificate adminBoard = (X509Certificate) PemUtils.certificateFromPem(adminBoardCert);

		final X509Certificate[] chain = { adminBoard, tenantCA };

		if (!certificateValidator.isValid(chain, returnCodesKeyRepository.getPlatformCACertificate())) {
			throw new CertificateChainValidationException("Validation of the chain failed.");
		}

		return adminBoard.getPublicKey();
	}

	private void decryptMessageAndSendToOutputQueue(final ReturnCodeComputationDTO<ReturnCodesInput> data) {
		LOGGER.info("Decrypting the encrypted partial Choice Return Codes.");

		final String electionEventId = data.getElectionEventId();
		final String verificationCardSetId = data.getVerificationCardSetId();
		final String verificationCardId = data.getVerificationCardId();

		// This is the gamma element of the encrypted partial choice return codes.
		final BigInteger gammaEncryptedPartialChoiceReturnCodesBigInteger = data.getPayload().getReturnCodesInputElements().get(0);

		final ElGamalPrivateKey ccrjChoiceReturnCodesEncryptionPrivateKey;
		final ZpGroupElement gammaEncryptedPartialChoiceReturnCodes;
		try {
			ccrjChoiceReturnCodesEncryptionPrivateKey = returnCodesKeyRepository
					.getCcrjChoiceReturnCodesEncryptionSecretKey(electionEventId, verificationCardSetId);

			gammaEncryptedPartialChoiceReturnCodes = new ZpGroupElement(gammaEncryptedPartialChoiceReturnCodesBigInteger,
					ccrjChoiceReturnCodesEncryptionPrivateKey.getGroup());
		} catch (GeneralCryptoLibException e) {
			LOGGER.error(FAILED_TO_OBTAIN_THE_DECRYPTION_INPUT_PARAMETERS, e);
			return;
		} catch (KeyManagementException e) {
			LOGGER.error(FAILED_FAILED_TO_GET_THE_CCR_J_CHOICE_RETURN_CODES_ENCRYPTION_PRIVATE_KEY_FROM_THE_KEY_REPOSITORY, e);
			return;
		}

		final List<Exponent> ccrjChoiceReturnCodesEncryptionPrivateKeyElements = ccrjChoiceReturnCodesEncryptionPrivateKey.getKeys();
		if (ccrjChoiceReturnCodesEncryptionPrivateKeyElements.isEmpty()) {
			LOGGER.error(CCR_J_CHOICE_RETURN_CODES_ENCRYPTION_PRIVATE_KEY_DID_NOT_CONTAIN_ANY_EXPONENT);
			throw new IllegalStateException(CCR_J_CHOICE_RETURN_CODES_ENCRYPTION_PRIVATE_KEY_DID_NOT_CONTAIN_ANY_EXPONENT);
		}

		// gamma element exponentiated to the CCR_j Choice Return Codes Encryption private key elements.
		final List<ZpGroupElement> gammaExponentiatedToPrivateKeyElements = new ArrayList<>(ccrjChoiceReturnCodesEncryptionPrivateKeyElements.size());

		// gamma element exponentiated to the CCR_j Choice Return Codes Encryption private key elements as a JSON element.
		final List<String> gammaExponentiatedToPrivateKeyElementsJson = new ArrayList<>(ccrjChoiceReturnCodesEncryptionPrivateKeyElements.size());

		try {
			for (final Exponent exponent : ccrjChoiceReturnCodesEncryptionPrivateKeyElements) {
				final ZpGroupElement exponentiated = gammaEncryptedPartialChoiceReturnCodes.exponentiate(exponent);
				gammaExponentiatedToPrivateKeyElements.add(exponentiated);
				gammaExponentiatedToPrivateKeyElementsJson.add(exponentiated.toJson());
			}
		} catch (GeneralCryptoLibException e) {
			LOGGER.error(FAILED_TO_OBTAIN_THE_DECRYPTION_INPUT_PARAMETERS, e);
			return;
		}

		LOGGER.info(
				"Partially decrypted the encrypted partial Choice Return Codes using the CCR_j Choice Return Codes encryption secret key for election event ID {}, verification card set ID {} and verification card ID{}",
				electionEventId, verificationCardSetId, verificationCardId);

		final ElGamalPublicKey ccrjChoiceReturnCodesEncryptionPublicKey;
		final Proof exponentiationProof;
		try {
			ccrjChoiceReturnCodesEncryptionPublicKey = returnCodesKeyRepository
					.getCcrjChoiceReturnCodesEncryptionPublicKey(electionEventId, verificationCardSetId);

			exponentiationProof = calculateExponentiationProof(ccrjChoiceReturnCodesEncryptionPrivateKey, ccrjChoiceReturnCodesEncryptionPublicKey,
					gammaEncryptedPartialChoiceReturnCodes, gammaExponentiatedToPrivateKeyElements);
			logPartialDecryptPccExponentiationProofSuccessfullyComputed(data, gammaEncryptedPartialChoiceReturnCodes,
					gammaExponentiatedToPrivateKeyElements, exponentiationProof);

		} catch (GeneralCryptoLibException e) {
			LOGGER.error("Error while generating proof of knowledge of the CCR_j Choice Return Codes secret key.");
			LOGGER.error(FAILED_TO_OBTAIN_THE_DECRYPTION_INPUT_PARAMETERS, e);
			return;
		} catch (KeyManagementException e) {
			LOGGER.error(FAILED_FAILED_TO_GET_THE_CCR_J_CHOICE_RETURN_CODES_ENCRYPTION_PRIVATE_KEY_FROM_THE_KEY_REPOSITORY, e);
			return;
		}

		// Add the verification card id to the list of partial decrypted PCC
		computedVerificationCardRepository.save(new ComputedVerificationCard(electionEventId, verificationCardId));

		final ChoiceCodesVerificationDecryptResPayload resultPayload = new ChoiceCodesVerificationDecryptResPayload();

		resultPayload.setDecryptContributionResult(gammaExponentiatedToPrivateKeyElementsJson);
		try {
			resultPayload.setExponentiationProofJson(exponentiationProof.toJson());
			resultPayload.setPublicKeyJson(ccrjChoiceReturnCodesEncryptionPublicKey.toJson());
		} catch (GeneralCryptoLibException e) {
			LOGGER.error(FAILED_TO_OBTAIN_THE_DECRYPTION_INPUT_PARAMETERS, e);
			return;
		}

		try {
			final PayloadSignature payloadSignature = payloadSigner.sign(resultPayload, keysManager.getElectionSigningPrivateKey(electionEventId),
					keysManager.getElectionSigningCertificateChain(electionEventId));
			resultPayload.setSignature(payloadSignature);
			LOGGER.info("Partially decrypted partial Choice Return Codes and corresponding proofs correctly signed.");
		} catch (KeyManagementException e) {
			LOGGER.error(FAILED_FAILED_TO_GET_THE_CCR_J_CHOICE_RETURN_CODES_ENCRYPTION_PRIVATE_KEY_FROM_THE_KEY_REPOSITORY, e);
			return;
		} catch (PayloadSignatureException e) {
			LOGGER.error(FAILED_TO_SIGN_PARTIAL_CHOICE_RETURN_CODES_DECRYPTION_RESULT, e);
			return;
		}

		final UUID correlationId = data.getCorrelationId();
		final String requestId = data.getRequestId();

		final ReturnCodeComputationDTO<ChoiceCodesVerificationDecryptResPayload> returnCodeComputationDTO = new ReturnCodeComputationDTO<>(
				correlationId, requestId, electionEventId, verificationCardSetId, verificationCardId, resultPayload);

		final Message amqpMessage = MessageSerialisation.getMessage(returnCodeComputationDTO);

		rabbitTemplate.send(decryptionOutputQueue, amqpMessage);
	}

	/**
	 * Calculates a Zero-Knowledge proof that the product of the CCR_j Choice Return Codes Encryption Public Keys and the product of the exponentiated
	 * gamma elements correspond to the exponentiation of the generator g and the gamma element to the sum of the CCR_j Choice Return Codes Encryption
	 * Private Key elements.
	 *
	 * @param ccrjChoiceReturnCodesEncryptionPrivateKey Private key of the CCR_j Choice Return Codes Encryption key pair
	 * @param ccrjChoiceReturnCodesEncryptionPublicKey  Public key of the CCR_j Choice Return Codes Encryption key pair
	 * @param gammaEncryptedPartialChoiceReturnCodes    base exponentiated value
	 * @param gammaExponentiatedToPrivateKeyElements    result of exponentiating the base value to the private key elements
	 * @return the calculated exponentiation proof
	 * @throws GeneralCryptoLibException if there is an error during the cryptographic operations
	 */
	private Proof calculateExponentiationProof(final ElGamalPrivateKey ccrjChoiceReturnCodesEncryptionPrivateKey,
			final ElGamalPublicKey ccrjChoiceReturnCodesEncryptionPublicKey, final ZpGroupElement gammaEncryptedPartialChoiceReturnCodes,
			final List<ZpGroupElement> gammaExponentiatedToPrivateKeyElements) throws GeneralCryptoLibException {

		final ZpGroupElement compressedExponentiatedGammas = groupElementsCompressor.compress(gammaExponentiatedToPrivateKeyElements);

		final List<ZpGroupElement> publicKeyElements = ccrjChoiceReturnCodesEncryptionPublicKey.getKeys();

		final ZpGroupElement compressedPublicKeyElements = groupElementsCompressor.compress(publicKeyElements);

		final ZpSubgroup privateKeyGroup = ccrjChoiceReturnCodesEncryptionPrivateKey.getGroup();

		final ExponentCompressor<ZpSubgroup> exponentCompressor = new ExponentCompressor<>(privateKeyGroup);

		final List<Exponent> privateKeyElements = ccrjChoiceReturnCodesEncryptionPrivateKey.getKeys();

		final Exponent compressedPrivateKey = exponentCompressor.compress(privateKeyElements);

		final List<ZpGroupElement> exponentiatedElements = Arrays.asList(compressedPublicKeyElements, compressedExponentiatedGammas);
		final List<ZpGroupElement> baseElements = Arrays.asList(privateKeyGroup.getGenerator(), gammaEncryptedPartialChoiceReturnCodes);
		final Witness witness = new WitnessImpl(compressedPrivateKey);

		return proofsService.createProofProverAPI(privateKeyGroup).createExponentiationProof(exponentiatedElements, baseElements, witness);

	}

	private void logPartialDecryptPccExponentiationProofSuccessfullyComputed(final ReturnCodeComputationDTO<ReturnCodesInput> data,
			final ZpGroupElement gammaEncryptedPartialChoiceReturnCodes, final List<ZpGroupElement> gammaExponentiatedToPrivateKeyElements,
			final Proof exponentiationProof) {

		ControlComponentContext context = new ControlComponentContext(data.getElectionEventId(), data.getVerificationCardSetId(), controlComponentId);

		PartialDecryptPccExponentiationProof partialDecryptPccExponentiationProof = new PartialDecryptPccExponentiationProof(
				data.getVerificationCardId(), gammaEncryptedPartialChoiceReturnCodes.getValue(), gammaExponentiatedToPrivateKeyElements,
				exponentiationProof);

		final ReturnCodesMessage message = returnCodesMessageFactory
				.buildPartialDecryptPccExponentiationProofLogMessage(context, partialDecryptPccExponentiationProof);

		SECURE_LOGGER.info(message);
	}

}
