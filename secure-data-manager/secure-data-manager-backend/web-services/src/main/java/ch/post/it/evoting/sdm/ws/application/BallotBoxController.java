/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.ws.application;

import static ch.post.it.evoting.cryptolib.commons.validations.Validate.validateUUID;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.application.service.BallotBoxDownloadException;
import ch.post.it.evoting.sdm.application.service.BallotBoxDownloadService;
import ch.post.it.evoting.sdm.application.service.BallotBoxMixingService;
import ch.post.it.evoting.sdm.application.service.BallotBoxService;
import ch.post.it.evoting.sdm.application.service.CleansingOutputsDownloadException;
import ch.post.it.evoting.sdm.application.service.CleansingOutputsDownloadService;
import ch.post.it.evoting.sdm.application.service.IdleStatusService;
import ch.post.it.evoting.sdm.domain.common.IdleState;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxSignInputData;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.utils.JsonUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * The ballot box end-point.
 */
@RestController
@RequestMapping("/sdm-ws-rest/ballotboxes")
@Api(value = "Ballot Box REST API")
public class BallotBoxController {

	private static final Logger LOGGER = LoggerFactory.getLogger(BallotBoxController.class);

	@Autowired
	IdleStatusService idleStatusService;

	// The ballot box repository
	@Autowired
	private BallotBoxRepository ballotBoxRepository;

	// The ballot box download service
	@Autowired
	private BallotBoxDownloadService ballotBoxDownloadService;

	// The ballot box processing service
	@Autowired
	private BallotBoxService ballotBoxService;

	@Autowired
	private BallotBoxMixingService ballotBoxMixingService;

	// The configuration of contests / elections
	@Value("${elections.config.filename}")
	private String configFile;

	@Autowired
	private CleansingOutputsDownloadService cleansingOutputsDownloadService;

	/**
	 * Check if ballot boxes are ready to be downloaded (mixed)
	 *
	 * @param electionEventId the election event id.
	 */
	@PostMapping(value = "/electionevent/{electionEventId}/status", produces = "application/json")
	@ResponseStatus(value = HttpStatus.OK)
	@ApiOperation(value = "Check if ballot boxes are ready to be downloaded (mixed)")
	public void updateBallotBoxesMixingStatus(
			@ApiParam(value = "String", required = true)
			@PathVariable
					String electionEventId) throws IOException {

		validateUUID(electionEventId);

		ballotBoxMixingService.updateBallotBoxesMixingStatus(electionEventId);
	}

	/**
	 * Returns an ballot box identified by election event and its id.
	 *
	 * @param electionEventId the election event id.
	 * @param ballotBoxId     the ballot box id.
	 * @return An ballot box identified by election event and its id.
	 */
	@GetMapping(value = "/electionevent/{electionEventId}/ballotbox/{ballotBoxId}", produces = "application/json")
	@ResponseBody
	@ApiOperation(value = "Get Ballot box", notes = "Service to retrieve a given ballot box.", response = String.class)
	public ResponseEntity<String> getBallotBox(
			@ApiParam(value = "String", required = true)
			@PathVariable
					String electionEventId,
			@ApiParam(value = "String", required = true)
			@PathVariable
					String ballotBoxId) {

		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		Map<String, Object> attributeValueMap = new HashMap<>();
		attributeValueMap.put(JsonConstants.ID, ballotBoxId);
		attributeValueMap.put(JsonConstants.ELECTION_EVENT_DOT_ID, electionEventId);
		String result = ballotBoxRepository.find(attributeValueMap);
		JsonObject jsonObject = JsonUtils.getJsonObject(result);
		if (!jsonObject.isEmpty()) {
			return new ResponseEntity<>(result, HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

	/**
	 * Returns a list of all ballot boxes.
	 *
	 * @param electionEventId the election event id.
	 * @return The list of ballot boxes.
	 */
	@GetMapping(value = "/electionevent/{electionEventId}", produces = "application/json")
	@ResponseBody
	@ApiOperation(value = "List of ballot boxes", notes = "Service to retrieve the list of ballot boxes.", response = String.class)
	public String getBallotBoxes(
			@ApiParam(value = "String", required = true)
			@PathVariable
					String electionEventId) {
		Map<String, Object> attributeValueMap = new HashMap<>();
		attributeValueMap.put(JsonConstants.ELECTION_EVENT_DOT_ID, electionEventId);

		validateUUID(electionEventId);

		return ballotBoxRepository.list(attributeValueMap);
	}

	/**
	 * Download a ballot box identified by the corresponding election event and its id.
	 *
	 * @param electionEventId the election event id.
	 * @param ballotBoxId     the ballot box id.
	 * @return
	 */
	@PostMapping(value = "/electionevent/{electionEventId}/ballotbox/{ballotBoxId}")
	@ApiOperation(value = "Download ballot box", notes = "Service to download a given ballot box.")
	@ApiResponses(value = { @ApiResponse(code = 403, message = "Forbidden") })
	public ResponseEntity<IdleState> downloadBallotBox(
			@ApiParam(value = "String", required = true)
			@PathVariable
					String electionEventId,
			@ApiParam(value = "String", required = true)
			@PathVariable
					String ballotBoxId) {

		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		// checks firstly if the ballot box id is already doing an operation
		IdleState idleState = new IdleState();
		if (idleStatusService.getIdLock(ballotBoxId)) {
			try {
				// Download the mixing payloads.
				ballotBoxDownloadService.downloadPayloads(electionEventId, ballotBoxId);
				// Download the raw ballot box.
				ballotBoxDownloadService.download(electionEventId, ballotBoxId);
				// Download the successful and failed votes.
				cleansingOutputsDownloadService.download(electionEventId, ballotBoxId);
				// Mark the ballot box as downloaded.
				ballotBoxDownloadService.updateBallotBoxStatus(ballotBoxId);
				return new ResponseEntity<>(idleState, HttpStatus.OK);
			} catch (BallotBoxDownloadException e) {
				LOGGER.error("Error downloading ballot box ", e);
				return new ResponseEntity<>(idleState, HttpStatus.FORBIDDEN);
			} catch (CleansingOutputsDownloadException e) {
				LOGGER.error("Error downloading cleansing outputs ", e);
				return new ResponseEntity<>(idleState, HttpStatus.FORBIDDEN);
			} finally {
				idleStatusService.freeIdLock(ballotBoxId);
			}
		} else {
			idleState.setIdle(true);
			return new ResponseEntity<>(idleState, HttpStatus.OK);
		}
	}

	/**
	 * Change the state of the ballot box from ready to signed for a given election event and ballot box id.
	 *
	 * @param electionEventId the election event id.
	 * @param ballotBoxId     the ballot box id.
	 * @return HTTP status code 200 - If the ballot box is successfully signed. HTTP status code 404 - If the resource is not found. HTTP status code
	 * 412 - If the ballot box is already signed.
	 */
	@PutMapping(value = "/electionevent/{electionEventId}/ballotbox/{ballotBoxId}")
	@ApiOperation(value = "Sign ballot box", notes = "Service to change the state of the ballot box from ready to signed "
			+ "for a given election event and ballot box id..", response = Void.class)
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Not Found"), @ApiResponse(code = 412, message = "Precondition Failed") })
	public ResponseEntity<Void> signBallotBox(
			@ApiParam(value = "String", required = true)
			@PathVariable
					String electionEventId,
			@ApiParam(value = "String", required = true)
			@PathVariable
					String ballotBoxId,
			@ApiParam(value = "BallotBoxSignInputData", required = true)
			@RequestBody
					BallotBoxSignInputData input) {

		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		try {
			ballotBoxService.sign(electionEventId, ballotBoxId, input.getPrivateKeyPEM());
		} catch (ResourceNotFoundException e) {
			String errorMessage = "An error occurred while fetching the given ballot box to sign";
			LOGGER.error(errorMessage, e);

			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} catch (GeneralCryptoLibException | IOException e) {
			String errorMessage = "An error occurred while signing the ballot box";
			LOGGER.error(errorMessage, e);
			return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping(value = "/electionevent/{electionEventId}/mix")
	@ApiOperation(value = "Mix ballot box", notes = "Service to mix a given ballot box.")
	@ApiResponses(value = { @ApiResponse(code = 403, message = "Forbidden") })
	public ResponseEntity<String> mixBallotBox(
			@ApiParam(value = "String", required = true)
			@PathVariable
					String electionEventId,
			@ApiParam(value = "String", required = true)
			@RequestBody
					List<String> ballotBoxesIds) {

		validateUUID(electionEventId);
		ballotBoxesIds.forEach(Validate::validateUUID);

		LOGGER.info("Received ids to mix: {}.", ballotBoxesIds);
		final String response = ballotBoxMixingService.mixBallotBoxes(electionEventId, ballotBoxesIds);

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
