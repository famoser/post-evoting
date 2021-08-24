/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_DIR_NAME_AUTHENTICATION;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_DIR_NAME_ELECTORAL_AUTHORITY;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_DIR_NAME_ONLINE;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_DIR_NAME_VOTINGWORKFLOW;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_FILES_BASE_DIR;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_FILE_NAME_AUTH_CONTEXT_DATA;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_FILE_NAME_AUTH_VOTER_DATA;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_FILE_NAME_ELECTION_INFORMATION_CONTESTS;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_FILE_NAME_ELECTION_PUBLIC_KEY_JSON;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_FILE_NAME_ELECTORAL_AUTHORITY_PUBLIC_KEY_JSON;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_FILE_NAME_SIGNED_AUTH_CONTEXT_DATA;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_FILE_NAME_SIGNED_AUTH_VOTER_DATA;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_FILE_NAME_SIGNED_ELECTION_INFORMATION_CONTENTS;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_FILE_NAME_SIGNED_ELECTION_PUBLIC_KEY_JSON;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_FILE_NAME_SIGNED_ELECTORAL_AUTHORITY_PUBLIC_KEY_JSON;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_FILE_NAME_VOTING_WORKFLOW_CONTEXT_DATA;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_FILE_NAME_VOTING_WORKFLOW_CONTEXT_DATA_SIGNED;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.cms.CMSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.extendedkeystore.KeyStoreService;
import ch.post.it.evoting.cryptolib.api.secretsharing.ThresholdSecretSharingServiceAPI;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.commons.serialization.JsonSignatureService;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.VerifiableElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;
import ch.post.it.evoting.cryptolib.extendedkeystore.cryptoapi.CryptoAPIExtendedKeyStore;
import ch.post.it.evoting.cryptolib.extendedkeystore.service.ExtendedKeyStoreService;
import ch.post.it.evoting.cryptolib.secretsharing.service.ThresholdSecretSharingService;
import ch.post.it.evoting.domain.election.AuthenticationContextData;
import ch.post.it.evoting.domain.election.AuthenticationVoterData;
import ch.post.it.evoting.domain.election.ElectionInformationContents;
import ch.post.it.evoting.domain.election.ElectionPublicKey;
import ch.post.it.evoting.domain.election.ElectoralAuthorityPublicKey;
import ch.post.it.evoting.domain.election.VotingWorkflowContextData;
import ch.post.it.evoting.domain.election.exceptions.LambdaException;
import ch.post.it.evoting.sdm.application.config.SmartCardConfig;
import ch.post.it.evoting.sdm.application.exception.ElectoralAuthorityServiceException;
import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.application.patch.CreateEBKeysSerializer;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.commons.domain.CreateElectoralBoardKeyPairInput;
import ch.post.it.evoting.sdm.config.shares.domain.CreateSharesOperationContext;
import ch.post.it.evoting.sdm.config.shares.domain.SharesType;
import ch.post.it.evoting.sdm.config.shares.exception.SharesException;
import ch.post.it.evoting.sdm.config.shares.handler.CreateSharesHandler;
import ch.post.it.evoting.sdm.config.shares.handler.StatelessReadSharesHandler;
import ch.post.it.evoting.sdm.config.shares.keys.PrivateKeySerializer;
import ch.post.it.evoting.sdm.config.shares.keys.elgamal.ElGamalKeyPairGenerator;
import ch.post.it.evoting.sdm.config.shares.keys.elgamal.ElGamalPrivateKeySerializer;
import ch.post.it.evoting.sdm.config.shares.keys.elgamal.ElGamalPublicKeyAdapter;
import ch.post.it.evoting.sdm.config.shares.service.PrivateKeySharesService;
import ch.post.it.evoting.sdm.config.shares.service.SmartCardService;
import ch.post.it.evoting.sdm.config.shares.service.SmartCardServiceFactory;
import ch.post.it.evoting.sdm.domain.common.SignedObject;
import ch.post.it.evoting.sdm.domain.model.administrationauthority.ActivateOutputData;
import ch.post.it.evoting.sdm.domain.model.electoralauthority.ElectoralAuthorityRepository;
import ch.post.it.evoting.sdm.domain.model.status.SmartCardStatus;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.domain.model.status.SynchronizeStatus;
import ch.post.it.evoting.sdm.domain.service.impl.CCPublicKeySignatureValidator;
import ch.post.it.evoting.sdm.domain.service.impl.ElectoralAuthorityDataGeneratorServiceImpl;
import ch.post.it.evoting.sdm.domain.service.utils.ElGamalPublicKeyCombinerWithCompression;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;
import ch.post.it.evoting.sdm.utils.ConfigObjectMapper;
import ch.post.it.evoting.sdm.utils.JsonUtils;

@Service
public class ElectoralAuthorityService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElectoralAuthorityService.class);

	private final Map<String, CreateSharesHandler> createSharesHandlerElGamalMap = new HashMap<>();

	private final CreateSharesOperationContext createSharesOperationContextElGamal = new CreateSharesOperationContext(SharesType.ELECTORAL_BOARD);

	private final PrivateKeySharesService privateKeySharesServiceElGamal = new PrivateKeySharesService(new ElGamalPrivateKeySerializer());

	@Autowired
	SmartCardConfig smartCardConfig;

	@Autowired
	private ElectoralAuthorityRepository electoralAuthorityRepository;

	@Autowired
	private ElectoralAuthorityDataGeneratorServiceImpl electoralAuthorityDataGeneratorServiceImpl;

	@Autowired
	private ConfigurationEntityStatusService statusService;

	@Autowired
	private PathResolver pathResolver;

	@Autowired
	private PlatformRootCAService platformRootCAService;

	@Autowired
	private CCPublicKeySignatureValidator keySignatureValidator;

	@Autowired
	private ControlComponentKeysAccessorService controlComponentKeysAccessorService;

	@Autowired
	private SignaturesVerifierService signaturesVerifierService;

	@Autowired
	private HashService hashService;

	private SmartCardService smartcardService;
	private CreateEBKeysSerializer createEBKeysSerializer;

	@Value("${smartcards.puk:222222}")
	private String puk;

	@Value("${tenantID}")
	private String tenantId;

	private KeyStoreService keyStoreService;
	private ElGamalServiceAPI elGamalServiceAPI;
	private StatelessReadSharesHandler statelessReadSharesHandler;
	private ConfigObjectMapper configObjectMapper;

	/**
	 * Inits the current instance.
	 */
	@PostConstruct
	public void init() {

		smartcardService = SmartCardServiceFactory.getSmartCardService(smartCardConfig.isSmartCardEnabled());

		keyStoreService = new ExtendedKeyStoreService();

		AsymmetricServiceAPI asymmetricServiceAPI = new AsymmetricService();

		elGamalServiceAPI = new ElGamalService();

		ThresholdSecretSharingServiceAPI thresholdSecretSharingServiceAPI = new ThresholdSecretSharingService();

		configObjectMapper = new ConfigObjectMapper();
		createEBKeysSerializer = new CreateEBKeysSerializer(configObjectMapper);

		PrivateKeySerializer elGamalPrivateKeySerializer = new ElGamalPrivateKeySerializer();

		statelessReadSharesHandler = new StatelessReadSharesHandler(elGamalPrivateKeySerializer, smartcardService, asymmetricServiceAPI,
				elGamalServiceAPI, thresholdSecretSharingServiceAPI);
	}

	/**
	 * Gets the smart card reader status.
	 *
	 * @return the smart card reader status
	 */
	public SmartCardStatus getSmartCardReaderStatus() {

		LOGGER.info("Checking smartCardReader status...");

		SmartCardStatus status = SmartCardStatus.EMPTY;
		if (smartcardService.isSmartcardOk()) {
			status = SmartCardStatus.INSERTED;
		}

		LOGGER.info("SmartCardReader status is {}", status);

		return status;
	}

	/**
	 * Constitute.
	 *
	 * @param electionEventId      the election event id
	 * @param electoralAuthorityId the electoral authority id
	 * @throws ResourceNotFoundException the resource not found exception
	 * @throws SharesException           the shares exception
	 * @throws GeneralCryptoLibException
	 */
	public void constitute(final String electionEventId, final String electoralAuthorityId)
			throws ResourceNotFoundException, SharesException, GeneralCryptoLibException {

		LOGGER.info("Constituting electoral authority...");

		ElGamalKeyPairGenerator elGamalKeyPairGenerator = createElGamalKeyPairGenerator(electionEventId, elGamalServiceAPI);

		CreateSharesHandler createSharesHandlerElGamal = new CreateSharesHandler(createSharesOperationContextElGamal, elGamalKeyPairGenerator,
				privateKeySharesServiceElGamal, smartcardService);

		createSharesHandlerElGamalMap.put(electoralAuthorityId, createSharesHandlerElGamal);

		// get threshold and number of members
		JsonObject electoralAuthority = getElectoralAuthorityJsonObject(electoralAuthorityId);

		int minimumThreshold = Integer.parseInt(electoralAuthority.getString(Constants.MINIMUM_THRESHOLD));

		JsonArray administrationBoardMembers = electoralAuthority.getJsonArray(Constants.ELECTORAL_BOARD_LABEL);
		int numberOfMembers = administrationBoardMembers.size();

		LOGGER.info("Threshold {}, numberOfMembers {}", minimumThreshold, numberOfMembers);

		createSharesHandlerElGamal.generateAndSplit(numberOfMembers, minimumThreshold);

		LOGGER.info("Electoral authority constituted.");
	}

	/**
	 * Change the state of the electoral authority from constituted to SIGNED for a given election event and electoral authority id.
	 *
	 * @param electionEventId      the election event id.
	 * @param electoralAuthorityId the electoral authority unique id.
	 * @param privateKeyPEM        the private key pem
	 * @return true if the status is successfully changed to signed. Otherwise, false.
	 * @throws ResourceNotFoundException if the electoral authority is not found.
	 * @throws GeneralCryptoLibException the general crypto lib exception
	 * @throws IOException               Signals that an I/O exception has occurred.
	 */
	public boolean sign(final String electionEventId, final String electoralAuthorityId, final String privateKeyPEM)
			throws ResourceNotFoundException, GeneralCryptoLibException, IOException {

		JsonObject electoralAuthorityJson = getElectoralAuthorityJsonObject(electoralAuthorityId);

		if (electoralAuthorityJson != null && electoralAuthorityJson.containsKey(JsonConstants.STATUS)) {
			String status = electoralAuthorityJson.getString(JsonConstants.STATUS);
			if (Status.READY.name().equals(status)) {

				PrivateKey privateKey = PemUtils.privateKeyFromPem(privateKeyPEM);

				LOGGER.info("Signing authentication context data");
				signAuthenticationContextData(electionEventId, privateKey);
				LOGGER.info("Signing voting workflow context data");
				signVotingWorkflowContextData(electionEventId, privateKey);

				LOGGER.info("Signing election information contents");
				signElectionInformationContents(electionEventId, privateKey);
				LOGGER.info("Signing electoral authority {}", electoralAuthorityId);
				signElectoralAuthority(electionEventId, electoralAuthorityId, privateKey);

				LOGGER.info("Changing the status of the electoral authority");
				statusService.updateWithSynchronizedStatus(Status.SIGNED.name(), electoralAuthorityId, electoralAuthorityRepository,
						SynchronizeStatus.PENDING);

				LOGGER.info("The electoral authority was successfully signed");

				return true;
			}
		}

		return false;
	}

	/**
	 * Write share.
	 *
	 * @param electionEventId      the election event id
	 * @param electoralAuthorityId the electoral authority id
	 * @param shareNumber          the share number
	 * @param pin                  the pin
	 * @throws ResourceNotFoundException the resource not found exception
	 * @throws IOException               Signals that an I/O exception has occurred.
	 * @throws GeneralCryptoLibException the general crypto lib exception
	 * @throws SharesException           the shares exception
	 */
	public void writeShare(final String electionEventId, final String electoralAuthorityId, final Integer shareNumber, final String pin)
			throws IOException, GeneralCryptoLibException, SharesException, ResourceNotFoundException {

		CreateSharesHandler createSharesHandlerElGamal = getHandler(electoralAuthorityId);

		JsonObject electoralAuthority = getElectoralAuthorityJsonObject(electoralAuthorityId);
		JsonArray electoralAuthorityMembers = electoralAuthority.getJsonArray(Constants.ELECTORAL_BOARD_LABEL);

		Runnable serializePublicKey = () -> {

			try {
				JsonArray mixingKeysJsonArray = controlComponentKeysAccessorService.downloadMixingKeys(electoralAuthorityId);

				controlComponentKeysAccessorService.writeMixingKeys(electionEventId, electoralAuthorityId, mixingKeysJsonArray);

				serializePublicKeysAndVerifyThatTheyWereWritten(electionEventId, electoralAuthorityId, createSharesHandlerElGamal);

				updateElectoralAuthorityStatus(electoralAuthorityId, electoralAuthority);

				createSharesHandlerElGamalMap.remove(electionEventId);
			} catch (ResourceNotFoundException | SharesException | IOException e) {
				throw new LambdaException(e);
			}
		};

		String member = electoralAuthorityMembers.getString(shareNumber);
		if (member == null) {
			throw new ResourceNotFoundException("Electoral board member for share number " + shareNumber + " is null.");
		}
		if (member.isEmpty()) {
			throw new ResourceNotFoundException("Electoral board member for share number " + shareNumber + " is empty.");
		}

		LOGGER.info("Writing share for shareNumber {} and member '{}'", shareNumber, member);
		String label;
		try {
			label = hashService.getHashValueForMember(member);
		} catch (HashServiceException e) {
			throw new ElectoralAuthorityServiceException("Failed to hash the electoral authority member", e);
		}
		if (label.length() > Constants.SMART_CARD_LABEL_MAX_LENGTH) {
			label = label.substring(0, Constants.SMART_CARD_LABEL_MAX_LENGTH);
		}

		try {
			createSharesHandlerElGamal.writeShare(shareNumber, label, puk, pin, getPrivateKeyToBeUsedForSigning(electionEventId), serializePublicKey);
		} catch (LambdaException | ResourceNotFoundException e) {
			LOGGER.error("Error trying to write share", e);
			Throwable cause = e.getCause();
			if (cause instanceof SharesException) {
				throw (SharesException) cause;
			} else {
				throw new ElectoralAuthorityServiceException("This lambda exception should have been properly handled", cause);
			}
		}
		String retrievedLabel = statelessReadSharesHandler.getSmartcardLabel();
		if (!retrievedLabel.equals(label)) {
			throw new IllegalStateException(
					"Label for share number " + shareNumber + " and member '" + member + "' was not correctly written. " + "Expected: '" + label
							+ "'; Found: '" + retrievedLabel + "'");
		}
		LOGGER.info("Share written.");
	}

	/**
	 * Sign electoral authority.
	 *
	 * @param electionEventId      the election event id
	 * @param electoralAuthorityId the electoral authority id
	 * @param privateKey           the private key
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void signElectoralAuthority(final String electionEventId, final String electoralAuthorityId, final PrivateKey privateKey)
			throws IOException {

		Path pathToFile = pathResolver
				.resolve(CONFIG_FILES_BASE_DIR, electionEventId, CONFIG_DIR_NAME_ONLINE, CONFIG_DIR_NAME_ELECTORAL_AUTHORITY, electoralAuthorityId);

		signFileDataJson(privateKey, pathToFile, CONFIG_FILE_NAME_ELECTION_PUBLIC_KEY_JSON, CONFIG_FILE_NAME_SIGNED_ELECTION_PUBLIC_KEY_JSON,
				ElectionPublicKey.class);

		signFileDataJson(privateKey, pathToFile, CONFIG_FILE_NAME_ELECTORAL_AUTHORITY_PUBLIC_KEY_JSON,
				CONFIG_FILE_NAME_SIGNED_ELECTORAL_AUTHORITY_PUBLIC_KEY_JSON, ElectoralAuthorityPublicKey.class);
	}

	/**
	 * Sign election information contents.
	 *
	 * @param electionEventId the election event id
	 * @param privateKey      the private key
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void signElectionInformationContents(final String electionEventId, final PrivateKey privateKey) throws IOException {

		Path pathToFile = pathResolver.resolve(CONFIG_FILES_BASE_DIR, electionEventId, CONFIG_DIR_NAME_ONLINE, CONFIG_DIR_NAME_ELECTIONINFORMATION);

		signFileDataJson(privateKey, pathToFile, CONFIG_FILE_NAME_ELECTION_INFORMATION_CONTESTS,
				CONFIG_FILE_NAME_SIGNED_ELECTION_INFORMATION_CONTENTS, ElectionInformationContents.class);
	}

	/**
	 * Sign authentication context data.
	 *
	 * @param electionEventId the election event id
	 * @param privateKey      the private key
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void signAuthenticationContextData(final String electionEventId, final PrivateKey privateKey) throws IOException {

		Path pathToFile = pathResolver.resolve(CONFIG_FILES_BASE_DIR, electionEventId, CONFIG_DIR_NAME_ONLINE, CONFIG_DIR_NAME_AUTHENTICATION);

		signFileDataJson(privateKey, pathToFile, CONFIG_FILE_NAME_AUTH_CONTEXT_DATA, CONFIG_FILE_NAME_SIGNED_AUTH_CONTEXT_DATA,
				AuthenticationContextData.class);

		signFileDataJson(privateKey, pathToFile, CONFIG_FILE_NAME_AUTH_VOTER_DATA, CONFIG_FILE_NAME_SIGNED_AUTH_VOTER_DATA,
				AuthenticationVoterData.class);
	}

	/**
	 * Sign voting workflow context data.
	 *
	 * @param electionEventId the election event id
	 * @param privateKey      the private key
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void signVotingWorkflowContextData(final String electionEventId, final PrivateKey privateKey) throws IOException {

		Path pathToFile = pathResolver.resolve(CONFIG_FILES_BASE_DIR, electionEventId, CONFIG_DIR_NAME_ONLINE, CONFIG_DIR_NAME_VOTINGWORKFLOW);

		signFileDataJson(privateKey, pathToFile, CONFIG_FILE_NAME_VOTING_WORKFLOW_CONTEXT_DATA, CONFIG_FILE_NAME_VOTING_WORKFLOW_CONTEXT_DATA_SIGNED,
				VotingWorkflowContextData.class);
	}

	/**
	 * Sign data json.
	 *
	 * @param privateKey              the private key
	 * @param pathToFile              the path to file
	 * @param fileNameToSign          the name of the file to sign
	 * @param fileNameSigned          the name of the file signed
	 * @param classJsonFileRepresents class that represents the file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private <T> void signFileDataJson(final PrivateKey privateKey, final Path pathToFile, final String fileNameToSign, final String fileNameSigned,
			final Class<T> classJsonFileRepresents) throws IOException {

		Path fileToSignPath = pathResolver.resolve(pathToFile.toString(), fileNameToSign);
		Path fileSignedPath = pathResolver.resolve(pathToFile.toString(), fileNameSigned);

		if (doesNotExist(fileSignedPath)) {
			T objectToSign = configObjectMapper.fromJSONFileToJava(new File(fileToSignPath.toString()), classJsonFileRepresents);

			String signedData = JsonSignatureService.sign(privateKey, objectToSign);
			SignedObject signedDataObject = new SignedObject();
			signedDataObject.setSignature(signedData);
			configObjectMapper.fromJavaToJSONFile(signedDataObject, new File(fileSignedPath.toString()));
		}
	}

	/**
	 * Does not exist.
	 *
	 * @param signedFilePath the signed file data path
	 * @return true, if successful
	 */
	private boolean doesNotExist(final Path signedFilePath) {
		return !signedFilePath.toFile().exists();
	}

	/**
	 * Gets the handler.
	 *
	 * @param electoralAuthorityId the electoral authority id
	 * @return the handler
	 * @throws ResourceNotFoundException the resource not found exception
	 */
	private CreateSharesHandler getHandler(final String electoralAuthorityId) throws ResourceNotFoundException {

		CreateSharesHandler createSharesHandlerElGamal = createSharesHandlerElGamalMap.get(electoralAuthorityId);
		if (createSharesHandlerElGamal == null) {
			throw new ResourceNotFoundException("CreateSharesHandler for this electoralAuthorityId '" + electoralAuthorityId + "' not found");
		}
		return createSharesHandlerElGamal;
	}

	/**
	 * Update electoral authority status.
	 *
	 * @param electoralAuthorityId the electoral authority id
	 * @param electoralAuthority   the electoral authority
	 */
	private void updateElectoralAuthorityStatus(final String electoralAuthorityId, final JsonObject electoralAuthority) {

		String status = electoralAuthority.getString("status");
		if (Status.LOCKED.name().equals(status)) {
			statusService.update(Status.READY.name(), electoralAuthorityId, electoralAuthorityRepository);
		}
	}

	/**
	 * Serialize the combined public keys and verify that something was written.
	 *
	 * @param electionEventId      the election event id
	 * @param electoralAuthorityId the electoral authority id
	 * @param createSharesHandler  the create shares handler el gamal
	 * @throws SharesException the shares exception
	 */
	private void serializePublicKeysAndVerifyThatTheyWereWritten(final String electionEventId, final String electoralAuthorityId,
			final CreateSharesHandler createSharesHandler) throws SharesException, ResourceNotFoundException {

		CreateElectoralBoardKeyPairInput createEbKeyPairInput = electoralAuthorityDataGeneratorServiceImpl
				.generate(electoralAuthorityId, electionEventId);
		Path outputFolder = pathResolver.resolve(createEbKeyPairInput.getOutputFolder());

		final ElGamalPublicKey electoralAuthorityPublicKey = getElectoralAuthorityPublicKey(createSharesHandler);

		JsonArray mixingKeysJsonArray = controlComponentKeysAccessorService.downloadMixingKeys(electoralAuthorityId);

		final List<ElGamalPublicKey> mixingPublicKeys = getMixingElGamalPublicKeys(electionEventId, electoralAuthorityId, mixingKeysJsonArray);

		ElGamalPublicKey electionPublicKey = combineUsingCompression(electoralAuthorityPublicKey, mixingPublicKeys);

		boolean areElectoralAuthorityKeysSerialized = createEBKeysSerializer
				.serializeElectionPublicKeys(outputFolder, electoralAuthorityId, electionPublicKey, electoralAuthorityPublicKey);
		if (!areElectoralAuthorityKeysSerialized) {
			throw new IllegalStateException(
					"The serialization of the Electoral Authority public keys failed. They might not be written to file. Stopping the process.");
		}

	}

	public ElGamalPublicKey combineUsingCompression(ElGamalPublicKey electoralAuthorityPublicKey, List<ElGamalPublicKey> mixingPublicKeys) {

		ElGamalPublicKey combinedPublicKey;
		try {
			combinedPublicKey = new ElGamalPublicKeyCombinerWithCompression().combine(electoralAuthorityPublicKey, mixingPublicKeys);
		} catch (GeneralCryptoLibException e) {
			throw new ElectoralAuthorityServiceException("Exception when trying to combine public keys: " + e.getMessage(), e);
		}

		return combinedPublicKey;
	}

	private List<ElGamalPublicKey> getMixingElGamalPublicKeys(String electionEventId, String electoralAuthorityId, JsonArray mixingKeysJsonArray) {
		List<ElGamalPublicKey> mixingPublicKeys = new ArrayList<>();
		try {
			X509Certificate rootCACertificate = platformRootCAService.load();
			for (JsonObject jsonObject : jsonArrayToJsonObjects(mixingKeysJsonArray)) {
				ElGamalPublicKey publicKey = ElGamalPublicKey.fromJson(jsonObject.get("publicKey").toString());
				byte[] signature = Base64.getDecoder().decode(jsonObject.getString("signature"));
				X509Certificate signingCertificate = (X509Certificate) PemUtils.certificateFromPem(jsonObject.getString("signerCertificate"));
				X509Certificate nodeCACertificate = (X509Certificate) PemUtils.certificateFromPem(jsonObject.getString("nodeCACertificate"));
				X509Certificate[] chain = { signingCertificate, nodeCACertificate, rootCACertificate };
				keySignatureValidator.checkMixingKeySignature(signature, chain, publicKey, electionEventId, electoralAuthorityId);
				mixingPublicKeys.add(publicKey);
			}
		} catch (SignatureException | GeneralCryptoLibException | CertificateManagementException e) {
			throw new IllegalStateException("Failed to get mixing ElGamal public keys", e);
		}
		return mixingPublicKeys;
	}

	private List<JsonObject> jsonArrayToJsonObjects(JsonArray array) {
		List<JsonObject> jsonObjects = new ArrayList<>(array.size());
		for (int i = 0; i < array.size(); i++) {
			jsonObjects.add(array.getJsonObject(i));
		}
		return jsonObjects;
	}

	/**
	 * Creates the encryption parameters generator.
	 *
	 * @param electionEventId the election event id.
	 * @return the ElGamalKeyPairGenerator.
	 * @throws ResourceNotFoundException if the encryption parameters can not be obtained.
	 * @throws GeneralCryptoLibException if the validation of encrpytion parameters fails.
	 */
	private ElGamalKeyPairGenerator createElGamalKeyPairGenerator(final String electionEventId, final ElGamalServiceAPI service)
			throws ResourceNotFoundException, GeneralCryptoLibException {

		final ElGamalEncryptionParameters encryptionParameters;
		try {
			encryptionParameters = getEncryptionParameters(electionEventId);
		} catch (GeneralCryptoLibException | IOException e) {
			throw new ResourceNotFoundException("Failed to obtain encryption parameters", e);
		} catch (CertificateException | CMSException e) {
			throw new GeneralCryptoLibException("Failed to validate encryption parameters", e);
		}

		// Enforce to 1 since we currently do not support write-ins.
		final int numberOfElectionPublicKeyElements = 1;

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Encryption parameters: {} with number of key elements: {}", encryptionParameters.toJson(),
					numberOfElectionPublicKeyElements);
		}

		return new ElGamalKeyPairGenerator(encryptionParameters, numberOfElectionPublicKeyElements, service);
	}

	/**
	 * Gets the electoral authority public key.
	 *
	 * @param createSharesHandler the create shares handler
	 * @return the electoral authority public key
	 * @throws SharesException the shares exception
	 */
	private ElGamalPublicKey getElectoralAuthorityPublicKey(final CreateSharesHandler createSharesHandler) throws SharesException {

		PublicKey publicKey = createSharesHandler.getPublicKey();
		if (!(publicKey instanceof ElGamalPublicKeyAdapter)) {
			throw new ElectoralAuthorityServiceException("Error while trying to obtain the electoral authority public key",
					new IllegalStateException());
		}
		return ((ElGamalPublicKeyAdapter) publicKey).getPublicKey();
	}

	/**
	 * Gets the private key to be used for signing.
	 *
	 * @param electionEventId the electionEventId
	 * @return the private key to be used for signing
	 * @throws IOException               Signals that an I/O exception has occurred.
	 * @throws GeneralCryptoLibException the general crypto lib exception
	 * @throws ResourceNotFoundException the resource not found exception
	 */
	private PrivateKey getPrivateKeyToBeUsedForSigning(final String electionEventId)
			throws IOException, GeneralCryptoLibException, ResourceNotFoundException {

		Path path = pathResolver.resolve(CONFIG_FILES_BASE_DIR);
		String authoritiesCAtag = "authoritiesca";
		String alias = "privatekey";

		return getAuthoritiesCAPrivateKey(path, electionEventId, authoritiesCAtag, alias);
	}

	/**
	 * Gets the authorities ca private key.
	 *
	 * @param absolutePath     the absolute path
	 * @param electionEventId  the electionEventId
	 * @param authoritiesCAtag the authorities CA tag
	 * @param alias            the alias
	 * @return the authorities ca private key
	 * @throws IOException               Signals that an I/O exception has occurred.
	 * @throws GeneralCryptoLibException the general crypto lib exception
	 * @throws ResourceNotFoundException the resource not found exception
	 */
	private PrivateKey getAuthoritiesCAPrivateKey(final Path absolutePath, final String electionEventId, final String authoritiesCAtag,
			final String alias) throws IOException, GeneralCryptoLibException, ResourceNotFoundException {

		final Path authoritiesCaKeyStorePath = pathResolver
				.resolve(absolutePath.toString(), electionEventId, Constants.CONFIG_DIR_NAME_OFFLINE, "authoritiesca.sks");
		final CryptoAPIExtendedKeyStore keyStore;
		try (final InputStream in = new FileInputStream(authoritiesCaKeyStorePath.toFile())) {
			final char[] password = getPassword(
					pathResolver.resolve(absolutePath.toString(), electionEventId, Constants.CONFIG_DIR_NAME_OFFLINE, Constants.PW_TXT),
					authoritiesCAtag);
			keyStore = keyStoreService.loadKeyStore(in, new KeyStore.PasswordProtection(password));
			return keyStore.getPrivateKeyEntry(alias, password);
		}
	}

	/**
	 * Gets the password.
	 *
	 * @param path the path
	 * @param name the name
	 * @return the password
	 * @throws IOException               Signals that an I/O exception has occurred.
	 * @throws ResourceNotFoundException the resource not found exception
	 */
	private char[] getPassword(final Path path, final String name) throws IOException, ResourceNotFoundException {

		final List<String> lines = Files.readAllLines(path);
		String password = null;

		for (final String line : lines) {
			final String[] splittedLine = line.split(",");

			if (splittedLine[0].equals(name)) {
				password = splittedLine[1];
			}
		}

		if (password == null) {
			throw new ResourceNotFoundException("The passwords file does not contain a password for " + name);
		}

		return password.toCharArray();
	}

	/**
	 * Gets the electoral authority json object.
	 *
	 * @param electoralAuthorityId the electoral authority id
	 * @return the electoral authority json object
	 * @throws ResourceNotFoundException the resource not found exception
	 */
	private JsonObject getElectoralAuthorityJsonObject(final String electoralAuthorityId) throws ResourceNotFoundException {

		String electoralAuthorityJSON = electoralAuthorityRepository.find(electoralAuthorityId);

		if (StringUtils.isEmpty(electoralAuthorityJSON) || JsonConstants.EMPTY_OBJECT.equals(electoralAuthorityJSON)) {
			throw new ResourceNotFoundException("Electoral Authority not found");
		}

		return JsonUtils.getJsonObject(electoralAuthorityJSON);
	}

	/**
	 * Gets the encryption parameters.
	 *
	 * @param electionEventId the election event id
	 * @return the encryption parameters
	 * @throws GeneralCryptoLibException the general crypto lib exception
	 * @throws IOException
	 * @throws CertificateException
	 */
	private ElGamalEncryptionParameters getEncryptionParameters(final String electionEventId)
			throws GeneralCryptoLibException, CertificateException, IOException, CMSException {

		VerifiableElGamalEncryptionParameters verifiedParams = signaturesVerifierService.verifyEncryptionParams(electionEventId);

		return new ElGamalEncryptionParameters(verifiedParams.getGroup().getP(), verifiedParams.getGroup().getQ(), verifiedParams.getGroup().getG());
	}

	/**
	 * Activate.
	 *
	 * @param electionEventId      the election event id
	 * @param electoralAuthorityId the electoral authority id
	 * @return the activate output data
	 * @throws GeneralCryptoLibException the general crypto lib exception
	 */
	public ActivateOutputData activate(final String electionEventId, final String electoralAuthorityId) throws GeneralCryptoLibException {

		LOGGER.info("Loading the authorities CA public key...");
		PublicKey publicKey = loadAuthoritiesCAPublicKey(electionEventId);
		LOGGER.info("Authorities CA public key successfully loaded");
		String issuerPublicKeyPEM = PemUtils.publicKeyToPem(publicKey);

		LOGGER.info("Loading the electoral authority {} public key...", electoralAuthorityId);
		ElGamalPublicKey elGamalPublicKey = loadElectoralAuthorityPublicKey(electionEventId, electoralAuthorityId);
		String elGamalPublicKeyB64 = new String(Base64.getEncoder().encode(elGamalPublicKey.toJson().getBytes(StandardCharsets.UTF_8)),
				StandardCharsets.UTF_8);
		LOGGER.info("Authorities CA public key successfully loaded");

		ActivateOutputData output = new ActivateOutputData();
		output.setIssuerPublicKeyPEM(issuerPublicKeyPEM);
		output.setSerializedSubjectPublicKey(elGamalPublicKeyB64);
		return output;
	}

	/**
	 * Read share.
	 *
	 * @param electionEventId      the election event id
	 * @param electoralAuthorityId the electoral authority id
	 * @param shareNumber          the share number
	 * @param pin                  the pin
	 * @param issuerPublicKeyPEM   the issuer public key pem
	 * @return the string
	 * @throws ResourceNotFoundException the resource not found exception
	 * @throws SharesException           the shares exception
	 * @throws IllegalArgumentException  the illegal argument exception
	 */
	public String readShare(final String electionEventId, final String electoralAuthorityId, final Integer shareNumber, final String pin,
			final String issuerPublicKeyPEM) throws ResourceNotFoundException, SharesException {

		String electoralAuthorityMember = getElectoralAuthorityMember(electoralAuthorityId, shareNumber);

		LOGGER.info("Reading share of election event {} for electoral authority {} and member {}...", electionEventId, electoralAuthorityId,
				electoralAuthorityMember);

		LOGGER.info("Checking that the smartcard corresponds to member {}...", electoralAuthorityMember);
		String label;
		try {
			label = hashService.getHashValueForMember(electoralAuthorityMember);
		} catch (HashServiceException e) {
			throw new ElectoralAuthorityServiceException("Failed to hash electoral authority member", e);
		}
		if (label.length() > Constants.SMART_CARD_LABEL_MAX_LENGTH) {
			label = label.substring(0, Constants.SMART_CARD_LABEL_MAX_LENGTH);
		}
		if (!statelessReadSharesHandler.getSmartcardLabel().equals(label)) {
			throw new ElectoralAuthorityServiceException(
					"The smartcard introduced does not correspond to the selected member: " + electoralAuthorityMember);
		}

		PublicKey authoritiesCAPublicKey;
		try {
			authoritiesCAPublicKey = PemUtils.publicKeyFromPem(issuerPublicKeyPEM);
		} catch (GeneralCryptoLibException e) {
			throw new IllegalArgumentException("Public key is not in valid PEM format", e);
		}

		LOGGER.info("Reading share from smartcard...");
		String shareSerialized = statelessReadSharesHandler.readShareAndStringifyElGamal(pin, authoritiesCAPublicKey);
		LOGGER.info("Share successfully read");

		return shareSerialized;
	}

	/**
	 * Gets the electoral authority member.
	 *
	 * @param electoralAuthorityId the electoral authority id
	 * @param shareNumber          the share number
	 * @return the electoral authority member
	 * @throws ResourceNotFoundException the resource not found exception
	 */
	private String getElectoralAuthorityMember(final String electoralAuthorityId, final Integer shareNumber) throws ResourceNotFoundException {
		JsonObject electoralAuthority = getElectoralAuthorityJsonObject(electoralAuthorityId);
		JsonArray electoralAuthorityMembers = electoralAuthority.getJsonArray(Constants.ELECTORAL_BOARD_LABEL);
		return electoralAuthorityMembers.getString(shareNumber);
	}

	/**
	 * Reconstruct.
	 *
	 * @param electoralAuthorityId the electoral authority id
	 * @param serializedShares     the serialized shares
	 * @param serializedPublicKey  the serialized public key
	 * @return the string
	 * @throws SharesException           the shares exception
	 * @throws GeneralCryptoLibException the general crypto lib exception
	 */
	public String reconstruct(final String electoralAuthorityId, final List<String> serializedShares, final String serializedPublicKey)
			throws SharesException, GeneralCryptoLibException {

		ElGamalPublicKey elGamalPublicKey = ElGamalPublicKey
				.fromJson(new String(Base64.getDecoder().decode(serializedPublicKey), StandardCharsets.UTF_8));

		LOGGER.info("Reconstructing private key of electoral authority {}...", electoralAuthorityId);
		ElGamalPrivateKey elGamalPrivateKey = statelessReadSharesHandler
				.getPrivateKeyWithSerializedSharesElGamal(new HashSet<>(serializedShares), elGamalPublicKey);

		String elGamalPrivateKeyB64 = new String(Base64.getEncoder().encode(elGamalPrivateKey.toJson().getBytes(StandardCharsets.UTF_8)),
				StandardCharsets.UTF_8);
		LOGGER.info("Private key successfully reconstructed");

		return elGamalPrivateKeyB64;
	}

	/**
	 * Load Electoral Authority public key.
	 *
	 * @param electionEventId      the electionEventId
	 * @param electoralAuthorityId the electoral authority id
	 * @return the el gamal public key
	 */
	private ElGamalPublicKey loadElectoralAuthorityPublicKey(final String electionEventId, final String electoralAuthorityId) {

		Path electoralAuthorityPubKeyPath = pathResolver
				.resolve(CONFIG_FILES_BASE_DIR, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE, Constants.CONFIG_DIR_NAME_ELECTORAL_AUTHORITY,
						electoralAuthorityId, CONFIG_FILE_NAME_ELECTORAL_AUTHORITY_PUBLIC_KEY_JSON);

		try {
			ElectoralAuthorityPublicKey electoralAuthorityPublicKey = configObjectMapper
					.fromJSONFileToJava(electoralAuthorityPubKeyPath.toFile(), ElectoralAuthorityPublicKey.class);

			String serializedPublicKey = electoralAuthorityPublicKey.getPublicKey();

			return ElGamalPublicKey.fromJson(new String(Base64.getDecoder().decode(serializedPublicKey), StandardCharsets.UTF_8));

		} catch (IOException | GeneralCryptoLibException e) {
			throw new ElectoralAuthorityServiceException("An error occurred while loading the electoral authority public key " + electoralAuthorityId,
					e);
		}
	}

	/**
	 * Load authorities ca public key.
	 *
	 * @param electionEventId the electionEventId
	 * @return the public key
	 */
	private PublicKey loadAuthoritiesCAPublicKey(final String electionEventId) {

		Path authoritiesCAPath = pathResolver
				.resolve(CONFIG_FILES_BASE_DIR, electionEventId, Constants.CONFIG_DIR_NAME_OFFLINE, "authoritiesca" + Constants.PEM);

		try {
			X509Certificate authoritiesCA = (X509Certificate) PemUtils
					.certificateFromPem(new String(Files.readAllBytes(authoritiesCAPath), StandardCharsets.UTF_8));
			return authoritiesCA.getPublicKey();
		} catch (GeneralCryptoLibException | IOException e) {
			throw new ElectoralAuthorityServiceException("An error occurred while loading the authorities CA public key", e);
		}

	}
}
