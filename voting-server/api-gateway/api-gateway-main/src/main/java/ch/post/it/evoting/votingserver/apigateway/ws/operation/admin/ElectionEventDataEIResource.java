/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.apigateway.ws.operation.admin;

import java.io.IOException;
import java.io.InputStream;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import ch.post.it.evoting.votingserver.apigateway.services.infrastructure.remote.admin.ElectionInformationAdminClient;
import ch.post.it.evoting.votingserver.apigateway.ws.RestApplication;
import ch.post.it.evoting.votingserver.apigateway.ws.proxy.XForwardedForFactory;
import ch.post.it.evoting.votingserver.apigateway.ws.proxy.XForwardedForFactoryImpl;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.InputStreamTypedOutput;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RestClientInterceptor;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RetrofitConsumer;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RetrofitException;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdGenerator;
import ch.post.it.evoting.votingserver.commons.util.PropertiesFileReader;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;

/**
 * Web service for handling election event resources of the election information context (EI)
 */
@Stateless(name = "ag-ei-ElectionEventDataResource")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@Path(ElectionEventDataEIResource.RESOURCE_PATH)
public class ElectionEventDataEIResource {

	static final String RESOURCE_PATH = "/ei/electioneventdata";
	private static final String PATH_TO_ELECTION_EVENT = "tenant/{tenantId}/electionevent/{electionEventId}";
	static final String SAVE_ELECTION_EVENT_DATA = PATH_TO_ELECTION_EVENT + "/adminboard/{adminBoardId}";
	static final String CHECK_IF_ELECTION_EVENT_DATA_IS_EMPTY = PATH_TO_ELECTION_EVENT + "/status";
	static final String GET_CASTED_VOTING_CARDS = PATH_TO_ELECTION_EVENT + "/cast";
	private static final String QUERY_PARAMETER_TENANT_ID = "tenantId";

	private static final String QUERY_PARAMETER_ELECTION_EVENT_ID = "electionEventId";

	private static final PropertiesFileReader PROPERTIES = PropertiesFileReader.getInstance();

	static final String ELECION_EVENT_DATA_PATH = PROPERTIES.getPropertyValue("ELECION_EVENT_DATA_PATH");

	private static final String QUERY_PARAMETER_ADMIN_BOARD_ID = "adminBoardId";
	private static final Logger LOGGER = LoggerFactory.getLogger(ElectionEventDataEIResource.class);
	private final XForwardedForFactory xForwardedForFactory = XForwardedForFactoryImpl.getInstance();
	private final ElectionInformationAdminClient electionInformationAdminClient;
	private final TrackIdGenerator trackIdGenerator;

	@Inject
	ElectionEventDataEIResource(ElectionInformationAdminClient electionInformationAdminClient, TrackIdGenerator trackIdGenerator) {
		this.electionInformationAdminClient = electionInformationAdminClient;
		this.trackIdGenerator = trackIdGenerator;
	}

	@POST
	@Path(SAVE_ELECTION_EVENT_DATA)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response saveElectionEventData(
			@PathParam(QUERY_PARAMETER_TENANT_ID)
			final String tenantId,
			@PathParam(QUERY_PARAMETER_ELECTION_EVENT_ID)
			final String electionEventId,
			@PathParam(QUERY_PARAMETER_ADMIN_BOARD_ID)
			final String adminBoardId,
			@NotNull
			final InputStream data,
			@Context
			final HttpServletRequest request) {
		String xForwardedFor = xForwardedForFactory.newXForwardedFor(request);
		String trackingId = trackIdGenerator.generate();

		RequestBody body = new InputStreamTypedOutput(MediaType.APPLICATION_JSON, data);

		try (ResponseBody responseBody = RetrofitConsumer.processResponse(electionInformationAdminClient
				.saveElectionEventData(ELECION_EVENT_DATA_PATH, tenantId, electionEventId, adminBoardId, xForwardedFor, trackingId, body))) {
			return Response.ok().build();
		} catch (RetrofitException rfE) {
			LOGGER.error("Error trying to save election event data.", rfE);
			return Response.status(rfE.getHttpCode()).build();
		}
	}

	/**
	 * Validate if the election event data for the election information context (EI) is empty.
	 *
	 * @param tenantId        - the tenant identifier.
	 * @param electionEventId - the election event identifier.
	 * @return Returns the result of the validation.
	 */
	@GET
	@Produces(RestApplication.MEDIA_TYPE_JSON_UTF_8)
	@Path(CHECK_IF_ELECTION_EVENT_DATA_IS_EMPTY)
	public Response checkIfElectionEventDataIsEmpty(
			@PathParam(QUERY_PARAMETER_TENANT_ID)
			final String tenantId,
			@PathParam(QUERY_PARAMETER_ELECTION_EVENT_ID)
			final String electionEventId,
			@Context
			final HttpServletRequest request) {
		String xForwardedFor = xForwardedForFactory.newXForwardedFor(request);
		String trackingId = trackIdGenerator.generate();
		try {
			JsonObject processResponse = RetrofitConsumer.processResponse(electionInformationAdminClient
					.checkIfElectionEventDataIsEmpty(ELECION_EVENT_DATA_PATH, tenantId, electionEventId, xForwardedFor, trackingId));
			return Response.ok().entity(processResponse.toString()).build();
		} catch (RetrofitException rfE) {
			LOGGER.error("Error trying to check if election event data is empty.", rfE);
			return Response.status(rfE.getHttpCode()).build();
		}
	}

	/**
	 * Return a signed list of voting card ids that were cast for the election.
	 *
	 * @param tenantId        - the tenant identifier.
	 * @param electionEventId - the election event identifier.
	 * @return Returns the result of the validation.
	 */
	@Path(GET_CASTED_VOTING_CARDS)
	@GET
	@Produces({ MediaType.APPLICATION_OCTET_STREAM, RestApplication.MEDIA_TYPE_JSON_UTF_8 })
	public Response getCastedVotingCards(
			@PathParam(QUERY_PARAMETER_TENANT_ID)
			final String tenantId,
			@PathParam(QUERY_PARAMETER_ELECTION_EVENT_ID)
			final String electionEventId,
			@Context
			final HttpServletRequest request) throws IOException {

		String xForwardedFor = xForwardedForFactory.newXForwardedFor(request);
		String trackingId = trackIdGenerator.generate();

		String signature = request.getHeader(RestClientInterceptor.HEADER_SIGNATURE);
		String originator = request.getHeader(RestClientInterceptor.HEADER_ORIGINATOR);
		try {
			retrofit2.Response<ResponseBody> processResponse = RetrofitConsumer.executeCall(electionInformationAdminClient
					.getCastedVotingCardsReport(ELECION_EVENT_DATA_PATH, tenantId, electionEventId, xForwardedFor, trackingId, originator,
							signature));
			return handleFileResponse(processResponse);
		} catch (RetrofitException rfE) {
			LOGGER.error("Error trying to get casted voting cards.", rfE);
			return Response.status(rfE.getHttpCode()).build();
		}

	}

	private Response handleFileResponse(retrofit2.Response<ResponseBody> response) {
		ResponseBody body = response.body();
		if (body == null || body.contentLength() == 0) {
			return Response.noContent().build();
		}
		InputStream byteStream = body.byteStream();

		// use the original status of the retrofit Response
		ResponseBuilder builder = Response.status(response.code());

		okhttp3.Headers headers = response.headers();

		for (String header : headers.names()) {
			builder.header(header, headers.get(header));
		}

		return builder.entity(byteStream).build();
	}
}
