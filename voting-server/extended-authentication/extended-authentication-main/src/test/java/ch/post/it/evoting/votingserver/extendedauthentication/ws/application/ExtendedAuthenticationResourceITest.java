/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.extendedauthentication.ws.application;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.delete;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.UserTransaction;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.ClassRule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.model.Information.VoterInformation;
import ch.post.it.evoting.domain.election.model.authentication.AuthenticationToken;
import ch.post.it.evoting.domain.election.model.authentication.ExtendedAuthenticationUpdate;
import ch.post.it.evoting.domain.election.model.authentication.ExtendedAuthenticationUpdateRequest;
import ch.post.it.evoting.domain.election.validation.ValidationResult;
import ch.post.it.evoting.votingserver.commons.sign.CSVSigner;
import ch.post.it.evoting.votingserver.extendedauthentication.ws.application.it.TestEAResponse;
import ch.post.it.evoting.votingserver.extendedauthentication.ws.application.operation.ExtendedAuthDataResource;
import ch.post.it.evoting.votingserver.extendedauthentication.ws.application.operation.ExtendedAuthentication;
import ch.post.it.evoting.votingserver.extendedauthentication.ws.application.operation.ExtendedAuthenticationResource;

import cucumber.api.CucumberOptions;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.runtime.arquillian.ArquillianCucumber;
import cucumber.runtime.arquillian.api.Features;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@RunAsClient
@RunWith(ArquillianCucumber.class)
@Features({ "features/ea_credentials.feature", "features/ea_functionality.feature" })
@CucumberOptions(features = "features", tags = { "~@ignore" }, plugin = { "pretty", "html:target/site/cucumber-pretty", "json:target/cucumber.json" })
public class ExtendedAuthenticationResourceITest extends ExtendedAuthenticationBaseITestCase {

	public static final String SAVE_AUTHENTICATION_PATH =
			ExtendedAuthDataResource.RESOURCE_PATH + ExtendedAuthDataResource.SAVE_EXTENDED_AUTHENTICATION_DATA;
	@ClassRule
	public static final TemporaryFolder TEMP_FOLDER = new TemporaryFolder();
	private static final String EXTENDED_AUTHENTICATION_FILENAME = "extendedAuthentication";
	private static final String EXTENDED_AUTHENTICATION_FILE_EXTENSION = ".csv";
	private static final String UPDATE_AUTHENTICATION = ExtendedAuthenticationResource.RESOURCE_PATH + ExtendedAuthenticationResource.UPDATE_PATH;
	private static final String TEST_BALLOT_BOX_ID = "ballotBoxId";
	@ClassRule
	public static EnvironmentVariables environmentVariables = new EnvironmentVariables();
	private static String auValidationsURL =
			"/AU/validations/tenant/" + TEST_TENANT_ID + "/electionevent/" + TEST_ELECTION_EVENT_ID + "/votingcard/" + TEST_VOTINGCARD_ID;
	private static String auChainValidationURL =
			"/AU/tokens/tenant/" + TEST_TENANT_ID + "/electionevent/" + TEST_ELECTION_EVENT_ID + "/chain/validate";
	private static String auValidateCertificateChainURL =
			"/AU/tokens/tenant/" + TEST_TENANT_ID + "/electionevent/" + TEST_ELECTION_EVENT_ID + "/chain/validate";

	static {
		environmentVariables.set("CERTIFICATES_CONTEXT_URL", "http://localhost:15241/CR");
		environmentVariables.set("AUTHENTICATION_CONTEXT_URL", "http://localhost:15241/AU");
	}

	@ArquillianResource
	URL url;
	@Resource()
	UserTransaction userTransaction;
	@PersistenceContext(unitName = "persistenceUnitJdbc")
	EntityManager entityManager;
	private String electionId;
	private TestEAResponse eaResponseEntity;

	@cucumber.api.java.After
	public void cleanDB() throws Exception {

		Query query = entityManager.createQuery("DELETE FROM "
				+ ch.post.it.evoting.votingserver.extendedauthentication.domain.model.extendedauthentication.ExtendedAuthentication.class
				.getSimpleName());

		userTransaction.begin();
		query.executeUpdate();
		userTransaction.commit();
	}

	@Given("^a predefined dataset for election with id (.+)$")
	public void uploadPredefinedDatasetForElectionId(final String electionId) {

		this.electionId = electionId;
		addHandlersToMockServer();
		final String testData = createAndSignTestData(electionId, TEST_AUTH_ID);

		final Response response = webTarget().path(SAVE_AUTHENTICATION_PATH).resolveTemplate(TENANT_ID, TEST_TENANT_ID)
				.resolveTemplate(ELECTION_EVENT, electionId).resolveTemplate(ADMINBOARD_ID, TEST_ADMINBOARD_ID)
				.request(MediaType.APPLICATION_JSON_TYPE).header(ExtendedAuthDataResource.PARAMETER_HEADER_X_REQUEST_ID, TEST_REQUEST_ID)
				.post(Entity.entity(testData, MEDIATYPE_TEXT_CSV));

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
	}

	@Given("^a predefined dataset for election with extra parameter (.+) and id (.+)$")
	public void uploadPredefinedDatasetForElectionIdAndExtraParam(final String extraParameter, final String electionId) throws Exception {

		this.electionId = electionId;
		final String testData = createAndSignTestDataWithExtraParam(electionId, extraParameter);

		final Response response = webTarget().path(SAVE_AUTHENTICATION_PATH).resolveTemplate(TENANT_ID, TEST_TENANT_ID)
				.resolveTemplate(ELECTION_EVENT, electionId).resolveTemplate(ADMINBOARD_ID, TEST_ADMINBOARD_ID)
				.request(MediaType.APPLICATION_JSON_TYPE).header(ExtendedAuthDataResource.PARAMETER_HEADER_X_REQUEST_ID, TEST_REQUEST_ID)
				.post(Entity.entity(testData, MEDIATYPE_TEXT_CSV));

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
	}

	@Given("^a predefined dataset for election with authId (.+) and election id (.+)$")
	public void uploadPredefinedDatasetForElectionIdAndAuthId(final String authId, final String electionId) {

		this.electionId = electionId;
		final String testData = createAndSignTestData(electionId, authId);

		final Response response = webTarget().path(SAVE_AUTHENTICATION_PATH).resolveTemplate(TENANT_ID, TEST_TENANT_ID)
				.resolveTemplate(ELECTION_EVENT, electionId).resolveTemplate(ADMINBOARD_ID, TEST_ADMINBOARD_ID)
				.request(MediaType.APPLICATION_JSON_TYPE).header(ExtendedAuthDataResource.PARAMETER_HEADER_X_REQUEST_ID, TEST_REQUEST_ID)
				.post(Entity.entity(testData, MEDIATYPE_TEXT_CSV));

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
	}

	@Given("^a clean scenario")
	public void cleanScenario() {
		// Nothing to do since after every scenario the DB is cleaned
	}

	@When("^uploading a dataset for election with id (.+)$")
	public void whenUploadPredefinedDatasetForElectionId(final String electionId) {

		final String testData = createAndSignTestData(electionId, TEST_AUTH_ID);

		final Response response = webTarget().path(SAVE_AUTHENTICATION_PATH).resolveTemplate(TENANT_ID, TEST_TENANT_ID)
				.resolveTemplate(ELECTION_EVENT, electionId).resolveTemplate(ADMINBOARD_ID, TEST_ADMINBOARD_ID)
				.request(MediaType.APPLICATION_JSON_TYPE).header(ExtendedAuthDataResource.PARAMETER_HEADER_X_REQUEST_ID, TEST_REQUEST_ID)
				.post(Entity.entity(testData, MEDIATYPE_TEXT_CSV));

		eaResponseEntity = response.readEntity(TestEAResponse.class);
	}

	@When("^searching for authId (.+) and extra parameter (.+)$")
	public void searchForAuthIdAndExtraParam(final String loginData, final String extraParameter) {

		ExtendedAuthentication authentication = new ExtendedAuthentication();
		authentication.setAuthId(loginData);
		authentication.setExtraParam(extraParameter);

		final Response response = webTarget().path(AUTHENTICATION_PATH).resolveTemplate(TENANT_ID, TEST_TENANT_ID)
				.resolveTemplate(ELECTION_EVENT, electionId).request(MediaType.APPLICATION_JSON_TYPE)
				.header(ExtendedAuthDataResource.PARAMETER_HEADER_X_REQUEST_ID, TEST_REQUEST_ID)
				.post(Entity.entity(authentication, MediaType.APPLICATION_JSON_TYPE));

		eaResponseEntity = response.readEntity(TestEAResponse.class);
	}

	@When("^credential with old authId (.+) is updated with new authId (.+) and SVK (.+)$")
	public void updateByAuthId(final String oldAuthId, final String newAuthId, final String svk) throws Exception {

		final VoterInformation testVoterInformation = buildVoterInformation();
		final AuthenticationToken authenticationToken = createAndSignAuthenticationToken(testVoterInformation, keyPairForSigning.getPrivate());
		final ExtendedAuthenticationUpdateRequest extendedAuthenticationUpdateRequest = buildExtendedAuthenticationUpdateRequest(authenticationToken,
				oldAuthId, newAuthId, svk);
		final String authenticationTokenJsonString = ObjectMappers.toJson(authenticationToken);

		final Response response = webTarget().path(UPDATE_AUTHENTICATION).resolveTemplate(TENANT_ID, TEST_TENANT_ID)
				.resolveTemplate(ELECTION_EVENT, electionId).request(MediaType.APPLICATION_JSON_TYPE)
				.header(ExtendedAuthDataResource.PARAMETER_HEADER_X_REQUEST_ID, TEST_REQUEST_ID)
				.header(PARAMETER_AUTHENTICATION_TOKEN, authenticationTokenJsonString)
				.put(Entity.entity(extendedAuthenticationUpdateRequest, MediaType.APPLICATION_JSON_TYPE));

		eaResponseEntity = response.readEntity(TestEAResponse.class);
	}

	@When("^searching for election event (.+) authId (.+) and extra parameter(.+)$")
	public void searchForElectionEventIdAuthIdAndExtraParam(final String electionId, final String loginData, final String extraParameter) {

		ExtendedAuthentication authentication = new ExtendedAuthentication();
		authentication.setAuthId(loginData);
		authentication.setExtraParam(extraParameter);

		final Response response = webTarget().path(AUTHENTICATION_PATH).resolveTemplate(TENANT_ID, TEST_TENANT_ID)
				.resolveTemplate(ELECTION_EVENT, electionId).request(MediaType.APPLICATION_JSON_TYPE)
				.header(ExtendedAuthDataResource.PARAMETER_HEADER_X_REQUEST_ID, TEST_REQUEST_ID)
				.post(Entity.entity(authentication, MediaType.APPLICATION_JSON_TYPE));

		eaResponseEntity = response.readEntity(TestEAResponse.class);
	}

	@When("^uploading the csv file (.+) for election event (.+)$")
	public void uploadValidCredentials(String filename, String electionId) throws Exception {

		final Path credentialsFilePath = Paths.get(TEST_FILES_PATH + filename);
		final String testData = signCredentialsFile(credentialsFilePath);

		final Response post = webTarget().path(SAVE_AUTHENTICATION_PATH).resolveTemplate(TENANT_ID, TEST_TENANT_ID)
				.resolveTemplate(ELECTION_EVENT, electionId).resolveTemplate(ADMINBOARD_ID, TEST_ADMINBOARD_ID)
				.request(MediaType.APPLICATION_JSON_TYPE).header(ExtendedAuthDataResource.PARAMETER_HEADER_X_REQUEST_ID, TEST_REQUEST_ID)
				.post(Entity.entity(testData, MEDIATYPE_TEXT_CSV));

		assertEquals(Response.Status.OK.getStatusCode(), post.getStatus());
	}

	@When("^searching for election id (.+) and authId (.+)$")
	public void searchForElectionIdAndExtraParam(final String electionId, final String loginData) {

		final ResteasyClient client = new ResteasyClientBuilder().build();
		final ResteasyWebTarget webTarget = client.target(url.toString());
		ExtendedAuthentication authentication = new ExtendedAuthentication();
		authentication.setAuthId(loginData);

		Response post = webTarget.path(AUTHENTICATION_PATH).resolveTemplate(TENANT_ID, TEST_TENANT_ID).resolveTemplate(ELECTION_EVENT, electionId)
				.request(MediaType.APPLICATION_JSON_TYPE).header(ExtendedAuthDataResource.PARAMETER_HEADER_X_REQUEST_ID, TEST_REQUEST_ID)
				.post(Entity.entity(authentication, MediaType.APPLICATION_JSON_TYPE));

		eaResponseEntity = post.readEntity(TestEAResponse.class);
	}

	@When("^searching for authId, that is the following string: (.+)$")
	public void searchByAuthId(final String loginData) {

		final ResteasyClient client = new ResteasyClientBuilder().build();
		final ResteasyWebTarget webTarget = client.target(url.toString());
		ExtendedAuthentication authentication = new ExtendedAuthentication();
		authentication.setAuthId(loginData);

		Response post = webTarget.path(AUTHENTICATION_PATH).resolveTemplate(TENANT_ID, TEST_TENANT_ID).resolveTemplate(ELECTION_EVENT, electionId)
				.request(MediaType.APPLICATION_JSON_TYPE).header(ExtendedAuthDataResource.PARAMETER_HEADER_X_REQUEST_ID, TEST_REQUEST_ID)
				.post(Entity.entity(authentication, MediaType.APPLICATION_JSON_TYPE));
		eaResponseEntity = post.readEntity(TestEAResponse.class);
	}

	@Then("^the result should be the encoded start voting key (.+)$")
	public void mustReturnSvk(final String svk) {

		assertEquals(svk, eaResponseEntity.getEncryptedSVK());
	}

	@Then("^there is an error for wrong SVK$")
	public void mustReturnNotFoundErrorForWrongSVK() {

		assertThat(NOT_FOUND, is(eaResponseEntity.getResponseCode()));
	}

	@Then("^there is an error for wrong extra parameter, (.+) attempts left$")
	public void mustReturnUnauthorizedOrForbiddenError(Integer attempts) {

		if (attempts != 0) {
			assertThat(UNAUTHORIZED, is(eaResponseEntity.getResponseCode()));
		} else {
			assertThat(FORBIDDEN, is(eaResponseEntity.getResponseCode()));
		}
		assertThat(attempts, is(eaResponseEntity.getNumberOfRemainingAttempts()));
	}

	@Then("^there is an error that user exceed the number of attempts$")
	public void mustReturnForbiddenError() {

		assertThat(FORBIDDEN, is(eaResponseEntity.getResponseCode()));
		assertThat(0, is(eaResponseEntity.getNumberOfRemainingAttempts()));
	}

	@Then("^credential does not exists")
	public void mustReturnNotFoundErrorForCredential() {

		assertThat(NOT_FOUND, is(eaResponseEntity.getResponseCode()));
	}

	/**
	 * Builds an ExtendedAuthenticationUpdateRequest instance.The request instance contains the
	 * signature of ExtendedAuthenticationUpdate params and the certificate associated to the private
	 * key used for signing. The signature field not only contains the object's signature but also
	 * includes the content to be sign since is built using JWT format.
	 */
	private ExtendedAuthenticationUpdateRequest buildExtendedAuthenticationUpdateRequest(AuthenticationToken authenticationToken, String oldAuthId,
			String newAuthId, String svk) throws Exception {

		ExtendedAuthenticationUpdate extendedAuthenticationUpdate = new ExtendedAuthenticationUpdate();
		extendedAuthenticationUpdate.setAuthenticationTokenSignature(authenticationToken.getSignature());
		extendedAuthenticationUpdate.setOldAuthID(oldAuthId);
		extendedAuthenticationUpdate.setNewAuthID(newAuthId);
		extendedAuthenticationUpdate.setNewSVK(svk);

		final String extendedAuthenticationUpdateSignature = signFromMap(keyPairForSigning.getPrivate(), extendedAuthenticationUpdate);

		ExtendedAuthenticationUpdateRequest extendedAuthenticationUpdateRequest = new ExtendedAuthenticationUpdateRequest();
		extendedAuthenticationUpdateRequest.setCertificate(certificateString);
		extendedAuthenticationUpdateRequest.setSignature(extendedAuthenticationUpdateSignature);

		return extendedAuthenticationUpdateRequest;
	}

	/**
	 * Builds VoterInformation entity with test params
	 */
	private VoterInformation buildVoterInformation() {

		VoterInformation testVoterInformation = new VoterInformation();
		testVoterInformation.setBallotBoxId(TEST_BALLOT_BOX_ID);
		testVoterInformation.setCredentialId(TEST_CREDENTIAL_ID);
		testVoterInformation.setElectionEventId(electionId);
		testVoterInformation.setTenantId(TEST_TENANT_ID);
		testVoterInformation.setBallotId(TEST_BALLOT_ID);
		testVoterInformation.setVotingCardId(TEST_VOTINGCARD_ID);
		testVoterInformation.setVerificationCardId(TEST_VERIFICATION_CARD_ID);
		testVoterInformation.setVerificationCardSetId(TEST_VERIFICATION_CARD_SET_ID);
		testVoterInformation.setVotingCardSetId(TEST_VC_SET_ID);

		return testVoterInformation;
	}

	/**
	 * Mock the external call responses and add context to current http mock server.
	 */
	private void addHandlersToMockServer() {

		auValidationsURL = auValidationsURL.replace(TEST_ELECTION_EVENT_ID, electionId);
		auChainValidationURL = auChainValidationURL.replace(TEST_ELECTION_EVENT_ID, electionId);
		auValidateCertificateChainURL = auValidateCertificateChainURL.replace(TEST_ELECTION_EVENT_ID, electionId);

		ExtendedAuthenticationArquillianDeployment.mockHTTPServer.createContext(auValidationsURL, auHandler -> {
			final ValidationResult validationResult = new ValidationResult(true);
			final String response = ObjectMappers.toJson(validationResult);
			auHandler.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
			auHandler.sendResponseHeaders(Response.Status.OK.getStatusCode(), response.length());
			final OutputStream os = auHandler.getResponseBody();
			os.write(response.getBytes(StandardCharsets.UTF_8));
			os.close();
		});

		ExtendedAuthenticationArquillianDeployment.mockHTTPServer.createContext(auChainValidationURL, auHandler -> {
			final ValidationResult validationResult = new ValidationResult(true);
			final String response = ObjectMappers.toJson(validationResult);
			auHandler.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
			auHandler.sendResponseHeaders(Response.Status.OK.getStatusCode(), response.length());
			final OutputStream os = auHandler.getResponseBody();
			os.write(response.getBytes(StandardCharsets.UTF_8));
			os.close();
		});

		ExtendedAuthenticationArquillianDeployment.mockHTTPServer.createContext(auValidateCertificateChainURL, auHandler -> {
			final ValidationResult validationResult = new ValidationResult(true);
			final String response = ObjectMappers.toJson(validationResult);
			auHandler.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
			auHandler.sendResponseHeaders(Response.Status.OK.getStatusCode(), response.length());
			final OutputStream os = auHandler.getResponseBody();
			os.write(response.getBytes(StandardCharsets.UTF_8));
			os.close();
		});
	}

	/**
	 * Builds a JWT using as claims the fields of the provided object to sign.
	 */
	private String signFromMap(PrivateKey privateKey, Object objectToSign) throws Exception {
		// this method has to be included into JSONSigner class when
		// creating a client for updating credentials to extended authentication
		String objectJson = ObjectMappers.toJson(objectToSign);
		@SuppressWarnings("unchecked")
		Map<String, Object> claimMap = new ObjectMapper().readValue(objectJson, HashMap.class);

		return Jwts.builder().setClaims(claimMap).signWith(SignatureAlgorithm.PS256, privateKey).compact();
	}

	/**
	 * Signs the credentials file for upload it. Because CSVSigner is adding the signature to the
	 * file, is needed to make a temporal copy of it in order for preserving its integrity and reuse
	 * it in further tests.
	 */
	private String signCredentialsFile(final Path csvFilePath) throws Exception {

		try (InputStream data = new ByteArrayInputStream(Files.readAllBytes(csvFilePath))) {

			final Path signedFileToUploadPath = createTemporaryFile(data);
			new CSVSigner().sign(keyPairForSigning.getPrivate(), signedFileToUploadPath);
			return new String(Files.readAllBytes(signedFileToUploadPath), StandardCharsets.UTF_8);
		}
	}

	/**
	 * Creates a temporary file using the provided stream
	 */
	private Path createTemporaryFile(final InputStream data) throws Exception {

		Path file = Files.createTempFile(EXTENDED_AUTHENTICATION_FILENAME, EXTENDED_AUTHENTICATION_FILE_EXTENSION);
		try {
			copy(data, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			delete(file);
			throw e;
		}
		return file;
	}

	/**
	 * Gets a Resteasy webTarget.
	 */
	private ResteasyWebTarget webTarget() {
		final ResteasyClient client = new ResteasyClientBuilder().build();
		return client.target(url.toString());
	}
}
