/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static ch.post.it.evoting.domain.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.signer.configuration.DigitalSignerPolicyFromProperties;
import ch.post.it.evoting.cryptolib.certificates.utils.CertificateChainValidator;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.commons.serialization.JsonSignatureService;
import ch.post.it.evoting.domain.election.BallotBox;
import ch.post.it.evoting.domain.election.BallotBoxContextData;
import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.application.exception.SignatureException;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.domain.common.SignedObject;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.domain.model.status.SynchronizeStatus;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.infrastructure.PathResolver;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;
import ch.post.it.evoting.sdm.utils.ConfigObjectMapper;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * Service to operate with ballot boxes
 */
@Service
public class BallotBoxService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BallotBoxService.class);

	// This insignificant lines correspond to the signature included in a downloaded ballot box file.
	private static final long INSIGNIFICANT_NUMBER_OF_LINES_IN_DOWNLOADED_BALLOT_BOX_FILE = 1L;
	private static final String BALLOT_BOX_CERT_NODE = "ballotBoxCert";
	private static final String SERVICES_CA_NODE = "servicesCA";
	private static final String ELECTION_ROOT_CA_NODE = "electionRootCA";

	@Autowired
	private ConfigurationEntityStatusService statusService;

	@Autowired
	private BallotBoxRepository ballotBoxRepository;

	@Autowired
	private PathResolver pathResolver;

	@Autowired
	private AsymmetricServiceAPI asymmetricService;

	private ConfigObjectMapper mapper;

	@PostConstruct
	public void init() {
		mapper = new ConfigObjectMapper();
	}

	/**
	 * Sign the ballot box configuration and change the state of the ballot box from ready to SIGNED for a given election event and ballot box id.
	 *
	 * @param electionEventId the election event id.
	 * @param ballotBoxId     the ballot box id.
	 * @param privateKeyPEM   the administration board private key in PEM format.
	 * @throws ResourceNotFoundException if the ballot box is not found.
	 * @throws GeneralCryptoLibException if the private key cannot be read.
	 */
	public void sign(String electionEventId, String ballotBoxId, final String privateKeyPEM)
			throws ResourceNotFoundException, GeneralCryptoLibException, IOException {

		PrivateKey privateKey = PemUtils.privateKeyFromPem(privateKeyPEM);

		JsonObject ballotBoxJsonObject = getValidBallotBox(electionEventId, ballotBoxId);
		String ballotId = ballotBoxJsonObject.getJsonObject(JsonConstants.BALLOT).getString(JsonConstants.ID);

		Path ballotBoxConfigurationFilesPath = pathResolver.resolveBallotBoxPath(electionEventId, ballotId, ballotBoxId);

		LOGGER.info("Signing ballot box {}", ballotBoxId);
		signBallotBoxJSON(privateKey, ballotBoxConfigurationFilesPath);

		LOGGER.info("Signing ballot box context data");
		signBallotBoxContextDataJSON(privateKey, ballotBoxConfigurationFilesPath);

		LOGGER.info("Changing the ballot box status");
		statusService.updateWithSynchronizedStatus(Status.SIGNED.name(), ballotBoxId, ballotBoxRepository, SynchronizeStatus.PENDING);

		LOGGER.info("The ballot box was successfully signed");

	}

	private void signBallotBoxContextDataJSON(final PrivateKey privateKey, final Path ballotBoxConfigurationFilesPath) throws IOException {
		Path ballotBoxContextDataJSONPath = ballotBoxConfigurationFilesPath.resolve(Constants.CONFIG_DIR_NAME_BALLOTBOX_CONTEXT_DATA_JSON)
				.toAbsolutePath();
		Path signedBallotBoxContextDataJSONPath = ballotBoxConfigurationFilesPath
				.resolve(Constants.CONFIG_FILE_NAME_SIGNED_BALLOTBOX_CONTEXT_DATA_JSON).toAbsolutePath();

		BallotBoxContextData ballotBoxContextData = mapper
				.fromJSONFileToJava(new File(ballotBoxContextDataJSONPath.toString()), BallotBoxContextData.class);

		String signedBallotBoxContextData = JsonSignatureService.sign(privateKey, ballotBoxContextData);
		SignedObject signedBallotBoxContextDataObject = new SignedObject();
		signedBallotBoxContextDataObject.setSignature(signedBallotBoxContextData);
		mapper.fromJavaToJSONFile(signedBallotBoxContextDataObject, new File(signedBallotBoxContextDataJSONPath.toString()));
	}

	private void signBallotBoxJSON(final PrivateKey privateKey, final Path ballotBoxConfigurationFilesPath) throws IOException {
		Path ballotBoxJSONPath = ballotBoxConfigurationFilesPath.resolve(Constants.CONFIG_DIR_NAME_BALLOTBOX_JSON).toAbsolutePath();
		Path signedBallotBoxJSONPath = ballotBoxConfigurationFilesPath.resolve(Constants.CONFIG_FILE_NAME_SIGNED_BALLOTBOX_JSON).toAbsolutePath();

		BallotBox ballotBox = mapper.fromJSONFileToJava(new File(ballotBoxJSONPath.toString()), BallotBox.class);

		String signedBallotBox = JsonSignatureService.sign(privateKey, ballotBox);
		SignedObject signedBallotBoxObject = new SignedObject();
		signedBallotBoxObject.setSignature(signedBallotBox);
		mapper.fromJavaToJSONFile(signedBallotBoxObject, new File(signedBallotBoxJSONPath.toString()));
	}

	private JsonObject getValidBallotBox(String electionEventId, String ballotBoxId) throws ResourceNotFoundException {

		Optional<JsonObject> possibleBallotBox = getPossibleValidBallotBox(electionEventId, ballotBoxId);

		if (!possibleBallotBox.isPresent()) {
			throw new ResourceNotFoundException("Ballot box not found");
		}

		return possibleBallotBox.get();
	}

	private Optional<JsonObject> getPossibleValidBallotBox(final String electionEventId, final String ballotBoxId) {

		Optional<JsonObject> ballotBox = Optional.empty();

		Map<String, Object> attributeValueMap = new HashMap<>();
		attributeValueMap.put(JsonConstants.ELECTION_EVENT_DOT_ID, electionEventId);
		attributeValueMap.put(JsonConstants.ID, ballotBoxId);
		attributeValueMap.put(JsonConstants.STATUS, Status.READY.name());
		String ballotBoxResultListAsJson = ballotBoxRepository.list(attributeValueMap);
		if (StringUtils.isEmpty(ballotBoxResultListAsJson)) {
			return ballotBox;
		} else {
			JsonArray ballotBoxResultList = JsonUtils.getJsonObject(ballotBoxResultListAsJson).getJsonArray(JsonConstants.RESULT);
			// Assume that there is just one element as result of the search.
			if (ballotBoxResultList != null && !ballotBoxResultList.isEmpty()) {
				ballotBox = Optional.of(ballotBoxResultList.getJsonObject(0));
			} else {
				return ballotBox;
			}
		}
		return ballotBox;
	}

	/**
	 * query ballot boxes to process in the synchronization process
	 *
	 * @return JsonArray with ballot boxes to upload
	 */
	public JsonArray getBallotBoxesReadyToSynchronize(String electionEvent) {

		Map<String, Object> params = new HashMap<>();

		params.put(JsonConstants.STATUS, Status.SIGNED.name());
		params.put(JsonConstants.SYNCHRONIZED, SynchronizeStatus.PENDING.getIsSynchronized().toString());
		// If there is an election event as parameter, it will be included in
		// the query
		if (!Constants.NULL_ELECTION_EVENT_ID.equals(electionEvent)) {
			params.put(JsonConstants.ELECTION_EVENT_DOT_ID, electionEvent);
		}
		String serializedBallotBoxes = ballotBoxRepository.list(params);

		return JsonUtils.getJsonObject(serializedBallotBoxes).getJsonArray(JsonConstants.RESULT);
	}

	/**
	 * Updates the state of the synchronization status of the ballot box
	 */
	public void updateSynchronizationStatus(String ballotBoxId, boolean success) {
		JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
		jsonObjectBuilder.add(JsonConstants.ID, ballotBoxId);
		if (success) {
			jsonObjectBuilder.add(JsonConstants.SYNCHRONIZED, SynchronizeStatus.SYNCHRONIZED.getIsSynchronized().toString());
			jsonObjectBuilder.add(JsonConstants.DETAILS, SynchronizeStatus.SYNCHRONIZED.getStatus());
		} else {
			jsonObjectBuilder.add(JsonConstants.DETAILS, SynchronizeStatus.FAILED.getStatus());
		}
		ballotBoxRepository.update(jsonObjectBuilder.build().toString());
	}

	/**
	 * Checks that the ballot box has status Status.BB_DOWNLOADED
	 *
	 * @throws ResourceNotFoundException if the ballot box cannot be found in the ballot box repository.
	 */
	public boolean isDownloaded(String ballotBoxId) throws ResourceNotFoundException {
		return hasStatus(ballotBoxId, Status.BB_DOWNLOADED);
	}

	/**
	 * Checks that the ballot box has status Status.BB_DOWNLOADED
	 *
	 * @throws ResourceNotFoundException if the ballot box cannot be found in the ballot box repository.
	 */
	public boolean hasStatus(String ballotBoxId, Status expectedStatus) throws ResourceNotFoundException {
		checkNotNull(ballotBoxId);
		validateUUID(ballotBoxId);

		JsonObject ballotBox = getBallotBoxInfo(ballotBoxId);
		String currentStatus;
		try {
			currentStatus = ballotBox.getString(JsonConstants.STATUS);
		} catch (IllegalArgumentException e) {
			//Ballot box does not have a status field
			return false;
		}
		return Status.valueOf(currentStatus).equals(expectedStatus);
	}

	/**
	 * Gets the ballot Id associated associated with this ballot box
	 */
	public String getBallotId(String ballotBoxId) throws ResourceNotFoundException {
		checkNotNull(ballotBoxId);
		validateUUID(ballotBoxId);

		JsonObject ballotBox = getBallotBoxInfo(ballotBoxId);
		return ballotBox.getJsonObject(JsonConstants.BALLOT).getString(JsonConstants.ID);
	}

	/**
	 * Returns the ballot box information as a JSON object.
	 */
	private JsonObject getBallotBoxInfo(String ballotBoxId) throws ResourceNotFoundException {
		JsonObject ballotBox = JsonUtils.getJsonObject(ballotBoxRepository.find(ballotBoxId));

		if (ballotBox == null || ballotBox.size() == 0) {
			throw new ResourceNotFoundException("Ballot box entity does not exist for id: " + ballotBoxId);
		}
		return ballotBox;
	}

	/**
	 * Checks if the ballot box is empty or not.
	 */
	public boolean isDownloadedBallotBoxEmpty(final String electionEventId, final String ballotId, final String ballotBoxId) {
		checkNotNull(electionEventId);
		checkNotNull(ballotId);
		checkNotNull(ballotBoxId);
		validateUUID(electionEventId);
		validateUUID(ballotId);
		validateUUID(ballotBoxId);

		final Path ballotBoxPath = pathResolver.resolveBallotBoxPath(electionEventId, ballotId, ballotBoxId);
		final Path downloadedBallotBoxPath = ballotBoxPath.resolve(Constants.CONFIG_FILE_NAME_ELECTION_INFORMATION_DOWNLOADED_BALLOT_BOX);

		if (!isSignatureValid(electionEventId, ballotBoxPath, downloadedBallotBoxPath)) {
			throw new SignatureException(
					String.format("The signature of file downloadedBallotBox.csv is invalid for ballot box: %s-%s-%s", electionEventId, ballotId,
							ballotBoxId));
		}

		long lineCount;
		try (final Stream<String> lines = Files.lines(downloadedBallotBoxPath)) {
			// We only count not empty or significant lines
			lineCount = lines.filter(line -> !line.isEmpty()).count();
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to read the downloaded ballot box CSV file", e);
		}

		// Must have smaller or equal number of lines than the insignificant number of lines in the downloaded ballot box file.
		return lineCount <= INSIGNIFICANT_NUMBER_OF_LINES_IN_DOWNLOADED_BALLOT_BOX_FILE;
	}

	/**
	 * Checks if the ballot box has confirmed votes.
	 */
	public boolean hasDownloadedBallotBoxConfirmedVotes(final String electionEventId, final String ballotId, final String ballotBoxId) {
		checkNotNull(electionEventId);
		checkNotNull(ballotId);
		checkNotNull(ballotBoxId);
		validateUUID(electionEventId);
		validateUUID(ballotId);
		validateUUID(ballotBoxId);

		final Path ballotBoxPath = pathResolver.resolveBallotBoxPath(electionEventId, ballotId, ballotBoxId);
		final Path downloadedBallotBoxPath = ballotBoxPath.resolve(Constants.CONFIG_FILE_NAME_ELECTION_INFORMATION_DOWNLOADED_BALLOT_BOX);

		long confirmedVoteCount;
		try (final Stream<String> lines = Files.lines(downloadedBallotBoxPath)) {
			confirmedVoteCount = lines.filter(line -> line.contains("encryptedOptions") && !line.contains("||")).count();
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to read the downloaded ballot box CSV file", e);
		}

		// Must have at least 1 confirmed vote.
		return confirmedVoteCount > 0;
	}

	private boolean isSignatureValid(final String electionEventId, final Path ballotBoxPath, final Path downloadedBallotBoxPath) {
		final Path electionInformationPath = pathResolver.resolveElectionInformationPath(electionEventId);
		final Path electionInformationContents = electionInformationPath.resolve(Constants.CONFIG_FILE_NAME_ELECTION_INFORMATION_CONTESTS);
		final Path ballotBoxJson = ballotBoxPath.resolve(Constants.CONFIG_DIR_NAME_BALLOTBOX_JSON);
		final ObjectMapper objectMapper = new ObjectMapper();

		// Get the signing, intermediate and root certificates.
		final byte[] signingCertificateBytes;
		final byte[] serviceCABytes;
		final byte[] rootCABytes;
		try {
			signingCertificateBytes = objectMapper.readTree(Files.readAllBytes(ballotBoxJson)).get(BALLOT_BOX_CERT_NODE).asText()
					.getBytes(StandardCharsets.UTF_8);
			serviceCABytes = objectMapper.readTree(Files.readAllBytes(electionInformationContents)).get(SERVICES_CA_NODE).asText()
					.getBytes(StandardCharsets.UTF_8);
			rootCABytes = objectMapper.readTree(Files.readAllBytes(electionInformationContents)).get(ELECTION_ROOT_CA_NODE).asText()
					.getBytes(StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read certificates.", e);
		}

		// Convert them to X509 certificates.
		final X509Certificate signingCertificate;
		final X509Certificate rootCA;
		final X509Certificate serviceCA;
		try {
			final CertificateFactory cf = CertificateFactory.getInstance("X.509");
			signingCertificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(signingCertificateBytes));
			rootCA = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(rootCABytes));
			serviceCA = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(serviceCABytes));
		} catch (CertificateException e) {
			throw new IllegalArgumentException("Failed to construct the X509 certificates.", e);
		}

		// Extract and decode the signature.
		final List<String> lines;
		try {
			lines = Files.readAllLines(downloadedBallotBoxPath);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read signature from file.", e);
		}
		byte[] signatureBase64 = lines.remove(lines.size() - 1).getBytes(StandardCharsets.UTF_8);
		byte[] signature = Base64.getDecoder().decode(signatureBase64);

		// Convert back the content without the signature.
		final String sourceString = String.join("\n", lines);
		final byte[] source = sourceString.getBytes(StandardCharsets.UTF_8);

		// Verify signature. If it is valid, verify certificate chain.
		try {
			final String algorithm = new DigitalSignerPolicyFromProperties().getDigitalSignerAlgorithmAndSpec().getAlgorithmAndPadding();

			final Signature signatureAlgorithm = Signature.getInstance(algorithm);
			signatureAlgorithm.initVerify(signingCertificate.getPublicKey());
			signatureAlgorithm.update(source);

			if (signatureAlgorithm.verify(signature)) {
				return CertificateChainValidator.isCertificateChainValid(new X509Certificate[] { signingCertificate, serviceCA }, rootCA);
			} else {
				return false;
			}
		} catch (GeneralCryptoLibException | java.security.SignatureException | InvalidKeyException | NoSuchAlgorithmException e) {
			throw new IllegalArgumentException("Failed to verify signature and validate chain.", e);
		}
	}

}
