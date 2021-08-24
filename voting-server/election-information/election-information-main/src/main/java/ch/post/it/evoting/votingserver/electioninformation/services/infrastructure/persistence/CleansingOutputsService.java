/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.persistence;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.output.CloseShieldOutputStream;
import org.slf4j.Logger;

import ch.post.it.evoting.cryptolib.certificates.utils.CryptographicOperationException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.crypto.PrivateKeyForObjectRepository;
import ch.post.it.evoting.votingserver.commons.signature.SignatureFactory;
import ch.post.it.evoting.votingserver.commons.signature.SignatureOutputStream;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.common.csv.FailedVotesWriter;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.common.csv.SuccessfulVotesItemWriter;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBox;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.SuccessfulVoteItem;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.vote.FailedVote;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.vote.SuccessfulVote;

/**
 * Obtain the cleansing outputs from the DB through the repositories.
 */
public class CleansingOutputsService {

	// 500 results per page
	private static final int PAGE_SIZE = 499;

	private static final String KEYSTORE_ALIAS = "privatekey";

	private static final byte[] LINE_SEPARATOR = "\n".getBytes(StandardCharsets.UTF_8);

	private static final String NOT_CONFIRMED_STATUS = "NOT_CONFIRMED";

	@Inject
	private SuccessfulVotesRepository successfulVotesRepository;

	@Inject
	private PrivateKeyForObjectRepository privateKeyRepository;

	@Inject
	private BallotBoxRepository ballotBoxRepository;

	@Inject
	private SignatureFactory signatureFactory;

	@Inject
	private Logger logger;

	/**
	 * Write the successful votes obtained from DB and their signature to the provided stream.
	 *
	 * @param stream          the stream where write the successful votes
	 * @param tenantId        the tenant id
	 * @param electionEventId the election event id
	 * @param ballotBoxId     the ballot box id
	 * @throws IOException if the successful votes signature fails
	 */
	public void writeSuccessfulVotes(OutputStream stream, String tenantId, String electionEventId, String ballotBoxId) throws IOException {
		logger.info("Retrieving successful votes {} for tenant {} and election event {}.", ballotBoxId, tenantId, electionEventId);
		Signature signature = getSignature(tenantId, electionEventId, ballotBoxId);
		doWriteSuccessful(new SignatureOutputStream(stream, signature), tenantId, electionEventId, ballotBoxId);
		writeSignature(stream, signature);
	}

	/**
	 * Write the failed votes obtained from DB to the provided stream.
	 *
	 * @param stream          the stream where write the successful votes
	 * @param tenantId        the tenant id
	 * @param electionEventId the election event id
	 * @param ballotBoxId     the ballot box id
	 * @throws IOException if the successful votes signature fails
	 */
	public void writeFailedVotes(OutputStream stream, String tenantId, String electionEventId, String ballotBoxId) throws IOException {
		logger.info("Retrieving failed votes {} for tenant {} and election event {}.", ballotBoxId, tenantId, electionEventId);
		Signature signature = getSignature(tenantId, electionEventId, ballotBoxId);
		doWriteFailed(new SignatureOutputStream(stream, signature), tenantId, electionEventId, ballotBoxId);
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

	/**
	 * Write the received signature to the provided output stream.
	 */
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
	 * Get successful votes from repository and write them to the supplied output stream.
	 */
	private void doWriteSuccessful(OutputStream stream, String tenantId, String electionEventId, String ballotBoxId) throws IOException {
		try (SuccessfulVotesItemWriter writer = new SuccessfulVotesItemWriter(new CloseShieldOutputStream(stream))) {
			int first = 0;
			List<SuccessfulVote> page = successfulVotesRepository.getSuccessfulVotes(tenantId, electionEventId, ballotBoxId, first, PAGE_SIZE);
			while (!page.isEmpty()) {
				for (final SuccessfulVote successfulVote : page) {
					SuccessfulVoteItem successfulVoteItem = new SuccessfulVoteItem();
					successfulVoteItem.setTimestamp(successfulVote.getTimestamp());
					successfulVoteItem.setVotingCardId(successfulVote.getVotingCardId());
					writer.write(successfulVoteItem);
				}
				first += PAGE_SIZE;
				page = successfulVotesRepository.getSuccessfulVotes(tenantId, electionEventId, ballotBoxId, first, PAGE_SIZE);
			}
		}
	}

	/**
	 * Get the not confirmed votes from ballot box repository, convert them to failed votes entities
	 * and write them to the supplied output stream.
	 */
	private void doWriteFailed(OutputStream stream, String tenantId, String electionEventId, String ballotBoxId) throws IOException {
		try (FailedVotesWriter writer = new FailedVotesWriter(new CloseShieldOutputStream(stream))) {
			int first = 0;
			List<BallotBox> page = ballotBoxRepository.getFailedVotes(tenantId, electionEventId, ballotBoxId, first, PAGE_SIZE);
			while (!page.isEmpty()) {
				for (final BallotBox item : page) {
					FailedVote failedVote = convertToFailedVote(item);
					writer.write(failedVote);
				}
				first += PAGE_SIZE;
				page = ballotBoxRepository.getFailedVotes(tenantId, electionEventId, ballotBoxId, first, PAGE_SIZE);
			}
		}
	}

	/**
	 * Converts a ballot box entry (vote) to failed vote format.
	 */
	private FailedVote convertToFailedVote(BallotBox vote) {
		ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
		FailedVote failedVote = new FailedVote();
		failedVote.setVotingCardId(vote.getVotingCardId());
		failedVote.setTimestamp(now);
		failedVote.setValidationError(NOT_CONFIRMED_STATUS);
		return failedVote;
	}

}
