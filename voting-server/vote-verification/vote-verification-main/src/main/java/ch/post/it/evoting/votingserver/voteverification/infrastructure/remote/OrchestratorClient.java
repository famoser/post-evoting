/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.infrastructure.remote;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MediaType;

import ch.post.it.evoting.domain.returncodes.ReturnCodesInput;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.ui.Constants;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

/**
 * Defines the methods to access via REST to a set of operations.
 */
public interface OrchestratorClient {

	/**
	 * Requests the Control Components contributions for the decryption of the partial choice codes
	 *
	 * @param pathOrchestrator      the path to the orchestrator
	 * @param tenantId              the tenant id
	 * @param electionEventId       the election event id
	 * @param verificationCardSetId the verification card set id
	 * @param verificationCardId    the verification card id
	 * @return the choice code nodes contributions necessary for decryption
	 * @throws ResourceNotFoundException the resource not found exception
	 */
	@POST("{pathOrchestrator}/tenant/{tenantId}/electionevent/{electionEventId}/verificationCardSetId/{verificationCardSetId}/verificationCardId/{verificationCardId}/decryptContributions")
	@Headers("Accept:" + MediaType.APPLICATION_JSON)
	Call<ResponseBody> getChoiceCodeNodesDecryptContributions(
			@Header(Constants.PARAMETER_X_REQUEST_ID)
					String trackId,
			@Path(Constants.PATH_ORCHESTRATOR)
					String pathOrchestrator,
			@Path(Constants.PARAMETER_VALUE_TENANT_ID)
					String tenantId,
			@Path(Constants.PARAMETER_VALUE_ELECTION_EVENT_ID)
					String electionEventId,
			@Path(Constants.PARAMETER_VALUE_VERIFICATION_CARD_SET_ID)
					String verificationCardSetId,
			@Path(Constants.VERIFICATION_CARD_ID)
					String verificationCardId,
			@NotNull
			@Body
					ReturnCodesInput returnCodesInput) throws ResourceNotFoundException;

	/**
	 * Requests the Control Components contributions for the computation of the partial choice codes
	 *
	 * @param pathOrchestrator      the path to the orchestrator
	 * @param tenantId              the tenant id
	 * @param electionEventId       the election event id
	 * @param verificationCardSetId the verification card set id
	 * @param verificationCardId    the verification card id
	 * @return the choice code nodes contributions necessary for computation
	 */
	@POST("{pathOrchestrator}/tenant/{tenantId}/electionevent/{electionEventId}/verificationCardSetId/{verificationCardSetId}/verificationCardId/{verificationCardId}/computeVotingContributions")
	@Headers("Accept:" + MediaType.APPLICATION_OCTET_STREAM)
	@Streaming
	Call<ResponseBody> getChoiceCodeNodesComputeContributions(
			@Header(Constants.PARAMETER_X_REQUEST_ID)
					String trackId,
			@Path(Constants.PATH_ORCHESTRATOR)
					String pathOrchestrator,
			@Path(Constants.PARAMETER_VALUE_TENANT_ID)
					String tenantId,
			@Path(Constants.PARAMETER_VALUE_ELECTION_EVENT_ID)
					String electionEventId,
			@Path(Constants.PARAMETER_VALUE_VERIFICATION_CARD_SET_ID)
					String verificationCardSetId,
			@Path(Constants.VERIFICATION_CARD_ID)
					String verificationCardId,
			@NotNull
			@Body
					ReturnCodesInput returnCodesInput);

}
