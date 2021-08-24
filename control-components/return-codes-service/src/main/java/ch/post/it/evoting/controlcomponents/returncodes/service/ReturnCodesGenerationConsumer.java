/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponents.commons.keymanagement.KeysManager;
import ch.post.it.evoting.controlcomponents.commons.payloadsignature.CryptolibPayloadSignatureService;
import ch.post.it.evoting.controlcomponents.returncodes.domain.CombinedCorrectnessInformationExtended;
import ch.post.it.evoting.controlcomponents.returncodes.domain.CombinedCorrectnessInformationExtendedRepository;
import ch.post.it.evoting.controlcomponents.returncodes.domain.ControlComponentContext;
import ch.post.it.evoting.controlcomponents.returncodes.domain.ReturnCodesMessage;
import ch.post.it.evoting.controlcomponents.returncodes.domain.ReturnCodesMessageFactory;
import ch.post.it.evoting.controlcomponents.returncodes.domain.VerificationCardPublicKeyExtended;
import ch.post.it.evoting.controlcomponents.returncodes.domain.VerificationCardPublicKeyExtendedRepository;
import ch.post.it.evoting.controlcomponents.returncodes.domain.primarykey.CombinedCorrectnessInformationExtendedPrimaryKey;
import ch.post.it.evoting.controlcomponents.returncodes.service.exception.MissingSignatureException;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.HashService;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.domain.cryptoadapters.CryptoAdapters;
import ch.post.it.evoting.domain.election.CombinedCorrectnessInformation;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;
import ch.post.it.evoting.domain.election.model.messaging.InvalidSignatureException;
import ch.post.it.evoting.domain.election.model.messaging.PayloadSignatureException;
import ch.post.it.evoting.domain.election.model.messaging.PayloadVerificationException;
import ch.post.it.evoting.domain.returncodes.ChoiceCodeGenerationDTO;
import ch.post.it.evoting.domain.returncodes.ReturnCodeGenerationInput;
import ch.post.it.evoting.domain.returncodes.ReturnCodeGenerationOutput;
import ch.post.it.evoting.domain.returncodes.ReturnCodeGenerationRequestPayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodeGenerationResponsePayload;

/**
 * Implements the GenEncLongCodeShares algorithm described in the cryptographic protocol.
 */
@Service
public class ReturnCodesGenerationConsumer {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReturnCodesGenerationConsumer.class);
	private static final org.apache.logging.log4j.Logger SECURE_LOGGER = LogManager.getLogger("SecureLog");
	private static final String CONFIRM_STRING_PADDING = "confirm";

	private final RabbitTemplate rabbitTemplate;
	private final VoterReturnCodeGenerationKeyDerivationService voterReturnCodeGenerationKeyDerivationService;
	private final ReturnCodesKeyRepository returnCodesKeyRepository;
	private final KeysManager keysManager;
	private final CombinedCorrectnessInformationExtendedRepository combinedCorrectnessInformationExtendedRepository;
	private final CryptolibPayloadSignatureService payloadSignatureService;
	private final HashService hashService;
	private final ZeroKnowledgeProof zeroKnowledgeProofService;
	private final ObjectMapper objectMapper;
	private final VerificationCardPublicKeyExtendedRepository verificationCardPublicKeyExtendedRepository;

	private final String computationOutputQueue;
	private final String controlComponentId;
	private final int nodeID;
	private final ReturnCodesMessageFactory returnCodesMessageFactory;
	private String electionEventId;
	private String verificationCardSetId;
	private GqGroup gqGroup;

	@Autowired
	public ReturnCodesGenerationConsumer(final RabbitTemplate rabbitTemplate,
			final VoterReturnCodeGenerationKeyDerivationService voterReturnCodeGenerationKeyDerivationService,
			final ReturnCodesKeyRepository returnCodesKeyRepository, final KeysManager keysManager,
			final CombinedCorrectnessInformationExtendedRepository combinedCorrectnessInformationExtendedRepository,
			final CryptolibPayloadSignatureService payloadSignatureService, final HashService hashService, final ObjectMapper objectMapper,
			final ZeroKnowledgeProof zeroKnowledgeProofService,
			final VerificationCardPublicKeyExtendedRepository verificationCardPublicKeyExtendedRepository,
			@Value("${generation.computation.response.queue}")
			final String computationOutputQueue,
			@Value("${keys.nodeId:defCcxId}")
			final String controlComponentId,
			@Value("${nodeID}")
			final Integer nodeID, final ReturnCodesMessageFactory returnCodesMessageFactory) {

		this.rabbitTemplate = rabbitTemplate;
		this.voterReturnCodeGenerationKeyDerivationService = voterReturnCodeGenerationKeyDerivationService;
		this.returnCodesKeyRepository = returnCodesKeyRepository;
		this.keysManager = keysManager;
		this.combinedCorrectnessInformationExtendedRepository = combinedCorrectnessInformationExtendedRepository;
		this.payloadSignatureService = payloadSignatureService;
		this.hashService = hashService;
		this.objectMapper = objectMapper;
		this.zeroKnowledgeProofService = zeroKnowledgeProofService;
		this.verificationCardPublicKeyExtendedRepository = verificationCardPublicKeyExtendedRepository;

		this.computationOutputQueue = computationOutputQueue;
		this.controlComponentId = controlComponentId;
		this.nodeID = nodeID;
		this.returnCodesMessageFactory = returnCodesMessageFactory;
	}

	@RabbitListener(queues = "${generation.computation.request.queue}", autoStartup = "false")
	public void onMessage(final Message message) throws IOException {
		final byte[] messageBody = message.getBody();
		final byte[] dtoBytes = new byte[messageBody.length - 1];
		System.arraycopy(messageBody, 1, dtoBytes, 0, messageBody.length - 1);

		final ChoiceCodeGenerationDTO<ReturnCodeGenerationRequestPayload> choiceCodeGenerationDTO = objectMapper
				.readValue(dtoBytes, new TypeReference<ChoiceCodeGenerationDTO<ReturnCodeGenerationRequestPayload>>() {
				});

		final ReturnCodeGenerationRequestPayload payload = choiceCodeGenerationDTO.getPayload();

		try {
			validateSignature(payload);

			final ElGamalPrivateKey ccrjReturnCodesGenerationSecretKey = getCcrjReturnCodesGenerationSecretKey(payload.getElectionEventId(),
					payload.getVerificationCardSetId());
			final ZpSubgroup group = ccrjReturnCodesGenerationSecretKey.getGroup();
			gqGroup = new GqGroup(group.getP(), group.getQ(), group.getG());
			electionEventId = payload.getElectionEventId();
			verificationCardSetId = payload.getVerificationCardSetId();

			final List<ReturnCodeGenerationOutput> returnCodeGenerationOutputs = genEncLongCodeShares(ccrjReturnCodesGenerationSecretKey,
					payload.getReturnCodeGenerationInputs());

			// When chunking occurs, we save the combined correctness information only once, since it is identical for all chunks of a given (eeid, vcsid) tuple.
			if (!combinedCorrectnessInformationExtendedRepository
					.existsById(new CombinedCorrectnessInformationExtendedPrimaryKey(electionEventId, verificationCardSetId))) {

				final CombinedCorrectnessInformation combinedCorrectnessInformation = payload.getCombinedCorrectnessInformation();
				combinedCorrectnessInformationExtendedRepository
						.save(new CombinedCorrectnessInformationExtended(electionEventId, verificationCardSetId, combinedCorrectnessInformation));
			}

			sendResponse(choiceCodeGenerationDTO, returnCodeGenerationOutputs);
		} catch (GeneralCryptoLibException | KeyManagementException | MessagingException e) {
			LOGGER.error("Failed to handle the return code generation request.", e);
		} catch (PayloadSignatureException e) {
			LOGGER.error("Failed to sign the return code generation response.", e);
		} catch (MissingSignatureException e) {
			LOGGER.error("Failed to find a signature in the received request.", e);
		} catch (InvalidSignatureException e) {
			LOGGER.error("The signature of the received request is invalid.", e);
		} catch (PayloadVerificationException e) {
			LOGGER.error("Exception while trying to verify the signature of the received request.", e);
		}
	}

	/**
	 * Generates the encrypted CCR_j long return code shares.
	 *
	 * @param ccrjReturnCodesGenerationSecretKey k'<sub>j</sub>, the return codes generation secret key. Must be non null.
	 * @param returnCodeGenerationInputs         the list of inputs. Each input contains the verification card id, the encrypted hashed partial choice
	 *                                           return codes and the encrypted hashed confirmation key of a specific voter. The list is of size
	 *                                           N<sub>E</sub>. Must be non null and non empty.
	 * @return A list of outputs, where each output corresponds to one verification card.
	 * @throws GeneralCryptoLibException if a problem occurred while deriving the private keys.
	 * @throws NullPointerException      if any of the inputs is null.
	 * @throws IllegalArgumentException  if {@code returnCodeGenerationInputs} is empty.
	 */
	@SuppressWarnings("java:S117")
	private List<ReturnCodeGenerationOutput> genEncLongCodeShares(final ElGamalPrivateKey ccrjReturnCodesGenerationSecretKey,
			final List<ReturnCodeGenerationInput> returnCodeGenerationInputs) throws GeneralCryptoLibException {

		checkNotNull(ccrjReturnCodesGenerationSecretKey);
		checkNotNull(returnCodeGenerationInputs);
		checkArgument(!returnCodeGenerationInputs.isEmpty(), "The list of inputs must not be empty.");

		final String ee = electionEventId;
		final GqElement g = gqGroup.getGenerator();
		final List<ReturnCodeGenerationOutput> returnCodeGenerationOutputs = new ArrayList<>();

		// Algorithm.
		for (final ReturnCodeGenerationInput returnCodeGenerationInput : returnCodeGenerationInputs) {
			final String vc_id = returnCodeGenerationInput.getVerificationCardId();

			final ZqElement k_j_id = deriveVoterChoiceReturnCodeGenerationSecretKey(returnCodeGenerationInput, ccrjReturnCodesGenerationSecretKey,
					vc_id);
			final GqElement K_j_id = g.exponentiate(k_j_id);
			final ZqElement kc_j_id = deriveVoterVoteCastReturnCodeGenerationSecretKey(returnCodeGenerationInput, ccrjReturnCodesGenerationSecretKey,
					vc_id);
			final GqElement Kc_j_id = g.exponentiate(kc_j_id);

			// Compute c_expPCC_j_id.
			final ElGamalMultiRecipientCiphertext c_pCC_id = returnCodeGenerationInput.getEncryptedHashedSquaredPartialChoiceReturnCodes();
			final ElGamalMultiRecipientCiphertext c_expPCC_j_id = c_pCC_id.exponentiate(k_j_id);

			final List<String> i_aux = Arrays.asList(ee, vc_id, "GenEncLongCodeShares", String.valueOf(nodeID));

			// Compute pi_expPCC_j_id.
			final GroupVector<GqElement, GqGroup> basesPCC = c_pCC_id.getPhi().prepend(c_pCC_id.getGamma()).prepend(g);
			final GroupVector<GqElement, GqGroup> exponentiationsPCC = c_expPCC_j_id.getPhi().prepend(c_expPCC_j_id.getGamma()).prepend(K_j_id);
			final ExponentiationProof pi_expPCC_j_id = zeroKnowledgeProofService.genExponentiationProof(basesPCC, k_j_id, exponentiationsPCC, i_aux);

			// Compute c_expCK_j_id.
			final ElGamalMultiRecipientCiphertext c_ck_id = returnCodeGenerationInput.getEncryptedHashedSquaredConfirmationKey();
			final ElGamalMultiRecipientCiphertext c_expCK_j_id = c_ck_id.exponentiate(kc_j_id);

			// Compute pi_expCK_j_id.
			final GroupVector<GqElement, GqGroup> basesCK = c_ck_id.getPhi().prepend(c_ck_id.getGamma()).prepend(g);
			final GroupVector<GqElement, GqGroup> exponentiationsCK = c_expCK_j_id.getPhi().prepend(c_expCK_j_id.getGamma()).prepend(Kc_j_id);
			final ExponentiationProof pi_expCK_j_id = zeroKnowledgeProofService.genExponentiationProof(basesCK, kc_j_id, exponentiationsCK, i_aux);

			// Output.
			final ReturnCodeGenerationOutput returnCodeGenerationOutput = new ReturnCodeGenerationOutput(vc_id,
					new ElGamalMultiRecipientPublicKey(Collections.singletonList(K_j_id)),
					new ElGamalMultiRecipientPublicKey(Collections.singletonList(Kc_j_id)), c_expPCC_j_id, pi_expPCC_j_id, c_expCK_j_id,
					pi_expCK_j_id);
			returnCodeGenerationOutputs.add(returnCodeGenerationOutput);

			// Secure log.
			logEncryptedConfirmationKeySuccessfullyExponentiated(verificationCardSetId, vc_id);
			logEncryptedPartialChoiceCodesSuccessfullyExponentiated(verificationCardSetId, vc_id);

			verificationCardPublicKeyExtendedRepository.save(new VerificationCardPublicKeyExtended(ee, vc_id, verificationCardSetId,
					returnCodeGenerationInput.getVerificationCardPublicKey()));

		}

		return returnCodeGenerationOutputs;
	}

	private void validateSignature(final ReturnCodeGenerationRequestPayload payload)
			throws MissingSignatureException, InvalidSignatureException, PayloadVerificationException {

		final String payloadId = String.format("[electionEventId:%s, verificationCardSetId:%s, chunkID:%s]", payload.getElectionEventId(),
				payload.getVerificationCardSetId(), payload.getChunkId());

		LOGGER.info("Checking the signature of payload {}...", payloadId);

		if (payload.getSignature() == null) {
			LOGGER.warn("REJECTED payload {} because it is not signed", payloadId);
			throw new MissingSignatureException(payloadId);
		}

		final byte[] payloadHash = hashService.recursiveHash(payload);
		final boolean isPayloadSignatureValid = payloadSignatureService
				.verify(payload.getSignature(), returnCodesKeyRepository.getPlatformCACertificate(), payloadHash);

		if (!isPayloadSignatureValid) {
			LOGGER.warn("REJECTED payload {} because the signature is not valid.", payloadId);
			throw new InvalidSignatureException(controlComponentId, payloadId);
		}

		LOGGER.info("Signature of payload {} accepted for generation.", payloadId);
	}

	private ElGamalPrivateKey getCcrjReturnCodesGenerationSecretKey(final String electionEventId, final String verificationCardSetId)
			throws KeyManagementException {

		return returnCodesKeyRepository.getCcrjReturnCodesGenerationSecretKey(electionEventId, verificationCardSetId);
	}

	private ZqElement deriveVoterChoiceReturnCodeGenerationSecretKey(final ReturnCodeGenerationInput generationInput,
			final ElGamalPrivateKey ccrjReturnCodeGenerationSecretKey, final String processingId) throws GeneralCryptoLibException {

		final Exponent exponent = ccrjReturnCodeGenerationSecretKey.getKeys().get(0);
		final BigInteger q = ccrjReturnCodeGenerationSecretKey.getGroup().getQ();
		final Exponent voterChoiceReturnCodeGenerationSecretKey;

		try {

			final String seed = generationInput.getVerificationCardId();
			voterChoiceReturnCodeGenerationSecretKey = voterReturnCodeGenerationKeyDerivationService
					.deriveVoterReturnCodeGenerationPrivateKey(exponent, seed, q);

			LOGGER.debug(
					"Successfully derived the Voter Choice Return Code Generation secret key from the CCR_j Return Code Generation secret key for {}.",
					processingId);

		} catch (GeneralCryptoLibException e) {
			LOGGER.error(
					"Could not derive the Voter Choice Return Code Generation secret key from the CCR_j Return Code Generation secret key for {}.",
					processingId);
			throw e;
		}

		return CryptoAdapters.convert(voterChoiceReturnCodeGenerationSecretKey);
	}

	private ZqElement deriveVoterVoteCastReturnCodeGenerationSecretKey(final ReturnCodeGenerationInput generationInput,
			final ElGamalPrivateKey ccrjReturnCodeGenerationSecretKey, final String processingId) throws GeneralCryptoLibException {

		final Exponent exponent = ccrjReturnCodeGenerationSecretKey.getKeys().get(0);
		final BigInteger q = ccrjReturnCodeGenerationSecretKey.getGroup().getQ();
		final Exponent voterVoteCastReturnCodeGenerationSecretKey;

		try {
			final String seed = generationInput.getVerificationCardId() + CONFIRM_STRING_PADDING;
			voterVoteCastReturnCodeGenerationSecretKey = voterReturnCodeGenerationKeyDerivationService
					.deriveVoterReturnCodeGenerationPrivateKey(exponent, seed, q);

			LOGGER.debug(
					"Successfully derived the Voter Vote Cast Return Code Generation secret key from the CCR_j Return Code Generation secret key for {}.",
					processingId);

		} catch (GeneralCryptoLibException e) {
			LOGGER.error(
					"Could not derive the Voter Vote Cast Return Code Generation secret key from the CCR_j Return Code Generation secret key for {}.",
					processingId);

			throw e;
		}

		return CryptoAdapters.convert(voterVoteCastReturnCodeGenerationSecretKey);
	}

	private void logEncryptedConfirmationKeySuccessfullyExponentiated(final String verificationCardSetId, final String verificationCardId) {
		ControlComponentContext context = new ControlComponentContext(electionEventId, verificationCardSetId, controlComponentId);
		ReturnCodesMessage message = returnCodesMessageFactory.buildEncryptedConfirmationKeyExponentiationLogMessage(context, verificationCardId);

		SECURE_LOGGER.info(message);
	}

	private void logEncryptedPartialChoiceCodesSuccessfullyExponentiated(final String verificationCardSetId, final String verificationCardId) {
		ControlComponentContext context = new ControlComponentContext(electionEventId, verificationCardSetId, controlComponentId);
		ReturnCodesMessage message = returnCodesMessageFactory
				.buildEncryptedPartialChoiceReturnCodeExponentiationLogMessage(context, verificationCardId);

		SECURE_LOGGER.info(message);
	}

	private void sendResponse(final ChoiceCodeGenerationDTO<ReturnCodeGenerationRequestPayload> choiceCodeGenerationDTO,
			final List<ReturnCodeGenerationOutput> returnCodeGenerationOutputList) throws KeyManagementException, PayloadSignatureException {

		final int chunkId = choiceCodeGenerationDTO.getPayload().getChunkId();

		final ReturnCodeGenerationResponsePayload resPayload = new ReturnCodeGenerationResponsePayload(
				choiceCodeGenerationDTO.getPayload().getTenantId(), electionEventId, verificationCardSetId, chunkId, gqGroup,
				returnCodeGenerationOutputList, nodeID);

		// Sign response payload.
		final byte[] responsePayloadHash = hashService.recursiveHash(resPayload);
		final CryptolibPayloadSignature payloadSignature = payloadSignatureService
				.sign(responsePayloadHash, keysManager.getElectionSigningPrivateKey(electionEventId),
						keysManager.getElectionSigningCertificateChain(electionEventId));

		resPayload.setSignature(payloadSignature);

		LOGGER.info(
				"Successfully signed the payload after the Return Code generation for electionEventId {}, verificationCardSetId {} and chunkID {}.",
				electionEventId, verificationCardSetId, chunkId);

		final ChoiceCodeGenerationDTO<ReturnCodeGenerationResponsePayload> choiceCodeGenerationDTOResponse = new ChoiceCodeGenerationDTO<>(
				choiceCodeGenerationDTO.getCorrelationId(), choiceCodeGenerationDTO.getRequestId(), resPayload);

		final String responseJson;
		try {
			responseJson = objectMapper.writeValueAsString(choiceCodeGenerationDTOResponse);
		} catch (JsonProcessingException e) {
			throw new UncheckedIOException("Failed to serialize response ChoiceCodeGenerationDTO.", e);
		}
		byte[] serializedResponsePayloadBytes = responseJson.getBytes(StandardCharsets.UTF_8);

		// The MessagingService in the voting-server expects the first byte to be the type.
		byte[] byteContent = new byte[serializedResponsePayloadBytes.length + 1];
		byteContent[0] = 0;
		System.arraycopy(serializedResponsePayloadBytes, 0, byteContent, 1, serializedResponsePayloadBytes.length);

		rabbitTemplate.convertAndSend(computationOutputQueue, byteContent);
	}

}
