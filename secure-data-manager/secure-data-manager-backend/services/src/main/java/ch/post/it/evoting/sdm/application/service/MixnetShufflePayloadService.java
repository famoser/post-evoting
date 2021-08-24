/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static ch.post.it.evoting.domain.Validations.validateUUID;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.hashing.HashService;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableBigInteger;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;
import ch.post.it.evoting.domain.election.model.messaging.PayloadVerificationException;
import ch.post.it.evoting.domain.mixnet.MixnetShufflePayload;
import ch.post.it.evoting.domain.mixnet.exceptions.FailedValidationException;
import ch.post.it.evoting.sdm.commons.CryptolibPayloadSignatureService;
import ch.post.it.evoting.sdm.infrastructure.mixnetpayload.MixnetShufflePayloadFileRepository;

@Service
public class MixnetShufflePayloadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MixnetShufflePayloadService.class);

	private final CryptolibPayloadSignatureService cryptolibPayloadSignatureService;
	private final MixnetShufflePayloadFileRepository mixnetShufflePayloadFileRepository;
	private final HashService hashService;
	private final PlatformRootCAService platformRootCAService;

	@Autowired
	public MixnetShufflePayloadService(final CryptolibPayloadSignatureService cryptolibPayloadSignatureService,
			final MixnetShufflePayloadFileRepository mixnetShufflePayloadFileRepository, final HashService hashService,
			final PlatformRootCAService platformRootCAService) {
		this.cryptolibPayloadSignatureService = cryptolibPayloadSignatureService;
		this.mixnetShufflePayloadFileRepository = mixnetShufflePayloadFileRepository;
		this.hashService = hashService;
		this.platformRootCAService = platformRootCAService;
	}

	/**
	 * Checks if the signature of each of the online payloads related to the given election event id, ballot id and ballot box id is valid.
	 *
	 * @param electionEventId the election event id.
	 * @param ballotId        the ballot id.
	 * @param ballotBoxId     the ballot box id.
	 * @return true if each signature of the online payloads were successfully verified, false otherwise.
	 * @throws NullPointerException      if any of the inputs is null.
	 * @throws FailedValidationException if any of the inputs is not a valid UUID.
	 */
	public boolean areOnlinePayloadSignaturesValid(final String electionEventId, final String ballotId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotId);
		validateUUID(ballotBoxId);

		LOGGER.info("Verifying the signatures of all online payloads for election {}, ballot {} and ballot box {} ...", electionEventId, ballotId,
				ballotBoxId);

		final List<Integer> onlineControlComponentsNodeIds = Arrays.asList(1, 2, 3);

		final boolean isOnlinePayloadSignatureValid = onlineControlComponentsNodeIds.stream().allMatch(onlineControlComponentNodeId -> {

			final MixnetShufflePayload payload = mixnetShufflePayloadFileRepository
					.getPayload(electionEventId, ballotId, ballotBoxId, onlineControlComponentNodeId);

			return isPayloadSignatureValid(electionEventId, ballotId, ballotBoxId, onlineControlComponentNodeId, payload);

		});

		LOGGER.info("{} verification of the signatures of all online payloads for election {}, ballot {} and ballot box {}.",
				isOnlinePayloadSignatureValid ? "Successful" : "Unsuccessful", electionEventId, ballotId, ballotBoxId);

		return isOnlinePayloadSignatureValid;
	}

	/**
	 * Checks if the signature of the given payload related to the given election event id, ballot id, ballot box id and control component id is
	 * valid.
	 *
	 * @param electionEventId         the election event id.
	 * @param ballotId                the ballot id.
	 * @param ballotBoxId             the ballot box id.
	 * @param controlComponentsNodeId the control component node id.
	 * @param payload                 the payload whose signature must be verified.
	 * @return true if the signature of the payload was successfully verified, false otherwise.
	 */
	private boolean isPayloadSignatureValid(final String electionEventId, final String ballotId, final String ballotBoxId,
			final Integer controlComponentsNodeId, final MixnetShufflePayload payload) {
		LOGGER.debug("Verifying the signature of payload for election {}, ballot {}, ballot box {} and control component {} ...", electionEventId,
				ballotId, ballotBoxId, controlComponentsNodeId);

		final CryptolibPayloadSignature signature = payload.getSignature();
		final byte[] payloadHash = calculateHashPayload(payload);

		final X509Certificate platformRootCertificate;
		try {
			platformRootCertificate = platformRootCAService.load();
		} catch (CertificateManagementException e) {
			LOGGER.error("Failed to load the platform root certificate.", e);
			return false;
		}

		boolean isPayloadSignatureValid = false;
		try {
			isPayloadSignatureValid = cryptolibPayloadSignatureService.verify(signature, platformRootCertificate, payloadHash);
		} catch (PayloadVerificationException e) {
			LOGGER.error(String.format(
					"An error occurred while verifying the signature for election %s, ballot %s, ballot box %s and control component %s.",
					electionEventId, ballotId, ballotBoxId, controlComponentsNodeId), e);
		}

		LOGGER.debug("{} verification of the signature of payload for election {}, ballot {}, ballot box {} and control component {}.",
				isPayloadSignatureValid ? "Successful" : "Unsuccessful", electionEventId, ballotId, ballotBoxId, controlComponentsNodeId);

		return isPayloadSignatureValid;
	}

	/**
	 * Calculates the hash of the given payload using a recursive hash.
	 *
	 * @param mixnetShufflePayload the {@link MixnetShufflePayload} to compute the hash. Must be non-null.
	 * @return the hash of the payload as a byte array.
	 */
	private byte[] calculateHashPayload(final MixnetShufflePayload mixnetShufflePayload) {
		LOGGER.debug("Hashing MixnetShufflePayload...");
		final int numberOfVotes = mixnetShufflePayload.getEncryptedVotes().size();
		if (numberOfVotes > 1) {
			LOGGER.debug("with VerifiableShuffle: number of votes = {}", numberOfVotes);
			return hashService.recursiveHash(mixnetShufflePayload.getEncryptionGroup(),
					mixnetShufflePayload.getVerifiableDecryptions(),
					mixnetShufflePayload.getVerifiableShuffle(),
					mixnetShufflePayload.getRemainingElectionPublicKey(),
					mixnetShufflePayload.getPreviousRemainingElectionPublicKey(),
					mixnetShufflePayload.getNodeElectionPublicKey(),
					HashableBigInteger.from(BigInteger.valueOf(mixnetShufflePayload.getNodeId())));
		} else {
			LOGGER.debug("without VerifiableShuffle: number of votes = {}", numberOfVotes);
			return hashService.recursiveHash(mixnetShufflePayload.getEncryptionGroup(),
					mixnetShufflePayload.getVerifiableDecryptions(),
					mixnetShufflePayload.getRemainingElectionPublicKey(),
					mixnetShufflePayload.getPreviousRemainingElectionPublicKey(),
					mixnetShufflePayload.getNodeElectionPublicKey(),
					HashableBigInteger.from(BigInteger.valueOf(mixnetShufflePayload.getNodeId())));
		}

	}

}
