/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.apigateway.ws.operation.voting;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import ch.post.it.evoting.votingserver.apigateway.services.infrastructure.remote.voting.VotingWorkflowVotingClient;
import ch.post.it.evoting.votingserver.apigateway.ws.RestApplication;
import ch.post.it.evoting.votingserver.apigateway.ws.proxy.XForwardedForFactory;
import ch.post.it.evoting.votingserver.apigateway.ws.proxy.XForwardedForFactoryImpl;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RetrofitConsumer;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RetrofitException;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdGenerator;
import ch.post.it.evoting.votingserver.commons.util.PropertiesFileReader;

/**
 * The end point for authentication information resource.
 */
@Stateless(name = "ag-CredentialInformationResource")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@Path(CredentialInformationResource.RESOURCE_PATH)
public class CredentialInformationResource {

	static final String RESOURCE_PATH = RestApplication.API_OV_VOTING_BASEURI_AUTH + "/auth" + "/request-token";

	// The properties file reader.
	private static final PropertiesFileReader PROPERTIES = PropertiesFileReader.getInstance();

	// The path to the resource authentication information.
	static final String CREDENTIAL_INFORMATION_PATH = PROPERTIES.getPropertyValue("CREDENTIAL_INFORMATION_PATH");
	private static final Logger LOGGER = LoggerFactory.getLogger(CredentialInformationResource.class);
	private final XForwardedForFactory xForwardedForFactory = XForwardedForFactoryImpl.getInstance();
	private final VotingWorkflowVotingClient votingWorkflowVotingClient;
	private final TrackIdGenerator trackIdGenerator;

	@Inject
	CredentialInformationResource(VotingWorkflowVotingClient votingWorkflowVotingClient, TrackIdGenerator trackIdGenerator) {
		this.votingWorkflowVotingClient = votingWorkflowVotingClient;
		this.trackIdGenerator = trackIdGenerator;
	}

	@GET
	@Path("")
	@Produces(RestApplication.MEDIA_TYPE_JSON_UTF_8)
	public Response getAuthenticationInformation(
			@PathParam(RestApplication.PARAMETER_VALUE_VERSION)
			final String version, //
			@PathParam(RestApplication.PARAMETER_VALUE_TENANT_ID)
			final String tenantId, //
			@PathParam(RestApplication.PARAMETER_VALUE_ELECTION_EVENT_ID)
			final String electionEventId, //
			@PathParam(RestApplication.PARAMETER_VALUE_CREDENTIAL_ID)
			final String credentialId,
			@Context
			final HttpServletRequest request) {
		String xForwardedFor = xForwardedForFactory.newXForwardedFor(request);
		String trackingId = trackIdGenerator.generate();

		try {
			JsonObject processResponse = RetrofitConsumer.processResponse(votingWorkflowVotingClient
					.findInformationsByTenantElectionEventCredential(CREDENTIAL_INFORMATION_PATH, tenantId, electionEventId, credentialId,
							xForwardedFor, trackingId));
			return Response.ok().entity(processResponse.toString()).build();
		} catch (RetrofitException rfE) {
			LOGGER.error("Error trying to get authentication information.", rfE);
			return Response.status(rfE.getHttpCode()).build();
		}
	}
}
