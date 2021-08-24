/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.validation.ValidationResult;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.electionevent.ElectionEventRepository;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * Service for uploading the ballot box information.
 */
@Service
public class BallotBoxInformationUploadService {

	public static final String ADMIN_BOARD_ID_PARAM = "adminBoardId";

	private static final String ENDPOINT_BALLOT_BOX_CONTENTS =
			"/ballotboxdata/tenant/{tenantId}/electionevent/{electionEventId}/ballotbox/{ballotBoxId}/adminboard" + "/{adminBoardId}";

	private static final String ENDPOINT_CHECK_IF_BALLOT_BOXES_EMPTY = "/ballotboxes/tenant/{tenantId}/electionevent/{electionEventId}/ballotbox/{ballotBoxId}/status";

	private static final Logger LOGGER = LoggerFactory.getLogger(BallotBoxInformationUploadService.class);

	private static final String CONSTANT_BALLOT_BOX_DATA = "ballotBox";

	private static final String CONSTANT_BALLOT_BOX_CONTENT_DATA = "ballotBoxContextData";

	private static final String TENANT_ID_PARAM = "tenantId";

	private static final String ELECTION_EVENT_ID_PARAM = "electionEventId";

	private static final String BALLOT_BOX_ID_PARAM = "ballotBoxId";

	@Autowired
	private BallotBoxService ballotBoxService;

	@Autowired
	private PathResolver pathResolver;

	@Autowired
	private ElectionEventRepository electionEventRepository;

	@Value("${EI_URL}")
	private String electionInformationBaseURL;

	@Value("${tenantID}")
	private String tenantId;

	/**
	 * Uploads additional information of all ballot boxes pending for upload to the voting portal.
	 */
	public void uploadSynchronizableBallotBoxInformation(String eeid) {

		JsonArray ballotBoxes = ballotBoxService.getBallotBoxesReadyToSynchronize(eeid);

		for (int i = 0; i < ballotBoxes.size(); i++) {

			JsonObject ballotBox = ballotBoxes.getJsonObject(i);

			String ballotBoxId = ballotBox.getString(JsonConstants.ID);
			String electionEventId = ballotBox.getJsonObject(JsonConstants.ELECTION_EVENT).getString(JsonConstants.ID);

			JsonObject electionEvent = JsonUtils.getJsonObject(electionEventRepository.find(electionEventId));
			JsonObject adminBoard = electionEvent.getJsonObject(JsonConstants.ADMINISTRATION_AUTHORITY);

			String adminBoardId = adminBoard.getString(JsonConstants.ID);

			if (isBallotBoxEmpty(electionEventId, ballotBoxId)) {
				String ballotId = ballotBox.getJsonObject(JsonConstants.BALLOT).getString(JsonConstants.ID);

				try {
					boolean uploadResultBBInformation = uploadBallotBoxConfiguration(electionEventId, ballotBoxId, ballotId, adminBoardId);

					uploadResultBallotBoxInformation(uploadResultBBInformation, ballotBoxId);

				} catch (IOException e) {
					LOGGER.error(MessageFormat
							.format("Error trying to find ballot box configuration to upload for ballot box id {0}, skipping: {1} ", ballotBoxId, e));
				}
			} else {
				LOGGER.info("Updating the synchronization status of the ballot box");
				ballotBoxService.updateSynchronizationStatus(ballotBoxId, false);
			}
		}
	}

	/**
	 * Uploads ballot box configuration
	 */
	private boolean uploadBallotBoxConfiguration(String electionEventId, String ballotBoxId, String ballotId, final String adminBoardId)
			throws IOException {

		LOGGER.info("Loading the signed ballot box configuration");
		JsonObject ballotBoxConfiguration = loadFilesToUpload(electionEventId, ballotBoxId, ballotId);
		LOGGER.info("Uploading the signed ballot box configuration");
		Response response = uploadBallotBoxConfiguration(electionEventId, ballotBoxId, adminBoardId, ballotBoxConfiguration);
		return response.getStatus() == Response.Status.OK.getStatusCode();

	}

	private Response uploadBallotBoxConfiguration(final String electionEventId, final String ballotBoxId, final String adminBoardId,
			final JsonObject ballotBoxConfiguration) {
		WebTarget target = ClientBuilder.newClient().target(electionInformationBaseURL + ENDPOINT_BALLOT_BOX_CONTENTS);
		return target.resolveTemplate(TENANT_ID_PARAM, tenantId).resolveTemplate(ELECTION_EVENT_ID_PARAM, electionEventId)
				.resolveTemplate(BALLOT_BOX_ID_PARAM, ballotBoxId).resolveTemplate(ADMIN_BOARD_ID_PARAM, adminBoardId).request()
				.post(Entity.entity(ballotBoxConfiguration.toString(), MediaType.APPLICATION_JSON_TYPE));
	}

	private void uploadResultBallotBoxInformation(boolean uploadResultBBInformation, String ballotBoxId) {
		LOGGER.info("Updating the synchronization status of the ballot box");
		ballotBoxService.updateSynchronizationStatus(ballotBoxId, uploadResultBBInformation);
		LOGGER.info("The ballot box configuration was uploaded successfully");
	}

	/**
	 * Retrieve json files on filesystem to upload
	 */
	private JsonObject loadFilesToUpload(String electionEventId, String ballotBoxId, String ballotId) throws IOException {

		JsonFactory jsonFactory = new JsonFactory();
		ObjectMapper jsonMapper = new ObjectMapper(jsonFactory);

		Path ballotBoxConfigurationFilesPath = getCommonPathForConfigurationFiles(electionEventId, ballotBoxId, ballotId);
		JsonNode ballotBoxNode = getBallotBoxSignature(jsonFactory, jsonMapper, ballotBoxConfigurationFilesPath);
		JsonNode ballotBoxContextDataNode = getBallotBoxContextDataSignature(jsonFactory, jsonMapper, ballotBoxConfigurationFilesPath);

		return (Json.createObjectBuilder().add(CONSTANT_BALLOT_BOX_DATA, ballotBoxNode.toString())
				.add(CONSTANT_BALLOT_BOX_CONTENT_DATA, ballotBoxContextDataNode.toString())).build();
	}

	private JsonNode getBallotBoxContextDataSignature(final JsonFactory jsonFactory, final ObjectMapper jsonMapper,
			final Path ballotBoxConfigurationFilesPath) throws IOException {
		return jsonMapper.readTree(jsonFactory.createParser(new File(
				ballotBoxConfigurationFilesPath.resolve(Constants.CONFIG_FILE_NAME_SIGNED_BALLOTBOX_CONTEXT_DATA_JSON).toAbsolutePath().toString())));
	}

	private JsonNode getBallotBoxSignature(final JsonFactory jsonFactory, final ObjectMapper jsonMapper, final Path ballotBoxConfigurationFilesPath)
			throws IOException {
		return jsonMapper.readTree(jsonFactory.createParser(
				new File(ballotBoxConfigurationFilesPath.resolve(Constants.CONFIG_FILE_NAME_SIGNED_BALLOTBOX_JSON).toAbsolutePath().toString())));
	}

	private Path getCommonPathForConfigurationFiles(final String electionEventId, final String ballotBoxId, final String ballotId) {
		return pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR).resolve(electionEventId).resolve(Constants.CONFIG_DIR_NAME_ONLINE)
				.resolve(Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION).resolve(Constants.CONFIG_DIR_NAME_BALLOTS).resolve(ballotId)
				.resolve(Constants.CONFIG_DIR_NAME_BALLOTBOXES).resolve(ballotBoxId);
	}

	private boolean isBallotBoxEmpty(String electionEvent, String ballotBoxId) {

		boolean result = Boolean.FALSE;
		WebTarget target = ClientBuilder.newClient().target(electionInformationBaseURL + ENDPOINT_CHECK_IF_BALLOT_BOXES_EMPTY);

		Response response = target.resolveTemplate(TENANT_ID_PARAM, tenantId).resolveTemplate(ELECTION_EVENT_ID_PARAM, electionEvent)
				.resolveTemplate(BALLOT_BOX_ID_PARAM, ballotBoxId).request(MediaType.APPLICATION_JSON).get();

		if (response.getStatus() == Response.Status.OK.getStatusCode()) {
			String json = response.readEntity(String.class);
			ValidationResult validationResult = new ValidationResult();
			try {
				validationResult = ObjectMappers.fromJson(json, ValidationResult.class);
			} catch (IOException e) {
				LOGGER.error("Error checking if a ballot box is empty", e);
			}
			result = validationResult.isResult();
		}
		return result;
	}
}
