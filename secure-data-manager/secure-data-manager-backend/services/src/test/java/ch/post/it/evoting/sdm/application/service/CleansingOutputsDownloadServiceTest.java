/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.infrastructure.clients.ElectionInformationClient;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

@ExtendWith(MockitoExtension.class)
class CleansingOutputsDownloadServiceTest {

	private final String ballotBoxId = "7674245a6382449fb1afedcce1810565";
	private final String ballotId = "76c70f14cd424cff89a3bfc6d1b6e51c";
	private final String ballotBoxJsonElectionMixed =
			"{\"result\": [ { \"id\":\"" + ballotBoxId + "\", \"ballot\":{ \"id\":\"76c70f14cd424cff89a3bfc6d1b6e51c\"} ,\"status\":\"" + Status.MIXED
					+ "\", \"test\":\"" + Boolean.FALSE + "\", \"synchronized\":\"" + Boolean.TRUE + "\"} ]}";
	private final String electionEventId = "1";
	private final String ballotBoxCsv = "10,10;100;100";
	@InjectMocks
	@Spy
	private final CleansingOutputsDownloadService cleansingOutputsDownloadService = new CleansingOutputsDownloadService();
	@Mock
	private BallotBoxRepository ballotBoxRepository;
	@Mock
	private ConfigurationEntityStatusService configurationEntityStatusService;
	@Mock
	private PathResolver pathResolver;
	@Mock
	private ElectionInformationClient clientMock;

	@Mock
	private Path pathMock;

	@BeforeEach
	public void injectValueAnnotatedFields() {
		ReflectionTestUtils.setField(cleansingOutputsDownloadService, "tenantId", "tenant");
	}

	@Test
	void whenDownloadSuccessfulVoteAndResponseNotOKFromVPThenException() throws Exception {
		ElectionInformationClient clientMock = mock(ElectionInformationClient.class);

		when(ballotBoxRepository.list(any())).thenReturn(ballotBoxJsonElectionMixed);
		doReturn(clientMock).when(cleansingOutputsDownloadService).getElectionInformationClient();
		@SuppressWarnings("unchecked")
		Call<ResponseBody> callMock = mock(Call.class);
		when(callMock.execute())
				.thenReturn(Response.error(500, ResponseBody.create(okhttp3.MediaType.parse(MediaType.APPLICATION_OCTET_STREAM), ballotBoxCsv)));
		doReturn(callMock).when(clientMock).downloadSuccessfulVotes(anyString(), anyString(), anyString());

		final CleansingOutputsDownloadException exception = assertThrows(CleansingOutputsDownloadException.class,
				() -> cleansingOutputsDownloadService.download(electionEventId, ballotBoxId));
		assertEquals(
				String.format("Failed to download successful votes [electionEvent=%s, ballotBox=%s]: %s", electionEventId, ballotBoxId, ballotBoxCsv),
				exception.getMessage());
	}

	@Test
	void whenDownloadSuccessfulVotesThenOK() throws IOException, CleansingOutputsDownloadException {
		ElectionInformationClient clientMock = mock(ElectionInformationClient.class);
		Path successfulVotesPath = Files.createTempDirectory("temp").resolve(Constants.CONFIG_FILE_NAME_SUCCESSFUL_VOTES);

		when(ballotBoxRepository.list(any())).thenReturn(ballotBoxJsonElectionMixed);
		doReturn(clientMock).when(cleansingOutputsDownloadService).getElectionInformationClient();
		@SuppressWarnings("unchecked")
		Call<ResponseBody> callMock = mock(Call.class);
		when(callMock.execute())
				.thenReturn(Response.success(ResponseBody.create(okhttp3.MediaType.parse(MediaType.APPLICATION_OCTET_STREAM), ballotBoxCsv)));
		doReturn(callMock).when(clientMock).downloadSuccessfulVotes(anyString(), anyString(), anyString());
		doReturn(callMock).when(clientMock).downloadFailedVotes(anyString(), anyString(), anyString());
		when(pathResolver.resolve(any())).thenReturn(successfulVotesPath);

		cleansingOutputsDownloadService.download(electionEventId, ballotBoxId);

		assertTrue(Files.exists(successfulVotesPath));
	}

	@Test
	void whenWritingSuccessfulVotesToWrongPathThenException() throws IOException {
		Path wrongPath = Paths.get("/wrongpath/successfulVotes.csv");

		when(ballotBoxRepository.list(any())).thenReturn(ballotBoxJsonElectionMixed);
		doReturn(clientMock).when(cleansingOutputsDownloadService).getElectionInformationClient();
		@SuppressWarnings("unchecked")
		Call<ResponseBody> callMock = mock(Call.class);
		when(callMock.execute())
				.thenReturn(Response.success(ResponseBody.create(okhttp3.MediaType.parse(MediaType.APPLICATION_OCTET_STREAM), ballotBoxCsv)));
		doReturn(callMock).when(clientMock).downloadSuccessfulVotes(anyString(), anyString(), anyString());
		when(pathResolver.resolve(any())).thenReturn(wrongPath);

		final CleansingOutputsDownloadException exception = assertThrows(CleansingOutputsDownloadException.class,
				() -> cleansingOutputsDownloadService.download(electionEventId, ballotBoxId));
		assertEquals(String.format("Failed to write successful votes [electionEvent=%s, ballotBox=%s]", electionEventId, ballotBoxId),
				exception.getMessage());
	}

	@Test
	void whenWritingFailedVotesToWrongPathThenException() throws IOException {
		Path failedVotesWrongPath = Paths.get("/wrongpath/successfulVotes.csv");
		Path successfulVotesCorrectPAth = Files.createTempDirectory("temp").resolve(Constants.CONFIG_FILE_NAME_SUCCESSFUL_VOTES);

		when(ballotBoxRepository.list(any())).thenReturn(ballotBoxJsonElectionMixed);
		doReturn(clientMock).when(cleansingOutputsDownloadService).getElectionInformationClient();
		@SuppressWarnings("unchecked")
		Call<ResponseBody> callMock = mock(Call.class);
		when(callMock.execute())
				.thenReturn(Response.success(ResponseBody.create(okhttp3.MediaType.parse(MediaType.APPLICATION_OCTET_STREAM), ballotBoxCsv)));
		doReturn(callMock).when(clientMock).downloadSuccessfulVotes(anyString(), anyString(), anyString());
		doReturn(callMock).when(clientMock).downloadFailedVotes(anyString(), anyString(), anyString());
		when(pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE,
				Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION, Constants.CONFIG_DIR_NAME_BALLOTS, ballotId, Constants.CONFIG_DIR_NAME_BALLOTBOXES,
				ballotBoxId, Constants.CONFIG_FILE_NAME_SUCCESSFUL_VOTES)).thenReturn(successfulVotesCorrectPAth);
		when(pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE,
				Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION, Constants.CONFIG_DIR_NAME_BALLOTS, ballotId, Constants.CONFIG_DIR_NAME_BALLOTBOXES,
				ballotBoxId, Constants.CONFIG_FILE_NAME_FAILED_VOTES)).thenReturn(failedVotesWrongPath);

		final CleansingOutputsDownloadException exception = assertThrows(CleansingOutputsDownloadException.class,
				() -> cleansingOutputsDownloadService.download(electionEventId, ballotBoxId));
		assertEquals(String.format("Failed to write failed votes [electionEvent=%s, ballotBox=%s]", electionEventId, ballotBoxId),
				exception.getMessage());
	}
}
