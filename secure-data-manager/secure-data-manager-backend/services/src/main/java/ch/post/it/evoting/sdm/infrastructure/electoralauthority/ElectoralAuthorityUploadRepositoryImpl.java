/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.electoralauthority;

import java.io.IOException;

import javax.json.JsonObject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.validation.ValidationResult;
import ch.post.it.evoting.sdm.domain.model.electoralauthority.ElectoralAuthorityUploadRepository;

/**
 * Implements the repository for electoral authority uploadDataInContext.
 */
@Repository
public class ElectoralAuthorityUploadRepositoryImpl implements ElectoralAuthorityUploadRepository {

	public static final String ADMIN_BOARD_ID_PARAM = "adminBoardId";

	private static final String ENDPOINT_CHECK_IF_EMPTY = "/electioneventdata/tenant/{tenantId}/electionevent/{electionEventId}/status";

	private static final Logger LOGGER = LoggerFactory.getLogger(ElectoralAuthorityUploadRepositoryImpl.class);

	private static final String ELECTION_EVENT_ID_PARAM = "electionEventId";

	private static final String TENANT_ID_PARAM = "tenantId";

	private static final String ELECTORAL_AUTHORITY_PARAM = "electoralAuthorityId";

	private static final String UPLOAD_ELECTION_EVENT_DATA_URL = "/electioneventdata/tenant/{tenantId}/electionevent/{electionEventId}/adminboard/{adminBoardId}";

	private static final String UPLOAD_ELECTORAL_DATA_VERIFICATION_CONTEXT_URL = "/electoraldata/tenant/{tenantId}/electionevent/{electionEventId}/electoralauthority/{electoralAuthorityId}/adminboard/{adminBoardId}";

	private static final String UPLOAD_ELECTORAL_DATA_ELECTION_INFORMATION_CONTEXT_URL = "/electoraldata/tenant/{tenantId}/electionevent/{electionEventId}/electoralauthority/{electoralAuthorityId}/adminboard/{adminBoardId}";

	@Value("${AU_URL}")
	private String authenticationURL;

	@Value("${EI_URL}")
	private String electionInformationURL;

	@Value("${VV_URL}")
	private String voteVerificationURL;

	@Value("${tenantID}")
	private String tenantId;

	@Override
	public boolean uploadAuthenticationContextData(String electionEventId, final String adminBoardId, JsonObject json) {

		return uploadContextData(electionEventId, adminBoardId, json, authenticationURL);

	}

	@Override
	public boolean uploadElectionInformationContextData(String electionEventId, final String adminBoardId, JsonObject json) {

		return uploadContextData(electionEventId, adminBoardId, json, electionInformationURL);

	}

	private boolean uploadContextData(String electionEventId, final String adminBoardId, JsonObject json, String url) {
		WebTarget target = ClientBuilder.newClient().target(url);
		Response response = target.path(UPLOAD_ELECTION_EVENT_DATA_URL).resolveTemplate(TENANT_ID_PARAM, tenantId)
				.resolveTemplate(ELECTION_EVENT_ID_PARAM, electionEventId).resolveTemplate(ADMIN_BOARD_ID_PARAM, adminBoardId).request()
				.post(Entity.entity(json.toString(), MediaType.APPLICATION_JSON_TYPE));

		return response.getStatus() == Response.Status.OK.getStatusCode();
	}

	@Override
	public boolean uploadElectoralDataInVerificationContext(String electionEventId, String electoralAuthorityId, final String adminBoardId,
			JsonObject json) {

		return uploadDataInContext(electionEventId, electoralAuthorityId, adminBoardId, json, voteVerificationURL,
				UPLOAD_ELECTORAL_DATA_VERIFICATION_CONTEXT_URL);
	}

	@Override
	public boolean uploadElectoralDataInElectionInformationContext(String electionEventId, String electoralAuthorityId, String adminBoardId,
			JsonObject json) {
		return uploadDataInContext(electionEventId, electoralAuthorityId, adminBoardId, json, electionInformationURL,
				UPLOAD_ELECTORAL_DATA_ELECTION_INFORMATION_CONTEXT_URL);
	}

	private boolean uploadDataInContext(String electionEventId, String electoralAuthorityId, String adminBoardId, JsonObject json, String url,
			String uploadUrl) {
		WebTarget target = ClientBuilder.newClient().target(url);
		Response response = target.path(uploadUrl).resolveTemplate(TENANT_ID_PARAM, tenantId)
				.resolveTemplate(ELECTION_EVENT_ID_PARAM, electionEventId).resolveTemplate(ELECTORAL_AUTHORITY_PARAM, electoralAuthorityId)
				.resolveTemplate(ADMIN_BOARD_ID_PARAM, adminBoardId).request().post(Entity.entity(json.toString(), MediaType.APPLICATION_JSON_TYPE));

		return response.getStatus() == Response.Status.OK.getStatusCode();
	}

	/**
	 * Checks if the information is empty for the given election event
	 *
	 * @param electionEvent - identifier of the electionevent
	 * @return
	 */
	@Override
	public boolean checkEmptyElectionEventDataInEI(String electionEvent) {
		return checkResult(electionEvent, electionInformationURL);
	}

	@Override
	public boolean checkEmptyElectionEventDataInAU(String electionEvent) {
		return checkResult(electionEvent, authenticationURL);
	}

	private boolean checkResult(String electionEvent, String url) {
		boolean result = false;
		WebTarget target = ClientBuilder.newClient().target(url + ENDPOINT_CHECK_IF_EMPTY);

		Response response = target.resolveTemplate(TENANT_ID_PARAM, tenantId).resolveTemplate(ELECTION_EVENT_ID_PARAM, electionEvent)
				.request(MediaType.APPLICATION_JSON).get();
		if (response.getStatus() == Response.Status.OK.getStatusCode()) {
			String json = response.readEntity(String.class);
			try {
				result = ObjectMappers.fromJson(json, ValidationResult.class).isResult();
			} catch (IOException e) {
				LOGGER.error("Error checking if a ballot box is empty", e);
			}
		}
		return result;
	}
}
