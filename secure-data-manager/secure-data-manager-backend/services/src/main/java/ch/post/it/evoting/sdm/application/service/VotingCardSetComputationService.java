/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static ch.post.it.evoting.cryptolib.commons.validations.Validate.validateUUID;

import java.io.IOException;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.post.it.evoting.cryptoprimitives.hashing.HashService;
import ch.post.it.evoting.domain.election.model.messaging.PayloadVerificationException;
import ch.post.it.evoting.domain.returncodes.ReturnCodeGenerationRequestPayload;
import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.commons.CryptolibPayloadSignatureService;
import ch.post.it.evoting.sdm.domain.model.status.InvalidStatusTransitionException;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.infrastructure.cc.PayloadStorageException;
import ch.post.it.evoting.sdm.infrastructure.cc.ReturnCodeGenerationRequestPayloadRepository;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;

/**
 * This is an application service that deals with the computation of voting card data.
 */
@Service
public class VotingCardSetComputationService extends BaseVotingCardSetService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VotingCardSetComputationService.class);

	@Autowired
	IdleStatusService idleStatusService;

	@Autowired
	private ConfigurationEntityStatusService configurationEntityStatusService;

	@Autowired
	private VotingCardSetChoiceCodesService votingCardSetChoiceCodesService;

	@Autowired
	private ReturnCodeGenerationRequestPayloadRepository returnCodeGenerationRequestPayloadRepository;

	@Autowired
	private PlatformRootCAService platformRootCAService;

	@Autowired
	@Qualifier("cryptoPrimitivesHashService")
	private HashService hashService;

	@Autowired
	private CryptolibPayloadSignatureService payloadSignatureService;

	/**
	 * Compute a voting card set.
	 *
	 * @param votingCardSetId the identifier of the voting card set
	 * @param electionEventId the identifier of the election event
	 * @throws ResourceNotFoundException
	 * @throws InvalidStatusTransitionException if the original status does not allow computing
	 * @throws JsonProcessingException
	 * @throws PayloadStorageException          if the payload could not be store
	 * @throws PayloadVerificationException     if the payload signature could not be verified
	 */
	public void compute(final String votingCardSetId, final String electionEventId)
			throws ResourceNotFoundException, InvalidStatusTransitionException, IOException, PayloadStorageException, PayloadVerificationException {

		if (!idleStatusService.getIdLock(votingCardSetId)) {
			return;
		}

		try {
			LOGGER.info("Starting computation of voting card set {}...", votingCardSetId);

			final Status fromStatus = Status.PRECOMPUTED;
			final Status toStatus = Status.COMPUTING;

			validateUUID(votingCardSetId);
			validateUUID(electionEventId);

			checkVotingCardSetStatusTransition(electionEventId, votingCardSetId, fromStatus, toStatus);

			final String verificationCardSetId = votingCardSetRepository.getVerificationCardSetId(votingCardSetId);

			final X509Certificate platformRootCACertificate;
			try {
				platformRootCACertificate = platformRootCAService.load();
			} catch (CertificateManagementException e) {
				// The payload cannot be verified because the certificate could not be loaded.
				throw new PayloadVerificationException(e);
			}

			final int chunkCount = returnCodeGenerationRequestPayloadRepository.getCount(electionEventId, verificationCardSetId);
			for (int i = 0; i < chunkCount; i++) {
				// Retrieve the payload.
				final ReturnCodeGenerationRequestPayload payload = returnCodeGenerationRequestPayloadRepository
						.retrieve(electionEventId, verificationCardSetId, i);

				// Validate the signature.
				final byte[] requestPayloadHash = hashService.recursiveHash(payload);
				final boolean isSignatureValid = payloadSignatureService
						.verify(payload.getSignature(), platformRootCACertificate, requestPayloadHash);

				if (isSignatureValid) {
					// The signature is valid, send for processing.
					votingCardSetChoiceCodesService.sendToCompute(payload);
					LOGGER.info("Chunk {} of {} from voting card set {} was sent", i, chunkCount, votingCardSetId);
				} else {
					// The signature is not valid: do not send.
					LOGGER.error("Chunk {} of {} from voting card set {} was NOT sent: signature is not valid", i, chunkCount, votingCardSetId);
				}
			}

			configurationEntityStatusService.update(toStatus.name(), votingCardSetId, votingCardSetRepository);

			LOGGER.info("Computation of voting card set {} started", votingCardSetId);

		} finally {
			idleStatusService.freeIdLock(votingCardSetId);
		}
	}
}
