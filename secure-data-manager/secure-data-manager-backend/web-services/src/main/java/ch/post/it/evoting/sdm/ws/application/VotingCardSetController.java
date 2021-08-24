	/*
	 * (c) Copyright 2021 Swiss Post Ltd.
	 */
	package ch.post.it.evoting.sdm.ws.application;

	import static ch.post.it.evoting.cryptolib.commons.validations.Validate.validateUUID;

	import java.io.IOException;
	import java.net.URI;
	import java.util.HashMap;
	import java.util.List;
	import java.util.Map;

	import javax.json.JsonObject;

	import org.apache.commons.lang3.StringUtils;
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
	import org.springframework.web.bind.annotation.RequestParam;
	import org.springframework.web.bind.annotation.ResponseBody;
	import org.springframework.web.bind.annotation.RestController;
	import org.springframework.web.util.UriComponentsBuilder;

	import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
	import ch.post.it.evoting.domain.election.model.messaging.PayloadSignatureException;
	import ch.post.it.evoting.domain.election.model.messaging.PayloadVerificationException;
	import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
	import ch.post.it.evoting.sdm.application.service.GenerateVerificationData;
	import ch.post.it.evoting.sdm.application.service.IdleStatusService;
	import ch.post.it.evoting.sdm.application.service.VotingCardSetComputationService;
	import ch.post.it.evoting.sdm.application.service.VotingCardSetPreparationService;
	import ch.post.it.evoting.sdm.application.service.VotingCardSetService;
	import ch.post.it.evoting.sdm.domain.model.config.VotingCardGenerationJobStatus;
	import ch.post.it.evoting.sdm.domain.model.generator.DataGeneratorResponse;
	import ch.post.it.evoting.sdm.domain.model.status.InvalidStatusTransitionException;
	import ch.post.it.evoting.sdm.domain.model.votingcardset.VotingCardSetRepository;
	import ch.post.it.evoting.sdm.domain.model.votingcardset.VotingCardSetUpdateInputData;
	import ch.post.it.evoting.sdm.domain.service.ProgressManagerService;
	import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
	import ch.post.it.evoting.sdm.infrastructure.cc.PayloadStorageException;
	import ch.post.it.evoting.sdm.utils.JsonUtils;

	import io.swagger.annotations.Api;
	import io.swagger.annotations.ApiOperation;
	import io.swagger.annotations.ApiParam;
	import io.swagger.annotations.ApiResponse;
	import io.swagger.annotations.ApiResponses;

	/**
	 * The voting card set end-point.
	 */
	@RestController
	@RequestMapping("/sdm-ws-rest/votingcardsets")
	@Api(value = "Voting card set REST API")
	public class VotingCardSetController {

		private static final Logger LOGGER = LoggerFactory.getLogger(VotingCardSetController.class);

		private static final String VOTING_CARD_SET_URL_PATH = "/electionevent/{electionEventId}/votingcardset/{votingCardSetId}";

		@Autowired
		IdleStatusService idleStatusService;

		@Autowired
		private VotingCardSetRepository votingCardSetRepository;

		// The configuration of contests / elections
		@Value("${elections.config.filename}")
		private String configFile;

		@Autowired
		private VotingCardSetPreparationService votingCardSetPreparationService;

		@Autowired
		private VotingCardSetService votingCardSetService;

		@Autowired
		private GenerateVerificationData generateVerificationData;

		@Autowired
		private VotingCardSetComputationService votingCardSetComputationService;

		@Autowired
		private ProgressManagerService<VotingCardGenerationJobStatus> progressManagerService;

		/**
		 * Changes the status of a list voting card sets by performing the appropriate operations. The HTTP call uses a PUT request to the voting card set
		 * endpoint with the desired status parameter. If the requested status cannot be transitioned to from the current one, the call will fail.
		 *
		 * @param electionEventId the election event id.
		 * @param votingCardSetId the voting card set id.
		 * @return a list of ids of the created voting card sets.
		 * @throws PayloadStorageException
		 * @throws PayloadSignatureException
		 * @throws PayloadVerificationException
		 */
		@PutMapping(value = VOTING_CARD_SET_URL_PATH, consumes = "application/json", produces = "application/json")
		@ApiOperation(value = "Change the status of a voting card set", notes = "Change the status of a voting card set, performing the necessary operations for the transition")
		@ApiResponses(value = { @ApiResponse(code = 404, message = "Not Found") })
		public ResponseEntity<Object> setVotingCardSetStatus(
				@ApiParam(value = "String", required = true)
				@PathVariable
				final String electionEventId,
				@ApiParam(value = "String", required = true)
				@PathVariable
				final String votingCardSetId,
				@ApiParam(required = true)
				@RequestBody
				final VotingCardSetUpdateInputData requestBody)
				throws ResourceNotFoundException, IOException, GeneralCryptoLibException, PayloadStorageException, PayloadSignatureException,
				PayloadVerificationException {

			validateUUID(electionEventId);
			validateUUID(votingCardSetId);

			ResponseEntity<Object> response;
			try {
				switch (requestBody.getStatus()) {
				case PRECOMPUTED:
					votingCardSetPreparationService.prepare(electionEventId, votingCardSetId, requestBody.getPrivateKeyPEM());
					generateVerificationData
							.precompute(votingCardSetId, electionEventId, requestBody.getPrivateKeyPEM(), requestBody.getAdminBoardId());
					response = new ResponseEntity<>(HttpStatus.NO_CONTENT);
					break;
				case COMPUTING:
					votingCardSetComputationService.compute(votingCardSetId, electionEventId);
					response = new ResponseEntity<>(HttpStatus.NO_CONTENT);
					break;
				case VCS_DOWNLOADED:
					votingCardSetService.download(votingCardSetId, electionEventId);
					response = new ResponseEntity<>(HttpStatus.NO_CONTENT);
					break;
				case SIGNED:
					response = new ResponseEntity<>(signVotingCardSet(electionEventId, votingCardSetId, requestBody.getPrivateKeyPEM()));
					break;
				default:
					response = new ResponseEntity<>("Status is not supported", HttpStatus.BAD_REQUEST);
				}

			} catch (InvalidStatusTransitionException e) {
				LOGGER.info("Error trying to set voting card set status.", e);
				response = ResponseEntity.badRequest().build();
			}
			return response;
		}

		/**
		 * Stores a list of voting card sets.
		 *
		 * @param electionEventId the election event id.
		 * @param votingCardSetId the voting card set id.
		 * @return a list of ids of the created voting card sets.
		 * @throws InvalidStatusTransitionException
		 */
		@PostMapping(value = VOTING_CARD_SET_URL_PATH, produces = "application/json")
		@ApiOperation(value = "Create voting card set", notes = "Service to create a voting card set.", response = Void.class)
		@ApiResponses(value = { @ApiResponse(code = 404, message = "Not Found") })
		public ResponseEntity<Object> createVotingCardSet(
				@ApiParam(value = "String", required = true)
				@PathVariable
				final String electionEventId,
				@ApiParam(value = "String", required = true)
				@PathVariable
				final String votingCardSetId, final UriComponentsBuilder uriBuilder)
				throws IOException, ResourceNotFoundException, InvalidStatusTransitionException {

			validateUUID(electionEventId);
			validateUUID(votingCardSetId);

			DataGeneratorResponse response = votingCardSetService.generate(votingCardSetId, electionEventId);

			if (response.isSuccessful()) {
				final URI uri = uriBuilder.path("/electionevent/{electionEventId}/progress/{jobId}")
						.buildAndExpand(electionEventId, votingCardSetId, response.getResult()).toUri();
				return ResponseEntity.created(uri).body(response);
			}

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}

		/**
		 * Returns an voting card set identified by election event and its id.
		 *
		 * @param electionEventId the election event id.
		 * @param votingCardSetId the voting card set id.
		 * @return An voting card set identified by election event and its id.
		 */
		@GetMapping(value = VOTING_CARD_SET_URL_PATH, produces = "application/json")
		@ResponseBody
		@ApiOperation(value = "Get voting card set", notes = "Service to retrieve a given voting card set.", response = String.class)
		@ApiResponses(value = { @ApiResponse(code = 404, message = "Not Found") })
		public ResponseEntity<String> getVotingCardSet(
				@ApiParam(value = "String", required = true)
				@PathVariable
				final String electionEventId,
				@ApiParam(value = "String", required = true)
				@PathVariable
				final String votingCardSetId) {

			validateUUID(electionEventId);
			validateUUID(votingCardSetId);

			Map<String, Object> attributeValueMap = new HashMap<>();
			attributeValueMap.put(JsonConstants.ID, votingCardSetId);
			attributeValueMap.put(JsonConstants.ELECTION_EVENT_DOT_ID, electionEventId);
			String result = votingCardSetRepository.find(attributeValueMap);
			JsonObject jsonObject = JsonUtils.getJsonObject(result);
			if (!jsonObject.isEmpty()) {
				return new ResponseEntity<>(result, HttpStatus.OK);
			}
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		/**
		 * Returns a list of all voting card sets.
		 *
		 * @param electionEventId the election event id.
		 * @return The list of voting card sets.
		 */
		@GetMapping(value = "/electionevent/{electionEventId}", produces = "application/json")
		@ResponseBody
		@ApiOperation(value = "List voting card sets", notes = "Service to retrieve the list of voting card sets.", response = String.class)
		public String getVotingCardSets(
				@PathVariable
						String electionEventId) {

			validateUUID(electionEventId);

			Map<String, Object> attributeValueMap = new HashMap<>();
			attributeValueMap.put(JsonConstants.ELECTION_EVENT_DOT_ID, electionEventId);
			return votingCardSetRepository.list(attributeValueMap);
		}

		/**
		 * Change the state of the voting card set from generated to signed for a given election event and voting card set id.
		 *
		 * @param electionEventId the election event id.
		 * @param votingCardSetId the voting card set id.
		 * @return HTTP status code 200 - If the voting card set is successfully signed. HTTP status code 404 - If the resource is not found. HTTP status
		 * code 412 - If the votig card set is already signed.
		 */
		private HttpStatus signVotingCardSet(String electionEventId, String votingCardSetId, String privateKeyPEM) {

			if (!idleStatusService.getIdLock(votingCardSetId)) {
				return HttpStatus.OK;
			}

			String fetchingErrorMessage = "An error occurred while fetching the given voting card set to sign";
			String signingErrorMessage = "An error occurred while signing the given voting card set";
			try {
				if (votingCardSetService.sign(electionEventId, votingCardSetId, privateKeyPEM)) {
					return HttpStatus.OK;
				} else {
					LOGGER.error(fetchingErrorMessage);

					return HttpStatus.PRECONDITION_FAILED;
				}
			} catch (ResourceNotFoundException e) {
				LOGGER.error(fetchingErrorMessage, e);
				return HttpStatus.NOT_FOUND;
			} catch (GeneralCryptoLibException | IOException e) {
				LOGGER.error(signingErrorMessage, e);

				return HttpStatus.PRECONDITION_FAILED;
			} finally {
				idleStatusService.freeIdLock(votingCardSetId);
			}
		}

		/**
		 * Get the status/progress of the specified voting card generation job.
		 *
		 * @param electionEventId the election event id.
		 * @param jobId           the job execution id.
		 * @return HTTP status code 200 - If we got the voting card status. HTTP status code 404 - If the resource is not found.
		 */
		@GetMapping(value = "/electionevent/{electionEventId}/progress/{jobId}")
		@ApiOperation(value = "Get voting card generation status", notes = "Service to get the status/progress of a specific voting card generation job", response = VotingCardGenerationJobStatus.class)
		@ApiResponses(value = { @ApiResponse(code = 404, message = "Not Found"), @ApiResponse(code = 412, message = "Precondition Failed") })
		public ResponseEntity<VotingCardGenerationJobStatus> getJobProgress(
				@ApiParam(value = "String", required = true)
				@PathVariable
						String electionEventId,
				@ApiParam(value = "String", required = true)
				@PathVariable
						String jobId) {

			validateUUID(electionEventId);
			validateUUID(jobId);

			final VotingCardGenerationJobStatus status = progressManagerService.getForJob(jobId);
			return ResponseEntity.ok(status);
		}

		/**
		 * Get the status/progress of all voting card generation jobs with a specific status (started by default) .
		 *
		 * @return HTTP status code 200 - If we got the voting card status. HTTP status code 404 - If the resource is not found.
		 */
		@GetMapping(value = "/progress/jobs")
		@ApiOperation(value = "Get voting card generation status", notes = "Service to get the status/progress of all started voting card generation job", response = VotingCardGenerationJobStatus.class)
		@ApiResponses(value = { @ApiResponse(code = 404, message = "Not Found"), @ApiResponse(code = 412, message = "Precondition Failed") })
		public ResponseEntity<List<VotingCardGenerationJobStatus>> getJobsProgress(
				@RequestParam(value = "status", required = false)
						String jobStatus) {

			final List<VotingCardGenerationJobStatus> jobsProgressByStatus = StringUtils.isBlank(jobStatus) ?
					progressManagerService.getAll() :
					progressManagerService.getAllByStatus(jobStatus);
			return ResponseEntity.ok(jobsProgressByStatus);
		}

	}
