/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static ch.post.it.evoting.cryptolib.commons.validations.Validate.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import ch.post.it.evoting.domain.mixnet.MixnetShufflePayload;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.infrastructure.RestClientService;
import ch.post.it.evoting.sdm.infrastructure.clients.ElectionInformationClient;
import ch.post.it.evoting.sdm.infrastructure.clients.OrchestratorClient;
import ch.post.it.evoting.sdm.infrastructure.mixnetpayload.MixnetShufflePayloadFileRepository;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;

import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Service for handling download encrypted ballot boxes.
 */
@Service
public class BallotBoxDownloadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BallotBoxDownloadService.class);

	@Autowired
	private BallotBoxRepository ballotBoxRepository;

	@Autowired
	private ConfigurationEntityStatusService configurationEntityStatusService;

	@Autowired
	private PathResolver pathResolver;

	@Value("${OR_URL}")
	private String orchestratorUrl;

	@Value("${EI_URL}")
	private String electionInformationUrl;

	@Value("${tenantID}")
	private String tenantId;

	@Autowired
	private KeyStoreService keystoreService;

	private Retrofit orchestratorRestAdapter;
	private Retrofit electionInformationRestAdapter;

	@Autowired
	private MixnetShufflePayloadFileRepository mixnetShufflePayloadFileRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@PostConstruct
	void configure() {
		PrivateKey requestSigningKey = keystoreService.getPrivateKey();

		orchestratorRestAdapter = RestClientService.getInstance()
				.getRestClientWithInterceptorAndJacksonConverter(orchestratorUrl, requestSigningKey, "SECURE_DATA_MANAGER");

		electionInformationRestAdapter = RestClientService.getInstance()
				.getRestClientWithInterceptorAndJacksonConverter(electionInformationUrl, requestSigningKey, "SECURE_DATA_MANAGER");

	}

	/**
	 * Download a ballot box based on election event id and its id.
	 *
	 * @param electionEventId the election event id.
	 * @param ballotBoxId     the ballot box id.
	 * @throws BallotBoxDownloadException if the ballot box to download does not exist in DB or the download/write of the ballot box fails.
	 */
	public void download(final String electionEventId, final String ballotBoxId) throws BallotBoxDownloadException {
		JsonNode ballotBox = getBallotBoxJson(ballotBoxId, electionEventId);
		checkBallotBoxStatusForDownload(ballotBox, ballotBoxId);

		ResponseBody responseBody = downloadBallotBox(electionEventId, ballotBoxId);
		String ballotId = ballotBox.get(JsonConstants.BALLOT).get(JsonConstants.ID).textValue();
		writeBallotBoxStreamToFile(electionEventId, ballotId, ballotBoxId, responseBody);
	}

	/**
	 * Downloads the outputs generated when mixing the requested ballot box on the online control component nodes.
	 *
	 * @param electionEventId the election event id.
	 * @param ballotBoxId     the ballot box id.
	 * @throws BallotBoxDownloadException if the ballot box to download does not exist in DB or the download/write of the ballot box fails.
	 */
	public void downloadPayloads(final String electionEventId, final String ballotBoxId) throws BallotBoxDownloadException {
		checkNotNull(electionEventId);
		checkNotNull(ballotBoxId);
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		LOGGER.info("Requesting payloads for ballot box {}...", ballotBoxId);
		JsonNode ballotBox = getBallotBoxJson(ballotBoxId, electionEventId);

		// Check that the full ballot box has been mixed.
		checkBallotBoxStatusForDownload(ballotBox, ballotBoxId);
		String ballotId = ballotBox.get(JsonConstants.BALLOT).get(JsonConstants.ID).textValue();

		// Get payloads, then persist them
		ResponseBody payloads = getMixingPayloads(electionEventId, ballotBoxId);
		persistBallotBoxPayloads(electionEventId, ballotId, ballotBoxId, payloads);
	}

	/**
	 * Downloads the ballot box and returns it in a ResponseBody stream to be processed.
	 *
	 * @param electionEventId the election event identifier.
	 * @param ballotBoxId     the ballot box identifier
	 * @throws BallotBoxDownloadException if the download call fails.
	 */
	private ResponseBody downloadBallotBox(String electionEventId, String ballotBoxId) throws BallotBoxDownloadException {
		final Response<ResponseBody> execute;
		try {
			execute = getElectionInformationClient().getRawBallotBox(tenantId, electionEventId, ballotBoxId).execute();
		} catch (final IOException e) {
			String errormessage = String
					.format("Failed to download encrypted BallotBox [electionEvent=%s, ballotBox=%s]: %s", electionEventId, ballotBoxId,
							e.getMessage());
			throw new BallotBoxDownloadException(errormessage, e);
		}

		if (!execute.isSuccessful()) {
			String errormessage = String
					.format("Failed to download encrypted BallotBox [electionEvent=%s, ballotBox=%s]: %s", electionEventId, ballotBoxId, "500");
			throw new BallotBoxDownloadException(errormessage);
		}

		return execute.body();
	}

	/**
	 * Downloads the payloads of the online mixing nodes.
	 *
	 * @param electionEventId the election event identifier.
	 * @param ballotBoxId     the ballot box identifier
	 * @throws BallotBoxDownloadException if the download call fails.
	 */
	private ResponseBody getMixingPayloads(String electionEventId, String ballotBoxId) throws BallotBoxDownloadException {
		LOGGER.info("Requesting to the Orchestrator the payloads for ballot box {} ", ballotBoxId);

		final Response<ResponseBody> response;
		try {
			response = getOrchestratorClient().getMixnetShufflePayloads(tenantId, electionEventId, ballotBoxId).execute();
		} catch (final IOException e) {
			throw new UncheckedIOException("Failed to communicate with orchestrator.", e);
		}

		if (!response.isSuccessful()) {
			final String errorBodyString;
			try {
				errorBodyString = response.errorBody().string();
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to convert response body to string.", e);
			}
			final String errormessage = String
					.format("Failed to download the mixing payloads [electionEvent=%s, ballotBox=%s]: %s", electionEventId, ballotBoxId,
							errorBodyString);
			throw new BallotBoxDownloadException(errormessage);
		}

		return response.body();
	}

	/**
	 * Extracts the ballot box bytes from the stream and write them to file.
	 *
	 * @param responseBody the stream containing the ballot box raw bytes
	 */
	private void writeBallotBoxStreamToFile(String electionEventId, String ballotId, String ballotBoxId, ResponseBody responseBody)
			throws BallotBoxDownloadException {
		Path ballotBoxFile;
		try (InputStream stream = responseBody.byteStream()) {
			ballotBoxFile = getDownloadedBallotBoxPath(electionEventId, ballotId, ballotBoxId);
			Files.copy(stream, ballotBoxFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			String errorMessage = String
					.format("Failed to write downloaded BallotBox [electionEvent=%s, ballotBox=%s] to file.", electionEventId, ballotBoxId);
			throw new BallotBoxDownloadException(errorMessage, e);
		}
	}

	/**
	 * Stores downloaded mixing payloads in SDM file system.
	 *
	 * @param payloadResponse the payloads in JSON format.
	 */
	private void persistBallotBoxPayloads(String electionEventId, String ballotId, String ballotBoxId, ResponseBody payloadResponse) {
		LOGGER.info("Persist locally the payloads for ballot box {} ", ballotBoxId);

		MixnetShufflePayload[] payloads;
		try {
			payloads = objectMapper.readValue(payloadResponse.string(), MixnetShufflePayload[].class);
		} catch (IOException e) {
			String errorMessage = String.format("The ballot box payloads can not be read, ballotBox: %s", ballotBoxId);
			throw new UncheckedIOException(errorMessage, e);
		}

		for (MixnetShufflePayload payload : payloads) {
			mixnetShufflePayloadFileRepository.savePayload(electionEventId, ballotId, ballotBoxId, payload);
			LOGGER.info("Payload stored for electionEvent:{}, ballot:{}, ballotBox:{}", electionEventId, ballotId, ballotBoxId);
		}
	}

	public void updateBallotBoxStatus(String ballotBoxId) {
		configurationEntityStatusService.update(Status.BB_DOWNLOADED.name(), ballotBoxId, ballotBoxRepository);
	}

	/**
	 * Returns a client for mixed decrypted ballot box in orchestrator context. Used for testing purposes with spy.
	 *
	 * @return the rest client.
	 */
	OrchestratorClient getOrchestratorClient() {
		return orchestratorRestAdapter.create(OrchestratorClient.class);

	}

	/**
	 * Returns a client for the raw ballot box in election information context. Used for testing purposes with spy.
	 *
	 * @return the rest client.
	 */
	ElectionInformationClient getElectionInformationClient() {
		return electionInformationRestAdapter.create(ElectionInformationClient.class);

	}

	/**
	 * Check if the ballot box has the proper status to be downloaded.
	 *
	 * @param ballotBox   the ballotBox json object obtained from DB.
	 * @param ballotBoxId the ballotBox identifier.
	 * @throws BallotBoxDownloadException if the ballot box status is not appropiate for downloading.
	 */
	private void checkBallotBoxStatusForDownload(JsonNode ballotBox, String ballotBoxId) throws BallotBoxDownloadException {
		LOGGER.info("Checking whether ballot box {} can be downloaded...", ballotBoxId);
		Status status = Status.valueOf(ballotBox.get(JsonConstants.STATUS).textValue());
		boolean isSynchronized = Boolean.parseBoolean(ballotBox.get(JsonConstants.SYNCHRONIZED).textValue());
		boolean test = Boolean.parseBoolean(ballotBox.get(JsonConstants.TEST).textValue());

		boolean result = Status.MIXED.equals(status) || (Status.SIGNED.equals(status) && isSynchronized && test);

		if (!result) {
			String errorMessage;
			if (!Status.SIGNED.equals(status)) {
				errorMessage = "Ballot box can not be downloaded because its status is " + status;
			} else {
				errorMessage = "Ballot box can not be downloaded because is not synchronized or it is not test";
			}

			LOGGER.warn(errorMessage);

			throw new BallotBoxDownloadException(errorMessage);
		}

		LOGGER.info("Ballot box {} can be downloaded.", ballotBoxId);
	}

	/**
	 * Retrieves a ballot box and returns its JSON representation
	 *
	 * @param ballotBoxId     the ballot box identifier
	 * @param electionEventId the election event the ballot box belongs to
	 * @return a JSON representation of the ballot box
	 */
	private JsonNode getBallotBoxJson(String ballotBoxId, String electionEventId) throws BallotBoxDownloadException {
		LOGGER.info("Retrieving the JSON representation of ballot box {}...", ballotBoxId);

		// Prepare query
		Map<String, Object> criteria = new HashMap<>();
		criteria.put(JsonConstants.ID, ballotBoxId);
		criteria.put(JsonConstants.ELECTION_EVENT_DOT_ID, electionEventId);
		String json = ballotBoxRepository.list(criteria);

		ObjectMapper mapper = new ObjectMapper();
		JsonNode ballotBoxJsonNode;
		try {
			ballotBoxJsonNode = mapper.readTree(json);
		} catch (JsonProcessingException e) {
			throw new BallotBoxDownloadException("Parsing error of json ballot box", e);
		}

		ArrayNode ballotBoxes = (ArrayNode) ballotBoxJsonNode.get(JsonConstants.RESULT);

		if (ballotBoxes.isEmpty()) {
			LOGGER.info("No ballot boxes found for ballotBoxId {}", ballotBoxId);
			throw new BallotBoxDownloadException("Ballot boxes are empty");
		}

		// The JSON structure is an array with a unique ballotBox.
		return ballotBoxes.get(0);
	}

	private Path getDownloadedBallotBoxPath(final String electionEventId, final String ballotId, final String ballotBoxId) {
		return pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE,
				Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION, Constants.CONFIG_DIR_NAME_BALLOTS, ballotId, Constants.CONFIG_DIR_NAME_BALLOTBOXES,
				ballotBoxId, Constants.CONFIG_FILE_NAME_ELECTION_INFORMATION_DOWNLOADED_BALLOT_BOX);
	}
}
