/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.ballotbox;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.io.output.CloseShieldOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.certificates.utils.CryptographicOperationException;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.domain.election.validation.ValidationResult;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.crypto.PrivateKeyForObjectRepository;
import ch.post.it.evoting.votingserver.commons.infrastructure.config.InfrastructureConfig;
import ch.post.it.evoting.votingserver.commons.signature.SignatureFactory;
import ch.post.it.evoting.votingserver.commons.signature.SignatureOutputStream;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.common.csv.ExportedBallotBoxItemWriter;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBox;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.ExportedBallotBoxItem;

/**
 * A service for handling ballot boxes.
 */
@Stateless
public class BallotBoxServiceImpl implements BallotBoxService {

	private static final int PAGE_SIZE = Integer.parseInt(InfrastructureConfig.getEnvWithDefaultOption("BALLOT_BOX_PAGE_SIZE", "7"));

	// The keystore alias
	private static final String KEYSTORE_ALIAS = "privatekey";

	private static final byte[] LINE_SEPARATOR = "\n".getBytes(StandardCharsets.UTF_8);
	private static final Logger LOGGER = LoggerFactory.getLogger(BallotBoxServiceImpl.class);
	@Inject
	private SignatureFactory signatureFactory;
	@Inject
	private BallotBoxRepository ballotBoxRepository;
	@Inject
	private BallotBoxInformationService ballotBoxInformationService;
	@Inject
	private PrivateKeyForObjectRepository privateKeyRepository;

	/**
	 * @see BallotBoxService#checkIfBallotBoxesAreEmpty(String,
	 * String, String)
	 */
	@Override
	public ValidationResult checkIfBallotBoxesAreEmpty(final String tenantId, final String electionEventId, final String ballotBoxId) {
		LOGGER.info("Validating if all ballot box {} for tenant {} and election event {} is empty.", ballotBoxId, tenantId, electionEventId);

		final List<BallotBox> listBallotBox = ballotBoxRepository.findByTenantIdElectionEventIdBallotBoxId(tenantId, electionEventId, ballotBoxId);
		final ValidationResult validationResult = new ValidationResult();
		validationResult.setResult(listBallotBox.isEmpty());
		final ValidationError validationError = new ValidationError();
		if (validationResult.isResult()) {
			validationError.setValidationErrorType(ValidationErrorType.SUCCESS);
		}
		validationResult.setValidationError(validationError);

		LOGGER.info("Ballot box {} is empty: {}", ballotBoxId, validationResult.isResult());
		return validationResult;
	}

	@Override
	public void writeEncryptedBallotBox(final OutputStream stream, final String tenantId, final String electionEventId, final String ballotBoxId,
			final boolean test) throws IOException {
		LOGGER.info("Retrieving encrypted ballot box box {} for tenant {} and election event {}.", ballotBoxId, tenantId, electionEventId);
		Signature signature = getSignature(tenantId, electionEventId, ballotBoxId);
		writeEncryptedBallotBoxItems(new SignatureOutputStream(stream, signature), tenantId, electionEventId, ballotBoxId);
		writeSignature(stream, signature);
	}

	private Signature getSignature(final String tenantId, final String electionEventId, final String ballotBoxId) throws IOException {
		Signature signature = signatureFactory.newSignature();
		try {
			PrivateKey privateKey = privateKeyRepository.findByTenantEEIDObjectIdAlias(tenantId, electionEventId, ballotBoxId, KEYSTORE_ALIAS);
			signature.initSign(privateKey);
		} catch (InvalidKeyException | ResourceNotFoundException | CryptographicOperationException e) {
			throw new IOException("Failed to get signature.", e);
		}
		return signature;
	}

	private void writeEncryptedBallotBoxItems(final OutputStream stream, final String tenantId, final String electionEventId,
			final String ballotBoxId) throws IOException {
		try (ExportedBallotBoxItemWriter writer = new ExportedBallotBoxItemWriter(new CloseShieldOutputStream(stream))) {
			int first = 1;
			int last = PAGE_SIZE;
			List<ExportedBallotBoxItem> page = ballotBoxRepository
					.getEncryptedVotesByTenantIdElectionEventIdBallotBoxId(tenantId, electionEventId, ballotBoxId, first, last);
			while (!page.isEmpty()) {
				for (final ExportedBallotBoxItem item : page) {
					writer.write(item);
				}
				first += PAGE_SIZE;
				last += PAGE_SIZE;
				page = ballotBoxRepository.getEncryptedVotesByTenantIdElectionEventIdBallotBoxId(tenantId, electionEventId, ballotBoxId, first, last);
			}
		}
	}

	private void writeSignature(final OutputStream stream, final Signature signature) throws IOException {
		byte[] base64Signature;
		try {
			base64Signature = Base64.getEncoder().encode(signature.sign());
		} catch (SignatureException e) {
			throw new IOException("Failed to sign encrypted ballot box.", e);
		}
		stream.write(LINE_SEPARATOR);
		stream.write(base64Signature);
	}

	/**
	 * Check if a ballot box is a test ballot box
	 *
	 * @param tenantId        - tenant identifier
	 * @param electionEventId - election event identifier
	 * @param ballotBoxId     - ballot box identifier
	 * @return if the ballot box is a test ballot box or not
	 * @throws ResourceNotFoundException
	 */
	@Override
	public boolean checkIfTest(String tenantId, String electionEventId, String ballotBoxId) throws ResourceNotFoundException {
		return ballotBoxInformationService.isBallotBoxForTest(tenantId, electionEventId, ballotBoxId);
	}

}
