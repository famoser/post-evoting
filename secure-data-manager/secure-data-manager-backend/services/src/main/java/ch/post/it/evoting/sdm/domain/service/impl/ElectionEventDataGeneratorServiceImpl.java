/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.json.JsonObject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptolib.certificates.bean.CertificateParameters.Type;
import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.commons.domain.CreateElectionEventCertificatePropertiesContainer;
import ch.post.it.evoting.sdm.commons.domain.CreateElectionEventInput;
import ch.post.it.evoting.sdm.domain.model.electionevent.ElectionEventRepository;
import ch.post.it.evoting.sdm.domain.model.generator.DataGeneratorResponse;
import ch.post.it.evoting.sdm.domain.service.ElectionEventDataGeneratorService;
import ch.post.it.evoting.sdm.domain.service.utils.SystemTenantPublicKeyLoader;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * This implementation contains a call to the configuration engine for generating the election event data.
 */
@Service
public class ElectionEventDataGeneratorServiceImpl implements ElectionEventDataGeneratorService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElectionEventDataGeneratorServiceImpl.class);
	private static final String JSON_PARAM_NAME_MAXIMUM_NUMBER_OF_ATTEMPTS = "maximumNumberOfAttempts";
	private static final String JSON_PARAM_NAME_NUMBER_VOTES_PER_AUTH_TOKEN = "numberVotesPerAuthToken";
	private static final String JSON_PARAM_NAME_NUMBER_VOTES_PER_VOTING_CARD = "numberVotesPerVotingCard";
	private static final String JSON_PARAM_NAME_AUTH_TOKEN_EXPIRATION_TIME = "authTokenExpirationTime";
	private static final String JSON_PARAM_NAME_CHALLENGE_LENGTH = "challengeLength";
	private static final String JSON_PARAM_NAME_CHALLENGE_RESPONSE_EXPIRATION_TIME = "challengeResponseExpirationTime";
	private static final String JSON_PARAM_NAME_CERTIFICATES_VALIDITY_PERIOD = "certificatesValidityPeriod";
	private static final String JSON_PARAM_NAME_DATE_TO = "dateTo";
	private static final String JSON_PARAM_NAME_DATE_FROM = "dateFrom";

	@Autowired
	private ElectionEventRepository electionEventRepository;

	@Autowired
	private PathResolver pathResolver;

	@Autowired
	private SystemTenantPublicKeyLoader systemTenantPublicKeyLoader;

	@Value("${tenantID}")
	private String tenantId;

	@Value("${CREATE_ELECTION_EVENT_URL}")
	private String createElectionEventURL;

	@Value("${services.ca.certificate.properties}")
	private String servicesCaCertificateProperties;

	@Value("${election.ca.certificate.properties}")
	private String electionCaCertificateProperties;

	@Value("${credentials.ca.certificate.properties}")
	private String credentialsCaCertificateProperties;

	@Value("${authorities.ca.certificate.properties}")
	private String authoritiesCaCertificateProperties;

	@Value("${auth.token.signer.certificate.properties}")
	private String authTokenSignerCertificateProperties;

	/**
	 * This method creates the input necessary for the configuration to work and calls it to generate the election event data for the one identified
	 * by the given id. It simulates the use from command line by calling directly the code. One of the inputs being a properties file, this is
	 * created and saved on disk.
	 *
	 * @see ElectionEventDataGeneratorService#generate(String)
	 */
	@Override
	public DataGeneratorResponse generate(final String electionEventId) throws IOException {
		DataGeneratorResponse result = new DataGeneratorResponse();

		// basic validation of the input
		if (StringUtils.isBlank(electionEventId)) {
			result.setSuccessful(false);

			return result;
		}

		// just in case the directory is not created
		Path configPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR);
		try {
			makePath(configPath);
		} catch (IOException e2) {
			LOGGER.error("", e2);

			result.setSuccessful(false);
			return result;
		}

		String electionEvent = electionEventRepository.find(electionEventId);
		if (JsonConstants.EMPTY_OBJECT.equals(electionEvent)) {

			result.setSuccessful(false);
			return result;
		}

		// call the configuration engine with the prepared parameters
		JsonObject electionEventJson = JsonUtils.getJsonObject(electionEvent);
		try {

			// prepare the parameters for call of configuration engine

			CreateElectionEventInput input = new CreateElectionEventInput();
			JsonObject settings = electionEventJson.getJsonObject(JsonConstants.SETTINGS);

			input.setAuthTokenExpTime(Integer.toString(settings.getInt(JSON_PARAM_NAME_AUTH_TOKEN_EXPIRATION_TIME)));
			input.setChallengeLength(Integer.toString(settings.getInt(JSON_PARAM_NAME_CHALLENGE_LENGTH)));
			input.setChallengeResExpTime(Integer.toString(settings.getInt(JSON_PARAM_NAME_CHALLENGE_RESPONSE_EXPIRATION_TIME)));
			input.setEeid(electionEventId);
			input.setEnd(electionEventJson.getString(JSON_PARAM_NAME_DATE_TO));
			input.setMaxNumberOfAttempts(Integer.toString(settings.getInt(JSON_PARAM_NAME_MAXIMUM_NUMBER_OF_ATTEMPTS)));
			input.setNumVotesPerAuthToken(Integer.toString(settings.getInt(JSON_PARAM_NAME_NUMBER_VOTES_PER_AUTH_TOKEN)));
			input.setNumVotesPerVotingCard(Integer.toString(settings.getInt(JSON_PARAM_NAME_NUMBER_VOTES_PER_VOTING_CARD)));
			input.setOutputPath(configPath.toString());
			input.setStart(electionEventJson.getString(JSON_PARAM_NAME_DATE_FROM));
			input.setValidityPeriod(settings.getInt(JSON_PARAM_NAME_CERTIFICATES_VALIDITY_PERIOD));

			input.setKeyForProtectingKeystorePassword(getPublicKeyForProtectingKeystorePassword());

			input.setCertificatePropertiesInput(getCertificateProperties());

			WebTarget electionEventClient = createWebClient();

			final Response response = electionEventClient.request().post(Entity.entity(ObjectMappers.toJson(input), MediaType.APPLICATION_JSON));

			result.setSuccessful(Response.Status.Family.SUCCESSFUL.equals(response.getStatusInfo().getFamily()));

		} catch (ProcessingException e) {
			LOGGER.error("Error performing post request", e);

			result.setSuccessful(false);
		}

		return result;
	}

	private CreateElectionEventCertificatePropertiesContainer getCertificateProperties() throws IOException {

		LOGGER.info("Obtaining certificate properties from the following paths:");
		LOGGER.info(" {}", servicesCaCertificateProperties);
		LOGGER.info(" {}", electionCaCertificateProperties);
		LOGGER.info(" {}", credentialsCaCertificateProperties);
		LOGGER.info(" {}", authoritiesCaCertificateProperties);
		LOGGER.info(" {}", authTokenSignerCertificateProperties);

		CreateElectionEventCertificatePropertiesContainer createElectionEventCertificateProperties = new CreateElectionEventCertificatePropertiesContainer();

		Properties loadedServicesCaCertificatePropertiesAsString = getCertificateParameters(servicesCaCertificateProperties);
		Properties loadedElectionCaCertificatePropertiesAsString = getCertificateParameters(electionCaCertificateProperties);
		Properties loadedCredentialsCaCertificatePropertiesAsString = getCertificateParameters(credentialsCaCertificateProperties);
		Properties loadedAuthoritiesCaCertificatePropertiesAsString = getCertificateParameters(authoritiesCaCertificateProperties);

		Properties loadedAuthTokenSignerCertificatePropertiesAsString = getCertificateParameters(authTokenSignerCertificateProperties);

		Map<String, Properties> configProperties = new HashMap<>();
		configProperties.put("electioneventca", loadedElectionCaCertificatePropertiesAsString);
		configProperties.put("authoritiesca", loadedAuthoritiesCaCertificatePropertiesAsString);
		configProperties.put("servicesca", loadedServicesCaCertificatePropertiesAsString);
		configProperties.put("credentialsca", loadedCredentialsCaCertificatePropertiesAsString);

		createElectionEventCertificateProperties.setAuthTokenSignerCertificateProperties(loadedAuthTokenSignerCertificatePropertiesAsString);
		createElectionEventCertificateProperties.setNameToCertificateProperties(configProperties);

		LOGGER.info("Obtained certificate properties");

		return createElectionEventCertificateProperties;
	}

	private Properties getCertificateParameters(String path) throws IOException {
		final Properties props = new Properties();
		try (FileInputStream fis = new FileInputStream(path)) {
			props.load(fis);
		}
		return props;
	}

	private String getPublicKeyForProtectingKeystorePassword() throws IOException {

		return systemTenantPublicKeyLoader.load(tenantId, "AU", Type.ENCRYPTION);
	}

	/**
	 * Creates all the directories from the path if they don't exist yet. This is wrapper over a static method in order to make the class testable.
	 *
	 * @param path The path to be created.
	 * @return a Path representing the directory created.
	 * @throws IOException in case there is a I/O problem.
	 */
	Path makePath(final Path path) throws IOException {
		return Files.createDirectories(path);
	}

	/**
	 * Generates a WebTarget client
	 *
	 * @return
	 */
	public WebTarget createWebClient() {
		return ClientBuilder.newClient().target(createElectionEventURL);
	}
}
