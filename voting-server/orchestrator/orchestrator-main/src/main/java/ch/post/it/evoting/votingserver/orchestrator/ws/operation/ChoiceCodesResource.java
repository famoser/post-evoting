/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.ws.operation;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.domain.returncodes.ChoiceCodesVerificationDecryptResPayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodeGenerationRequestPayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodesExponentiationResponsePayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodesInput;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationExceptionMessages;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.DuplicateEntryException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.beans.key.generation.request.KeyGenerationRequestParameters;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdInstance;
import ch.post.it.evoting.votingserver.commons.ui.Constants;
import ch.post.it.evoting.votingserver.commons.ui.ws.rs.persistence.ErrorCodes;
import ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.model.computedvalues.ChoiceCodesComputationStatus;
import ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.services.ChoiceCodesDecryptionContributionsService;
import ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.services.ChoiceCodesGenerationContributionsService;
import ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.services.ChoiceCodesKeyGenerationService;
import ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.services.ChoiceCodesKeysResponse;
import ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.services.ChoiceCodesVerificationContributionsService;

@Path(ChoiceCodesResource.RESOURCE_PATH)
@Stateless(name = "or-ChoiceCodesResource")
public class ChoiceCodesResource {

	/* Base path to resource */
	static final String RESOURCE_PATH = "choicecodes";

	static final String PATH_COMPUTE_VOTING_CONTRIBUTIONS = "tenant/{tenantId}/electionevent/{electionEventId}/verificationCardSetId/{verificationCardSetId}/verificationCardId/{verificationCardId}/computeVotingContributions";

	static final String PATH_COMPUTE_GENERATION_CONTRIBUTIONS_REQUEST = "computeGenerationContributions";

	static final String PATH_COMPUTE_GENERATION_CONTRIBUTIONS_RETRIEVAL = "tenant/{tenantId}/electionevent/{electionEventId}/verificationCardSetId/{verificationCardSetId}/chunkId/{chunkId}/computeGenerationContributions";

	static final String PATH_COMPUTE_GENERATION_CONTRIBUTIONS_STATUS = "tenant/{tenantId}/electionevent/{electionEventId}/verificationCardSetId/{verificationCardSetId}/generationContributions/status";

	static final String PATH_DECRYPT_CONTRIBUTIONS = "tenant/{tenantId}/electionevent/{electionEventId}/verificationCardSetId/{verificationCardSetId}/verificationCardId/{verificationCardId}/decryptContributions";

	static final String PATH_KEYS = "tenant/{tenantId}/electionevent/{electionEventId}/keys";

	static final String PATH_PARAMETER_TENANT_ID = "tenantId";

	static final String PATH_PARAMETER_ELECTION_EVENT_ID = "electionEventId";

	static final String PATH_PARAMETER_VERIFICATION_CARD_ID = "verificationCardId";

	static final String PATH_PARAMETER_VERIFICATION_CARD_SET_ID = "verificationCardSetId";

	static final String PATH_PARAMETER_CHUNK_ID = "chunkId";

	static final String QUERY_PARAMETER_CHUNK_COUNT = "chunkCount";

	@Inject
	private Logger logger;

	@Inject
	private TrackIdInstance trackIdInstance;

	@Inject
	private ChoiceCodesDecryptionContributionsService choiceCodesDecryptionContributionsService;

	@Inject
	private ChoiceCodesVerificationContributionsService choiceCodesVerificationContributionsService;

	@Inject
	private ChoiceCodesGenerationContributionsService choiceCodesGenerationContributionsService;

	@Inject
	private ChoiceCodesKeyGenerationService choiceCodesKeyGenerationService;

	private static void toOutputStream(List<ReturnCodesExponentiationResponsePayload> body, OutputStream outputStream) throws IOException {
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
		objectOutputStream.writeObject(body);
		objectOutputStream.flush();
	}

	private static ElGamalEncryptionParameters recoverEncryptionParameters(String encodedEncryptionParameters) throws GeneralCryptoLibException {

		String encryptionParamsAsJson = new String(Base64.getDecoder().decode(encodedEncryptionParameters), StandardCharsets.UTF_8);
		return ElGamalEncryptionParameters.fromJson(encryptionParamsAsJson);
	}

	private static void validateTenantIDAndElectionEventID(final String tenantId, final String electionEventId) throws ApplicationException {
		if (tenantId == null || tenantId.isEmpty()) {
			throw new ApplicationException(ApplicationExceptionMessages.EXCEPTION_MESSAGE_QUERY_PARAMETER_IS_NULL, RESOURCE_PATH,
					ErrorCodes.MISSING_QUERY_PARAMETER, PATH_PARAMETER_TENANT_ID);
		}

		if (electionEventId == null || electionEventId.isEmpty()) {
			throw new ApplicationException(ApplicationExceptionMessages.EXCEPTION_MESSAGE_QUERY_PARAMETER_IS_NULL, RESOURCE_PATH,
					ErrorCodes.MISSING_QUERY_PARAMETER, PATH_PARAMETER_ELECTION_EVENT_ID);
		}
	}

	@POST
	@Path(PATH_COMPUTE_VOTING_CONTRIBUTIONS)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getChoiceCodeNodesComputeForVotingContributions(
			@HeaderParam(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@PathParam(PATH_PARAMETER_TENANT_ID)
			final String tenantId,
			@PathParam(PATH_PARAMETER_ELECTION_EVENT_ID)
			final String electionEventId,
			@PathParam(PATH_PARAMETER_VERIFICATION_CARD_SET_ID)
			final String verificationCardSetId,
			@PathParam(PATH_PARAMETER_VERIFICATION_CARD_ID)
			final String verificationCardId,
			@NotNull
					ReturnCodesInput partialCodes,
			@Context
			final HttpServletRequest request) throws ApplicationException, ResourceNotFoundException {

		trackIdInstance.setTrackId(trackingId);

		validateParameters(tenantId, electionEventId, verificationCardId);

		logger.info("Requesting collection of the choice codes voting phase compute contributions for tenant {} electionEventId {} "
				+ "verificationCardSetId {} and verificationCardId {}", tenantId, electionEventId, verificationCardSetId, verificationCardId);

		List<ReturnCodesExponentiationResponsePayload> result = choiceCodesVerificationContributionsService
				.request(trackingId, electionEventId, verificationCardSetId, verificationCardId, partialCodes);

		StreamingOutput entity = stream -> toOutputStream(result, stream);

		return Response.ok().entity(entity).build();
	}

	@POST
	@Path(PATH_COMPUTE_GENERATION_CONTRIBUTIONS_REQUEST)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getChoiceCodeNodesComputeForGenerationContributions(
			@HeaderParam(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@NotNull
					ReturnCodeGenerationRequestPayload payload,
			@Context
			final HttpServletRequest request) throws ApplicationException, DuplicateEntryException, ResourceNotFoundException {

		trackIdInstance.setTrackId(trackingId);

		String tenantId = payload.getTenantId();
		String electionEventId = payload.getElectionEventId();
		String verificationCardSetId = payload.getVerificationCardSetId();
		int chunkId = payload.getChunkId();

		validateParameters(tenantId, electionEventId, verificationCardSetId);

		logger.info("OR:{} - Requesting collection of the choice codes generation phase compute contributions for tenant {}"
						+ " electionEventId {} verificationCardSetId {} and chunkId {}", trackingId, tenantId, electionEventId, verificationCardSetId,
				chunkId);

		choiceCodesGenerationContributionsService.request(trackingId, payload);

		return Response.ok().build();
	}

	@GET
	@Path(PATH_COMPUTE_GENERATION_CONTRIBUTIONS_RETRIEVAL)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getChoiceCodeNodesComputeForGenerationContributions(
			@HeaderParam(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@PathParam(PATH_PARAMETER_TENANT_ID)
			final String tenantId,
			@PathParam(PATH_PARAMETER_ELECTION_EVENT_ID)
			final String electionEventId,
			@PathParam(PATH_PARAMETER_VERIFICATION_CARD_SET_ID)
			final String verificationCardSetId,
			@PathParam(PATH_PARAMETER_CHUNK_ID)
			final int chunkId,
			@Context
			final HttpServletRequest request) throws ApplicationException, ResourceNotFoundException {

		trackIdInstance.setTrackId(trackingId);

		validateParameters(tenantId, electionEventId, verificationCardSetId);

		logger.info("Retrieving collection of the choice codes generation phase compute contributions for tenant {} electionEventId {} "
				+ "and verificationCardSetId {}", tenantId, electionEventId, verificationCardSetId);

		ChoiceCodesComputationStatus contributionsStatus = choiceCodesGenerationContributionsService
				.getComputedValuesStatus(electionEventId, tenantId, verificationCardSetId, chunkId);

		if (ChoiceCodesComputationStatus.COMPUTED.equals(contributionsStatus)) {

			StreamingOutput entity = stream -> {
				try {
					choiceCodesGenerationContributionsService.writeToStream(stream, tenantId, electionEventId, verificationCardSetId, chunkId);
				} catch (ResourceNotFoundException e) {
					throw new WebApplicationException(e, Status.NOT_FOUND);
				}
			};

			return Response.ok().entity(entity).header("Content-Disposition", "attachment;").build();
		} else {
			return Response.status(Status.NOT_FOUND).build();
		}
	}

	@GET
	@Path(PATH_COMPUTE_GENERATION_CONTRIBUTIONS_STATUS)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getChoiceCodesGenerationComputationStatus(
			@HeaderParam(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@PathParam(PATH_PARAMETER_TENANT_ID)
			final String tenantId,
			@PathParam(PATH_PARAMETER_ELECTION_EVENT_ID)
			final String electionEventId,
			@PathParam(PATH_PARAMETER_VERIFICATION_CARD_SET_ID)
			final String verificationCardSetId,
			@QueryParam(QUERY_PARAMETER_CHUNK_COUNT)
			final int chunkCount,
			@Context
			final HttpServletRequest request) throws ApplicationException {

		trackIdInstance.setTrackId(trackingId);

		validateParameters(tenantId, electionEventId, verificationCardSetId);

		logger.info("Checking status of the choice codes generation phase compute contributions for tenant {} electionEventId {} "
				+ "and verificationCardSetId {}", tenantId, electionEventId, verificationCardSetId);

		try {
			ChoiceCodesComputationStatus contributionsStatus = choiceCodesGenerationContributionsService
					.getCompositeComputedValuesStatus(electionEventId, tenantId, verificationCardSetId, chunkCount);
			String result = Json.createObjectBuilder().add("status", contributionsStatus.name()).build().toString();
			return Response.ok().entity(result).build();

		} catch (ResourceNotFoundException e) {
			logger.error("Resource not found matching the received parameters", e);
			return Response.status(Status.NOT_FOUND).build();
		}
	}

	@POST
	@Path(PATH_DECRYPT_CONTRIBUTIONS)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getChoiceCodeNodesDecryptContributions(
			@HeaderParam(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@PathParam(PATH_PARAMETER_TENANT_ID)
			final String tenantId,
			@PathParam(PATH_PARAMETER_ELECTION_EVENT_ID)
			final String electionEventId,
			@PathParam(PATH_PARAMETER_VERIFICATION_CARD_SET_ID)
			final String verificationCardSetId,
			@PathParam(PATH_PARAMETER_VERIFICATION_CARD_ID)
			final String verificationCardId,
			@NotNull
			final ReturnCodesInput partialCodes,
			@Context
			final HttpServletRequest request) throws ApplicationException, ResourceNotFoundException {

		trackIdInstance.setTrackId(trackingId);

		validateParameters(tenantId, electionEventId, verificationCardId);

		logger.info("Requesting collection of the choice codes decrypt contributions for tenant {} electionEventId {} " + "and verificationCardId {}",
				tenantId, electionEventId, verificationCardId);

		List<ChoiceCodesVerificationDecryptResPayload> result = choiceCodesDecryptionContributionsService
				.requestChoiceCodesDecryptionContributionsSync(trackingId, tenantId, electionEventId, verificationCardSetId, verificationCardId,
						partialCodes);
		return Response.ok().entity(result).build();

	}

	@POST
	@Path(PATH_KEYS)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response generateKeys(
			@HeaderParam(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@PathParam(PATH_PARAMETER_TENANT_ID)
			final String tenantId,
			@PathParam(PATH_PARAMETER_ELECTION_EVENT_ID)
			final String electionEventId,
			@NotNull
			final KeyGenerationRequestParameters keyGenerationRequestParameters,
			@Context
			final HttpServletRequest request) throws ApplicationException, ResourceNotFoundException, GeneralCryptoLibException {

		trackIdInstance.setTrackId(trackingId);

		validateTenantIDAndElectionEventID(tenantId, electionEventId);

		List<String> verificationCardSetIds = keyGenerationRequestParameters.getResourceIds();
		ZonedDateTime keysDateFrom = ZonedDateTime.parse(keyGenerationRequestParameters.getKeysDateFrom());
		ZonedDateTime keysDateTo = ZonedDateTime.parse(keyGenerationRequestParameters.getKeysDateTo());
		ElGamalEncryptionParameters elGamalEncryptionParameters = recoverEncryptionParameters(
				keyGenerationRequestParameters.getElGamalEncryptionParameters());

		logger.info("Requesting Choice Codes key generation for tenant {} electionEventId {}", tenantId, electionEventId);

		ChoiceCodesKeysResponse choiceCodesKeyResponse = choiceCodesKeyGenerationService
				.requestChoiceCodesKeyGenerationSync(trackingId, tenantId, electionEventId, verificationCardSetIds, keysDateFrom, keysDateTo,
						elGamalEncryptionParameters);

		return Response.ok().entity(choiceCodesKeyResponse).build();
	}

	private void validateParameters(final String tenantId, final String electionEventId, final String verificationCardSetId)
			throws ApplicationException {

		validateTenantIDAndElectionEventID(tenantId, electionEventId);

		if (verificationCardSetId == null || verificationCardSetId.isEmpty()) {
			throw new ApplicationException(ApplicationExceptionMessages.EXCEPTION_MESSAGE_QUERY_PARAMETER_IS_NULL, RESOURCE_PATH,
					ErrorCodes.MISSING_QUERY_PARAMETER, PATH_PARAMETER_VERIFICATION_CARD_ID);
		}
	}
}
