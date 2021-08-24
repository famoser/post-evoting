/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.election;

import java.io.IOException;
import java.io.OutputStream;

import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.validation.ElectionValidationRequest;

/**
 *
 */
public interface ElectionService {

	/**
	 * Validates if the election is open
	 *
	 * @param electionValidationRequest a request object including the ids and whether the grace
	 *                                  period has to be applied or not
	 * @return
	 */
	ValidationError validateIfElectionIsOpen(final ElectionValidationRequest electionValidationRequest);

	/**
	 * Writes cast votes to a given output stream. The written data has ZIP format and has two
	 * entries: a CSV entry with votes and it's PKCS#7 signature.
	 *
	 * @param tenantId        the tenant identifier
	 * @param electionEventId the election event identifier
	 * @param filenamePrefix  the filename prefix used to name the entries
	 * @param stream          the stream to write to
	 * @throws IOException I/O error occurred
	 */
	void writeCastVotes(String tenantId, String electionEventId, String filenamePrefix, OutputStream stream) throws IOException;

	/**
	 * Writes verified votes to a given output stream. The written data has ZIP format and has two
	 * entries: a CSV entry with votes and it's PKCS#7 signature.
	 *
	 * @param tenantId        the tenant identifier
	 * @param electionEventId the election event identifier
	 * @param filenamePrefix  the filename prefix used to name the entries
	 * @param stream          the stream to write to
	 * @throws IOException I/O error occurred
	 */
	void writeVerifiedVotes(String tenantId, String electionEventId, String filenamePrefix, OutputStream stream) throws IOException;
}
