/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.owasp.esapi.ESAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.factory.CryptoX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.validation.ValidationResult;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.administrationauthority.AdministrationAuthorityRepository;
import ch.post.it.evoting.sdm.domain.model.certificateRegistry.Certificate;
import ch.post.it.evoting.sdm.domain.model.electionevent.ElectionEventRepository;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.domain.model.status.SynchronizeStatus;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.infrastructure.RestClientService;
import ch.post.it.evoting.sdm.infrastructure.clients.CertificateRegistryClient;
import ch.post.it.evoting.sdm.utils.JsonUtils;

import okhttp3.ResponseBody;
import retrofit2.Retrofit;

/**
 * Service for uploading the Admin board information.
 */
@Service
public class AdminBoardUploadService {

	private static final String ENDPOINT_ADMIN_BOARD_CONTENTS_GET = "/certificates/tenant/{tenantId}/name/{certificateName}/status";

	private static final Logger LOGGER = LoggerFactory.getLogger(AdminBoardUploadService.class);

	private static final String NULL_ELECTION_EVENT_ID = "";

	private static final String TENANT_ID_PARAM = "tenantId";

	private static final String CERTIFICATE_NAME_PARAM = "certificateName";

	@Autowired
	HashService hashService;

	@Autowired
	private AdministrationAuthorityRepository administrationAuthorityRepository;

	@Autowired
	private ElectionEventRepository electionEventRepository;

	@Autowired
	private PathResolver pathResolver;

	@Value("${CR_URL}")
	private String apiGateWayURL;

	@Value("${tenantID}")
	private String tenantId;

	/**
	 * Uploads admin board certificates of all adminBoards pending to synchronize
	 */
	public void uploadSynchronizableAdminBoards(String electionEventId, PrivateKey requestSigningKey) {

		// query ballot boxes to process
		JsonArray adminBoards = getAdminBoardDataToUpload(electionEventId);

		try {
			// process them
			for (int i = 0; i < adminBoards.size(); i++) {
				JsonObject adminBoard = adminBoards.getJsonObject(i);
				String adminBoardId = adminBoard.getString(JsonConstants.ID);

				Path signedCertificatePath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, Constants.CSR_FOLDER, adminBoardId + ".pem");
				FileInputStream fisTargetFile = new FileInputStream(signedCertificatePath.toFile());
				String content = IOUtils.toString(fisTargetFile, StandardCharsets.UTF_8);
				X509Certificate certificate = (X509Certificate) PemUtils.certificateFromPem(content);
				CryptoX509Certificate cryptoX509Certificate = new CryptoX509Certificate(certificate);
				String certificateName = cryptoX509Certificate.getSubjectDn().getCommonName();

				LOGGER.info("Checking if the Administration board certificate has to be uploaded");

				if (checkAdminBoardCertificateIsEmpty(tenantId, certificateName)) {
					LOGGER.info("Uploading administration board certificate");
					uploadCertificate(adminBoardId, tenantId, content, certificateName, requestSigningKey);
				} else {

					LOGGER.info("The administration board certificate was already uploaded");

					JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
					jsonObjectBuilder.add(JsonConstants.ID, adminBoardId);
					jsonObjectBuilder.add(JsonConstants.SYNCHRONIZED, SynchronizeStatus.SYNCHRONIZED.getIsSynchronized().toString());
					jsonObjectBuilder.add(JsonConstants.DETAILS, SynchronizeStatus.SYNCHRONIZED.getStatus());
					administrationAuthorityRepository.update(jsonObjectBuilder.build().toString());
				}
			}
		} catch (IOException | GeneralCryptoLibException e) {
			LOGGER.error("Error trying to get the certificate information", e);
		}

	}

	/**
	 * query admin boards to process
	 *
	 * @return JsonArray with ballot boxes to upload
	 */
	private JsonArray getAdminBoardDataToUpload(String electionEvent) {

		Map<String, Object> params = new HashMap<>();

		params.put(JsonConstants.STATUS, Status.CONSTITUTED.name());
		params.put(JsonConstants.SYNCHRONIZED, SynchronizeStatus.PENDING.getIsSynchronized().toString());
		// If there is an election event as parameter, it will be included in
		// the query
		if (!NULL_ELECTION_EVENT_ID.equals(electionEvent)) {
			JsonObject electionEventObject = JsonUtils.getJsonObject(electionEventRepository.find(electionEvent));
			String adminBoardId = electionEventObject.getJsonObject(JsonConstants.ADMINISTRATION_AUTHORITY).getString(JsonConstants.ID);
			params.put(JsonConstants.ID, adminBoardId);
		}
		String serializedBallotBoxes = administrationAuthorityRepository.list(params);

		return JsonUtils.getJsonObject(serializedBallotBoxes).getJsonArray(JsonConstants.RESULT);
	}

	/**
	 * Uploads certificated
	 */
	private void uploadCertificate(String adminBoardId, String tenantId, String content, String certName, PrivateKey requestSigningKey) {

		LOGGER.info("Trying to upload Certificate...");

		Retrofit restAdapter = RestClientService.getInstance()
				.getRestClientWithInterceptorAndJacksonConverter(apiGateWayURL, requestSigningKey, "SECURE_DATA_MANAGER");

		CertificateRegistryClient crClient = restAdapter.create(CertificateRegistryClient.class);

		Certificate certificate = new Certificate();
		certificate.setCertificateName(certName);
		String sanitizedContent = ESAPI.encoder().canonicalize(content);
		certificate.setCertificateContent(sanitizedContent);
		JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();

		final retrofit2.Response<ResponseBody> response;
		try {
			response = crClient.saveCertificate(tenantId, certificate).execute();
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to communicate with certificate registry.", e);
		}

		if (response.isSuccessful()) {
			jsonObjectBuilder.add(JsonConstants.ID, adminBoardId);
			jsonObjectBuilder.add(JsonConstants.SYNCHRONIZED, SynchronizeStatus.SYNCHRONIZED.getIsSynchronized().toString());
			jsonObjectBuilder.add(JsonConstants.DETAILS, SynchronizeStatus.SYNCHRONIZED.getStatus());
			LOGGER.info("The administration board certificate was successfully uploaded");
		} else {
			final String errorBodyString;
			try {
				errorBodyString = response.errorBody().string();
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to convert response body to string.", e);
			}
			LOGGER.error("Request to certificate registry failed with error: {}", errorBodyString);
			jsonObjectBuilder.add(JsonConstants.ID, adminBoardId);
			jsonObjectBuilder.add(JsonConstants.DETAILS, SynchronizeStatus.FAILED.getStatus());
		}

		LOGGER.info("Trying to update repository...");
		administrationAuthorityRepository.update(jsonObjectBuilder.build().toString());
	}

	private boolean checkAdminBoardCertificateIsEmpty(String tenantId, String name) {

		boolean result = Boolean.TRUE;
		WebTarget target = ClientBuilder.newClient().target(apiGateWayURL + ENDPOINT_ADMIN_BOARD_CONTENTS_GET);

		Response response = target.resolveTemplate(TENANT_ID_PARAM, tenantId).resolveTemplate(CERTIFICATE_NAME_PARAM, name)
				.request(MediaType.APPLICATION_JSON).get();
		if (response.getStatus() == Response.Status.OK.getStatusCode()) {
			String json = response.readEntity(String.class);
			try {
				ValidationResult validationResult = ObjectMappers.fromJson(json, ValidationResult.class);
				// if the certificate exists the result will be
				// TRUE/SUCCESS so we have to return the inverse
				// if we want to check if "empty"
				result = !validationResult.isResult();
			} catch (IOException e) {
				LOGGER.error("Error checking if a ballot box is empty", e);
			}
		}
		return result;
	}

}
