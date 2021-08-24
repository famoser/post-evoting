/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.webapp.mvc;

import static ch.post.it.evoting.sdm.commons.Constants.ELECTION_EVENT_ID;
import static ch.post.it.evoting.sdm.commons.Constants.JOB_INSTANCE_ID;
import static ch.post.it.evoting.sdm.commons.Constants.STATUS;
import static ch.post.it.evoting.sdm.commons.Constants.TENANT_ID;

import java.io.PrintWriter;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import ch.post.it.evoting.sdm.commons.domain.CreateBallotBoxesInput;
import ch.post.it.evoting.sdm.commons.domain.CreateElectionEventInput;
import ch.post.it.evoting.sdm.commons.domain.CreateVerificationCardIdsInput;
import ch.post.it.evoting.sdm.commons.domain.CreateVotingCardSetInput;
import ch.post.it.evoting.sdm.commons.domain.StartVotingCardGenerationJobResponse;
import ch.post.it.evoting.sdm.config.commands.api.ConfigurationService;
import ch.post.it.evoting.sdm.config.commands.api.output.BallotBoxesServiceOutput;
import ch.post.it.evoting.sdm.config.commands.api.output.ElectionEventServiceOutput;
import ch.post.it.evoting.sdm.config.commands.ballotbox.BallotBoxParametersHolder;
import ch.post.it.evoting.sdm.config.commands.electionevent.CreateElectionEventParametersHolder;
import ch.post.it.evoting.sdm.config.commands.voters.VotersParametersHolder;
import ch.post.it.evoting.sdm.config.commons.beans.VotingCardGenerationJobStatus;
import ch.post.it.evoting.sdm.config.exceptions.ConfigurationEngineException;
import ch.post.it.evoting.sdm.config.webapp.mvc.commands.ballotbox.BallotBoxWebappAdapter;
import ch.post.it.evoting.sdm.config.webapp.mvc.commands.electionevent.ElectionEventWebappAdapter;
import ch.post.it.evoting.sdm.config.webapp.mvc.commands.voters.VotersWebappAdapter;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Controller
@Api(value = "Configuration Engine REST API")
public class ConfigurationEngineController {

	static final String SEPARATOR = "\n";

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationEngineController.class);

	@Autowired
	private ConfigurationService configurationService;

	@Autowired
	private ElectionEventWebappAdapter electionEventWebappAdapter;

	@Autowired
	private VotersWebappAdapter votersWebappAdapter;

	@Autowired
	private BallotBoxWebappAdapter ballotBoxWebappAdapter;

	/**
	 * REST Service for connectivity validation purposes
	 */
	@RequestMapping(value = "/check", method = RequestMethod.GET)
	@ResponseBody
	@ApiOperation(value = "Health Check Service", response = Void.class, notes = "Service to validate application is up & running.")
	@ApiResponses(value = { @ApiResponse(code = 500, message = "Internal Server Error") })
	public ResponseEntity<Void> serviceCheck(final HttpServletRequest httpServletRequest) {
		return new ResponseEntity<>(HttpStatus.OK);
	}

	/**
	 * REST Service for createElectionEvent
	 */
	@RequestMapping(value = "/createElectionEvent", method = RequestMethod.POST)
	@ResponseBody
	@ApiOperation(value = "Create Election Event Service", notes =
			"Executes the 'create election event' action. Receives as a input a json with the election event id (eeid), "
					+ "a set of properties (validityPeriod, challengeResExpTime, authTokenExpTime, challengeLength, numVotesPerVotingCard, "
					+ "numVotesPerAuthToken, maxNumberOfAttempts) and the output path (including timestamp folder). "
					+ "Creates all election datapacks (keypair certificate, keystore and keystore password) for the ElectionEventCA, AuthoritiesCA, ServicesCA, CredentialsCA and "
					+ "AdminBoard.", response = ElectionEventServiceOutput.class)
	@ApiResponses(value = { @ApiResponse(code = 500, message = "Internal Server Error") })
	public ResponseEntity<ElectionEventServiceOutput> createElectionEvent(final HttpServletRequest httpServletRequest,
			@ApiParam(value = "CreateElectionEventInput", required = true)
			@RequestBody
			final CreateElectionEventInput input) {

		CreateElectionEventParametersHolder holder = electionEventWebappAdapter.adapt(input);

		ElectionEventServiceOutput electionEventServiceOutput = configurationService.createElectionEvent(holder);

		return new ResponseEntity<>(electionEventServiceOutput, HttpStatus.OK);
	}

	/**
	 * REST Service for createBallotBoxes
	 */
	@RequestMapping(value = "/createBallotBoxes", method = RequestMethod.POST)
	@ResponseBody
	@ApiOperation(value = "Create Ballot Boxes Service", notes =
			"Executes the 'create ballot boxes' action. Receives as a input a json with the ballotID, the list of Ballots Boxes Ids, "
					+ "a set of properties (validityPeriod, start and end) and the output path (including timestamp and eeid folders). "
					+ "Creates all datapacks (keypair certificate, keystore and keystore password) for the ballot boxes and save them on the path: "
					+ "<b>output/{timestamp}/{eeid}/ONLINE/electionInformation/ballots/{ballotId}/ballotBoxes/{ballotBoxId}</b> folder.", response = BallotBoxesServiceOutput.class)
	@ApiResponses(value = { @ApiResponse(code = 500, message = "Internal Server Error") })
	public ResponseEntity<BallotBoxesServiceOutput> createBallotBoxes(final HttpServletRequest httpServletRequest,
			@ApiParam(value = "CreateBallotBoxesInput", required = true)
			@RequestBody
			final CreateBallotBoxesInput input) {

		BallotBoxParametersHolder holder = ballotBoxWebappAdapter.adapt(input);

		BallotBoxesServiceOutput ballotBoxesServiceOutput = configurationService.createBallotBoxes(holder);

		return new ResponseEntity<>(ballotBoxesServiceOutput, HttpStatus.OK);
	}

	/**
	 * Runs a voting card set pre-computation, that is, the generation of verification card set IDs for those voting cards. The output is a list of
	 * verification card IDs, one per line (hence the 'text/csv' MIME type)
	 */
	@RequestMapping(value = "/precompute", method = RequestMethod.POST, consumes = "application/json", produces = "text/csv")
	@ResponseBody
	@ApiOperation(value = "Precompute operation", notes = "Executes the 'precompute' action. "
			+ "Generates the requested number of verification card IDs for a voting card set", response = List.class)
	public ResponseEntity<StreamingResponseBody> precompute(final HttpServletRequest request,
			@RequestBody
			final CreateVerificationCardIdsInput input) {

		// Get a stream of verification card IDs.
		Stream<String> verificationCardIds = configurationService
				.createVerificationCardIdStream(input.getElectionEventId(), input.getVerificationCardSetId(), input.getNumberOfVerificationCardIds());

		// Push each verification card ID to the response output stream.
		StreamingResponseBody responseBody = outputStream -> {
			try (PrintWriter pw = new PrintWriter(outputStream)) {
				verificationCardIds.forEach(vcid -> {
					pw.print(vcid);
					// Ensure consistent new-line characters.
					pw.print(SEPARATOR);
					pw.flush();
				});
			}
		};

		return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv")).body(responseBody);
	}

	@ExceptionHandler(ConfigurationEngineException.class)
	public ResponseEntity<Void> handleConfigurationEngineError(final HttpServletRequest req, final Exception exception) {

		LOGGER.error("Request: {} raised", req.getRequestURL(), exception);
		return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handleError(final HttpServletRequest req, final Exception exception) {

		LOGGER.error("Request: {} raised", req.getRequestURL(), exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ExceptionUtils.getRootCauseMessage(exception));
	}

	@RequestMapping(value = "{tenantId}/{electionEventId}/jobs", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<StartVotingCardGenerationJobResponse> startVotingCardGenerationJob(
			@PathVariable(TENANT_ID)
			final String tenantId,
			@PathVariable(ELECTION_EVENT_ID)
			final String electionEventId,
			@RequestBody
			final CreateVotingCardSetInput input, final HttpServletRequest servletRequest, final UriComponentsBuilder uriBuilder) {
		VotersParametersHolder holder = votersWebappAdapter.adapt(input);
		final StartVotingCardGenerationJobResponse response = configurationService.startVotingCardGenerationJob(tenantId, electionEventId, holder);

		final String jobId = response.getJobId();
		final URI location = uriBuilder.path(servletRequest.getServletPath() + "/{jobId}").buildAndExpand(jobId).toUri();

		return ResponseEntity.created(location).body(response);
	}

	@ApiOperation(value = "Get list of jobs", notes = "Gets the list of jobs for the specified Tenant and ElectionEvent. "
			+ "Returns a list of objects with details about the jobs, including, the Id, Status and Start Time  and "
			+ "Progress details. May return an empty list.", response = VotingCardGenerationJobStatus.class, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = List.class),
			@ApiResponse(code = 500, message = "Internal Server Error", response = String.class) })
	@RequestMapping(value = "{tenantId}/{electionEventId}/jobs", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<List<VotingCardGenerationJobStatus>> getJobs(
			@PathVariable(TENANT_ID)
			final String tenantId,
			@PathVariable(ELECTION_EVENT_ID)
			final String electionEventId) {

		List<VotingCardGenerationJobStatus> status = configurationService.getJobs();
		return ResponseEntity.ok(status);
	}

	@ApiOperation(value = "Get list of jobs with specific status", notes =
			"Gets the list of jobs with a specific status, for the specified Tenant and ElectionEvent. "
					+ "Returns a list of objects with details about the Jobs, including, the Id, Status and Start Time  and "
					+ "Progress details. May return an empty list.", response = VotingCardGenerationJobStatus.class, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = List.class),
			@ApiResponse(code = 500, message = "Internal Server Error", response = String.class) })
	@RequestMapping(value = "{tenantId}/{electionEventId}/jobs", method = RequestMethod.GET, params = "status", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<List<VotingCardGenerationJobStatus>> getJobsWithStatus(
			@PathVariable(TENANT_ID)
			final String tenantId,
			@PathVariable(ELECTION_EVENT_ID)
			final String electionEventId,
			@RequestParam(STATUS)
			final String jobStatus) {

		List<VotingCardGenerationJobStatus> status = configurationService.getJobsWithStatus(tenantId, electionEventId, jobStatus);
		return ResponseEntity.ok(status);
	}

	@ApiOperation(value = "Get status information of job instance", notes =
			"Gets status information of a specific job instance of a specific election event and tenant. Returned data "
					+ "includes Id, Status, Start time and detailed progress status.", response = VotingCardGenerationJobStatus.class, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = VotingCardGenerationJobStatus.class),
			@ApiResponse(code = 404, message = "Not Found"), @ApiResponse(code = 500, message = "Internal Server Error", response = String.class) })
	@RequestMapping(value = "{tenantId}/{electionEventId}/jobs/{jobInstanceId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<VotingCardGenerationJobStatus> getVotingCardGenerationJobStatus(
			@PathVariable(TENANT_ID)
			final String tenantId,
			@PathVariable(ELECTION_EVENT_ID)
			final String electionEventId,
			@PathVariable(JOB_INSTANCE_ID)
			final String jobInstanceId) {
		final Optional<VotingCardGenerationJobStatus> jobStatus = configurationService
				.getVotingCardGenerationJobStatus(tenantId, electionEventId, jobInstanceId);

		if (jobStatus.isPresent()) {
			return ResponseEntity.ok(jobStatus.get());
		} else {
			return ResponseEntity.notFound().build();
		}
	}
}
