/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.apigateway.ws.operation.admin;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.votingserver.apigateway.services.infrastructure.remote.admin.ElectionInformationAdminClient;
import ch.post.it.evoting.votingserver.apigateway.ws.proxy.XForwardedForFactory;
import ch.post.it.evoting.votingserver.apigateway.ws.proxy.XForwardedForFactoryImpl;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RestClientInterceptor;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RetrofitConsumer;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RetrofitException;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdGenerator;
import ch.post.it.evoting.votingserver.commons.util.PropertiesFileReader;

import okhttp3.ResponseBody;

@Stateless(name = "ag-ei-CleansingOutputsResource")
@Path(CleansingOutputsResource.RESOURCE_NAME)
public class CleansingOutputsResource {

	static final String RESOURCE_NAME = "/ei/cleansingoutputs";

	static final String PATH_SUCCESSFUL_VOTES = "tenant/{tenantId}/electionevent/{electionEventId}/ballotbox/{ballotBoxId}/successfulvotes";

	static final String PATH_FAILED_VOTES = "tenant/{tenantId}/electionevent/{electionEventId}/ballotbox/{ballotBoxId}/failedvotes";

	static final String PATH_PARAMETER_TENANT_ID = "tenantId";

	static final String PATH_PARAMETER_ELECTION_EVENT_ID = "electionEventId";

	static final String PATH_PARAMETER_BALLOT_BOX_ID = "ballotBoxId";
	private static final PropertiesFileReader PROPERTIES = PropertiesFileReader.getInstance();
	static final String CLEANSING_OUTPUTS_PATH = PROPERTIES.getPropertyValue("CLEANSING_OUTPUTS_PATH");
	private static final Logger LOGGER = LoggerFactory.getLogger(CleansingOutputsResource.class);
	private final XForwardedForFactory xForwardedForFactory = XForwardedForFactoryImpl.getInstance();
	private final ElectionInformationAdminClient electionInformationAdminClient;
	private final TrackIdGenerator trackIdGenerator;

	@Inject
	CleansingOutputsResource(ElectionInformationAdminClient electionInformationAdminClient, TrackIdGenerator trackIdGenerator) {
		this.electionInformationAdminClient = electionInformationAdminClient;
		this.trackIdGenerator = trackIdGenerator;
	}

	@GET
	@Path(PATH_SUCCESSFUL_VOTES)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getSuccessfulVotes(
			@PathParam(PATH_PARAMETER_TENANT_ID)
			final String tenantId,
			@PathParam(PATH_PARAMETER_ELECTION_EVENT_ID)
			final String electionEventId,
			@PathParam(PATH_PARAMETER_BALLOT_BOX_ID)
			final String ballotBoxId,
			@HeaderParam(RestClientInterceptor.HEADER_ORIGINATOR)
			final String originator,
			@HeaderParam(RestClientInterceptor.HEADER_SIGNATURE)
			final String signature,
			@Context
			final HttpServletRequest request) {

		String xForwardedFor = xForwardedForFactory.newXForwardedFor(request);
		String trackingId = trackIdGenerator.generate();

		try {
			ResponseBody responseBody = RetrofitConsumer.processResponse(electionInformationAdminClient
					.getSuccessfulVotes(CLEANSING_OUTPUTS_PATH, tenantId, electionEventId, ballotBoxId, originator, signature, xForwardedFor,
							trackingId));

			return Response.ok().entity(responseBody.byteStream()).build();
		} catch (RetrofitException rfE) {
			LOGGER.error("Error trying to get successful votes.", rfE);
			return Response.status(rfE.getHttpCode()).build();
		}
	}

	@GET
	@Path(PATH_FAILED_VOTES)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getFailedVotes(
			@PathParam(PATH_PARAMETER_TENANT_ID)
			final String tenantId,
			@PathParam(PATH_PARAMETER_ELECTION_EVENT_ID)
			final String electionEventId,
			@PathParam(PATH_PARAMETER_BALLOT_BOX_ID)
			final String ballotBoxId,
			@HeaderParam(RestClientInterceptor.HEADER_ORIGINATOR)
			final String originator,
			@HeaderParam(RestClientInterceptor.HEADER_SIGNATURE)
			final String signature,
			@Context
			final HttpServletRequest request) {

		String xForwardedFor = xForwardedForFactory.newXForwardedFor(request);
		String trackingId = trackIdGenerator.generate();

		try {
			ResponseBody responseBody = RetrofitConsumer.processResponse(electionInformationAdminClient
					.getFailedVotes(CLEANSING_OUTPUTS_PATH, tenantId, electionEventId, ballotBoxId, originator, signature, xForwardedFor,
							trackingId));

			return Response.ok().entity(responseBody.byteStream()).build();
		} catch (RetrofitException rfE) {
			LOGGER.error("Error trying to get failed votes.", rfE);
			return Response.status(rfE.getHttpCode()).build();
		}
	}
}
