/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.sdm.application.exception.DatabaseException;
import ch.post.it.evoting.sdm.domain.model.ballot.BallotRepository;
import ch.post.it.evoting.sdm.domain.model.ballot.BallotUploadRepository;
import ch.post.it.evoting.sdm.domain.model.ballottext.BallotTextRepository;
import ch.post.it.evoting.sdm.domain.model.electionevent.ElectionEventRepository;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.domain.model.status.SynchronizeStatus;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * Service responsible of uploading ballots and ballot texts
 */
@Service
public class BallotUploadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BallotUploadService.class);

	private static final String NULL_ELECTION_EVENT_ID = "";

	private static final String BALLOT_ID_FIELD = "ballot.id";

	@Autowired
	private ElectionEventRepository electionEventRepository;

	@Autowired
	private BallotRepository ballotRepository;

	@Autowired
	private BallotTextRepository ballotTextRepository;

	@Autowired
	private BallotUploadRepository ballotUploadRepository;

	/**
	 * Uploads the available ballots and ballot texts to the voter portal.
	 */
	public void uploadSynchronizableBallots(String eeid) {

		Map<String, Object> ballotParams = new HashMap<>();

		addSignedBallots(ballotParams);
		addPendingToSynchBallots(ballotParams);

		if (thereIsAnElectionEventAsAParameter(eeid)) {
			addBallotsOfElectionEvent(eeid, ballotParams);
		}

		String ballotDocuments = ballotRepository.list(ballotParams);
		JsonArray ballots = JsonUtils.getJsonObject(ballotDocuments).getJsonArray(JsonConstants.RESULT);

		for (int i = 0; i < ballots.size(); i++) {

			JsonObject ballotInArray = ballots.getJsonObject(i);
			String signedBallot = ballotInArray.getString(JsonConstants.SIGNED_OBJECT);

			String electionEventId = ballotInArray.getJsonObject(JsonConstants.ELECTION_EVENT).getString(JsonConstants.ID);
			String ballotId = ballotInArray.getString(JsonConstants.ID);

			JsonObject electionEvent = JsonUtils.getJsonObject(electionEventRepository.find(electionEventId));
			JsonObject adminBoard = electionEvent.getJsonObject(JsonConstants.ADMINISTRATION_AUTHORITY);

			String adminBoardId = adminBoard.getString(JsonConstants.ID);

			Map<String, Object> ballotTextParams = new HashMap<>();
			ballotTextParams.put(BALLOT_ID_FIELD, ballotId);

			LOGGER.info("Loading the signed ballot");
			JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
			for (String signature : ballotTextRepository.listSignatures(ballotTextParams)) {
				JsonObject object = Json.createObjectBuilder().add(JsonConstants.SIGNED_OBJECT, signature).build();
				arrayBuilder.add(object);
			}
			JsonArray signedBallotTexts = arrayBuilder.build();
			LOGGER.info("Loading the signed ballot texts");
			JsonObject jsonInput = Json.createObjectBuilder().add(JsonConstants.BALLOT, signedBallot)
					.add(JsonConstants.BALLOTTEXT, signedBallotTexts.toString()).build();

			LOGGER.info("Uploading the signed ballot and ballot texts");
			boolean result = ballotUploadRepository.uploadBallot(jsonInput, electionEventId, ballotId, adminBoardId);
			JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
			if (result) {

				LOGGER.info("Changing the state of the ballot");
				jsonObjectBuilder.add(JsonConstants.ID, ballotId);
				jsonObjectBuilder.add(JsonConstants.SYNCHRONIZED, SynchronizeStatus.SYNCHRONIZED.getIsSynchronized().toString());
				jsonObjectBuilder.add(JsonConstants.DETAILS, SynchronizeStatus.SYNCHRONIZED.getStatus());
				LOGGER.info("The signed ballot was uploaded successfully");

			} else {
				String error = "An error occurred while uploading the signed ballot";
				LOGGER.error(error);
				jsonObjectBuilder.add(JsonConstants.ID, ballotId);
				jsonObjectBuilder.add(JsonConstants.DETAILS, SynchronizeStatus.FAILED.getStatus());

			}

			try {
				ballotRepository.update(jsonObjectBuilder.build().toString());
			} catch (DatabaseException ex) {
				LOGGER.error("An error occurred while updating the signed ballot", ex);
			}

		}

	}

	private void addBallotsOfElectionEvent(final String electionEvent, final Map<String, Object> ballotParams) {
		ballotParams.put(JsonConstants.ELECTION_EVENT_DOT_ID, electionEvent);
	}

	private void addPendingToSynchBallots(final Map<String, Object> ballotParams) {
		ballotParams.put(JsonConstants.SYNCHRONIZED, SynchronizeStatus.PENDING.getIsSynchronized().toString());
	}

	private void addSignedBallots(final Map<String, Object> ballotParams) {
		ballotParams.put(JsonConstants.STATUS, Status.SIGNED.name());
	}

	private boolean thereIsAnElectionEventAsAParameter(final String electionEvent) {
		return !NULL_ELECTION_EVENT_ID.equals(electionEvent);
	}

}
