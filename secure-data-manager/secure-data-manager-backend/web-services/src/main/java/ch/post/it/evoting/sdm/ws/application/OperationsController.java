/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.ws.application;

import static ch.post.it.evoting.cryptolib.commons.validations.Validate.validateUUID;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignerDigestMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.SAXException;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.sdm.application.exception.ConsistencyCheckException;
import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.application.service.ElectionEventService;
import ch.post.it.evoting.sdm.application.service.ExportImportService;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.operation.OperationResult;
import ch.post.it.evoting.sdm.domain.model.operation.OperationsData;
import ch.post.it.evoting.sdm.domain.model.operation.OperationsOutputCode;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.plugin.ExecutionListenerImpl;
import ch.post.it.evoting.sdm.plugin.KeyParameter;
import ch.post.it.evoting.sdm.plugin.Parameters;
import ch.post.it.evoting.sdm.plugin.PhaseName;
import ch.post.it.evoting.sdm.plugin.PluginSequenceResolver;
import ch.post.it.evoting.sdm.plugin.Plugins;
import ch.post.it.evoting.sdm.plugin.SequentialExecutor;
import ch.post.it.evoting.sdm.plugin.XmlObjectsLoader;

import io.jsonwebtoken.SignatureException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping("/sdm-ws-rest/operation")
@Api(value = "SDM Operations REST API")
public class OperationsController {

	private static final Logger LOGGER = LoggerFactory.getLogger(OperationsController.class);
	private static final String REQUEST_CAN_NOT_BE_PERFORMED = "The request can not be performed for the current resource";
	private static final String PATH_REQUIRED = "path parameter is required";
	private static final String PLUGIN_FILE_NAME = "plugin.xml";
	private ExportImportService exportImportService;
	private ElectionEventService electionEventService;
	private PathResolver pathResolver;
	private SequentialExecutor sequentialExecutor;

	@Autowired
	public OperationsController(ExportImportService exportImportService, ElectionEventService electionEventService, PathResolver pathResolver,
			SequentialExecutor sequentialExecutor) {
		this.exportImportService = exportImportService;
		this.electionEventService = electionEventService;
		this.pathResolver = pathResolver;
		this.sequentialExecutor = sequentialExecutor;
	}

	public OperationsController() {
	}

	@PostMapping(value = "/generate-pre-voting-outputs/{electionEventId}")
	@ApiOperation(value = "Export operation service")
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Not Found"), @ApiResponse(code = 403, message = "Forbidden"),
			@ApiResponse(code = 500, message = "Internal Server Error") })
	public ResponseEntity<OperationResult> generatePreVotingOutputsOperation(
			@ApiParam(value = "String", required = true)
			@PathVariable
					String electionEventId,
			@RequestBody
			final OperationsData request) {

		validateUUID(electionEventId);

		if (StringUtils.isEmpty(request.getPrivateKeyInBase64())) {
			throw new IllegalArgumentException("PrivateKey parameter is required");
		}

		Parameters parameters = buildParameters(electionEventId, request.getPrivateKeyInBase64(), "");
		return executeOperationForPhase(parameters, PhaseName.GENERATE_PRE_VOTING_OUTPUTS, true);

	}

	@PostMapping(value = "/generate-post-voting-outputs/{electionEventId}/{ballotBoxStatus}")
	@ApiOperation(value = "Export operation service")
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Not Found"), @ApiResponse(code = 403, message = "Forbidden"),
			@ApiResponse(code = 500, message = "Internal Server Error") })
	public ResponseEntity<OperationResult> generatePostVotingOutputsOperation(
			@ApiParam(value = "String", required = true)
			@PathVariable
					String electionEventId,
			@ApiParam(value = "String", required = true)
			@PathVariable
					String ballotBoxStatus,
			@RequestBody
			final OperationsData request) {

		validateUUID(electionEventId);

		if (StringUtils.isEmpty(request.getPrivateKeyInBase64())) {
			throw new IllegalArgumentException("PrivateKey parameter is required");
		}
		PhaseName phaseName;
		Parameters parameters;
		try {
			Status status = Status.valueOf(ballotBoxStatus.toUpperCase());
			phaseName = determinePhase(status);
			if (phaseName == null) {
				return handleException(OperationsOutputCode.ERROR_STATUS_NAME);
			}

			parameters = buildParameters(electionEventId, request.getPrivateKeyInBase64(), "");
		} catch (InvalidParameterException e) {
			int errorCode = OperationsOutputCode.MISSING_PARAMETER.value();
			return handleInvalidParamException(e, errorCode);
		} catch (Exception e) {
			int errorCode = OperationsOutputCode.ERROR_STATUS_NAME.value();
			return handleException(e, errorCode);
		}

		return executeOperationForPhase(parameters, phaseName, true);
	}

	private PhaseName determinePhase(Status status) {
		PhaseName phaseName = null;
		switch (status) {
		case BB_DOWNLOADED:
			phaseName = PhaseName.DOWNLOAD;
			break;
		case DECRYPTED:
			phaseName = PhaseName.DECRYPTION;
			break;
		default:
			break;
		}
		return phaseName;
	}

	@PostMapping(value = "/export/{electionEventId}")
	@ApiOperation(value = "Export operation service")
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Not Found"), @ApiResponse(code = 403, message = "Forbidden"),
			@ApiResponse(code = 500, message = "Internal Server Error") })
	public ResponseEntity<OperationResult> exportOperation(
			@ApiParam(value = "String", required = true)
			@PathVariable
					String electionEventId,
			@RequestBody
			final OperationsData request) {
		try {

			validateUUID(electionEventId);

			if (StringUtils.isEmpty(request.getPath())) {
				throw new ResourceNotFoundException(PATH_REQUIRED);
			}

			String normalizedPath = Paths.get(request.getPath()).toFile().getAbsolutePath();

			Parameters parameters = buildParameters(electionEventId, "", normalizedPath);

			exportImportService.dumpDatabase(electionEventId);

			exportImportService.signDumpDatabaseAndElectionsConfig(request.getPassword());

			String eeAlias = electionEventService.getElectionEventAlias(electionEventId);

			if (request.isElectionEventData()) {
				exportImportService.exportElectionEventWithoutElectionInformation(request.getPath(), electionEventId, eeAlias);
				exportImportService.exportElectionEventElectionInformation(request.getPath(), electionEventId, eeAlias);
			}

			if (request.isVotingCardsData()) {
				exportImportService.exportVotingCards(request.getPath(), electionEventId, eeAlias);
			}

			if (request.isCustomerData()) {
				exportImportService.exportCustomerSpecificData(request.getPath(), electionEventId, eeAlias);
			}

			if (request.isComputedChoiceCodes()) {
				exportImportService.exportComputedChoiceCodes(request.getPath(), electionEventId, eeAlias);
			}

			if (request.isPreComputedChoiceCodes()) {
				exportImportService.exportPreComputedChoiceCodes(request.getPath(), electionEventId, eeAlias);
			}

			if (request.isBallotBoxes()) {
				exportImportService.exportBallotBoxes(request.getPath(), electionEventId, eeAlias);
			}

		} catch (InvalidParameterException e) {
			OperationsOutputCode code = OperationsOutputCode.MISSING_PARAMETER;
			return handleInvalidParamException(e, code.value());
		} catch (ResourceNotFoundException e) {
			OperationsOutputCode code = OperationsOutputCode.MISSING_PARAMETER;
			return handleException(e, code.value());
		} catch (IOException e) {
			OperationsOutputCode code = OperationsOutputCode.ERROR_IO_OPERATIONS;
			return handleException(e, code.value());
		} catch (CMSSignerDigestMismatchException e) {
			OperationsOutputCode code = OperationsOutputCode.SIGNATURE_VERIFICATION_FAILED;
			return handleException(e, code.value());
		} catch (CMSException e) {
			OperationsOutputCode code = OperationsOutputCode.ERROR_SIGNING_OPERATIONS;
			return handleException(e, code.value());
		} catch (GeneralCryptoLibException e) {
			OperationsOutputCode code = OperationsOutputCode.KEYSTORE_READING_FAILED;
			return handleException(e, code.value());
		} catch (Exception e) {
			OperationsOutputCode code = OperationsOutputCode.GENERAL_ERROR;
			return handleException(e, code.value());
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping(value = "/import")
	@ApiOperation(value = "Import operation service")
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Not Found"), @ApiResponse(code = 403, message = "Forbidden"),
			@ApiResponse(code = 500, message = "Internal Server Error") })
	public ResponseEntity<OperationResult> importOperation(
			@RequestBody
			final OperationsData request) {

		if (StringUtils.isEmpty(request.getPath())) {
			return handleIncompleteRequest();
		}

		try {
			exportImportService.importData(request.getPath());
			exportImportService.verifySignaturesOnImport();
			exportImportService.importDatabase();

		} catch (IOException e) {
			OperationsOutputCode code = OperationsOutputCode.ERROR_IO_OPERATIONS;
			return handleException(e, code.value());
		} catch (InvalidParameterException e) {
			OperationsOutputCode code = OperationsOutputCode.MISSING_PARAMETER;
			return handleInvalidParamException(e, code.value());
		} catch (CMSException | SignatureException e) {
			OperationsOutputCode code = OperationsOutputCode.SIGNATURE_VERIFICATION_FAILED;
			return handleException(e, code.value());
		} catch (GeneralCryptoLibException e) {
			OperationsOutputCode code = OperationsOutputCode.CHAIN_VALIDATION_FAILED;
			return handleException(e, code.value());
		} catch (CertificateException e) {
			OperationsOutputCode code = OperationsOutputCode.ERROR_CERTIFICATE_PARSING;
			return handleException(e, code.value());
		} catch (ConsistencyCheckException e) {
			OperationsOutputCode code = OperationsOutputCode.CONSISTENCY_ERROR;
			return handleException(e, code.value());
		} catch (Exception e) {
			OperationsOutputCode code = OperationsOutputCode.GENERAL_ERROR;
			return handleException(e, code.value());
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	protected List<String> getCommands(PhaseName phaseName) throws IOException, JAXBException, SAXException, XMLStreamException {

		Path pluginXmlPath = pathResolver.resolve(Constants.SDM_DIR_NAME).resolve(PLUGIN_FILE_NAME);
		if (!pluginXmlPath.toFile().exists()) {
			pluginXmlPath = pathResolver.resolve(Constants.SDM_DIR_NAME).resolve(Constants.SDM_CONFIG_DIR_NAME).resolve(PLUGIN_FILE_NAME);
			if (!pluginXmlPath.toFile().exists()) {
				LOGGER.error("The plugin.xml file is not found");
				return new ArrayList<>();
			}
		}

		Plugins plugins = XmlObjectsLoader.unmarshal(pluginXmlPath);
		PluginSequenceResolver pluginSequence = new PluginSequenceResolver(plugins);
		return pluginSequence.getActionsForPhase(phaseName);
	}

	private ResponseEntity<OperationResult> executeOperationForPhase(Parameters parameters, PhaseName phaseName,
			boolean failOnEmptyCommandsForPhase) {
		try {
			List<String> commandsForPhase = getCommands(phaseName);

			if (failOnEmptyCommandsForPhase && commandsForPhase.isEmpty()) {
				return handleException(OperationsOutputCode.MISSING_COMMANDS_FOR_PHASE);
			}

			ExecutionListenerImpl listener = new ExecutionListenerImpl();
			sequentialExecutor.execute(commandsForPhase, parameters, listener);

			if (listener.getError() != 0) {
				return handleException(listener);
			}

		} catch (IOException e) {
			int errorCode = OperationsOutputCode.ERROR_IO_OPERATIONS.value();
			return handleException(e, errorCode);
		} catch (JAXBException | SAXException e) {
			int errorCode = OperationsOutputCode.ERROR_PARSING_FILE.value();
			return handleException(e, errorCode);
		} catch (Exception e) {
			int errorCode = OperationsOutputCode.GENERAL_ERROR.value();
			return handleException(e, errorCode);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	private Parameters buildParameters(String electionEventId, String privateKeyInBase64, String path) {
		Parameters parameters = new Parameters();

		if (StringUtils.isNotEmpty(electionEventId)) {
			String electionEventAlias = electionEventService.getElectionEventAlias(electionEventId);
			if (StringUtils.isBlank(electionEventAlias)) {
				throw new InvalidParameterException("Invalid Election Event Id: " + electionEventId);
			}
			parameters.addParam(KeyParameter.EE_ALIAS.name(), electionEventAlias);
			parameters.addParam(KeyParameter.EE_ID.name(), electionEventId);
		}
		Path sdmPath = pathResolver.resolve(Constants.SDM_DIR_NAME);
		parameters.addParam(KeyParameter.SDM_PATH.name(), sdmPath.toString().replace("\\", "/"));
		if (StringUtils.isNotEmpty(privateKeyInBase64)) {
			parameters.addParam(KeyParameter.PRIVATE_KEY.name(), privateKeyInBase64);
		}
		if (StringUtils.isNotEmpty(path)) {
			parameters.addParam(KeyParameter.USB_LETTER.name(), path.replace("\\", "/"));
		}
		return parameters;
	}

	private ResponseEntity<OperationResult> handleIncompleteRequest() {
		LOGGER.error("{}: {}", OperationsOutputCode.MISSING_PARAMETER.getReasonPhrase(), PATH_REQUIRED);
		OperationResult output = new OperationResult();
		output.setError(OperationsOutputCode.MISSING_PARAMETER.value());
		output.setMessage(PATH_REQUIRED);
		return new ResponseEntity<>(output, HttpStatus.BAD_REQUEST);
	}

	private ResponseEntity<OperationResult> handleException(Exception e, int errorCode) {
		LOGGER.error("{}{}", REQUEST_CAN_NOT_BE_PERFORMED, errorCode, e);
		OperationResult output = new OperationResult();
		output.setError(errorCode);
		output.setException(e.getClass().getName());
		output.setMessage(e.getMessage());
		return new ResponseEntity<>(output, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private ResponseEntity<OperationResult> handleException(int error, String message) {
		LOGGER.error("{}{}: {}", REQUEST_CAN_NOT_BE_PERFORMED, error, message);
		OperationResult output = new OperationResult();
		output.setError(error);
		output.setMessage(message);
		return new ResponseEntity<>(output, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private ResponseEntity<OperationResult> handleInvalidParamException(Exception e, int errorCode) {
		LOGGER.error("{}{}", REQUEST_CAN_NOT_BE_PERFORMED, errorCode, e);
		OperationResult output = new OperationResult();
		output.setError(errorCode);
		output.setException(e.getClass().getName());
		output.setMessage(e.getMessage());
		return new ResponseEntity<>(output, HttpStatus.NOT_FOUND);
	}

	private ResponseEntity<OperationResult> handleException(ExecutionListenerImpl listener) {
		return handleException(listener.getError(), listener.getMessage());
	}

	private ResponseEntity<OperationResult> handleException(OperationsOutputCode operationsOutputCode) {
		return handleException(operationsOutputCode.value(), operationsOutputCode.getReasonPhrase());
	}

}
