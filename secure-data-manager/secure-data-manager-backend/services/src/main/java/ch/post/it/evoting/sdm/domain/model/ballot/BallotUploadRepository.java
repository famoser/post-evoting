/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.ballot;

import javax.json.JsonObject;

/**
 * Interface for uploading the information of Ballots and Ballot Texts
 */
public interface BallotUploadRepository {

	/**
	 * Uploads the available ballots and ballot texts to the voter portal.
	 *
	 * @param jsonBallot      - the json object of a ballot.
	 * @param electionEventId - the election event identifier.
	 * @param ballotId        - the ballot identifier.
	 * @param adminBoardId
	 * @return True if the ballot if successfully uploaded. Otherwise, false.
	 */
	boolean uploadBallot(JsonObject jsonBallot, String electionEventId, String ballotId, final String adminBoardId);
}
