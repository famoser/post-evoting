/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.ws.application;

import static ch.post.it.evoting.cryptolib.commons.validations.Validate.validateUUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.sdm.application.exception.CheckedIllegalStateException;
import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.application.service.IdleStatusService;
import ch.post.it.evoting.sdm.application.service.MixOfflineFacade;
import ch.post.it.evoting.sdm.domain.common.IdleState;
import ch.post.it.evoting.sdm.domain.model.mixing.MixingKeys;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * The mixing end-point.
 */
@RestController
@RequestMapping("/sdm-ws-rest/mixing")
@Api(value = "Mixing REST API")
public class MixDecryptController {

	private static final Logger LOGGER = LoggerFactory.getLogger(MixDecryptController.class);

	@Autowired
	private IdleStatusService idleStatusService;

	@Autowired
	private MixOfflineFacade mixOfflineFacade;

	/**
	 * Performs the mixing of a specific ballot box
	 */
	@PostMapping(value = "/electionevent/{electionEventId}/ballotbox/{ballotBoxId}", produces = "application/json")
	@ResponseBody
	@ApiOperation(value = "Mix ballot box", notes = "Service to mix a given ballot box.", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Not Found"), @ApiResponse(code = 403, message = "Forbidden"),
			@ApiResponse(code = 500, message = "Internal Server Error") })
	public ResponseEntity<IdleState> mixOffline(
			@ApiParam(value = "String", required = true)
			@PathVariable
					String electionEventId,
			@ApiParam(value = "String", required = true)
			@PathVariable
					String ballotBoxId,
			@ApiParam(value = "MixingData", required = true)
			@RequestBody
			final MixingKeys mixingKeys) {

		try {
			validateUUID(electionEventId);
			validateUUID(ballotBoxId);
		} catch (IllegalArgumentException e) {
			LOGGER.warn("A malformed ID was requested to be mixed");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		// If the ballot box is already being operated on, exit immediately.
		IdleState idleState = new IdleState();
		if (!(idleStatusService.getIdLock(ballotBoxId))) {
			idleState.setIdle(true);
			return new ResponseEntity<>(idleState, HttpStatus.OK);
		}

		try {
			mixOfflineFacade.mixOffline(electionEventId, ballotBoxId, mixingKeys);
		} catch (ResourceNotFoundException e) {
			LOGGER.error("This ballot box could not be found", e);
			idleStatusService.freeIdLock(ballotBoxId);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} catch (CheckedIllegalStateException e) {
			LOGGER.error("The ballot box cannot be mixed", e);
			idleStatusService.freeIdLock(ballotBoxId);
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		} catch (Exception e) {
			LOGGER.error("Internal error when performing the mixing", e);
			idleStatusService.freeIdLock(ballotBoxId);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		idleStatusService.freeIdLock(ballotBoxId);
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
