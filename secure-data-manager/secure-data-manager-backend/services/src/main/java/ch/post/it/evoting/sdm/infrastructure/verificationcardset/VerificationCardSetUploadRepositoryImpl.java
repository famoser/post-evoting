/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.verificationcardset;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import javax.json.JsonObject;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import ch.post.it.evoting.sdm.domain.model.verification.VerificationCardSetDataUploadRepository;
import ch.post.it.evoting.sdm.infrastructure.InputStreamTypedOutput;
import ch.post.it.evoting.sdm.infrastructure.RestClientService;
import ch.post.it.evoting.sdm.infrastructure.clients.VoteVerificationClient;
import ch.post.it.evoting.sdm.infrastructure.exception.VerificationCardSetUploadRepositoryException;

import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * implementation of the repository using a REST CLIENT
 */
@Repository
public class VerificationCardSetUploadRepositoryImpl implements VerificationCardSetDataUploadRepository {

	private static final MediaType TEXT_CSV_TYPE = new MediaType("text", "csv");

	final VoteVerificationClient voteVerificationClient;
	@Value("${tenantID}")
	private String tenantId;

	@Autowired
	public VerificationCardSetUploadRepositoryImpl(
			@Value("${VV_URL}")
					String voteVerificationURL,
			@Value("${connection.time.out}")
					String connectionTimeOut,
			@Value("${read.time.out}")
					String readTimeOut,
			@Value("${write.time.out}")
					String writeTimeOut) {
		setTimeouts(connectionTimeOut, readTimeOut, writeTimeOut);
		voteVerificationClient = getVoteVerificationClient(voteVerificationURL);
	}

	@Override
	public void uploadCodesMapping(String electionEventId, String verificationCardSetId, String adminBoardId, InputStream stream) throws IOException {
		final InputStreamTypedOutput body = new InputStreamTypedOutput(TEXT_CSV_TYPE.toString(), stream);

		final Response<ResponseBody> response = voteVerificationClient
				.saveCodesMappingData(tenantId, electionEventId, verificationCardSetId, adminBoardId, body).execute();

		handleErrorResponse(response, "Failed to upload codes mapping: ");
	}

	@Override
	public void uploadVerificationCardData(String electionEventId, String verificationCardSetId, String adminBoardId, InputStream stream)
			throws IOException {

		final InputStreamTypedOutput body = new InputStreamTypedOutput(TEXT_CSV_TYPE.toString(), stream);

		final Response<ResponseBody> response = voteVerificationClient
				.saveVerificationCardData(tenantId, electionEventId, verificationCardSetId, adminBoardId, body).execute();

		handleErrorResponse(response, "Failed to upload verification card data: ");
	}

	@Override
	public void uploadVerificationCardSetData(String electionEventId, String verificationCardSetId, String adminBoardId,
			JsonObject verificationCardSetData) throws IOException {

		final InputStreamTypedOutput body = new InputStreamTypedOutput(MediaType.APPLICATION_JSON,
				new ByteArrayInputStream(verificationCardSetData.toString().getBytes(StandardCharsets.UTF_8)));

		final Response<ResponseBody> response = voteVerificationClient
				.saveVerificationCardSetData(tenantId, electionEventId, verificationCardSetId, adminBoardId, body).execute();

		handleErrorResponse(response, "Failed to upload verification card set data: ");
	}

	@Override
	public void uploadVerificationCardDerivedKeys(String electionEventId, String verificationCardSetId, String adminBoardId, InputStream stream)
			throws IOException {

		final InputStreamTypedOutput body = new InputStreamTypedOutput(TEXT_CSV_TYPE.toString(), stream);

		final Response<ResponseBody> response = voteVerificationClient
				.saveVerificationCardDerivedKeys(tenantId, electionEventId, verificationCardSetId, adminBoardId, body).execute();

		handleErrorResponse(response, "Failed to upload verification card derived keys: ");
	}

	private VoteVerificationClient getVoteVerificationClient(final String voteVerificationURL) {
		Retrofit restAdapter = RestClientService.getInstance().getRestClientWithJacksonConverter(voteVerificationURL);
		return restAdapter.create(VoteVerificationClient.class);
	}

	private void setTimeouts(String connectionTimeOut, String readTimeOut, String writeTimeOut) {
		System.setProperty("connection.time.out", connectionTimeOut);
		System.setProperty("read.time.out", readTimeOut);
		System.setProperty("write.time.out", writeTimeOut);
	}

	/**
	 * Handles response in case it is not successful.
	 *
	 * @param response     the response to handle.
	 * @param errorMessage an error message in case the response is not successful.
	 * @throws VerificationCardSetUploadRepositoryException with {@code errorMessage} if the response is not successful.
	 */
	private void handleErrorResponse(final Response<ResponseBody> response, final String errorMessage) {
		if (!response.isSuccessful()) {
			final String errorBodyString;
			try {
				errorBodyString = response.errorBody().string();
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to convert response body to string.", e);
			}
			throw new VerificationCardSetUploadRepositoryException(String.format("%s%s", errorMessage, errorBodyString));
		}
	}

}
