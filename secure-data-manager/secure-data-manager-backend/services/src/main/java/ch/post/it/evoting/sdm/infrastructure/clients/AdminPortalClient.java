/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.clients;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface AdminPortalClient {

	@GET("export/tenants/{tenantId}")
	Call<ResponseBody> export(
			@Path("tenantId")
			final String tenantId);

}
