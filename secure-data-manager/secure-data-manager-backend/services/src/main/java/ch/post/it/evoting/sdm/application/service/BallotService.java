/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.commons.serialization.JsonSignatureService;
import ch.post.it.evoting.domain.election.Ballot;
import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.ballot.BallotRepository;
import ch.post.it.evoting.sdm.domain.model.ballottext.BallotTextRepository;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.domain.model.status.SynchronizeStatus;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * Service for operates with ballots.
 */
@Service
public class BallotService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BallotService.class);

	private static final String BALLOT_ID_FIELD = "ballot.id";

	@Autowired
	private ConfigurationEntityStatusService statusService;

	@Autowired
	private BallotRepository ballotRepository;

	@Autowired
	private BallotTextRepository ballotTextRepository;

	@Autowired
	private ConsistencyCheckService consistencyCheckService;

	@Autowired
	private PathResolver pathResolver;

	@Autowired
	private ObjectMapper objectMapper;

	/**
	 * Sign the given ballot and related ballot texts, and change the state of the ballot from locked to SIGNED for a given election event and ballot
	 * id.
	 *
	 * @param electionEventId the election event id.
	 * @param ballotId        the ballot id.
	 * @param privateKeyPEM   the administration board private key in PEM format.
	 * @throws ResourceNotFoundException if the ballot is not found.
	 * @throws GeneralCryptoLibException if the private key cannot be read.
	 */
	public void sign(String electionEventId, String ballotId, final String privateKeyPEM)
			throws ResourceNotFoundException, GeneralCryptoLibException {

		PrivateKey privateKey = PemUtils.privateKeyFromPem(privateKeyPEM);

		JsonObject ballot = getValidBallot(electionEventId, ballotId);
		JsonObject modifiedBallot = removeBallotMetaData(ballot);

		if (!validateElectionEventRepresentations(electionEventId, ballot)) {
			throw new GeneralCryptoLibException("Validation of the representations used on the ballot options failed.");
		}

		LOGGER.info("Signing ballot {}.", ballotId);
		String signedBallot = JsonSignatureService.sign(privateKey, modifiedBallot.toString());

		ballotRepository.updateSignedBallot(ballotId, signedBallot);
		JsonArray ballotTexts = getBallotTexts(ballotId);

		LOGGER.info("Signing ballot texts");
		for (int i = 0; i < ballotTexts.size(); i++) {

			JsonObject ballotText = ballotTexts.getJsonObject(i);
			String ballotTextId = ballotText.getString(JsonConstants.ID);
			JsonObject modifiedBallotText = removeBallotTextMetaData(ballotText);
			String signedBallotText = JsonSignatureService.sign(privateKey, modifiedBallotText.toString());
			ballotTextRepository.updateSignedBallotText(ballotTextId, signedBallotText);
		}

		LOGGER.info("Changing the ballot status");
		statusService.updateWithSynchronizedStatus(Status.SIGNED.name(), ballotId, ballotRepository, SynchronizeStatus.PENDING);

		LOGGER.info("The ballot was successfully signed");
	}

	/**
	 * Validate the representations used on the ballot against the assigned to the election event Validate the representations file signature
	 */
	private boolean validateElectionEventRepresentations(String electionEventId, JsonObject ballot) throws GeneralCryptoLibException {

		// Get the representations file for the Election Event
		Path representationsFile = pathResolver
				.resolve(Constants.SDM_DIR_NAME, Constants.CONFIG_DIR_NAME, electionEventId, Constants.CONFIG_DIR_NAME_CUSTOMER,
						Constants.CONFIG_DIR_NAME_OUTPUT, Constants.CONFIG_FILE_NAME_REPRESENTATIONS_CSV);

		try {
			// Check consistency of db stored representations of the options in the ballot
			return consistencyCheckService.representationsConsistent(ballot.toString(), representationsFile);
		} catch (IOException e) {
			throw new GeneralCryptoLibException("CSV file with representations prime numbers not found.", e);
		}
	}

	private JsonObject removeBallotTextMetaData(final JsonObject ballotText) {
		return removeField(JsonConstants.SIGNED_OBJECT, ballotText);
	}

	private JsonObject removeBallotMetaData(final JsonObject ballot) {

		JsonObject modifiedBallot = removeField(JsonConstants.STATUS, ballot);
		modifiedBallot = removeField(JsonConstants.DETAILS, modifiedBallot);
		modifiedBallot = removeField(JsonConstants.SYNCHRONIZED, modifiedBallot);
		return removeField(JsonConstants.SIGNED_OBJECT, modifiedBallot);
	}

	private JsonObject removeField(String field, JsonObject obj) {

		JsonObjectBuilder builder = Json.createObjectBuilder();

		for (final Map.Entry<String, JsonValue> e : obj.entrySet()) {
			String key = e.getKey();
			JsonValue value = e.getValue();
			if (!key.equals(field)) {
				builder.add(key, value);
			}

		}
		return builder.build();
	}

	private JsonArray getBallotTexts(final String ballotId) {
		Map<String, Object> ballotTextParams = new HashMap<>();
		ballotTextParams.put(BALLOT_ID_FIELD, ballotId);
		return JsonUtils.getJsonObject(ballotTextRepository.list(ballotTextParams)).getJsonArray(JsonConstants.RESULT);
	}

	private JsonObject getValidBallot(final String electionEventId, final String ballotId) throws ResourceNotFoundException {

		Optional<JsonObject> possibleBallot = getPossibleValidBallot(electionEventId, ballotId);

		if (!possibleBallot.isPresent()) {
			throw new ResourceNotFoundException("Ballot not found");
		}

		return possibleBallot.get();
	}

	/**
	 * Pre: there is just one matching element
	 *
	 * @return single {@link Status.LOCKED} ballot in json object format
	 */
	private Optional<JsonObject> getPossibleValidBallot(String electionEventId, String ballotId) {

		Optional<JsonObject> ballot = Optional.empty();
		Map<String, Object> attributeValueMap = new HashMap<>();

		attributeValueMap.put(JsonConstants.ELECTION_EVENT_DOT_ID, electionEventId);
		attributeValueMap.put(JsonConstants.ID, ballotId);
		attributeValueMap.put(JsonConstants.STATUS, Status.LOCKED.name());
		String ballotResultListAsJson = ballotRepository.list(attributeValueMap);

		if (StringUtils.isEmpty(ballotResultListAsJson)) {
			return ballot;
		} else {
			JsonArray ballotResultList = JsonUtils.getJsonObject(ballotResultListAsJson).getJsonArray(JsonConstants.RESULT);

			if (ballotResultList != null && !ballotResultList.isEmpty()) {
				ballot = Optional.of(ballotResultList.getJsonObject(0));
			} else {
				return ballot;
			}
		}
		return ballot;
	}

	public Ballot getBallot(String electionEventId, String ballotId) {
		Map<String, Object> attributeValueMap = new HashMap<>();
		attributeValueMap.put(JsonConstants.ELECTION_EVENT_DOT_ID, electionEventId);
		attributeValueMap.put(JsonConstants.ID, ballotId);
		String ballotAsJson = ballotRepository.find(attributeValueMap);
		try {
			return objectMapper.readValue(ballotAsJson, Ballot.class);
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot deserialize the ballot box json string to a valid Ballot object.", e);
		}
	}
}
