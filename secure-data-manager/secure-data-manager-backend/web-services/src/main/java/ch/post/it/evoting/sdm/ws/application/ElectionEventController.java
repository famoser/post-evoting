/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.ws.application;

import static ch.post.it.evoting.cryptolib.commons.validations.Validate.validateUUID;

import java.io.IOException;

import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.sdm.application.service.ElectionEventService;
import ch.post.it.evoting.sdm.domain.model.electionevent.ElectionEventRepository;
import ch.post.it.evoting.sdm.domain.model.generator.DataGeneratorResponse;
import ch.post.it.evoting.sdm.utils.JsonUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * The election event end-point.
 */
@RestController
@RequestMapping("/sdm-ws-rest/electionevents")
@Api(value = "Election event REST API")
public class ElectionEventController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElectionEventController.class);

	@Autowired
	private ElectionEventRepository electionEventRepository;

	// The configuration of contests / elections
	@Value("${elections.config.filename}")
	private String configFile;

	@Autowired
	private ElectionEventService electionEventService;

	/**
	 * Runs the securization of an election event.
	 *
	 * @param electionEventId the election event id.
	 * @return a list of ids of the created election events.
	 */
	@PostMapping(value = "/{electionEventId}", produces = "application/json")
	@ApiOperation(value = "Secure election event", notes = "Service to secure an election event.", response = Void.class)
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Not Found") })
	public ResponseEntity<Void> secureElectionEvent(
			@ApiParam(value = "String", required = true)
			@PathVariable
					String electionEventId) throws IOException {

		validateUUID(electionEventId);

		DataGeneratorResponse response = electionEventService.create(electionEventId);

		if (response.isSuccessful()) {
			return new ResponseEntity<>(HttpStatus.CREATED);
		}

		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

	/**
	 * Returns an election event identified by its id.
	 *
	 * @param electionEventId the election event id.
	 * @return An election event identified by its id.
	 */
	@GetMapping(value = "/{electionEventId}", produces = "application/json")
	@ResponseBody
	@ApiOperation(value = "Get election event", notes = "Service to retrieve a given election event.", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Not Found") })
	public ResponseEntity<String> getElectionEvent(
			@ApiParam(value = "String", required = true)
			@PathVariable
					String electionEventId) {

		validateUUID(electionEventId);

		String result = electionEventRepository.find(electionEventId);
		JsonObject jsonObject = JsonUtils.getJsonObject(result);
		if (!jsonObject.isEmpty()) {
			return new ResponseEntity<>(result, HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

	/**
	 * Returns a list of all election events.
	 *
	 * @return The list of election events.
	 */
	@GetMapping(produces = "application/json")
	@ResponseBody
	@ApiOperation(value = "Get election events", notes = "Service to retrieve the list of events.", response = String.class)
	public ResponseEntity<String> getElectionEvents() {
		return ResponseEntity.ok(electionEventRepository.list());
	}

	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> error(HttpServletRequest req, Exception exception) {
		LOGGER.error("Failed to process request to '{}.", req.getRequestURI(), exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.TEXT_PLAIN)
				.body(ExceptionUtils.getRootCauseMessage(exception));
	}
}
