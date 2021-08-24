/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.votingcardset;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import ch.post.it.evoting.sdm.domain.model.votingcardset.VotingCardSetUploadRepository;
import ch.post.it.evoting.sdm.infrastructure.InputStreamTypedOutput;
import ch.post.it.evoting.sdm.infrastructure.RestClientService;
import ch.post.it.evoting.sdm.infrastructure.clients.ExtendedAuthenticationClient;
import ch.post.it.evoting.sdm.infrastructure.clients.VoterMaterialClient;
import ch.post.it.evoting.sdm.infrastructure.exception.VotingCardSetUploadRepositoryException;

import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * implementation of the repository using a REST CLIENT
 */
@Repository
public class VotingCardSetUploadRepositoryImpl implements VotingCardSetUploadRepository {

	private static final MediaType TEXT_CSV_TYPE = new MediaType("text", "csv");

	private final VoterMaterialClient voterMaterialClient;
	private final ExtendedAuthenticationClient extendedAuthenticationClient;
	@Value("${tenantID}")
	private String tenantId;

	@Autowired
	public VotingCardSetUploadRepositoryImpl(
			@Value("${VM_URL}")
					String voterMaterialURL,
			@Value("${EA_URL}")
					String extendedAuthenticationURL,
			@Value("${connection.time.out}")
					String connectionTimeOut,
			@Value("${read.time.out}")
					String readTimeOut,
			@Value("${write.time.out}")
					String writeTimeOut) {

		setTimeouts(connectionTimeOut, readTimeOut, writeTimeOut);
		voterMaterialClient = getVoterMaterialClient(voterMaterialURL);
		extendedAuthenticationClient = getExtendedAuthenticationClient(extendedAuthenticationURL);
	}

	@Override
	public void uploadVoterInformation(final String electionEventId, final String votingCardSetId, final String adminBoardId,
			final InputStream stream) throws IOException {
		final InputStreamTypedOutput body = new InputStreamTypedOutput(TEXT_CSV_TYPE.toString(), stream);

		final Response<ResponseBody> response = voterMaterialClient
				.saveVoterInformationData(tenantId, electionEventId, votingCardSetId, adminBoardId, body).execute();

		handleErrorResponse(response, "Failed to upload voter information: ");
	}

	@Override
	public void uploadCredentialData(final String electionEventId, final String votingCardSetId, final String adminBoardId, final InputStream stream)
			throws IOException {

		final InputStreamTypedOutput body = new InputStreamTypedOutput(TEXT_CSV_TYPE.toString(), stream);

		final Response<ResponseBody> response = voterMaterialClient.saveCredentialData(tenantId, electionEventId, votingCardSetId, adminBoardId, body)
				.execute();

		handleErrorResponse(response, "Failed to upload credential data: ");
	}

	@Override
	public void uploadExtendedAuthData(final String electionEventId, final String adminBoardId, final InputStream stream) throws IOException {
		final InputStreamTypedOutput body = new InputStreamTypedOutput(TEXT_CSV_TYPE.toString(), stream);

		final Response<ResponseBody> response = extendedAuthenticationClient
				.saveExtendedAuthenticationData(tenantId, electionEventId, adminBoardId, body).execute();

		handleErrorResponse(response, "Failed to upload extended authentication data: ");
	}

	private VoterMaterialClient getVoterMaterialClient(final String voterMaterialURL) {
		Retrofit restAdapter = RestClientService.getInstance().getRestClientWithJacksonConverter(voterMaterialURL);
		return restAdapter.create(VoterMaterialClient.class);
	}

	private ExtendedAuthenticationClient getExtendedAuthenticationClient(final String extendedAuthenticationURL) {
		Retrofit restAdapter = RestClientService.getInstance().getRestClientWithJacksonConverter(extendedAuthenticationURL);
		return restAdapter.create(ExtendedAuthenticationClient.class);
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
	 * @throws VotingCardSetUploadRepositoryException with {@code errorMessage} if the response is not successful.
	 */
	private void handleErrorResponse(final Response<ResponseBody> response, final String errorMessage) {
		if (!response.isSuccessful()) {
			final String errorBodyString;
			try {
				errorBodyString = response.errorBody().string();
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to convert response body to string.", e);
			}
			throw new VotingCardSetUploadRepositoryException(String.format("%s%s", errorMessage, errorBodyString));
		}
	}

}
