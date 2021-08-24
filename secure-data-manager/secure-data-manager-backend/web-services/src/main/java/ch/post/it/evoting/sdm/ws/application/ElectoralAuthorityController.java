/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.ws.application;

import static ch.post.it.evoting.cryptolib.commons.validations.Validate.validateUUID;

import java.io.IOException;

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
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.application.service.ElectoralAuthorityService;
import ch.post.it.evoting.sdm.config.shares.exception.SharesException;
import ch.post.it.evoting.sdm.domain.model.administrationauthority.ActivateOutputData;
import ch.post.it.evoting.sdm.domain.model.administrationauthority.ReadShareInputData;
import ch.post.it.evoting.sdm.domain.model.administrationauthority.ReadShareOutputData;
import ch.post.it.evoting.sdm.domain.model.administrationauthority.ReconstructInputData;
import ch.post.it.evoting.sdm.domain.model.administrationauthority.ReconstructOutputData;
import ch.post.it.evoting.sdm.domain.model.administrationauthority.WriteShareInputData;
import ch.post.it.evoting.sdm.domain.model.electoralauthority.ElectoralAuthorityRepository;
import ch.post.it.evoting.sdm.domain.model.electoralauthority.ElectoralAuthoritySignInputData;
import ch.post.it.evoting.sdm.domain.model.status.SmartCardStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * The REST endpoint for accessing electoral authority data.
 */
@RestController
@RequestMapping("/sdm-ws-rest/electoralauthorities")
@Api(value = "Electoral authorities REST API")
public class ElectoralAuthorityController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElectoralAuthorityController.class);

	@Autowired
	private ElectoralAuthorityRepository electoralAuthorityRepository;

	@Autowired
	private ElectoralAuthorityService electoralAuthorityService;

	/**
	 * Returns a list of electoral authorities identified by an election event identifier.
	 *
	 * @param electionEventId the election event id.
	 * @return a list of electoral authorities belong to an election event.
	 */
	@GetMapping(value = "/electionevent/{electionEventId}", produces = "application/json")
	@ResponseBody
	@ApiOperation(value = "List electoral authorities", response = String.class, notes = "Service to retrieve the list "
			+ "of electoral authorities for a given election event.")
	public String getElectoralAuthorities4ElectionEventId(
			@PathVariable
			final String electionEventId) {

		validateUUID(electionEventId);

		return electoralAuthorityRepository.listByElectionEvent(electionEventId);
	}

	/**
	 * Execute the constitute action: create keypair, split private key into shares and keep them in
	 * memory.
	 *
	 * @param electionEventId      the election event id.
	 * @param electoralAuthorityId the electoral authority id.
	 */
	@PostMapping(value = "/constitute/{electionEventId}/{electoralAuthorityId}")
	@ApiOperation(value = "Constitute Service", notes = "Service to generate a key pair and splits the private key into shares.", response = Void.class)
	public ResponseEntity<Void> constitute(
			@ApiParam(value = "String", required = true)
			@PathVariable
			final String electionEventId,
			@ApiParam(value = "String", required = true)
			@PathVariable
			final String electoralAuthorityId) throws ResourceNotFoundException, SharesException, GeneralCryptoLibException {

		validateUUID(electionEventId);
		validateUUID(electoralAuthorityId);

		electoralAuthorityService.constitute(electionEventId, electoralAuthorityId);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	/**
	 * Check smartcard reader status: it can be EMPTY or INSERTED.
	 *
	 * @return STATUS
	 */
	@GetMapping(value = "/shares/status")
	@ApiOperation(value = "Check smartcard reader status", notes = "Checks the smartcard reader status.", response = SmartCardStatus.class)
	public ResponseEntity<SmartCardStatus> status() {

		SmartCardStatus status = electoralAuthorityService.getSmartCardReaderStatus();

		return new ResponseEntity<>(status, HttpStatus.OK);
	}

	/**
	 * Write share for the corresponding member.
	 *
	 * @return STATUS
	 */
	@PostMapping(value = "/{electionEventId}/{electoralAuthorityId}/shares/{shareNumber}")
	@ApiOperation(value = "Write share", notes = "Writes a the share specified by the share number into the smartcard", response = Void.class)
	public ResponseEntity<Void> writeShare(
			@ApiParam(value = "String", required = true)
			@PathVariable
			final String electionEventId,
			@ApiParam(value = "String", required = true)
			@PathVariable
			final String electoralAuthorityId,
			@ApiParam(value = "Integer", required = true)
			@PathVariable
			final Integer shareNumber,
			@ApiParam(value = "WriteShareInputData", required = true)
			@RequestBody
			final WriteShareInputData inputData) throws SharesException, IOException, GeneralCryptoLibException, ResourceNotFoundException {

		validateUUID(electionEventId);
		validateUUID(electoralAuthorityId);

		electoralAuthorityService.writeShare(electionEventId, electoralAuthorityId, shareNumber, inputData.getPin());

		return new ResponseEntity<>(HttpStatus.OK);
	}

	/**
	 * Executes the activate action.
	 */
	@PostMapping(value = "/{electionEventId}/{electoralAuthorityId}/activate")
	@ApiOperation(value = "Activate", notes = "Starts the process to activate an electoral authority. "
			+ "The issuer and the subject public key are returned.", response = ActivateOutputData.class)
	public ResponseEntity<ActivateOutputData> activate(
			@ApiParam(value = "String", required = true)
			@PathVariable
			final String electionEventId,
			@ApiParam(value = "String", required = true)
			@PathVariable
			final String electoralAuthorityId) throws GeneralCryptoLibException {

		validateUUID(electionEventId);
		validateUUID(electoralAuthorityId);

		ActivateOutputData output = electoralAuthorityService.activate(electionEventId, electoralAuthorityId);
		return new ResponseEntity<>(output, HttpStatus.OK);
	}

	/**
	 * Executes the read share action, and returns the serialized share in Base64 encoded format.
	 */
	@PostMapping(value = "/{electionEventId}/{electoralAuthorityId}/read/{shareNumber}")
	@ApiOperation(value = "Read share", notes = "Service to read a share and returns it serialized.", response = ReadShareOutputData.class)
	public ResponseEntity<ReadShareOutputData> readShare(
			@ApiParam(value = "String", required = true)
			@PathVariable
			final String electionEventId,
			@ApiParam(value = "String", required = true)
			@PathVariable
			final String electoralAuthorityId,
			@ApiParam(value = "Integer", required = true)
			@PathVariable
					Integer shareNumber,
			@ApiParam(value = "ReadShareInputData", required = true)
			@RequestBody
					ReadShareInputData inputData) throws ResourceNotFoundException, SharesException {

		validateUUID(electionEventId);
		validateUUID(electoralAuthorityId);

		String share = electoralAuthorityService
				.readShare(electionEventId, electoralAuthorityId, shareNumber, inputData.getPin(), inputData.getPublicKeyPEM());

		ReadShareOutputData outputData = new ReadShareOutputData();
		outputData.setSerializedShare(share);

		return new ResponseEntity<>(outputData, HttpStatus.OK);
	}

	/**
	 * Executes the reconstruct action, given a list of serialized electoral authority shares.
	 */
	@PostMapping(value = "/{electionEventId}/{electoralAuthorityId}/reconstruct")
	@ApiOperation(value = "Reconstruct", notes = "Reconstructs the private key of an electoral authority, "
			+ "given a list of serialized private keys.", response = ReconstructOutputData.class)
	public ResponseEntity<ReconstructOutputData> reconstruct(
			@ApiParam(value = "String", required = true)
			@PathVariable
			final String electionEventId,
			@ApiParam(value = "String", required = true)
			@PathVariable
			final String electoralAuthorityId,
			@ApiParam(value = "ReconstructInputData", required = true)
			@RequestBody
					ReconstructInputData inputData) throws SharesException, GeneralCryptoLibException {

		validateUUID(electionEventId);
		validateUUID(electoralAuthorityId);

		String serializedPrivateKey = electoralAuthorityService
				.reconstruct(electoralAuthorityId, inputData.getSerializedShares(), inputData.getSerializedPublicKey());
		ReconstructOutputData output = new ReconstructOutputData();
		output.setSerializedPrivateKey(serializedPrivateKey);
		return new ResponseEntity<>(output, HttpStatus.OK);
	}

	/**
	 * Change the state of the electoral authority from constituted to signed for a given election
	 * event and electoral authority id.
	 *
	 * @param electionEventId      the election event id.
	 * @param electoralAuthorityId the electoral authority id.
	 * @return HTTP status code 200 - If the electoral authority is successfully signed. HTTP status
	 * code 404 - If the resource is not found. HTTP status code 412 - If the electoral
	 * authority is already signed.
	 */
	@PutMapping(value = "/electionevent/{electionEventId}/electoralauthority/{electoralAuthorityId}")
	@ApiOperation(value = "Sign electoral authority", notes = "Service to change the state of the electoral authority from constituted to signed "
			+ "for a given election event and electoral authority id..", response = Void.class)
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Not Found"), @ApiResponse(code = 412, message = "Precondition Failed") })
	public ResponseEntity<Void> signElectoralAuthority(
			@ApiParam(value = "String", required = true)
			@PathVariable
					String electionEventId,
			@ApiParam(value = "String", required = true)
			@PathVariable
					String electoralAuthorityId,
			@ApiParam(value = "ElectoralAuthoritySignInputData", required = true)
			@RequestBody
					ElectoralAuthoritySignInputData inputData) {

		validateUUID(electionEventId);
		validateUUID(electoralAuthorityId);

		try {

			if (electoralAuthorityService.sign(electionEventId, electoralAuthorityId, inputData.getPrivateKeyPEM())) {
				return new ResponseEntity<>(HttpStatus.OK);
			} else {
				LOGGER.error("An error occurred while fetching the given electoral authority to sign");
				return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
			}
		} catch (ResourceNotFoundException e) {
			LOGGER.error("An error occurred while fetching the given electoral authority to sign", e);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} catch (GeneralCryptoLibException | IOException e) {
			LOGGER.error("An error occurred while signing the given electoral authority", e);
			return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
		}
	}
}
