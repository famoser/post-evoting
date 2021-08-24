/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.certificateregistry.ws.operation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.json.JsonObject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.RandomUtils;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.extension.rest.client.ArquillianResteasyResource;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateParameters;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.votingserver.certificateregistry.services.domain.model.certificate.CertificateEntity;
import ch.post.it.evoting.votingserver.commons.util.CryptoUtils;
import ch.post.it.evoting.votingserver.commons.util.JsonUtils;

@RunAsClient
@RunWith(Arquillian.class)
public class CertificateRegistryResourceArquillianTest {

	// CODES

	private static final String DUPLICATE_ENTRY_CODE = "duplicate_entry";

	private static final String RESOURCE_NOT_FOUND_CODE = "resource_not_found";

	// TEST VARIABLES

	private static final String TRACK_ID_HEADER = "X-Request-ID";

	private static final String TEST_TRACK_ID = "TestTrackingId";

	private static final String TEST_CERTIFICATE_NAME = "TestCertificateName" + RandomUtils.nextInt();

	private static final String TEST_PLATFORM_NAME = "Swiss Post";

	private static final String BASE_PATH = CertificateRegistryResource.RESOURCE_PATH + "/";

	private static final String URL_GET_CERTIFICATE = BASE_PATH + CertificateRegistryResource.GET_CERTIFICATE;

	private static final String URL_GET_CERTIFICATE_FOR_TENANT = BASE_PATH + CertificateRegistryResource.GET_CERTIFICATE_FOR_TENANT;

	private static final String URL_GET_CERTIFICATE_FOR_TENANT_EEID = BASE_PATH + CertificateRegistryResource.GET_CERTIFICATE_FOR_TENANT_EEID;

	private static final String URL_POST_CERTIFICATE = BASE_PATH + CertificateRegistryResource.SAVE_CERTIFICATE;

	private static final String URL_POST_CERTIFICATE_BY_ID = BASE_PATH + CertificateRegistryResource.SAVE_CERTIFICATE_FOR_TENANT;

	private static final String URL_POST_CERTIFICATE_FOR_TENANT_EEID = BASE_PATH + CertificateRegistryResource.SAVE_CERTIFICATE_FOR_ELECTION_EVENT;
	private static final String ELECTION_EVENT_TEMPLATE = "electionEventId";
	private static final String TENANT_TEMPLATE = "tenantId";
	private static final String CERTIFICATE_NAME = "certificateName";
	private static final String TENANT_CERTIFICATE_NAME = "Tenant 100 CA";
	private static final String TEST_TENANT_ID = "100";
	private static final String TEST_ELECTION_EVENT_ID = "electionEventId";
	private static final Map<String, Object> templates;
	private static final SystemPropertiesLoader SYSTEM_PROPERTIES_LOADER = new SystemPropertiesLoader();

	static {
		SYSTEM_PROPERTIES_LOADER.setProperties();
		templates = new HashMap<>();
		templates.put(TENANT_TEMPLATE, TEST_TENANT_ID);
		templates.put(ELECTION_EVENT_TEMPLATE, TEST_ELECTION_EVENT_ID);
		templates.put(CERTIFICATE_NAME, TENANT_CERTIFICATE_NAME);
	}

	@PersistenceContext(unitName = "persistenceUnitJdbc")
	EntityManager entityManager;
	@Resource
	UserTransaction userTransaction;

	@Before
	public void init() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
		userTransaction.begin();
		Query query = entityManager.createQuery("DELETE FROM " + CertificateEntity.class.getSimpleName());
		query.executeUpdate();
		userTransaction.commit();
	}

	// GET TENANT BY NAME

	@Test
	public void getExistingTenantCertificateByNameAndGetSuccessStatus(
			@ArquillianResteasyResource("")
					ResteasyWebTarget webTarget)
			throws NotSupportedException, HeuristicMixedException, GeneralCryptoLibException, SystemException, HeuristicRollbackException,
			RollbackException {

		createAndPersistTestTenantCertificate(TENANT_CERTIFICATE_NAME, "");

		Response response = webTarget.path(URL_GET_CERTIFICATE).resolveTemplates(templates).request(MediaType.APPLICATION_JSON).get();

		assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
	}

	@Test
	public void getNonExistingTenantCertificateByNameAndGetNotFoundCodeAndStatus(
			@ArquillianResteasyResource("")
					ResteasyWebTarget webTarget) {

		ClientResponse response = (ClientResponse) webTarget.path(URL_GET_CERTIFICATE).resolveTemplates(templates).request(MediaType.APPLICATION_JSON)
				.get();

		String responseCode = obtainErrorCodeFromResponse(response);

		assertThat(responseCode, is(RESOURCE_NOT_FOUND_CODE));
		assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));

	}

	// GET TENANT BY NAME AND ID

	@Test
	public void getExistingTenantCertificateByNameAndTenantIdAndGetSuccessStatus(
			@ArquillianResteasyResource("")
					ResteasyWebTarget webTarget)
			throws NotSupportedException, HeuristicMixedException, GeneralCryptoLibException, SystemException, HeuristicRollbackException,
			RollbackException {

		createAndPersistTestTenantCertificate(TENANT_CERTIFICATE_NAME, "");

		Response response = webTarget.path(URL_GET_CERTIFICATE_FOR_TENANT).resolveTemplates(templates).request(MediaType.APPLICATION_JSON).get();

		assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

	}

	@Test
	public void getNonExistingTenantCertificateByNameAndTenantIdAndGetNotFoundCodeAndStatus(
			@ArquillianResteasyResource("")
					ResteasyWebTarget webTarget) {

		ClientResponse response = (ClientResponse) webTarget.path(URL_GET_CERTIFICATE_FOR_TENANT).resolveTemplates(templates)
				.request(MediaType.APPLICATION_JSON).get();

		String responseCode = obtainErrorCodeFromResponse(response);

		assertThat(responseCode, is(RESOURCE_NOT_FOUND_CODE));
		assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));

	}

	// GET TENANT BY NAME,ID AND EEID

	@Test
	public void getExistingTenantCertificateByNameAndEEIdAndTenantIdAndGetSuccessStatus(
			@ArquillianResteasyResource("")
					ResteasyWebTarget webTarget)
			throws NotSupportedException, HeuristicMixedException, GeneralCryptoLibException, SystemException, HeuristicRollbackException,
			RollbackException {

		createAndPersistTestTenantCertificate(TENANT_CERTIFICATE_NAME, TEST_ELECTION_EVENT_ID);

		Response response = webTarget.path(URL_GET_CERTIFICATE_FOR_TENANT_EEID).resolveTemplates(templates).request(MediaType.APPLICATION_JSON).get();

		assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

	}

	@Test
	public void getNonExistingTenantCertificateByNameAndTenantIdAndEEIdAndGetNotFoundCodeAndStatus(
			@ArquillianResteasyResource("")
					ResteasyWebTarget webTarget) {

		Response response = webTarget.path(URL_GET_CERTIFICATE_FOR_TENANT).resolveTemplates(templates).request(MediaType.APPLICATION_JSON).get();

		String responseCode = obtainErrorCodeFromResponse(response);

		assertThat(responseCode, is(RESOURCE_NOT_FOUND_CODE));
		assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));

	}

	// POST TENANT BY ID

	@Test
	public void postNewTenantCertificateByTenantIdAndGetSuccessStatus(
			@ArquillianResteasyResource("")
					ResteasyWebTarget webTarget)
			throws GeneralCryptoLibException, SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException,
			RollbackException {

		// Save certificate as CA (is selfsigned)
		CertificateEntity testCertificateEntity = createAndPersistTestTenantCertificate(TENANT_CERTIFICATE_NAME, "");

		testCertificateEntity.setCertificateName(TEST_CERTIFICATE_NAME);

		ClientResponse response = (ClientResponse) webTarget.path(URL_POST_CERTIFICATE_BY_ID).resolveTemplates(templates)
				.request(MediaType.APPLICATION_JSON).header(TRACK_ID_HEADER, TEST_TRACK_ID)
				.post(Entity.entity(testCertificateEntity, MediaType.APPLICATION_JSON_TYPE));

		assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

	}

	@Test
	public void postExistingTenantCertificateByTenantIdAndGetDuplicateEntryCode(
			@ArquillianResteasyResource("")
					ResteasyWebTarget webTarget)
			throws GeneralCryptoLibException, SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException,
			RollbackException {

		CertificateEntity testCertificateEntity = createAndPersistTestTenantCertificate(TENANT_CERTIFICATE_NAME, TEST_ELECTION_EVENT_ID);

		ClientResponse response = (ClientResponse) webTarget.path(URL_POST_CERTIFICATE_BY_ID).resolveTemplates(templates)
				.request(MediaType.APPLICATION_JSON).header(TRACK_ID_HEADER, TEST_TRACK_ID)
				.post(Entity.entity(testCertificateEntity, MediaType.APPLICATION_JSON_TYPE));

		String obtainedErrorCode = obtainErrorCodeFromResponse(response);

		assertThat(obtainedErrorCode, is(DUPLICATE_ENTRY_CODE));
		assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

	}

	// SIMPLE POST

	@Test
	public void postNewTenantCertificateAndGetSuccessStatus(
			@ArquillianResteasyResource("")
					ResteasyWebTarget webTarget)
			throws GeneralCryptoLibException, SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException,
			RollbackException {

		// Save certificate as CA (is selfsigned)
		CertificateEntity testCertificateEntity = createAndPersistTestTenantCertificate(TENANT_CERTIFICATE_NAME, TEST_ELECTION_EVENT_ID);

		testCertificateEntity.setCertificateName(TEST_CERTIFICATE_NAME);

		ClientResponse response = (ClientResponse) webTarget.path(URL_POST_CERTIFICATE).resolveTemplates(templates)
				.request(MediaType.APPLICATION_JSON).header(TRACK_ID_HEADER, TEST_TRACK_ID)
				.post(Entity.entity(testCertificateEntity, MediaType.APPLICATION_JSON_TYPE));

		assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

	}

	@Test
	public void postExistingTenantCertificateAndGetDuplicateEntryCode(
			@ArquillianResteasyResource("")
					ResteasyWebTarget webTarget)
			throws GeneralCryptoLibException, SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException,
			RollbackException {

		CertificateEntity testCertificateEntity = createAndPersistTestTenantCertificate(TENANT_CERTIFICATE_NAME, TEST_ELECTION_EVENT_ID);

		ClientResponse response = (ClientResponse) webTarget.path(URL_POST_CERTIFICATE_BY_ID).resolveTemplates(templates)
				.request(MediaType.APPLICATION_JSON).header(TRACK_ID_HEADER, TEST_TRACK_ID)
				.post(Entity.entity(testCertificateEntity, MediaType.APPLICATION_JSON_TYPE));

		String obtainedErrorCode = obtainErrorCodeFromResponse(response);

		assertThat(obtainedErrorCode, is(DUPLICATE_ENTRY_CODE));
		assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

	}

	// POST TENANT BY ID AND EE ID

	@Test
	public void postNewTenantCertificateByElectionEventIdAndGetSuccessfulResponse(
			@ArquillianResteasyResource("")
					ResteasyWebTarget webTarget)
			throws GeneralCryptoLibException, SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException,
			RollbackException {

		// Save certificate as CA (is selfsigned)
		CertificateEntity testCertificateEntity = createAndPersistTestTenantCertificate(TENANT_CERTIFICATE_NAME, TEST_ELECTION_EVENT_ID);

		testCertificateEntity.setCertificateName(TEST_CERTIFICATE_NAME);

		ClientResponse response = (ClientResponse) webTarget.path(URL_POST_CERTIFICATE_FOR_TENANT_EEID).resolveTemplates(templates)
				.request(MediaType.APPLICATION_JSON).header(TRACK_ID_HEADER, TEST_TRACK_ID)
				.post(Entity.entity(testCertificateEntity, MediaType.APPLICATION_JSON_TYPE));

		assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

	}

	@Test
	public void postExistingTenantCertificateByElectionEventAndGetDuplicateEntryCode(
			@ArquillianResteasyResource("")
					ResteasyWebTarget webTarget)
			throws GeneralCryptoLibException, HeuristicRollbackException, HeuristicMixedException, NotSupportedException, RollbackException,
			SystemException {

		CertificateEntity testCertificateEntity = createAndPersistTestTenantCertificate(TENANT_CERTIFICATE_NAME, TEST_ELECTION_EVENT_ID);

		ClientResponse response = (ClientResponse) webTarget.path(URL_POST_CERTIFICATE_FOR_TENANT_EEID).resolveTemplates(templates)
				.request(MediaType.APPLICATION_JSON).header(TRACK_ID_HEADER, TEST_TRACK_ID)
				.post(Entity.entity(testCertificateEntity, MediaType.APPLICATION_JSON_TYPE));

		String obtainedErrorCode = obtainErrorCodeFromResponse(response);

		assertThat(obtainedErrorCode, is(DUPLICATE_ENTRY_CODE));
		assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

	}

	private String getCertificateAsString(String cN) throws GeneralCryptoLibException {

		final KeyPair keyPairForSigning = CryptoUtils.getKeyPairForSigning();
		CryptoAPIX509Certificate certificate = CryptoUtils.createCryptoAPIx509Certificate(cN, CertificateParameters.Type.SIGN, keyPairForSigning);

		return new String(certificate.getPemEncoded(), StandardCharsets.UTF_8);

	}

	private CertificateEntity createAndPersistTestTenantCertificate(String certificateName, String eeId)
			throws GeneralCryptoLibException, SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException,
			RollbackException {

		String testCertificate = getCertificateAsString(TENANT_CERTIFICATE_NAME);
		CertificateEntity testCertificateEntity = new CertificateEntity();
		testCertificateEntity.setCertificateContent(testCertificate);
		testCertificateEntity.setCertificateName(certificateName);
		testCertificateEntity.setPlatformName(TEST_PLATFORM_NAME);
		testCertificateEntity.setTenantId(TEST_TENANT_ID);
		testCertificateEntity.setElectionEventId(eeId);

		userTransaction.begin();
		entityManager.persist(testCertificateEntity);
		userTransaction.commit();

		return testCertificateEntity;
	}

	private String obtainErrorCodeFromResponse(Response response) {

		String jsonString = response.readEntity(String.class);
		JsonObject json = JsonUtils.getJsonObject(jsonString);

		return json.getJsonArray("errors").getJsonObject(0).getString("code");
	}

}
