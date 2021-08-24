/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import javax.json.JsonObject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

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
import ch.post.it.evoting.sdm.commons.domain.CreateBallotBoxesInput;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.electionevent.ElectionEventRepository;
import ch.post.it.evoting.sdm.domain.model.generator.DataGeneratorResponse;
import ch.post.it.evoting.sdm.domain.service.BallotBoxDataGeneratorService;
import ch.post.it.evoting.sdm.domain.service.utils.SystemTenantPublicKeyLoader;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * This implementation contains a call to the configuration engine for generating the ballot box data.
 */
@Service
public class BallotBoxDataGeneratorServiceImpl implements BallotBoxDataGeneratorService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BallotBoxDataGeneratorServiceImpl.class);

	@Autowired
	private BallotBoxRepository ballotBoxRepository;

	@Autowired
	private ElectionEventRepository electionEventRepository;

	@Autowired
	private PathResolver pathResolver;

	@Autowired
	private SystemTenantPublicKeyLoader systemTenantPublicKeyLoader;

	@Value("${tenantID}")
	private String tenantId;

	@Value("${CREATE_BALLOT_BOX_URL}")
	private String createBallotBoxURL;

	@Value("${ballotbox.certificate.properties}")
	private String ballotBoxCertificateProperties;

	/**
	 * The method prepares an input to call the configuration engine like it is done from command line. The input consists of data from the ballot in
	 * json format and paths that we use.
	 */
	@Override
	public DataGeneratorResponse generate(final String ballotBoxId, final String electionEventId) throws IOException {
		// some basic validation of the input
		DataGeneratorResponse result = new DataGeneratorResponse();
		if (StringUtils.isEmpty(ballotBoxId)) {
			result.setSuccessful(false);
			return result;
		}

		String ballotBoxAsJson = ballotBoxRepository.find(ballotBoxId);
		// simple check if exists data
		if (JsonConstants.EMPTY_OBJECT.equals(ballotBoxAsJson)) {
			result.setSuccessful(false);
			return result;
		}

		JsonObject ballotBox = JsonUtils.getJsonObject(ballotBoxAsJson);
		String ballotId = ballotBox.getJsonObject(JsonConstants.BALLOT).getString(JsonConstants.ID);

		// The election event is retrieved in order to check later some settings
		String electionEventAsJson = electionEventRepository.find(electionEventId);
		if (JsonConstants.EMPTY_OBJECT.equals(electionEventAsJson)) {
			result.setSuccessful(false);
			return result;
		}
		JsonObject electionEvent = JsonUtils.getJsonObject(electionEventAsJson);

		Path configPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR);
		Path configElectionEventPath = configPath.resolve(electionEventId);

		CreateBallotBoxesInput input = new CreateBallotBoxesInput();
		input.setBallotBoxID(ballotBoxId);
		input.setBallotID(ballotId);
		input.setAlias(ballotBox.getString(JsonConstants.ALIAS));
		input.setElectoralAuthorityID(ballotBox.getJsonObject(JsonConstants.ELECTORAL_AUTHORITY).getString(JsonConstants.ID));
		input.setOutputFolder(configElectionEventPath.toString());
		input.setKeyForProtectingKeystorePassword(getPublicKeyForProtectingKeystorePassword());
		input.setTest(ballotBox.getString(JsonConstants.TEST));
		input.setGracePeriod(ballotBox.getString(JsonConstants.GRACE_PERIOD));
		input.setBallotBoxCertificateProperties(getCertificateProperties());

		LOGGER.debug("------------------------------ Dates in BB");
		input.setStart(ballotBox.getString(JsonConstants.DATE_FROM));
		input.setEnd(ballotBox.getString(JsonConstants.DATE_TO));
		input.setValidityPeriod(electionEvent.getJsonObject(JsonConstants.SETTINGS).getInt(JsonConstants.CERTIFICATES_VALIDITY_PERIOD));
		input.setWriteInAlphabet(electionEvent.getJsonObject(JsonConstants.SETTINGS).getString(JsonConstants.WRITE_IN_ALPHABET));
		LOGGER.debug("Start {}", input.getStart());
		LOGGER.debug("End {}", input.getEnd());
		LOGGER.debug("------------------------------ End Dates in BB");

		// call the configuration engine with the prepared parameters
		try {

			WebTarget ballotBoxClient = createWebClient();

			ballotBoxClient.request().post(Entity.entity(ObjectMappers.toJson(input), MediaType.APPLICATION_JSON));

		} catch (ProcessingException e) {
			LOGGER.error("Error performing post request", e);
			result.setSuccessful(false);
		}

		return result;
	}

	/**
	 * Generates a WebTarget client
	 */
	public WebTarget createWebClient() {
		return ClientBuilder.newClient().target(createBallotBoxURL);
	}

	private Properties getCertificateProperties() throws IOException {

		LOGGER.info("Obtaining certificate properties from the following paths:");
		LOGGER.info(" {}", ballotBoxCertificateProperties);

		Properties loadedBallotBoxCertificateProperties = getCertificateParameters(ballotBoxCertificateProperties);
		LOGGER.info("Obtained certificate properties");

		return loadedBallotBoxCertificateProperties;
	}

	private Properties getCertificateParameters(String path) throws IOException {
		final Properties props = new Properties();
		try (FileInputStream fis = new FileInputStream(path)) {
			props.load(fis);
		}
		return props;
	}

	private String getPublicKeyForProtectingKeystorePassword() throws IOException {

		return systemTenantPublicKeyLoader.load(tenantId, "EI", Type.ENCRYPTION);
	}
}
