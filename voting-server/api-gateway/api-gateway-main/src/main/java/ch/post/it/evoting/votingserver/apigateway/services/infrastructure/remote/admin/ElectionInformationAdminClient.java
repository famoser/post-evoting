/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.apigateway.services.infrastructure.remote.admin;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MediaType;

import com.google.gson.JsonObject;

import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RestClientInterceptor;
import ch.post.it.evoting.votingserver.commons.ui.Constants;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

/**
 * The Interface of election information client for admin.
 */
public interface ElectionInformationAdminClient {

	@GET("{pathBallotboxes}/secured/tenant/{tenantId}/electionevent/{electionEventId}/ballotbox/{ballotBoxId}")
	@Headers("Accept: text/csv")
	Call<ResponseBody> getEncryptedBallotBoxAsString(
			@Path(Constants.PARAMETER_PATH_BALLOTBOXES)
					String pathBallotboxes,
			@Path(Constants.PARAMETER_VALUE_TENANT_ID)
					String tenantId,
			@Path(Constants.PARAMETER_VALUE_ELECTION_EVENT_ID)
					String electionEventId,
			@Path(Constants.PARAMETER_VALUE_BALLOT_BOX_ID)
					String ballotBoxId,
			@NotNull
			@Header(Constants.PARAMETER_X_FORWARDED_FOR)
					String xForwardedFor,
			@NotNull
			@Header(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@NotNull
			@Header(RestClientInterceptor.HEADER_ORIGINATOR)
					String originator,
			@NotNull
			@Header(RestClientInterceptor.HEADER_SIGNATURE)
					String signature);

	@GET("{pathBallotboxes}/secured/tenant/{tenantId}/electionevent/{electionEventId}/ballotbox/{ballotBoxId}")
	@Headers("Accept:" + MediaType.APPLICATION_OCTET_STREAM)
	@Streaming
	Call<ResponseBody> getEncryptedBallotBox(
			@Path(Constants.PARAMETER_PATH_BALLOTBOXES)
					String pathBallotboxes,
			@Path(Constants.PARAMETER_VALUE_TENANT_ID)
					String tenantId,
			@Path(Constants.PARAMETER_VALUE_ELECTION_EVENT_ID)
					String electionEventId,
			@Path(Constants.PARAMETER_VALUE_BALLOT_BOX_ID)
					String ballotBoxId,
			@NotNull
			@Header(Constants.PARAMETER_X_FORWARDED_FOR)
					String xForwardedFor,
			@NotNull
			@Header(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@NotNull
			@Header(RestClientInterceptor.HEADER_ORIGINATOR)
					String originator,
			@NotNull
			@Header(RestClientInterceptor.HEADER_SIGNATURE)
					String signature);

	@POST("{pathBallotdata}/tenant/{tenantId}/electionevent/{electionEventId}/ballot/{ballotId}/adminboard/{adminBoardId}")
	Call<ResponseBody> saveBallotData(
			@Path(Constants.PARAMETER_PATH_BALLOTDATA)
					String pathBallotdata,
			@Path(Constants.PARAMETER_VALUE_TENANT_ID)
					String tenantId,
			@Path(Constants.PARAMETER_VALUE_ELECTION_EVENT_ID)
					String electionEventId,
			@Path(Constants.PARAMETER_VALUE_BALLOT_ID)
					String ballotId,
			@Path(Constants.PARAMETER_VALUE_ADMIN_BOARD_ID)
					String adminBoardId,
			@NotNull
			@Header(Constants.PARAMETER_X_FORWARDED_FOR)
					String xForwardedFor,
			@NotNull
			@Header(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@NotNull
			@Body
					RequestBody ballotData);

	@POST("{pathBallotboxdata}/tenant/{tenantId}/electionevent/{electionEventId}/ballotbox/{ballotBoxId}/adminboard/{adminBoardId}")
	Call<ResponseBody> addBallotBoxContentAndInformation(
			@Path(Constants.PARAMETER_PATH_BALLOTBOXDATA)
					String pathBallotboxdata,
			@Path(Constants.PARAMETER_VALUE_TENANT_ID)
					String tenantId,
			@Path(Constants.PARAMETER_VALUE_ELECTION_EVENT_ID)
					String electionEventId,
			@Path(Constants.PARAMETER_VALUE_BALLOT_BOX_ID)
					String ballotBoxId,
			@Path(Constants.PARAMETER_VALUE_ADMIN_BOARD_ID)
					String adminBoardId,
			@NotNull
			@Header(Constants.PARAMETER_X_FORWARDED_FOR)
					String xForwardedFor,
			@NotNull
			@Header(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@NotNull
			@Body
					RequestBody ballotBoxData);

	@POST("{pathElectoraldata}/tenant/{tenantId}/electionevent/{electionEventId}/electoralauthority/{electoralAuthorityId}/adminboard/{adminBoardId}")
	Call<ResponseBody> saveElectoralData(
			@Path(Constants.PARAMETER_PATH_ELECTORALDATA)
					String pathElectoraldata,
			@Path(Constants.PARAMETER_VALUE_TENANT_ID)
					String tenantId,
			@Path(Constants.PARAMETER_VALUE_ELECTION_EVENT_ID)
					String electionEventId,
			@Path(Constants.PARAMETER_VALUE_ELECTORAL_AUTHORITY_ID)
					String electoralAuthorityId,
			@Path(Constants.PARAMETER_VALUE_ADMIN_BOARD_ID)
					String adminBoardId,
			@NotNull
			@Header(Constants.PARAMETER_X_FORWARDED_FOR)
					String xForwardedFor,
			@NotNull
			@Header(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@NotNull
			@Body
					RequestBody electoralData);

	@POST("{pathElectioneventdata}/tenant/{tenantId}/electionevent/{electionEventId}/adminboard/{adminBoardId}")
	Call<ResponseBody> saveElectionEventData(
			@Path(Constants.PARAMETER_PATH_ELECTIONEVENTDATA)
					String pathElectioneventdata,
			@Path(Constants.PARAMETER_VALUE_TENANT_ID)
					String tenantId,
			@Path(Constants.PARAMETER_VALUE_ELECTION_EVENT_ID)
					String electionEventId,
			@Path(Constants.PARAMETER_VALUE_ADMIN_BOARD_ID)
					String adminBoardId,
			@NotNull
			@Header(Constants.PARAMETER_X_FORWARDED_FOR)
					String xForwardedFor,
			@NotNull
			@Header(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@NotNull
			@Body
					RequestBody electioneventData);

	@GET("{pathBallotboxstatus}/tenant/{tenantId}/electionevent/{electionEventId}/ballotbox/{ballotBoxId}/status")
	Call<JsonObject> checkIfBallotBoxIsEmpty(
			@Path(Constants.PARAMETER_PATH_BALLOTBOXSTATUS)
					String pathBallotboxstatus,
			@Path(Constants.PARAMETER_VALUE_TENANT_ID)
					String tenantId,
			@Path(Constants.PARAMETER_VALUE_ELECTION_EVENT_ID)
					String electionEventId,
			@Path(Constants.PARAMETER_VALUE_BALLOT_BOX_ID)
					String ballotBoxId,
			@NotNull
			@Header(Constants.PARAMETER_X_FORWARDED_FOR)
					String xForwardedFor,
			@NotNull
			@Header(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId);

	@GET("{pathBallotboxstatus}/tenant/{tenantId}/electionevent/{electionEventId}/ballotbox/{ballotBoxId}/available")
	Call<JsonObject> checkIfBallotBoxIsAvailable(
			@Path(Constants.PARAMETER_PATH_BALLOTBOXSTATUS)
					String pathBallotboxstatus,
			@Path(Constants.PARAMETER_VALUE_TENANT_ID)
					String tenantId,
			@Path(Constants.PARAMETER_VALUE_ELECTION_EVENT_ID)
					String electionEventId,
			@Path(Constants.PARAMETER_VALUE_BALLOT_BOX_ID)
					String ballotBoxId,
			@NotNull
			@Header(Constants.PARAMETER_X_FORWARDED_FOR)
					String xForwardedFor,
			@NotNull
			@Header(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId);

	@GET("{pathElectioneventdata}/tenant/{tenantId}/electionevent/{electionEventId}/status")
	Call<JsonObject> checkIfElectionEventDataIsEmpty(
			@Path(Constants.PARAMETER_PATH_ELECTIONEVENTDATA)
					String pathElectioneventdata,
			@Path(Constants.PARAMETER_VALUE_TENANT_ID)
					String tenantId,
			@Path(Constants.PARAMETER_VALUE_ELECTION_EVENT_ID)
					String electionEventId,
			@NotNull
			@Header(Constants.PARAMETER_X_FORWARDED_FOR)
					String xForwardedFor,
			@NotNull
			@Header(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId);

	@GET("{pathElectioneventdata}/secured/tenant/{tenantId}/electionevent/{electionEventId}/used-voting-cards")
	@Streaming
	Call<ResponseBody> getCastedVotingCardsReport(
			@Path(Constants.PARAMETER_PATH_ELECTIONEVENTDATA)
					String pathElectioneventdata,
			@Path(Constants.PARAMETER_VALUE_TENANT_ID)
					String tenantId,
			@Path(Constants.PARAMETER_VALUE_ELECTION_EVENT_ID)
					String electionEventId,
			@NotNull
			@Header(Constants.PARAMETER_X_FORWARDED_FOR)
					String xForwardedFor,
			@NotNull
			@Header(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@NotNull
			@Header(RestClientInterceptor.HEADER_ORIGINATOR)
					String originator,
			@NotNull
			@Header(RestClientInterceptor.HEADER_SIGNATURE)
					String signature);

	@GET("{pathTenantData}/activatetenant/tenant/{tenantId}")
	Call<JsonObject> checkTenantActivation(
			@Path(Constants.PARAMETER_PATH_TENANT_DATA)
					String pathTenantData,
			@Path(Constants.PARAMETER_VALUE_TENANT_ID)
					String tenantId,
			@NotNull
			@Header(Constants.PARAMETER_X_FORWARDED_FOR)
					String xForwardedFor,
			@NotNull
			@Header(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId);

	@GET("{pathCleansingOutputs}/secured/tenant/{tenantId}/electionevent/{electionEventId}/ballotbox/{ballotBoxId}/successfulvotes")
	@Headers("Accept:" + MediaType.APPLICATION_OCTET_STREAM)
	@Streaming
	Call<ResponseBody> getSuccessfulVotes(
			@Path(Constants.PARAMETER_PATH_CLEANSING_OUTPUTS)
					String pathCleansingOutputs,
			@Path(Constants.PARAMETER_VALUE_TENANT_ID)
					String tenantId,
			@Path(Constants.PARAMETER_VALUE_ELECTION_EVENT_ID)
					String electionEventId,
			@Path(Constants.PARAMETER_VALUE_BALLOT_BOX_ID)
					String ballotBoxId,
			@NotNull
			@Header(RestClientInterceptor.HEADER_ORIGINATOR)
					String originator,
			@NotNull
			@Header(RestClientInterceptor.HEADER_SIGNATURE)
					String signature,
			@NotNull
			@Header(Constants.PARAMETER_X_FORWARDED_FOR)
					String xForwardedFor,
			@NotNull
			@Header(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId);

	@GET("{pathCleansingOutputs}/secured/tenant/{tenantId}/electionevent/{electionEventId}/ballotbox/{ballotBoxId}/failedvotes")
	@Headers("Accept:" + MediaType.APPLICATION_OCTET_STREAM)
	@Streaming
	Call<ResponseBody> getFailedVotes(
			@Path(Constants.PARAMETER_PATH_CLEANSING_OUTPUTS)
					String pathCleansingOutputs,
			@Path(Constants.PARAMETER_VALUE_TENANT_ID)
					String tenantId,
			@Path(Constants.PARAMETER_VALUE_ELECTION_EVENT_ID)
					String electionEventId,
			@Path(Constants.PARAMETER_VALUE_BALLOT_BOX_ID)
					String ballotBoxId,
			@NotNull
			@Header(RestClientInterceptor.HEADER_ORIGINATOR)
					String originator,
			@NotNull
			@Header(RestClientInterceptor.HEADER_SIGNATURE)
					String signature,
			@NotNull
			@Header(Constants.PARAMETER_X_FORWARDED_FOR)
					String xForwardedFor,
			@NotNull
			@Header(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId);
}
