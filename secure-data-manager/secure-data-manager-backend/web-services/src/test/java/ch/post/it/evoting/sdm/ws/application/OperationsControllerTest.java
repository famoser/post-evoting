/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.ws.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.cms.CMSException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.sdm.application.exception.ConsistencyCheckException;
import ch.post.it.evoting.sdm.application.service.ElectionEventService;
import ch.post.it.evoting.sdm.application.service.ExportImportService;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.operation.OperationResult;
import ch.post.it.evoting.sdm.domain.model.operation.OperationsData;
import ch.post.it.evoting.sdm.domain.model.operation.OperationsOutputCode;
import ch.post.it.evoting.sdm.plugin.ExecutionListener;
import ch.post.it.evoting.sdm.plugin.PhaseName;
import ch.post.it.evoting.sdm.plugin.ResultCode;
import ch.post.it.evoting.sdm.plugin.SequentialExecutor;

@ExtendWith(MockitoExtension.class)
class OperationsControllerTest {

	private static final String TEST_PATH = "testPath";
	private static final String BASE64_PRIVATE_KEY = "NDQ0NDQ0NDQ0NDQ0NA";
	private static final String ERROR = "Error";
	private static final String BALLOT_BOX_STATUS = "ballotBoxStatus";
	private static final String BB_DOWNLOADED = "bb_downloaded";
	private static final String ELECTION_EVENT_ALIAS = "eeAlias";
	private static final String ELECTION_EVENT_ID = "a9d805a0d9ef4deeb4b14838f7e8ed8a";
	private static final String INVALID_ELECTION_EVENT_ID = "invalid-ee-id";

	@InjectMocks
	@Spy
	OperationsController operationsController = new OperationsControllerForTest();

	@Mock
	private ExportImportService exportImportService;

	@Mock
	private ElectionEventService electionEventService;

	@Mock
	private PathResolver pathResolver;

	@Mock
	private Path pathMock;

	@Mock
	private SequentialExecutor sequentialExecutor;

	@Test
	void testExportOperationPathParameterIsRequired() {

		OperationsData requestBody = new OperationsData();

		ResponseEntity<OperationResult> exportOperation = operationsController.exportOperation(ELECTION_EVENT_ID, requestBody);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exportOperation.getStatusCode());
		assertEquals(exportOperation.getBody().getError(), OperationsOutputCode.MISSING_PARAMETER.value());
	}

	@Test
	void testExportOperationElectionEventNotFound() {
		String electionEventId = "17ccbe962cf341bc93208c26e911090c";
		OperationsData requestBody = new OperationsData();
		requestBody.setPath(TEST_PATH);

		ResponseEntity<OperationResult> exportOperation = operationsController.exportOperation(electionEventId, requestBody);

		assertEquals(HttpStatus.NOT_FOUND, exportOperation.getStatusCode());
		assertEquals(exportOperation.getBody().getError(), OperationsOutputCode.MISSING_PARAMETER.value());
	}

	@Test
	void testExportOperationWhenExportElectionEventError() throws IOException {
		OperationsData requestBody = new OperationsData();
		requestBody.setPath(TEST_PATH);
		requestBody.setElectionEventData(true);

		when(pathResolver.resolve(anyString())).thenReturn(pathMock);

		doThrow(new IOException("Export Election Event Error")).when(exportImportService)
				.exportElectionEventWithoutElectionInformation(anyString(), anyString(), anyString());

		when(electionEventService.getElectionEventAlias(anyString())).thenReturn("electionEventAlias");

		ResponseEntity<OperationResult> exportOperation = operationsController.exportOperation(ELECTION_EVENT_ID, requestBody);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exportOperation.getStatusCode());
		assertEquals(exportOperation.getBody().getError(), OperationsOutputCode.ERROR_IO_OPERATIONS.value());
	}

	@Test
	void testExportOperationHappyPath() {
		OperationsData requestBody = new OperationsData();
		requestBody.setPath(TEST_PATH);

		when(electionEventService.getElectionEventAlias(ELECTION_EVENT_ID)).thenReturn(ELECTION_EVENT_ALIAS);
		when(pathResolver.resolve(anyString())).thenReturn(pathMock);

		ResponseEntity<OperationResult> exportOperation = operationsController.exportOperation(ELECTION_EVENT_ID, requestBody);

		assertEquals(HttpStatus.OK, exportOperation.getStatusCode());
		assertNull(exportOperation.getBody());
	}

	@Test
	void testGeneratePreVotingOutputsOperationPrivateKeyInBase64Missing() {
		OperationsData requestBody = new OperationsData();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> operationsController.generatePreVotingOutputsOperation(ELECTION_EVENT_ID, requestBody));
		assertEquals("PrivateKey parameter is required", exception.getMessage());
	}

	@Test
	void testGeneratePreVotingOutputsOperationWhenCallbackError() {
		OperationsData requestBody = new OperationsData();
		requestBody.setPrivateKeyInBase64(BASE64_PRIVATE_KEY);

		when(electionEventService.getElectionEventAlias(ELECTION_EVENT_ID)).thenReturn(ELECTION_EVENT_ALIAS);
		when(pathResolver.resolve(anyString())).thenReturn(pathMock);

		doAnswer((Answer<ExecutionListener>) invocation -> {
			Object[] args = invocation.getArguments();
			ExecutionListener callback = (ExecutionListener) args[2];
			callback.onError(ResultCode.GENERAL_ERROR.value());
			callback.onMessage(ERROR);
			return null;
		}).when(sequentialExecutor).execute(any(), any(), any(ExecutionListener.class));

		ResponseEntity<OperationResult> preVotingOutputs = operationsController.generatePreVotingOutputsOperation(ELECTION_EVENT_ID, requestBody);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, preVotingOutputs.getStatusCode());
		assertEquals(preVotingOutputs.getBody().getError(), ResultCode.GENERAL_ERROR.value());
	}

	@Test
	void testGeneratePreVotingOutputsHappyPath() {
		OperationsData requestBody = new OperationsData();
		requestBody.setPrivateKeyInBase64(BASE64_PRIVATE_KEY);

		when(electionEventService.getElectionEventAlias(ELECTION_EVENT_ID)).thenReturn(ELECTION_EVENT_ALIAS);
		when(pathResolver.resolve(anyString())).thenReturn(pathMock);

		ResponseEntity<OperationResult> preVotingOutputs = operationsController.generatePreVotingOutputsOperation(ELECTION_EVENT_ID, requestBody);

		assertEquals(HttpStatus.OK, preVotingOutputs.getStatusCode());
		assertNull(preVotingOutputs.getBody());
	}

	@Test
	void testGeneratePostVotingOutputsOperationPrivateKeyInBase64Missing() {
		OperationsData requestBody = new OperationsData();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> operationsController.generatePostVotingOutputsOperation(ELECTION_EVENT_ID, BALLOT_BOX_STATUS, requestBody));
		assertEquals("PrivateKey parameter is required", exception.getMessage());
	}

	@Test
	void testGeneratePostVotingOutputsOperationErrorStatusNameNoEnumConstant() {
		OperationsData requestBody = new OperationsData();
		requestBody.setPrivateKeyInBase64(BASE64_PRIVATE_KEY);

		ResponseEntity<OperationResult> postVotingOutputs = operationsController
				.generatePostVotingOutputsOperation(ELECTION_EVENT_ID, BALLOT_BOX_STATUS, requestBody);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, postVotingOutputs.getStatusCode());
		assertEquals(postVotingOutputs.getBody().getError(), OperationsOutputCode.ERROR_STATUS_NAME.value());
	}

	@Test
	void testGeneratePostVotingOutputsOperationStatusNameErrorPhaseNameNull() {
		String ballotBoxStatus = "new";
		OperationsData requestBody = new OperationsData();
		requestBody.setPrivateKeyInBase64(BASE64_PRIVATE_KEY);

		ResponseEntity<OperationResult> postVotingOutputs = operationsController
				.generatePostVotingOutputsOperation(ELECTION_EVENT_ID, ballotBoxStatus, requestBody);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, postVotingOutputs.getStatusCode());
		assertEquals(postVotingOutputs.getBody().getError(), OperationsOutputCode.ERROR_STATUS_NAME.value());
	}

	@Test
	void testGeneratePostVotingOutputsOperationWhenCallbackError() {
		OperationsData requestBody = new OperationsData();
		requestBody.setPrivateKeyInBase64(BASE64_PRIVATE_KEY);

		when(electionEventService.getElectionEventAlias(ELECTION_EVENT_ID)).thenReturn(ELECTION_EVENT_ALIAS);
		when(pathResolver.resolve(anyString())).thenReturn(pathMock);

		doAnswer((Answer<ExecutionListener>) invocation -> {
			Object[] args = invocation.getArguments();
			ExecutionListener callback = (ExecutionListener) args[2];
			callback.onError(ResultCode.GENERAL_ERROR.value());
			callback.onMessage(ERROR);
			return null;
		}).when(sequentialExecutor).execute(any(), any(), any(ExecutionListener.class));

		ResponseEntity<OperationResult> postVotingOutputs = operationsController
				.generatePostVotingOutputsOperation(ELECTION_EVENT_ID, BB_DOWNLOADED, requestBody);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, postVotingOutputs.getStatusCode());
		assertEquals(postVotingOutputs.getBody().getError(), ResultCode.GENERAL_ERROR.value());
	}

	@Test
	void testGeneratePostVotingOutputsOperationHappyPath() {
		OperationsData requestBody = new OperationsData();
		requestBody.setPrivateKeyInBase64(BASE64_PRIVATE_KEY);

		when(electionEventService.getElectionEventAlias(ELECTION_EVENT_ID)).thenReturn(ELECTION_EVENT_ALIAS);
		when(pathResolver.resolve(anyString())).thenReturn(pathMock);

		ResponseEntity<OperationResult> postVotingOutputs = operationsController
				.generatePostVotingOutputsOperation(ELECTION_EVENT_ID, BB_DOWNLOADED, requestBody);

		assertEquals(HttpStatus.OK, postVotingOutputs.getStatusCode());
		assertNull(postVotingOutputs.getBody());
	}

	@Test
	void testGeneratePostVotingOutputsOperationInvalidEEId() {
		OperationsData requestBody = new OperationsData();
		requestBody.setPrivateKeyInBase64(BASE64_PRIVATE_KEY);

		ResponseEntity<OperationResult> postVotingOutputs = operationsController
				.generatePostVotingOutputsOperation(ELECTION_EVENT_ID, BB_DOWNLOADED, requestBody);

		assertEquals(HttpStatus.NOT_FOUND, postVotingOutputs.getStatusCode());
		assertEquals(postVotingOutputs.getBody().getError(), OperationsOutputCode.MISSING_PARAMETER.value());
	}

	@Test
	void testImportOperationPathParameterMissing() {
		OperationsData requestBody = new OperationsData();

		ResponseEntity<OperationResult> importOperation = operationsController.importOperation(requestBody);

		assertEquals(HttpStatus.BAD_REQUEST, importOperation.getStatusCode());
		assertEquals(OperationsOutputCode.MISSING_PARAMETER.value(), importOperation.getBody().getError());
	}

	@Test
	void testImportOperationLoadDatabaseError()
			throws IOException, CertificateException, ConsistencyCheckException, GeneralCryptoLibException, CMSException {
		OperationsData requestBody = new OperationsData();
		requestBody.setPath(TEST_PATH);

		doThrow(new IOException(ERROR)).when(exportImportService).importDatabase();

		ResponseEntity<OperationResult> importOperation = operationsController.importOperation(requestBody);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, importOperation.getStatusCode());
		assertEquals(importOperation.getBody().getError(), OperationsOutputCode.ERROR_IO_OPERATIONS.value());
	}

	@Test
	void testImportOperationWhenCallBackError() throws IOException {
		OperationsData requestBody = new OperationsData();
		requestBody.setPath(TEST_PATH);

		doThrow(new IOException(ERROR)).when(exportImportService).importData(anyString());

		ResponseEntity<OperationResult> importOpertaion = operationsController.importOperation(requestBody);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, importOpertaion.getStatusCode());
		assertEquals(importOpertaion.getBody().getError(), OperationsOutputCode.ERROR_IO_OPERATIONS.value());
	}

	@Test
	void testImportOperationConsistencyCheckError()
			throws CertificateException, ConsistencyCheckException, GeneralCryptoLibException, CMSException, IOException {
		OperationsData requestBody = new OperationsData();
		requestBody.setPath(TEST_PATH);

		doThrow(new ConsistencyCheckException("Encryption parameters consistency check between election event data and signed jwt failed."))
				.when(exportImportService).importDatabase();

		ResponseEntity<OperationResult> importOpertaion = operationsController.importOperation(requestBody);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, importOpertaion.getStatusCode());
		assertEquals(OperationsOutputCode.CONSISTENCY_ERROR.value(), importOpertaion.getBody().getError());
	}

	@Test
	void testImportOperationHappyPath() {
		OperationsData requestBody = new OperationsData();
		requestBody.setPath(TEST_PATH);

		ResponseEntity<OperationResult> importOpertaion = operationsController.importOperation(requestBody);

		assertEquals(HttpStatus.OK, importOpertaion.getStatusCode());
		assertNull(importOpertaion.getBody());
	}

	@Test
	void generatePreVotingOutputsOperationWrongElectionEventId() {
		final OperationsData operationsData = new OperationsData();
		assertThrows(IllegalArgumentException.class, () -> operationsController.generatePreVotingOutputsOperation("34dfasG3", operationsData));
	}

	@Test
	void generatePreVotingOutputsOperationEmptyElectionEventId() {
		final OperationsData operationsData = new OperationsData();
		assertThrows(IllegalArgumentException.class, () -> operationsController.generatePreVotingOutputsOperation("", operationsData));
	}

	@Test
	void generatePostVotingOutputsOperationWrongElectionEventId() {
		final OperationsData operationsData = new OperationsData();
		assertThrows(IllegalArgumentException.class,
				() -> operationsController.generatePostVotingOutputsOperation(INVALID_ELECTION_EVENT_ID, BALLOT_BOX_STATUS, operationsData));
	}

	@Test
	void testExportOperationPathParameterIsRequireds() {
		ResponseEntity<OperationResult> exportOperation = operationsController.exportOperation(ELECTION_EVENT_ID, new OperationsData());

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exportOperation.getStatusCode());
		assertEquals(exportOperation.getBody().getError(), OperationsOutputCode.MISSING_PARAMETER.value());
	}

	static class OperationsControllerForTest extends OperationsController {
		@Override
		protected List<String> getCommands(PhaseName phaseName) {
			List<String> result = new ArrayList<>();
			result.add("command");
			return result;
		}
	}

}
