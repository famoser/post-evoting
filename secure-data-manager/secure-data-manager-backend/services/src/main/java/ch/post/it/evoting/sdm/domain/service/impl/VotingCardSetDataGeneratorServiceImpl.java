/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateParameters.Type;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.sdm.application.exception.DatabaseException;
import ch.post.it.evoting.sdm.application.service.CertificateManagementException;
import ch.post.it.evoting.sdm.application.service.ControlComponentKeysAccessorService;
import ch.post.it.evoting.sdm.application.service.ElectionEventService;
import ch.post.it.evoting.sdm.application.service.PlatformRootCAService;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.commons.domain.CreateVotingCardSetCertificatePropertiesContainer;
import ch.post.it.evoting.sdm.commons.domain.CreateVotingCardSetInput;
import ch.post.it.evoting.sdm.commons.domain.StartVotingCardGenerationJobResponse;
import ch.post.it.evoting.sdm.domain.common.JobStatus;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.config.VotingCardGenerationJobStatus;
import ch.post.it.evoting.sdm.domain.model.electionevent.ElectionEventRepository;
import ch.post.it.evoting.sdm.domain.model.generator.DataGeneratorResponse;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.domain.model.votingcardset.VotingCardSetRepository;
import ch.post.it.evoting.sdm.domain.service.ProgressManagerService;
import ch.post.it.evoting.sdm.domain.service.VotingCardSetDataGeneratorService;
import ch.post.it.evoting.sdm.domain.service.exception.VotingCardSetDataGeneratorServiceException;
import ch.post.it.evoting.sdm.domain.service.utils.SystemTenantPublicKeyLoader;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * This implementation contains a call to the configuration engine for generating the voting card set data.
 */
@Service
public class VotingCardSetDataGeneratorServiceImpl implements VotingCardSetDataGeneratorService {

	// The name of the json parameter number of voting card to generate.
	private static final String JSON_PARAM_NAME_NR_OF_VC_TO_GENERATE = "numberOfVotingCardsToGenerate";

	private static final Logger LOGGER = LoggerFactory.getLogger(VotingCardSetDataGeneratorServiceImpl.class);

	private static final String BASE_JOBS_URL_PATH = "/{tenantId}/{electionEventId}/jobs";

	@Autowired
	private VotingCardSetRepository votingCardSetRepository;

	@Autowired
	private BallotBoxRepository ballotBoxRepository;

	@Autowired
	private ElectionEventRepository electionEventRepository;

	@Autowired
	private SystemTenantPublicKeyLoader systemTenantPublicKeyLoader;

	@Autowired
	private PathResolver pathResolver;

	@Autowired
	private ConfigurationEntityStatusService configurationEntityStatusService;

	@Autowired
	private ControlComponentKeysAccessorService controlComponentKeysAccessorService;

	@Value("${tenantID}")
	private String tenantId;

	@Value("${CONFIG_GENERATOR_URL}")
	private String configServiceBaseUrl;

	@Autowired
	private RestTemplate restClient;

	@Autowired
	private ProgressManagerService<VotingCardGenerationJobStatus> progressManagerService;

	@Autowired
	private PlatformRootCAService platformRootCAService;

	@Autowired
	private CCPublicKeySignatureValidator keySignatureValidator;

	@Autowired
	private ElectionEventService electionEventService;

	@Value("${credential.sign.certificate.properties}")
	private String credentialSignCertificateProperties;

	@Value("${credential.auth.certificate.properties}")
	private String credentialAuthCertificateProperties;

	@Value("${voting.card.set.certificate.properties}")
	private String votingCardSetCertificateProperties;

	@Value("${verification.card.set.certificate.properties}")
	private String verificationCardSetCertificateProperties;

	private ExecutorService jobCompletionExecutor;

	@PostConstruct
	void setup() {
		jobCompletionExecutor = Executors.newCachedThreadPool();
	}

	@Override
	public DataGeneratorResponse generate(final String id, final String electionEventId) {
		DataGeneratorResponse result = new DataGeneratorResponse();

		try {

			// basic validation of the input
			if (id == null || id.isEmpty()) {
				result.setSuccessful(false);
				return result;
			}

			String votingCardSetAsJson = votingCardSetRepository.find(id);
			// simple check if there is a voting card set data returned
			if (JsonConstants.EMPTY_OBJECT.equals(votingCardSetAsJson)) {
				result.setSuccessful(false);
				return result;
			}

			// create the list of parameters to call the configuration json
			JsonObject votingCardSet = JsonUtils.getJsonObject(votingCardSetAsJson);
			String verificationCardSetId = votingCardSet.getString(JsonConstants.VERIFICATION_CARD_SET_ID);
			Path configPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR);
			Path configElectionEventPath = configPath.resolve(electionEventId);
			String ballotBoxId = votingCardSet.getJsonObject(JsonConstants.BALLOT_BOX).getString(JsonConstants.ID);
			String ballotBoxAsJson = ballotBoxRepository.find(ballotBoxId);
			JsonObject ballotBox = JsonUtils.getJsonObject(ballotBoxAsJson);
			String ballotId = ballotBox.getJsonObject(JsonConstants.BALLOT).getString(JsonConstants.ID);
			String electoralAuthorityId = ballotBox.getJsonObject(JsonConstants.ELECTORAL_AUTHORITY).getString(JsonConstants.ID);
			JsonObject electionEvent = JsonUtils.getJsonObject(electionEventRepository.find(electionEventId));
			Path destinationBallotFilePath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR).resolve(electionEventId)
					.resolve(Constants.CONFIG_DIR_NAME_ONLINE).resolve(Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION)
					.resolve(Constants.CONFIG_DIR_NAME_BALLOTS).resolve(ballotId).resolve(Constants.CONFIG_FILE_NAME_BALLOT_JSON);

			JsonArray choiceCodeKeysJsonArray = controlComponentKeysAccessorService.downloadChoiceCodeKeys(id);

			List<String> choiceCodeEncryptionKey = getChoiceCodesEncryptionKey(choiceCodeKeysJsonArray);
			validateChoiceCodesEncryptionKey(electionEventId, verificationCardSetId, choiceCodeEncryptionKey);

			controlComponentKeysAccessorService.writeChoiceCodeKeys(electionEventId, verificationCardSetId, choiceCodeKeysJsonArray);

			CreateVotingCardSetInput createVotingCardSetInput = new CreateVotingCardSetInput();
			createVotingCardSetInput.setStart(ballotBox.getString(JsonConstants.DATE_FROM));
			createVotingCardSetInput.setElectoralAuthorityID(electoralAuthorityId);
			createVotingCardSetInput.setEnd(ballotBox.getString(JsonConstants.DATE_TO));
			createVotingCardSetInput
					.setValidityPeriod(electionEvent.getJsonObject(JsonConstants.SETTINGS).getInt(JsonConstants.CERTIFICATES_VALIDITY_PERIOD));
			createVotingCardSetInput.setBasePath(configElectionEventPath.toString());
			createVotingCardSetInput.setBallotBoxID(ballotBoxId);
			createVotingCardSetInput.setBallotID(ballotId);
			createVotingCardSetInput.setBallotPath(destinationBallotFilePath.toString());
			createVotingCardSetInput.setEeID(electionEventId);
			createVotingCardSetInput.setNumberVotingCards(votingCardSet.getInt(JSON_PARAM_NAME_NR_OF_VC_TO_GENERATE));
			createVotingCardSetInput.setVerificationCardSetID(verificationCardSetId);
			createVotingCardSetInput.setVotingCardSetID(id);

			// INCLUDE ALIAS INSIDE THE OBJECT...
			createVotingCardSetInput.setVotingCardSetAlias(votingCardSet.getString(JsonConstants.ALIAS, ""));

			createVotingCardSetInput.setKeyForProtectingKeystorePassword(getPublicKeyForProtectingKeystorePassword());
			createVotingCardSetInput.setChoiceCodesEncryptionKey(choiceCodeEncryptionKey);

			createVotingCardSetInput.setPlatformRootCACertificate(PemUtils.certificateToPem(platformRootCAService.load()));

			createVotingCardSetInput.setCreateVotingCardSetCertificateProperties(getCertificateProperties());

			final ResponseEntity<StartVotingCardGenerationJobResponse> startJobResponse;
			try {
				startJobResponse = sendStartJobRequest(tenantId, electionEventId, createVotingCardSetInput);
			} catch (Exception e) {
				LOGGER.error("Failed to send start voting card generation job request.", e);
				result.setSuccessful(false);
				return result;
			}

			if (didJobStart(startJobResponse)) {
				StartVotingCardGenerationJobResponse jobResponse = startJobResponse.getBody();
				if (jobResponse == null) {
					LOGGER.error("Malformed response for the start voting card generation job.");
					result.setSuccessful(false);
				} else {
					final String jobId = jobResponse.getJobId();
					final Future<VotingCardGenerationJobStatus> future = registerJobForStatusProgressUpdate(jobId, startJobResponse);

					// wait for job to complete to update the verification card set
					// id and make it possible
					// to sign the voting card set.
					waitForJobCompletion(id, jobId, future);
					result.setResult(jobId);
					result.setSuccessful(true);
				}

			} else {
				LOGGER.error("Voting card generation job failed to start. Server response: {}", startJobResponse);
				result.setSuccessful(false);
			}

		} catch (DatabaseException e) {
			LOGGER.error("Error storing in database", e);
			result.setSuccessful(false);
		} catch (Exception e) {
			LOGGER.error("Error processing request", e);
			result.setSuccessful(false);
		}
		return result;
	}

	private CreateVotingCardSetCertificatePropertiesContainer getCertificateProperties() throws IOException {

		LOGGER.info("Obtaining certificate properties from the following paths:");
		LOGGER.info(" {}", credentialSignCertificateProperties);
		LOGGER.info(" {}", credentialAuthCertificateProperties);
		LOGGER.info(" {}", verificationCardSetCertificateProperties);

		CreateVotingCardSetCertificatePropertiesContainer createVotingCardSetCertificateProperties = new CreateVotingCardSetCertificatePropertiesContainer();

		Properties loadedCredentialSignCertificateProperties = getCertificateParameters(credentialSignCertificateProperties);
		Properties loadedCredentialAuthCertificateProperties = getCertificateParameters(credentialAuthCertificateProperties);
		Properties loadedVerificationCardSetCertificateProperties = getCertificateParameters(verificationCardSetCertificateProperties);

		createVotingCardSetCertificateProperties.setCredentialSignCertificateProperties(loadedCredentialSignCertificateProperties);
		createVotingCardSetCertificateProperties.setCredentialAuthCertificateProperties(loadedCredentialAuthCertificateProperties);
		createVotingCardSetCertificateProperties.setVerificationCardSetCertificateProperties(loadedVerificationCardSetCertificateProperties);

		LOGGER.info("Obtained certificate properties");

		return createVotingCardSetCertificateProperties;
	}

	private Properties getCertificateParameters(String path) throws IOException {
		final Properties props = new Properties();
		try (FileInputStream fis = new FileInputStream(path)) {
			props.load(fis);
		}
		return props;
	}

	private void validateChoiceCodesEncryptionKey(String electionEventId, String verificationCardId, List<String> keys) {
		try {
			X509Certificate rootCACertificate = platformRootCAService.load();
			Decoder decoder = Base64.getDecoder();
			for (String string : keys) {
				JsonObject object = JsonUtils.getJsonObject(string);
				ElGamalPublicKey key = ElGamalPublicKey.fromJson(object.get("publicKey").toString());
				byte[] signature = decoder.decode(object.getString("signature"));
				X509Certificate signingCertificate = (X509Certificate) PemUtils.certificateFromPem(object.getString("signerCertificate"));
				X509Certificate nodeCACertificate = (X509Certificate) PemUtils.certificateFromPem(object.getString("nodeCACertificate"));
				X509Certificate[] chain = { signingCertificate, nodeCACertificate, rootCACertificate };
				keySignatureValidator.checkChoiceCodesEncryptionKeySignature(signature, chain, key, electionEventId, verificationCardId);
			}
		} catch (SignatureException | GeneralCryptoLibException | CertificateManagementException e) {
			throw new IllegalStateException("Invalid choice codes encryption keys.", e);
		}
	}

	private List<String> getChoiceCodesEncryptionKey(JsonArray array) {
		List<String> result = new ArrayList<>(array.size());
		for (int i = 0; i < array.size(); i++) {
			result.add(array.getJsonObject(i).toString());
		}
		return result;
	}

	private void waitForJobCompletion(final String votingCardSetId, final String jobId, final Future<VotingCardGenerationJobStatus> future) {
		jobCompletionExecutor.submit(() -> {
			final VotingCardGenerationJobStatus votingCardGenerationJobStatus;
			try {
				votingCardGenerationJobStatus = future.get();
				if (JobStatus.COMPLETED.equals(votingCardGenerationJobStatus.getStatus())) {
					// update the status of the voting card set
					configurationEntityStatusService.update(Status.GENERATED.name(), votingCardSetId, votingCardSetRepository);
					LOGGER.info("VotingCardSet generation job '{}' has completed successfully. Final status: {}", jobId,
							votingCardGenerationJobStatus);
				} else {
					LOGGER.error("Voting card generation job failed to complete. Response: {}", votingCardGenerationJobStatus);
				}
			} catch (InterruptedException e) {
				LOGGER.error("We got interrupted while waiting for job completion.");
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				LOGGER.error("Unexpected exception while waiting for job completion.", e);
			}
		});
	}

	private Future<VotingCardGenerationJobStatus> registerJobForStatusProgressUpdate(final String jobId, final ResponseEntity<?> response) {
		URI statusLocation = response.getHeaders().getLocation();
		// shouldn't be null, but...
		if (statusLocation != null) {
			return progressManagerService.registerJob(jobId, statusLocation);
		} else {
			final String error = String.format("Failed to get location header for status update for job '%s'", jobId);
			LOGGER.error(error);
			throw new VotingCardSetDataGeneratorServiceException(error);
		}
	}

	private ResponseEntity<StartVotingCardGenerationJobResponse> sendStartJobRequest(final String tenantId, final String electionEventId,
			final CreateVotingCardSetInput input) {

		final String targetUrl = configServiceBaseUrl + BASE_JOBS_URL_PATH;
		try {
			return restClient.postForEntity(targetUrl, input, StartVotingCardGenerationJobResponse.class, tenantId, electionEventId);
		} catch (RestClientException e) {
			throw new VotingCardSetDataGeneratorServiceException(String.format("Error performing post request to endpoint %s", targetUrl), e);
		}
	}

	private boolean didJobStart(final ResponseEntity<?> response) {
		return HttpStatus.CREATED.value() == response.getStatusCode().value();
	}

	private String getPublicKeyForProtectingKeystorePassword() throws IOException {
		return systemTenantPublicKeyLoader.load(tenantId, "VV", Type.ENCRYPTION);
	}
}
