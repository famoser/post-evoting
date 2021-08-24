/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.votingcardset;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface for uploading the information of Voting Card Sets
 */
public interface VotingCardSetUploadRepository {

	/**
	 * Uploads the voter information represented by a given stream. Client is responsible for closing
	 * the stream.
	 *
	 * @param electionEventId the election event identifier
	 * @param votingCardSetId the voting card set identifier
	 * @param adminBoardId    the administrative board identifier
	 * @param stream          the information
	 * @throws IOException failed to upload the information.
	 */
	void uploadVoterInformation(String electionEventId, String votingCardSetId, String adminBoardId, InputStream stream) throws IOException;

	/**
	 * Uploads the credential data represented by a given stream. Client is responsible for closing
	 * the stream.
	 *
	 * @param electionEventId the election event identifier
	 * @param votingCardSetId the voting card set identifier
	 * @param stream          the data
	 * @throws IOException failed to upload the data.
	 */
	void uploadCredentialData(String electionEventId, String votingCardSetId, String adminBoardId, InputStream stream) throws IOException;

	/**
	 * Uploads the extended authentication represented by a given stream, Client is responsible for
	 * closing the stream.
	 *
	 * @param electionEventId the election event identifier
	 * @param adminBoardId    the adminBoard id
	 * @param stream          the data
	 * @throws IOException failed to upload the data.
	 */
	void uploadExtendedAuthData(String electionEventId, String adminBoardId, InputStream stream) throws IOException;

}
