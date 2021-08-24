/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.mixing.service;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;

import ch.post.it.evoting.controlcomponents.commons.payloadsignature.CryptolibPayloadSignatureService;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.HashService;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableBigInteger;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;
import ch.post.it.evoting.domain.election.model.messaging.PayloadSignatureException;
import ch.post.it.evoting.domain.election.model.messaging.PayloadVerificationException;
import ch.post.it.evoting.domain.mixnet.MixnetInitialPayload;
import ch.post.it.evoting.domain.mixnet.MixnetPayload;
import ch.post.it.evoting.domain.mixnet.MixnetShufflePayload;
import ch.post.it.evoting.domain.mixnet.MixnetState;

@Service
public class MixDecryptMessageConsumer {

	private static final Logger LOGGER = LoggerFactory.getLogger(MixDecryptMessageConsumer.class);

	private final CcmjKeyRepository ccmjKeyRepository;
	private final ObjectMapper objectMapper;
	private final MixDecryptOnlineService mixDecryptOnlineService;
	private final CryptolibPayloadSignatureService signatureService;
	private final HashService hashService;
	private final RabbitTemplate rabbitTemplate;

	private final String mixingOutputQueue;
	private final Integer nodeID;

	@Autowired
	public MixDecryptMessageConsumer(final RabbitTemplate rabbitTemplate, final CcmjKeyRepository ccmjKeyRepository,
			final ObjectMapper objectMapper,
			final MixDecryptOnlineService mixDecryptOnlineService, final CryptolibPayloadSignatureService signatureService,
			final HashService hashService,
			@Value("${partialMixingDecryptionResponseQueue}")
			final String mixingOutputQueue,
			@Value("${nodeID}")
			final Integer nodeID) {

		this.rabbitTemplate = rabbitTemplate;
		this.ccmjKeyRepository = ccmjKeyRepository;
		this.objectMapper = objectMapper;
		this.mixDecryptOnlineService = mixDecryptOnlineService;
		this.signatureService = signatureService;
		this.hashService = hashService;

		this.mixingOutputQueue = mixingOutputQueue;
		this.nodeID = nodeID;
	}

	/**
	 * Creates an ElGamal key pair from the provided private key.
	 *
	 * @param privateKey the private key of the key pair
	 * @return a key pair
	 */
	private static ElGamalMultiRecipientKeyPair getCCMElectionKeyPair(ElGamalPrivateKey privateKey) {
		ZpSubgroup decryptionKeyGroup = privateKey.getGroup();
		BigInteger p = decryptionKeyGroup.getP();
		BigInteger q = decryptionKeyGroup.getQ();
		BigInteger g = decryptionKeyGroup.getG();
		GqGroup decryptionGroup = new GqGroup(p, q, g);
		List<Exponent> keys = privateKey.getKeys();
		List<ZqElement> ccmPrivateKeyElements = keys.stream().map(Exponent::getValue)
				.map(bi -> ZqElement.create(bi, ZqGroup.sameOrderAs(decryptionGroup))).collect(Collectors.toList());
		ElGamalMultiRecipientPrivateKey ccmElectionPrivateKey = new ElGamalMultiRecipientPrivateKey(ccmPrivateKeyElements);
		return ElGamalMultiRecipientKeyPair.from(ccmElectionPrivateKey, decryptionGroup.getGenerator());
	}

	private static boolean containsNullFields(final MixnetPayload mixnetPayload) {
		if (mixnetPayload instanceof MixnetInitialPayload) {
			final MixnetInitialPayload mixnetInitialPayload = (MixnetInitialPayload) mixnetPayload;
			return mixnetInitialPayload.getEncryptionGroup() == null || mixnetInitialPayload.getEncryptedVotes() == null
					|| mixnetInitialPayload.getElectionPublicKey() == null;
		} else {
			final MixnetShufflePayload mixnetShufflePayload = (MixnetShufflePayload) mixnetPayload;
			return mixnetShufflePayload.getEncryptionGroup() == null || mixnetShufflePayload.getVerifiableDecryptions() == null
					|| mixnetShufflePayload.getEncryptedVotes() == null || mixnetShufflePayload.getNodeElectionPublicKey() == null
					|| mixnetShufflePayload.getPreviousRemainingElectionPublicKey() == null
					|| mixnetShufflePayload.getRemainingElectionPublicKey() == null;
		}
	}

	@RabbitListener(queues = "${partialMixingDecryptionRequestQueue}", autoStartup = "false")
	public void onMessage(Message message) throws IOException, KeyManagementException, PayloadSignatureException {
		byte[] messageBody = message.getBody();
		byte[] mixnetStateBytes = new byte[messageBody.length - 1];
		System.arraycopy(messageBody, 1, mixnetStateBytes, 0, messageBody.length - 1);

		MixnetState mixnetState = objectMapper.readValue(mixnetStateBytes, MixnetState.class);

		List<String> validationErrors = validateData(mixnetState);
		if (!validationErrors.isEmpty()) {
			sendWithError(mixnetState, "The following fields present validation errors: " + validationErrors);
			return;
		}
		final List<ElGamalMultiRecipientCiphertext> ciphertexts = mixnetState.getPayload().getEncryptedVotes();
		final ElGamalMultiRecipientPublicKey remainingPublicKey = mixnetState.getPayload().getRemainingElectionPublicKey();
		String ballotBoxId = mixnetState.getBallotBoxDetails().getBallotBoxId();
		String electionEventId = mixnetState.getBallotBoxDetails().getElectionEventId();
		LOGGER.info("Received ballot box {} for election event {} from {} for mixing and decrypting", ballotBoxId, electionEventId, message);

		ElGamalPrivateKey decryptionKey = ccmjKeyRepository.getCcmjElectionSecretKey(electionEventId);
		ElGamalMultiRecipientKeyPair ccmElectionKeyPair = getCCMElectionKeyPair(decryptionKey);

		LOGGER.info("Mixing and decryption of {} from control component node {} finished, sending results to {}", ballotBoxId,
				mixnetState.getNodeToVisit(), mixingOutputQueue);

		MixDecryptOutput mixDecryptOutput;
		try {
			mixDecryptOutput = mixDecryptOnlineService
					.mixDecOnline(electionEventId, ballotBoxId, ciphertexts, remainingPublicKey, ccmElectionKeyPair);
		} catch (IllegalArgumentException e) {
			sendWithError(mixnetState, "Incompatible input arguments: " + e.getMessage());
			return;
		} catch (NullPointerException e) {
			sendWithError(mixnetState, "The payload contains null objects.");
			return;
		}

		LOGGER.info("Mixing and decryption of {} from control component node {} finished, preparing shuffle payload.", ballotBoxId,
				mixnetState.getNodeToVisit());

		// Pack result back into a MixnetState
		MixnetShufflePayload payload = new MixnetShufflePayload(ccmElectionKeyPair.getGroup(),
				mixDecryptOutput.getVerifiableDecryptions(),
				mixDecryptOutput.getVerifiableShuffle(),
				mixDecryptOutput.getRemainingElectionPublicKey(),
				remainingPublicKey,
				ccmElectionKeyPair.getPublicKey(),
				mixnetState.getNodeToVisit());

		LOGGER.info("Signing mixnet payload...");

		final byte[] payloadHash = hashPayload(payload);
		final PrivateKey ccnSigningKey = ccmjKeyRepository.getSigningKey(electionEventId);
		final X509Certificate[] ccnCertificateChain = ccmjKeyRepository.getVerificationCertificateChain(electionEventId);
		final CryptolibPayloadSignature signature = signatureService.sign(payloadHash, ccnSigningKey, ccnCertificateChain);

		payload.setSignature(signature);

		LOGGER.info("Payload signed, sending mixnet state to queue {}", mixingOutputQueue);

		mixnetState = new MixnetState(mixnetState.getBallotBoxDetails(), mixnetState.getNodeToVisit(), payload, mixnetState.getRetryCount(), null);
		send(mixnetState);
	}

	/**
	 * Checks that the fields of a given MixnetState object are set correctly.
	 *
	 * @param mixnetState the MixnetState object to be validated.
	 * @return a list of error messages, if there are any
	 */
	private List<String> validateData(final MixnetState mixnetState) {
		List<String> errors = new ArrayList<>();

		final MixnetPayload payload = mixnetState.getPayload();

		if (mixnetState.getNodeToVisit() != nodeID) {
			String errorMessage = String.format("Node to visit is expected to be %d, but was %d", nodeID, mixnetState.getNodeToVisit());
			errors.add(errorMessage);
			LOGGER.error(errorMessage);
		}
		if (payload == null) {
			String errorMessage = "No payload provided";
			errors.add(errorMessage);
			LOGGER.error(errorMessage);
		} else if (containsNullFields(payload)) {
			String errorMessage = "The payload contains null objects.";
			errors.add(errorMessage);
			LOGGER.error(errorMessage);
		} else {
			LOGGER.info("Verifying signature...");
			final CryptolibPayloadSignature signature = payload.getSignature();
			final byte[] payloadHash = hashPayload(payload);

			try {
				final boolean validSignature = signatureService.verify(signature, ccmjKeyRepository.getPlatformCACertificate(), payloadHash);

				if (!validSignature) {
					String errorMessage = "Invalid signature.";
					errors.add(errorMessage);
					LOGGER.error(errorMessage);
				} else {
					LOGGER.info("The signature is valid.");
				}
			} catch (PayloadVerificationException e) {
				String errorMessage = "Signature verification failed.";
				errors.add(errorMessage);
				LOGGER.error(errorMessage);
			}
		}
		if (mixnetState.getBallotBoxDetails() == null) {
			String errorMessage = "No ballot box details provided";
			errors.add(errorMessage);
			LOGGER.error(errorMessage);
		}

		return errors;
	}

	/**
	 * Hashes the given payload using a recursive hash.
	 *
	 * @param payload the {@link MixnetPayload} to be hashed. Must be non-null.
	 * @return the hash of the payload as a byte array.
	 */
	@VisibleForTesting
	byte[] hashPayload(final MixnetPayload payload) {
		checkNotNull(payload);
		if (payload instanceof MixnetInitialPayload) {
			LOGGER.debug("Hashing MixnetInitialPayload");
			final MixnetInitialPayload initialPayload = (MixnetInitialPayload) payload;
			return hashService.recursiveHash(initialPayload.getEncryptionGroup(), HashableList.from(initialPayload.getEncryptedVotes()),
					initialPayload.getElectionPublicKey());
		} else {
			LOGGER.debug("Hashing MixnetShufflePayload...");
			final MixnetShufflePayload shufflePayload = (MixnetShufflePayload) payload;
			final int numberOfVotes = shufflePayload.getEncryptedVotes().size();
			if (numberOfVotes > 1) {
				LOGGER.debug("with VerifiableShuffle: number of votes = {}", numberOfVotes);
				return hashService.recursiveHash(shufflePayload.getEncryptionGroup(),
						shufflePayload.getVerifiableDecryptions(),
						shufflePayload.getVerifiableShuffle(),
						shufflePayload.getRemainingElectionPublicKey(),
						shufflePayload.getPreviousRemainingElectionPublicKey(),
						shufflePayload.getNodeElectionPublicKey(),
						HashableBigInteger.from(BigInteger.valueOf(shufflePayload.getNodeId())));
			} else {
				LOGGER.debug("without VerifiableShuffle: number of votes = {}", numberOfVotes);
				return hashService.recursiveHash(shufflePayload.getEncryptionGroup(),
						shufflePayload.getVerifiableDecryptions(),
						shufflePayload.getRemainingElectionPublicKey(),
						shufflePayload.getPreviousRemainingElectionPublicKey(),
						shufflePayload.getNodeElectionPublicKey(),
						HashableBigInteger.from(BigInteger.valueOf(shufflePayload.getNodeId())));
			}
		}
	}

	/**
	 * Sends the provided MixnetState object to the node's response queue
	 *
	 * @param mixnetState the MixnetState object to be send
	 */
	private void send(MixnetState mixnetState) {
		try {
			final String outputMixnetStateJson = objectMapper.writeValueAsString(mixnetState);

			byte[] serializedOutputMixnetState = outputMixnetStateJson.getBytes(StandardCharsets.UTF_8);
			byte[] byteContent = new byte[serializedOutputMixnetState.length + 1];
			byteContent[0] = 0;
			System.arraycopy(serializedOutputMixnetState, 0, byteContent, 1, serializedOutputMixnetState.length);

			rabbitTemplate.convertAndSend(mixingOutputQueue, byteContent);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to send the mixing DTO", e);
		}
	}

	/**
	 * Sets the provided MixnetState object's error message and sends it to the node's response queue.
	 *
	 * @param mixnetState  the MixnetState object to be send
	 * @param errorMessage the error message to be set
	 */
	private void sendWithError(MixnetState mixnetState, String errorMessage) {
		mixnetState.setMixnetError(errorMessage);
		send(mixnetState);
	}
}
