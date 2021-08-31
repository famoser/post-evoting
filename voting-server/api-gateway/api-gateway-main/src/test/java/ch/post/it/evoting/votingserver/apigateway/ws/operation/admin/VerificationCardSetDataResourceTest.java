/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.apigateway.ws.operation.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import ch.post.it.evoting.votingserver.apigateway.services.infrastructure.remote.admin.VoteVerificationAdminClient;
import ch.post.it.evoting.votingserver.apigateway.ws.proxy.XForwardedForFactoryImpl;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdGenerator;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class VerificationCardSetDataResourceTest extends JerseyTest {

	private static final String ADMIN_BOARD_ID = "1a";
	private static final String ELECTION_EVENT_ID = "1e";
	private static final String VERIFICATION_CARD_SET_ID = "1vv";
	private static final String TRACK_ID = "trackId";
	private static final String TENANT_ID = "100t";
	private static final String X_FORWARDER_VALUE = "localhost,";
	private static final String URL_SAVE_VV_CONTEXT_DATA =
			VerificationCardSetDataResource.RESOURCE_PATH + "/" + VerificationCardSetDataResource.SAVE_VERIFICATION_CARD_SET_DATA;
	@ClassRule
	public static EnvironmentVariables environmentVariables = new EnvironmentVariables();
	@Rule
	public TestRule restoreSystemProperties = new RestoreSystemProperties();

	@Mock
	Logger logger;

	@Mock
	TrackIdGenerator trackIdGenerator;

	@Mock
	HttpServletRequest servletRequest;

	@Mock
	VoteVerificationAdminClient voteVerificationAdminClient;

	VerificationCardSetDataResource sut;

	@Test
	public void saveVerificationCardSetData() throws IOException {

		int mockedInvocationStatus = 200;

		@SuppressWarnings("unchecked")
		Call<ResponseBody> callMock = (Call<ResponseBody>) Mockito.mock(Call.class);
		when(callMock.execute()).thenReturn(Response.success(ResponseBody.create(okhttp3.MediaType.parse("text/html"), new byte[0])));

		commonPreparation();
		when(voteVerificationAdminClient
				.saveVerificationCardSetData(eq(VerificationCardSetDataResource.VERIFICATION_CARD_SET_DATA_PATH), eq(TENANT_ID),
						eq(ELECTION_EVENT_ID), eq(VERIFICATION_CARD_SET_ID), eq(ADMIN_BOARD_ID), eq(X_FORWARDER_VALUE), eq(TRACK_ID), any()))
				.thenReturn(callMock);

		int status = target(URL_SAVE_VV_CONTEXT_DATA).resolveTemplate("tenantId", TENANT_ID).resolveTemplate("electionEventId", ELECTION_EVENT_ID)
				.resolveTemplate("adminBoardId", ADMIN_BOARD_ID).resolveTemplate("verificationCardSetId", VERIFICATION_CARD_SET_ID).request()
				.post(Entity.entity(new ArrayList<Object>(), MediaType.APPLICATION_JSON_TYPE)).getStatus();

		Assert.assertEquals(mockedInvocationStatus, status);
	}

	@Test
	public void saveVerificationCardSetDataUnsuccessful() throws IOException {

		int mockedInvocationStatus = 407;

		@SuppressWarnings("unchecked")
		Call<ResponseBody> callMock = (Call<ResponseBody>) Mockito.mock(Call.class);
		when(callMock.execute())
				.thenReturn(Response.error(mockedInvocationStatus, ResponseBody.create(okhttp3.MediaType.parse("text/html"), new byte[0])));

		commonPreparation();
		when(voteVerificationAdminClient
				.saveVerificationCardSetData(eq(VerificationCardSetDataResource.VERIFICATION_CARD_SET_DATA_PATH), eq(TENANT_ID),
						eq(ELECTION_EVENT_ID), eq(VERIFICATION_CARD_SET_ID), eq(ADMIN_BOARD_ID), eq(X_FORWARDER_VALUE), eq(TRACK_ID), any()))
				.thenReturn(callMock);

		int status = target(URL_SAVE_VV_CONTEXT_DATA).resolveTemplate("tenantId", TENANT_ID).resolveTemplate("electionEventId", ELECTION_EVENT_ID)
				.resolveTemplate("adminBoardId", ADMIN_BOARD_ID).resolveTemplate("verificationCardSetId", VERIFICATION_CARD_SET_ID).request()
				.post(Entity.entity(new ArrayList<Object>(), MediaType.APPLICATION_JSON_TYPE)).getStatus();

		Assert.assertEquals(mockedInvocationStatus, status);
	}

	private void commonPreparation() {
		when(servletRequest.getHeader(eq(XForwardedForFactoryImpl.HEADER))).thenReturn("localhost");

		when(servletRequest.getRemoteAddr()).thenReturn("");
		when(servletRequest.getLocalAddr()).thenReturn("");
		when(trackIdGenerator.generate()).thenReturn(TRACK_ID);
	}

	@Override
	protected Application configure() {

		environmentVariables.set("VERIFICATION_CONTEXT_URL", "localhost");

		MockitoAnnotations.initMocks(this);

		AbstractBinder binder = new AbstractBinder() {
			@Override
			protected void configure() {
				bind(logger).to(Logger.class);
				bind(servletRequest).to(HttpServletRequest.class);
			}
		};
		sut = new VerificationCardSetDataResource(voteVerificationAdminClient, trackIdGenerator);
		forceSet(TestProperties.CONTAINER_PORT, "0");
		return new ResourceConfig().register(sut).register(binder);
	}
}
