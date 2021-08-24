/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service.impl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.commons.domain.CreateElectoralBoardKeyPairInput;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.generator.DataGeneratorResponse;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * Implementation of a service which gives access to a generator of electoral board keys data from the configuration engine.
 */
@Service
public class ElectoralAuthorityDataGeneratorServiceImpl {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElectoralAuthorityDataGeneratorServiceImpl.class);

	@Autowired
	private BallotBoxRepository ballotBoxRepository;

	@Autowired
	private PathResolver pathResolver;

	@Value("${CREATE_ELECTORAL_BOARD_KEYS_URL}")
	private String createElectoralBoardKeysURL;

	public CreateElectoralBoardKeyPairInput generate(final String electoralAuthorityId, final String electionEventId) {

		DataGeneratorResponse result = validateInput(electoralAuthorityId, electionEventId);
		if (!result.isSuccessful()) {
			throw new IllegalStateException("invalid electoral authority and electionEventId");
		}

		// get all the ballot boxes related to the electoral authority
		String ballotBoxesResult = ballotBoxRepository.findByElectoralAuthority(electoralAuthorityId);

		JsonArray ballotBoxesList = JsonUtils.getJsonObject(ballotBoxesResult).getJsonArray(JsonConstants.RESULT);
		if (ballotBoxesList == null || ballotBoxesList.isEmpty()) {
			LOGGER.info("No ballot box found for election event with id-> {}", electionEventId);
			result.setSuccessful(false);
			throw new IllegalStateException("ballotBoxes not found");
		}
		Path outputElectionEventPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR).resolve(electionEventId);

		CreateElectoralBoardKeyPairInput createElectoralBoardKeyPairInput = new CreateElectoralBoardKeyPairInput();
		createElectoralBoardKeyPairInput.setOutputFolder(outputElectionEventPath.toString());
		createElectoralBoardKeyPairInput.setBallotMappings(createPairsBallotIdBallotBoxId(ballotBoxesList));

		return createElectoralBoardKeyPairInput;
	}

	// Validates the input data
	private DataGeneratorResponse validateInput(final String electoralAuthorityId, final String electionEventId) {
		DataGeneratorResponse response = new DataGeneratorResponse();
		// some basic validation of the input
		if (StringUtils.isBlank(electoralAuthorityId)) {
			response.setSuccessful(false);
			return response;
		}
		if (StringUtils.isBlank(electionEventId)) {
			response.setSuccessful(false);
			return response;
		}
		return response;
	}

	// create the set of pairs ballot id-ballot box id
	private Map<String, List<String>> createPairsBallotIdBallotBoxId(final JsonArray ballotBoxesList) {

		Map<String, List<String>> ballotId2BallotBoxListMap = new HashMap<>();
		for (int index = 0; index < ballotBoxesList.size(); index++) {
			JsonObject ballotBoxAsJson = ballotBoxesList.getJsonObject(index);
			String ballotId = ballotBoxAsJson.getJsonObject(JsonConstants.BALLOT).getString(JsonConstants.ID);
			String ballotBoxId = ballotBoxAsJson.getString(JsonConstants.ID);
			if (ballotId2BallotBoxListMap.containsKey(ballotId)) {
				List<String> ballotBoxIdList = ballotId2BallotBoxListMap.get(ballotId);
				ballotBoxIdList.add(ballotBoxId);
			} else {
				List<String> ballotBoxIdList = new ArrayList<>();
				ballotBoxIdList.add(ballotBoxId);
				ballotId2BallotBoxListMap.put(ballotId, ballotBoxIdList);
			}
		}

		return ballotId2BallotBoxListMap;
	}

	/**
	 * Generates a WebTarget client
	 *
	 * @return
	 */
	public WebTarget createWebClient() {
		return ClientBuilder.newClient().target(createElectoralBoardKeysURL);
	}

}
