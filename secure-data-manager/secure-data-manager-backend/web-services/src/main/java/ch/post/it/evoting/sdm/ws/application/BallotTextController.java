/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.ws.application;

import static ch.post.it.evoting.cryptolib.commons.validations.Validate.validateUUID;

import java.util.HashMap;
import java.util.Map;

import javax.json.JsonObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.sdm.domain.model.ballottext.BallotTextRepository;
import ch.post.it.evoting.sdm.utils.JsonUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Endpoint for managing ballot texts
 */
@RestController
@RequestMapping("/sdm-ws-rest/ballottexts")
@Api(value = "Ballot text REST API")
public class BallotTextController {

	@Autowired
	private BallotTextRepository ballotTextRepository;

	/**
	 * Returns an ballot text identified by its id.
	 *
	 * @param electionEventId the election event id.
	 * @param ballotId        ballot id.
	 * @return An election event identified by its id.
	 */
	@GetMapping(value = "/electionevent/{electionEventId}/ballottext/{ballotId}", produces = "application/json")
	@ResponseBody
	@ApiOperation(value = "List of ballots texts", notes = "Service to retrieve the list of ballots texts.", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Not Found") })
	public ResponseEntity<String> getBallotTexts(
			@ApiParam(value = "String", required = true)
			@PathVariable
					String electionEventId,
			@ApiParam(value = "String", required = true)
			@PathVariable
					String ballotId) {

		validateUUID(electionEventId);
		validateUUID(ballotId);

		Map<String, Object> params = new HashMap<>();
		params.put("ballot.id", ballotId);
		String result = ballotTextRepository.list(params);
		JsonObject jsonObject = JsonUtils.getJsonObject(result);
		if (!jsonObject.isEmpty()) {
			return new ResponseEntity<>(result, HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

}
