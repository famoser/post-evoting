/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.clients;

import javax.ws.rs.core.MediaType;

import ch.post.it.evoting.sdm.commons.Constants;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

public interface ElectionInformationClient {

	@GET("electioneventdata/tenant/{tenantId}/electionevent/{electionEventId}/cast")
	@Streaming
	Call<ResponseBody> getCastedVotingCards(
			@Path(Constants.TENANT_ID)
			final String tenantId,
			@Path(Constants.ELECTION_EVENT_ID)
			final String electionEventId);

	@GET("ballotboxes/tenant/{tenantId}/electionevent/{electionEventId}/ballotbox/{ballotBoxId}")
	@Headers("Accept:" + MediaType.APPLICATION_OCTET_STREAM)
	@Streaming
	Call<ResponseBody> getRawBallotBox(
			@Path(Constants.TENANT_ID)
			final String tenantId,
			@Path(Constants.ELECTION_EVENT_ID)
			final String electionEventId,
			@Path(Constants.BALLOT_BOX_ID)
			final String ballotBoxId);

	@GET("cleansingoutputs/tenant/{tenantId}/electionevent/{electionEventId}/ballotbox/{ballotBoxId}/successfulvotes")
	@Headers("Accept:" + MediaType.APPLICATION_OCTET_STREAM)
	@Streaming
	Call<ResponseBody> downloadSuccessfulVotes(
			@Path(Constants.TENANT_ID)
			final String tenantId,
			@Path(Constants.ELECTION_EVENT_ID)
			final String electionEventId,
			@Path(Constants.BALLOT_BOX_ID)
			final String ballotBoxId);

	@GET("cleansingoutputs/tenant/{tenantId}/electionevent/{electionEventId}/ballotbox/{ballotBoxId}/failedvotes")
	@Headers("Accept:" + MediaType.APPLICATION_OCTET_STREAM)
	@Streaming
	Call<ResponseBody> downloadFailedVotes(
			@Path(Constants.TENANT_ID)
			final String tenantId,
			@Path(Constants.ELECTION_EVENT_ID)
			final String electionEventId,
			@Path(Constants.BALLOT_BOX_ID)
			final String ballotBoxId);
}
