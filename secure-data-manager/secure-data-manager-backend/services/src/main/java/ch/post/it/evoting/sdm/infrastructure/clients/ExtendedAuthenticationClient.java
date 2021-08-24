/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.clients;

import javax.validation.constraints.NotNull;

import ch.post.it.evoting.sdm.commons.Constants;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ExtendedAuthenticationClient {

	@POST("extendedauthentication/tenant/{tenantId}/electionevent/{electionEventId}/adminboard/{adminBoardId}")
	Call<ResponseBody> saveExtendedAuthenticationData(
			@Path(Constants.TENANT_ID)
					String tenantId,
			@Path(Constants.ELECTION_EVENT_ID)
					String electionEventId,
			@Path(Constants.ADMIN_BOARD_ID)
					String adminBoardId, @NotNull
	@Body
			RequestBody body);
}
