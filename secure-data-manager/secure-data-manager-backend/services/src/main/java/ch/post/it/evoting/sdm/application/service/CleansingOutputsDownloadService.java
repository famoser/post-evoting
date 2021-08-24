/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

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
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.infrastructure.RestClientService;
import ch.post.it.evoting.sdm.infrastructure.clients.ElectionInformationClient;
import ch.post.it.evoting.sdm.utils.JsonUtils;

import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;

@Service
public class CleansingOutputsDownloadService {

	@Value("${tenantID}")
	private String tenantId;

	@Value("${EI_URL}")
	private String eiBaseURL;

	@Autowired
	private PathResolver pathResolver;

	@Autowired
	private BallotBoxRepository ballotBoxRepository;

	@Autowired
	private KeyStoreService keystoreService;

	private Retrofit restAdapter;

	@PostConstruct
	void configure() {
		PrivateKey requestSigningKey = keystoreService.getPrivateKey();
		restAdapter = RestClientService.getInstance()
				.getRestClientWithInterceptorAndJacksonConverter(eiBaseURL, requestSigningKey, "SECURE_DATA_MANAGER");
	}

	/**
	 * Download the cleansing outputs of a ballot box based on election event id and its id.
	 *
	 * @param electionEventId the election event id.
	 * @param ballotBoxId     the ballot box id.
	 * @throws CleansingOutputsDownloadException if something fails when downloading the cleansing outputs.
	 */
	public void download(final String electionEventId, final String ballotBoxId) throws CleansingOutputsDownloadException {
		Map<String, Object> criteria = new HashMap<>();
		criteria.put(JsonConstants.ID, ballotBoxId);
		criteria.put(JsonConstants.ELECTION_EVENT_DOT_ID, electionEventId);
		String json = ballotBoxRepository.list(criteria);
		JsonArray ballotBoxes = JsonUtils.getJsonObject(json).getJsonArray(JsonConstants.RESULT);
		JsonObject ballotBox = ballotBoxes.getJsonObject(0);
		String ballotId = ballotBox.getJsonObject(JsonConstants.BALLOT).getString(JsonConstants.ID);

		ResponseBody successfulVotesResponseBody = downloadSuccessfulVotes(electionEventId, ballotBoxId);

		writeSuccessfulVotesStreamToFile(electionEventId, ballotId, ballotBoxId, successfulVotesResponseBody);

		ResponseBody failedVotesResponseBody = downloadFailedVotes(electionEventId, ballotBoxId);

		writeFailedVotesStreamToFile(electionEventId, ballotId, ballotBoxId, failedVotesResponseBody);
	}

	/**
	 * Download successful votes from election information context.
	 *
	 * @param electionEventId the election event identifier.
	 * @param ballotBoxId     the ballot box identifier.
	 * @throws CleansingOutputsDownloadException if the download of successful votes fails.
	 */
	private ResponseBody downloadSuccessfulVotes(String electionEventId, String ballotBoxId) throws CleansingOutputsDownloadException {
		final Response<ResponseBody> response;
		try {
			response = getElectionInformationClient().downloadSuccessfulVotes(tenantId, electionEventId, ballotBoxId).execute();
		} catch (final IOException e) {
			throw new UncheckedIOException("Failed to communicate with election-information.", e);
		}

		if (!response.isSuccessful()) {
			final String errorBodyString;
			try {
				errorBodyString = response.errorBody().string();
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to convert response body to string.", e);
			}
			final String error = String
					.format("Failed to download successful votes [electionEvent=%s, ballotBox=%s]: %s", electionEventId, ballotBoxId,
							errorBodyString);
			throw new CleansingOutputsDownloadException(error);
		}

		return response.body();
	}

	/**
	 * Download failed votes from election information context.
	 *
	 * @param electionEventId the election event identifier.
	 * @param ballotBoxId     the ballot box identifier.
	 * @throws CleansingOutputsDownloadException if the download of failed votes fails.
	 */
	private ResponseBody downloadFailedVotes(String electionEventId, String ballotBoxId) throws CleansingOutputsDownloadException {
		final Response<ResponseBody> response;
		try {
			response = getElectionInformationClient().downloadFailedVotes(tenantId, electionEventId, ballotBoxId).execute();
		} catch (final IOException e) {
			throw new UncheckedIOException("Failed to communicate with election-information.", e);
		}

		if (!response.isSuccessful()) {
			final String errorBodyString;
			try {
				errorBodyString = response.errorBody().string();
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to convert response body to string.", e);
			}
			final String error = String
					.format("Failed to download failed votes [electionEvent=%s, ballotBox=%s]: %s", electionEventId, ballotBoxId, errorBodyString);
			throw new CleansingOutputsDownloadException(error);
		}

		return response.body();
	}

	/**
	 * Extracts the successful votes bytes from the stream and write them to temp file.
	 *
	 * @param responseBody the stream containing the bsuccessful vote raw bytes
	 */
	private void writeSuccessfulVotesStreamToFile(String electionEventId, String ballotId, String ballotBoxId, ResponseBody responseBody)
			throws CleansingOutputsDownloadException {
		try (InputStream stream = responseBody.byteStream()) {
			Path ballotBoxFile = getDownloadedSuccessfulVotesPath(electionEventId, ballotId, ballotBoxId);
			Files.copy(stream, ballotBoxFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			String errorMessage = String.format("Failed to write successful votes [electionEvent=%s, ballotBox=%s]", electionEventId, ballotBoxId);
			throw new CleansingOutputsDownloadException(errorMessage, e);
		}
	}

	/**
	 * Extracts the failed votes bytes from the stream and write them to temp file.
	 *
	 * @param responseBody the stream containing the failed votes raw bytes
	 */
	private void writeFailedVotesStreamToFile(String electionEventId, String ballotId, String ballotBoxId, ResponseBody responseBody)
			throws CleansingOutputsDownloadException {
		try (InputStream stream = responseBody.byteStream()) {
			Path ballotBoxFile = getDownloadedFailedVotesPath(electionEventId, ballotId, ballotBoxId);
			Files.copy(stream, ballotBoxFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			String errorMessage = String.format("Failed to write failed votes [electionEvent=%s, ballotBox=%s]", electionEventId, ballotBoxId);
			throw new CleansingOutputsDownloadException(errorMessage, e);
		}
	}

	private Path getDownloadedSuccessfulVotesPath(final String electionEventId, final String ballotId, final String ballotBoxId) {
		return pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE,
				Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION, Constants.CONFIG_DIR_NAME_BALLOTS, ballotId, Constants.CONFIG_DIR_NAME_BALLOTBOXES,
				ballotBoxId, Constants.CONFIG_FILE_NAME_SUCCESSFUL_VOTES);
	}

	private Path getDownloadedFailedVotesPath(final String electionEventId, final String ballotId, final String ballotBoxId) {
		return pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE,
				Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION, Constants.CONFIG_DIR_NAME_BALLOTS, ballotId, Constants.CONFIG_DIR_NAME_BALLOTBOXES,
				ballotBoxId, Constants.CONFIG_FILE_NAME_FAILED_VOTES);
	}

	/**
	 * Returns a client for election information.
	 *
	 * @return the rest client.
	 */
	ElectionInformationClient getElectionInformationClient() {
		return restAdapter.create(ElectionInformationClient.class);
	}

}
