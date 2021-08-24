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

import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.infrastructure.clients.ElectionInformationClient;
import ch.post.it.evoting.sdm.infrastructure.clients.OrchestratorClient;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Test different scenarios when downloading ballot box
 */
@ExtendWith(MockitoExtension.class)
class BallotBoxDownloadServiceTest {

	@InjectMocks
	@Spy
	private final BallotBoxDownloadService ballotBoxDownloadService = new BallotBoxDownloadService();

	private final String ballotBoxId = "7674245a6382449fb1afedcce1810565";
	private final String ballotBoxJsonLocked =
			"{\"result\": [ { \"id\":\"" + ballotBoxId + "\", \"ballot\":{ \"id\":\"76c70f14cd424cff89a3bfc6d1b6e51c\"} ,\"status\":\""
					+ Status.LOCKED + "\", \"test\":\"" + Boolean.FALSE + "\", \"synchronized\":\"" + Boolean.FALSE + "\"} ]}";
	private final String ballotBoxJsonElectionClosed =
			"{\"result\": [ { \"id\":\"" + ballotBoxId + "\", \"ballot\":{ \"id\":\"76c70f14cd424cff89a3bfc6d1b6e51c\"} ,\"status\":\"" + Status.MIXED
					+ "\", \"test\":\"" + Boolean.FALSE + "\", \"synchronized\":\"" + Boolean.TRUE + "\"} ]}";
	private final String testBallotBoxJson =
			"{\"result\": [ { \"id\":\"" + ballotBoxId + "\", \"ballot\":{ \"id\":\"76c70f14cd424cff89a3bfc6d1b6e51c\"} ,\"status\":\""
					+ Status.SIGNED + "\", \"test\":\"" + Boolean.TRUE + "\", \"synchronized\":\"" + Boolean.TRUE + "\"} ]}";
	private final String ballotBoxJsonEmpty = "{\"result\": []}";
	private final String electionEventId = "1";
	private final String ballotBoxCsv = "10,10;100;100";

	@Mock
	private BallotBoxRepository ballotBoxRepository;

	@Mock
	private ConfigurationEntityStatusService configurationEntityStatusService;

	@Mock
	private PathResolver pathResolver;

	@Mock
	private HashService hashService;

	@Mock
	private OrchestratorClient orchestratorClientMock;

	@Mock
	private ElectionInformationClient electionInformationClientMock;

	@Mock
	private Path pathMock;

	@BeforeEach
	void injectValueAnnotatedFields() {
		ReflectionTestUtils.setField(ballotBoxDownloadService, "tenantId", "tenant");
	}

	@Test
	void whenDownloadEmptyBallotBoxThenException() {
		when(ballotBoxRepository.list(any())).thenReturn(ballotBoxJsonEmpty);

		final BallotBoxDownloadException exception = assertThrows(BallotBoxDownloadException.class,
				() -> ballotBoxDownloadService.download(electionEventId, ballotBoxId));
		assertEquals("Ballot boxes are empty", exception.getMessage());
	}

	@Test
	void whenDownloadBallotBoxWithIncorrectStatusThenException() {
		when(ballotBoxRepository.list(any())).thenReturn(ballotBoxJsonLocked);

		final BallotBoxDownloadException exception = assertThrows(BallotBoxDownloadException.class,
				() -> ballotBoxDownloadService.download(electionEventId, ballotBoxId));
		assertEquals("Ballot box can not be downloaded because its status is LOCKED", exception.getMessage());
	}

	@Test
	void whenDownloadBallotBoxThenGetResponseNotOKFromVP() throws Exception {
		when(ballotBoxRepository.list(any())).thenReturn(ballotBoxJsonElectionClosed);
		doReturn(electionInformationClientMock).when(ballotBoxDownloadService).getElectionInformationClient();
		@SuppressWarnings("unchecked")
		Call<ResponseBody> callMock = mock(Call.class);
		when(callMock.execute())
				.thenReturn(Response.error(500, ResponseBody.create(okhttp3.MediaType.parse(MediaType.APPLICATION_OCTET_STREAM), ballotBoxCsv)));
		doReturn(callMock).when(electionInformationClientMock).getRawBallotBox(anyString(), anyString(), anyString());

		final BallotBoxDownloadException exception = assertThrows(BallotBoxDownloadException.class,
				() -> ballotBoxDownloadService.download(electionEventId, ballotBoxId));
		assertEquals(String.format("Failed to download encrypted BallotBox [electionEvent=%s, ballotBox=%s]: 500", electionEventId, ballotBoxId),
				exception.getMessage());
	}

	@Test
	void whenDownloadClosedBallotBoxThenOk() throws IOException, BallotBoxDownloadException {
		Path targetPath = Files.createTempDirectory("temp").resolve("downloadedBallotBox.csv");

		when(ballotBoxRepository.list(any())).thenReturn(ballotBoxJsonElectionClosed);
		doReturn(electionInformationClientMock).when(ballotBoxDownloadService).getElectionInformationClient();
		@SuppressWarnings("unchecked")
		Call<ResponseBody> callMock = mock(Call.class);
		when(callMock.execute())
				.thenReturn(Response.success(ResponseBody.create(okhttp3.MediaType.parse(MediaType.APPLICATION_OCTET_STREAM), ballotBoxCsv)));
		doReturn(callMock).when(electionInformationClientMock).getRawBallotBox(anyString(), anyString(), anyString());
		when(pathResolver.resolve(any())).thenReturn(targetPath);

		ballotBoxDownloadService.download(electionEventId, ballotBoxId);

		assertTrue(Files.exists(targetPath));
	}

	@Test
	void whenDownloadingTestBallotBoxThenOk() throws IOException, BallotBoxDownloadException {
		Path targetPath = Files.createTempDirectory("temp").resolve("downloadedBallotBox.csv");

		when(ballotBoxRepository.list(any())).thenReturn(testBallotBoxJson);
		doReturn(electionInformationClientMock).when(ballotBoxDownloadService).getElectionInformationClient();
		@SuppressWarnings("unchecked")
		Call<ResponseBody> callMock = mock(Call.class);
		when(callMock.execute())
				.thenReturn(Response.success(ResponseBody.create(okhttp3.MediaType.parse(MediaType.APPLICATION_OCTET_STREAM), ballotBoxCsv)));
		doReturn(callMock).when(electionInformationClientMock).getRawBallotBox(anyString(), anyString(), anyString());
		when(pathResolver.resolve(any())).thenReturn(targetPath);

		ballotBoxDownloadService.download(electionEventId, ballotBoxId);

		assertTrue(Files.exists(targetPath));
	}

	@Test
	void whenDownloadingBallotBoxToWrongPathThenFailWriting() throws IOException {
		Path wrongPath = Paths.get("/wrongpath/downloadedBallotBox.csv");

		when(ballotBoxRepository.list(any())).thenReturn(ballotBoxJsonElectionClosed);
		doReturn(electionInformationClientMock).when(ballotBoxDownloadService).getElectionInformationClient();
		@SuppressWarnings("unchecked")
		Call<ResponseBody> callMock = mock(Call.class);
		when(callMock.execute())
				.thenReturn(Response.success(ResponseBody.create(okhttp3.MediaType.parse(MediaType.APPLICATION_OCTET_STREAM), ballotBoxCsv)));
		doReturn(callMock).when(electionInformationClientMock).getRawBallotBox(anyString(), anyString(), anyString());
		when(pathResolver.resolve(any())).thenReturn(wrongPath);

		final BallotBoxDownloadException exception = assertThrows(BallotBoxDownloadException.class,
				() -> ballotBoxDownloadService.download(electionEventId, ballotBoxId));
		assertEquals(String.format("Failed to write downloaded BallotBox [electionEvent=%s, ballotBox=%s] to file.", electionEventId, ballotBoxId),
				exception.getMessage());
	}
}
