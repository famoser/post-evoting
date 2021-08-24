/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
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
import ch.post.it.evoting.cryptolib.certificates.bean.CSRSigningInputProperties;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateParameters;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.csr.BouncyCastleCertificateRequestSigner;
import ch.post.it.evoting.cryptolib.certificates.csr.CSRGenerator;
import ch.post.it.evoting.cryptolib.certificates.csr.CertificateRequestSigner;
import ch.post.it.evoting.cryptolib.certificates.factory.CryptoX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;
import ch.post.it.evoting.cryptolib.extendedkeystore.cryptoapi.CryptoAPIExtendedKeyStore;
import ch.post.it.evoting.cryptolib.extendedkeystore.service.ExtendedKeyStoreService;
import ch.post.it.evoting.cryptolib.secretsharing.service.ThresholdSecretSharingService;
import ch.post.it.evoting.domain.election.exceptions.LambdaException;
import ch.post.it.evoting.sdm.application.config.SmartCardConfig;
import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.config.exceptions.ConfigurationEngineException;
import ch.post.it.evoting.sdm.config.shares.domain.CreateSharesOperationContext;
import ch.post.it.evoting.sdm.config.shares.domain.SharesType;
import ch.post.it.evoting.sdm.config.shares.exception.SharesException;
import ch.post.it.evoting.sdm.config.shares.handler.CreateSharesHandler;
import ch.post.it.evoting.sdm.config.shares.handler.StatelessReadSharesHandler;
import ch.post.it.evoting.sdm.config.shares.keys.PrivateKeySerializer;
import ch.post.it.evoting.sdm.config.shares.keys.rsa.RSAKeyPairGenerator;
import ch.post.it.evoting.sdm.config.shares.keys.rsa.RSAPrivateKeySerializer;
import ch.post.it.evoting.sdm.config.shares.service.PrivateKeySharesService;
import ch.post.it.evoting.sdm.config.shares.service.SmartCardService;
import ch.post.it.evoting.sdm.config.shares.service.SmartCardServiceFactory;
import ch.post.it.evoting.sdm.domain.model.administrationauthority.ActivateOutputData;
import ch.post.it.evoting.sdm.domain.model.administrationauthority.AdministrationAuthorityRepository;
import ch.post.it.evoting.sdm.domain.model.status.SmartCardStatus;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.domain.model.status.SynchronizeStatus;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * Service for handling admin boards.
 */
@Service
public class AdminBoardService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AdminBoardService.class);

	private static final String PRIVATE_KEY_ALIAS = "privatekey";

	private final Map<String, CreateSharesHandler> createSharesHandlerRSAMap = new HashMap<>();

	private final PrivateKeySharesService privateKeySharesServiceRSA = new PrivateKeySharesService(new RSAPrivateKeySerializer());

	private final CreateSharesOperationContext createSharesOperationContextRSA = new CreateSharesOperationContext(SharesType.ADMIN_BOARD);

	private final CSRGenerator csrGenerator = new CSRGenerator();
	@Autowired
	SmartCardConfig smartCardConfig;

	private RSAKeyPairGenerator rsaKeyPairGenerator;

	@Autowired
	private AdministrationAuthorityRepository administrationAuthorityRepository;
	@Autowired
	private ConfigurationEntityStatusService statusService;
	@Autowired
	private PathResolver pathResolver;
	@Autowired
	private RootCAService platformRootCAService;
	@Autowired
	private RootCAService tenantCAService;
	@Autowired
	private HashService hashService;
	@Value("${smartcards.puk:222222}")
	private String puk;
	@Value("${tenantID}")
	private String tenantId;

	private SmartCardService smartcardService;

	private PrivateKeyEntry tenantKeys;

	private StatelessReadSharesHandler statelessReadSharesHandler;
	private KeyStoreService keyStoreService;

	/**
	 * Inits the current instance.
	 *
	 * @throws GeneralCryptoLibException if there was a problem when trying to create an AsymmetricService.
	 */
	@PostConstruct
	public void init() throws GeneralCryptoLibException {

		AsymmetricServiceAPI asymmetricServiceAPI = new AsymmetricService();
		ElGamalServiceAPI elGamalServiceAPI = new ElGamalService();
		ThresholdSecretSharingServiceAPI thresholdSecretSharingServiceAPI = new ThresholdSecretSharingService();

		rsaKeyPairGenerator = new RSAKeyPairGenerator(asymmetricServiceAPI);

		smartcardService = SmartCardServiceFactory.getSmartCardService(smartCardConfig.isSmartCardEnabled());

		PrivateKeySerializer rsaPrivateKeySerializer = new RSAPrivateKeySerializer();

		statelessReadSharesHandler = new StatelessReadSharesHandler(rsaPrivateKeySerializer, smartcardService, asymmetricServiceAPI,
				elGamalServiceAPI, thresholdSecretSharingServiceAPI);

		keyStoreService = new ExtendedKeyStoreService();
	}

	/**
	 * Gets the admin boards.
	 *
	 * @return the admin boards
	 */
	public String getAdminBoards() {
		return administrationAuthorityRepository.list();
	}

	/**
	 * Constitute.
	 *
	 * @param adminBoardId the admin board id
	 * @param in           the keystore data
	 * @param password     the keystore password
	 * @throws ResourceNotFoundException the resource not found exception
	 * @throws SharesException           the shares exception
	 */
	public void constitute(final String adminBoardId, final InputStream in, char[] password) throws ResourceNotFoundException, SharesException {

		LOGGER.info("Constituting adminBoard...");

		tenantKeys = extractTenantKeys(in, password);

		LOGGER.info("Extracted contents from tenant keystore.");

		CreateSharesHandler createSharesHandlerRSA = new CreateSharesHandler(createSharesOperationContextRSA, rsaKeyPairGenerator,
				privateKeySharesServiceRSA, smartcardService);

		createSharesHandlerRSAMap.put(adminBoardId, createSharesHandlerRSA);

		JsonObject administrationAuthority = getAdminBoardJsonObject(adminBoardId);
		int minimumThreshold = Integer.parseInt(administrationAuthority.getString(Constants.MINIMUM_THRESHOLD));
		JsonArray administrationBoardMembers = administrationAuthority.getJsonArray(Constants.ADMINISTRATION_BOARD_LABEL);
		int numberOfMembers = administrationBoardMembers.size();

		LOGGER.info("Threshold {}, numberOfMembers {}", minimumThreshold, numberOfMembers);

		createSharesHandlerRSA.generateAndSplit(numberOfMembers, minimumThreshold);

		LOGGER.info("AdminBoard constituted.");
	}

	/**
	 * Gets the smart card reader status.
	 *
	 * @return the smart card reader status
	 */
	public SmartCardStatus getSmartCardReaderStatus() {

		LOGGER.debug("Checking smartCardReader status...");

		SmartCardStatus status = SmartCardStatus.EMPTY;
		if (smartcardService.isSmartcardOk()) {
			status = SmartCardStatus.INSERTED;
		}

		LOGGER.debug("SmartCardReader status is {}", status);

		return status;
	}

	/**
	 * Write share.
	 *
	 * @param adminBoardId the admin board id
	 * @param shareNumber  the share number
	 * @param pin          the pin
	 * @throws ResourceNotFoundException the resource not found exception
	 * @throws SharesException           the shares exception
	 * @throws IOException               Signals that an I/O exception has occurred.
	 */
	public void writeShare(final String adminBoardId, final Integer shareNumber, final String pin)
			throws ResourceNotFoundException, SharesException, IOException {

		CreateSharesHandler createSharesHandlerRSA = createSharesHandlerRSAMap.get(adminBoardId);
		if (createSharesHandlerRSA == null) {
			throw new ResourceNotFoundException("CreateSharesHandler for this adminBoardID '" + adminBoardId + "' not found");
		}

		JsonObject administrationAuthority = getAdminBoardJsonObject(adminBoardId);

		Runnable createAdminBoardCertificate = () -> {
			try {

				createAdminBoardCertificate(adminBoardId, createSharesHandlerRSA, administrationAuthority);
				createSharesHandlerRSAMap.remove(adminBoardId);
			} catch (IOException | SharesException | CertificateManagementException e) {
				throw new LambdaException(e);
			}
		};

		String member = getMemberFromRepository(administrationAuthority, shareNumber);
		LOGGER.info("Writing share for share number {} and member '{}'", shareNumber, member);
		String label;
		try {
			label = hashService.getHashValueForMember(member);
		} catch (HashServiceException e) {
			throw new AdminBoardServiceException("Failed to get hash value for admin board member", e);
		}
		if (label.length() > Constants.SMART_CARD_LABEL_MAX_LENGTH) {
			label = label.substring(0, Constants.SMART_CARD_LABEL_MAX_LENGTH);
		}
		try {
			createSharesHandlerRSA.writeShareAndSelfSign(shareNumber, label, puk, pin, createAdminBoardCertificate);
		} catch (LambdaException e) {
			Exception cause = e.getCause();
			if (cause instanceof IOException) {
				throw (IOException) cause;
			} else if (cause instanceof SharesException) {
				throw (SharesException) cause;
			} else {
				throw new AdminBoardServiceException("This lambda exception should have been properly handled", cause);
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
	 * Creates the admin board certificate.
	 *
	 * @param adminBoardId            the admin board id
	 * @param createSharesHandlerRSA  the create shares handler rsa
	 * @param administrationAuthority the administration authority
	 * @throws IOException                    Signals that an I/O exception has occurred.
	 * @throws SharesException                the shares exception
	 * @throws CertificateManagementException
	 */
	public void createAdminBoardCertificate(final String adminBoardId, final CreateSharesHandler createSharesHandlerRSA,
			final JsonObject administrationAuthority) throws IOException, SharesException, CertificateManagementException {
		String status = administrationAuthority.getString("status");
		if (Status.LOCKED.name().equals(status)) {
			serializeDataToFile(adminBoardId, createSharesHandlerRSA);
			statusService.updateWithSynchronizedStatus(Status.CONSTITUTED.name(), adminBoardId, administrationAuthorityRepository,
					SynchronizeStatus.PENDING);
		} else {
			throw new AdminBoardServiceException("Invalid board status");
		}
	}

	/**
	 * Extracts the tenant keys from a given keystore.
	 *
	 * @param in       the keystore input stream
	 * @param password the keystore password
	 * @return the keys.
	 */
	private PrivateKeyEntry extractTenantKeys(final InputStream in, char[] password) {
		PrivateKey key;
		Certificate[] chain;
		try {
			CryptoAPIExtendedKeyStore store = keyStoreService.loadKeyStore(in, password);
			key = store.getPrivateKeyEntry(PRIVATE_KEY_ALIAS, password);
			chain = store.getCertificateChain(PRIVATE_KEY_ALIAS);
		} catch (GeneralCryptoLibException e) {
			throw new IllegalStateException("Failed to extract the tenant keys.", e);
		}
		return new PrivateKeyEntry(key, chain);
	}

	/**
	 * Gets an administration board's certificate, and its corresponding tenant certificate.
	 *
	 * @param adminBoardId the identifier of the administration board
	 * @return the admin board certificate chain, wit the admin board certificate as the first certificate
	 * @throws CertificateManagementException
	 */
	public X509Certificate[] getCertificateChain(String adminBoardId) throws CertificateManagementException {
		X509Certificate adminBoardCertificate = getAdminBoardCertificate(adminBoardId);
		X509Certificate tenantCACertificate = tenantCAService.load();

		return new X509Certificate[] { adminBoardCertificate, tenantCACertificate };
	}

	/**
	 * Serialize data to file.
	 *
	 * @param adminBoardId           the admin board id
	 * @param createSharesHandlerRSA the create shares handler rsa
	 * @throws IOException                    Signals that an I/O exception has occurred.
	 * @throws SharesException                the shares exception
	 * @throws CertificateManagementException
	 */
	private void serializeDataToFile(final String adminBoardId, final CreateSharesHandler createSharesHandlerRSA)
			throws IOException, SharesException, CertificateManagementException {

		LOGGER.info("Serializing data to file");

		X500Principal subjectX500Principal = getSubjectX500Principal(adminBoardId);

		PKCS10CertificationRequest csr = createSharesHandlerRSA.generateCSR(subjectX500Principal, csrGenerator);

		LOGGER.info("Created CSR");

		Path outputFilePath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, Constants.CSR_FOLDER);

		LOGGER.info("Path: {}", outputFilePath.toAbsolutePath());

		Files.createDirectories(outputFilePath);

		saveCsrToFile(adminBoardId, csr, outputFilePath);
		LOGGER.info("Saved CSR");

		saveSignedCertificateToFile(adminBoardId, csr, outputFilePath);
		LOGGER.info("Saved signed certificate");

		Certificate[] certificateChain = tenantKeys.getCertificateChain();
		if (certificateChain.length < 2) {
			throw new IllegalStateException("Platform CA certificate may be missing");
		}

		saveTenantCACertificate(certificateChain);
		LOGGER.info("Saved tenant CA certificate");

		savePlatformRootCACertificate(certificateChain);
		LOGGER.info("Saved PlatformRoot CA certificate");
	}

	private void saveTenantCACertificate(Certificate[] certificateChain) throws CertificateManagementException {
		if (certificateChain.length < 1) {
			throw new IllegalArgumentException("No certificates in this chain");
		}
		X509Certificate tenantCACertificate = (X509Certificate) certificateChain[0];
		tenantCAService.save(tenantCACertificate);
	}

	private void savePlatformRootCACertificate(Certificate[] certificateChain) throws CertificateManagementException {
		if (certificateChain.length < 2) {
			throw new IllegalArgumentException("The platform root CA certificate may be missing");
		}
		X509Certificate platformCACertificate = (X509Certificate) certificateChain[1];
		platformRootCAService.save(platformCACertificate);
	}

	/**
	 * Gets the subject x500 principal.
	 *
	 * @param adminBoardId the admin board id
	 * @return the subject x500 principal
	 */
	private X500Principal getSubjectX500Principal(final String adminBoardId) {

		X509DistinguishedName x509DistinguishedName = getX509DistinguishedName((X509Certificate) tenantKeys.getCertificate());

		String subjectCommonName = "AdministrationBoard " + adminBoardId;
		String subjectOrganizationalUnit = x509DistinguishedName.getOrganizationalUnit();
		String subjectOrganization = x509DistinguishedName.getOrganization();
		String subjectCountry = x509DistinguishedName.getCountry();

		return new X500Principal(
				"C=" + subjectCountry + ", O=" + subjectOrganization + ", OU=" + subjectOrganizationalUnit + ", CN=" + subjectCommonName);
	}

	/**
	 * Gets the x509 distinguished name.
	 *
	 * @param x509Certificate the x509 certificate
	 * @return the x509 distinguished name
	 */
	private X509DistinguishedName getX509DistinguishedName(final X509Certificate x509Certificate) {

		CryptoAPIX509Certificate cryptoAPIX509Certificate = getCryptoAPIX509CertificateFromX509Certificate(x509Certificate);

		return cryptoAPIX509Certificate.getSubjectDn();
	}

	/**
	 * Save csr to file.
	 *
	 * @param adminBoardId   the admin board id
	 * @param csr            the csr
	 * @param outputFilePath the output file path
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void saveCsrToFile(final String adminBoardId, final PKCS10CertificationRequest csr, final Path outputFilePath) throws IOException {

		if (!outputFilePath.toFile().exists()) {
			throw new AdminBoardServiceException("Error while creating the CSR folder structure");
		}
		Path csrPath = pathResolver.resolve(outputFilePath.toString(), adminBoardId + "_CSR.pem");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(csrPath.toFile()))) {
			String data = PemUtils.certificateSigningRequestToPem(csr);
			writer.write(data);
		} catch (GeneralCryptoLibException e) {
			throw new AdminBoardServiceException("Error while creating the CSR folder structure", e);
		}
	}

	/**
	 * Save signed certificate to file.
	 *
	 * @param adminBoardId   the admin board id
	 * @param csr            the csr
	 * @param outputFilePath the output file path
	 */
	private void saveSignedCertificateToFile(final String adminBoardId, final PKCS10CertificationRequest csr, final Path outputFilePath) {

		X509Certificate x509CertificateFromKeystore = (X509Certificate) tenantKeys.getCertificate();
		CryptoAPIX509Certificate cryptoAPIX509Certificate = getCryptoAPIX509CertificateFromX509Certificate(x509CertificateFromKeystore);

		CSRSigningInputProperties csrSingingInputProperties = constructCsrSigningInputProperties(cryptoAPIX509Certificate);

		JcaPKCS10CertificationRequest jcaPKCS10CertificationRequest = new JcaPKCS10CertificationRequest(csr);

		CryptoAPIX509Certificate signedCertificate;
		try {
			CertificateRequestSigner certificateRequestSigner = new BouncyCastleCertificateRequestSigner();
			signedCertificate = certificateRequestSigner
					.signCSR(x509CertificateFromKeystore, tenantKeys.getPrivateKey(), jcaPKCS10CertificationRequest, csrSingingInputProperties);
		} catch (GeneralCryptoLibException e) {
			throw new AdminBoardServiceException("Could not sign CSR", e);
		}

		LOGGER.info("Created signed certificate");

		Path signedCertificatePath = pathResolver.resolve(outputFilePath.toString(), adminBoardId + ".pem");

		try {
			ch.post.it.evoting.cryptolib.certificates.utils.PemUtils.saveCertificateToFile(signedCertificate, signedCertificatePath);
		} catch (GeneralCryptoLibException e) {
			throw new AdminBoardServiceException(e);
		}

		LOGGER.info("Stored signed certificate to file");
	}

	/**
	 * Construct csr signing input properties.
	 *
	 * @param cryptoAPIX509Certificate the crypto api x509 certificate
	 * @return the CSR signing input properties
	 */
	private CSRSigningInputProperties constructCsrSigningInputProperties(final CryptoAPIX509Certificate cryptoAPIX509Certificate) {

		ZonedDateTime notBefore = ZonedDateTime.now(ZoneOffset.UTC);
		ZonedDateTime notAfter = cryptoAPIX509Certificate.getNotAfter().toInstant().atZone(ZoneOffset.UTC).minusSeconds(1);
		return new CSRSigningInputProperties(notBefore, notAfter, CertificateParameters.Type.SIGN);
	}

	/**
	 * Gets the crypto api x509 certificate from x509 certificate.
	 *
	 * @param x509Certificate the x509 certificate
	 * @return the crypto api x509 certificate from x509 certificate
	 */
	private CryptoAPIX509Certificate getCryptoAPIX509CertificateFromX509Certificate(final X509Certificate x509Certificate) {

		try {
			return new CryptoX509Certificate(x509Certificate);
		} catch (GeneralCryptoLibException e) {
			throw new ConfigurationEngineException("Exceptional while trying to creating CryptoAPIX509Certificate from X509Certificate", e);
		}
	}

	/**
	 * Gets the admin board json object.
	 *
	 * @param adminBoardId the admin board id
	 * @return the admin board json object
	 * @throws ResourceNotFoundException the resource not found exception
	 */
	private JsonObject getAdminBoardJsonObject(final String adminBoardId) throws ResourceNotFoundException {

		// get number of members, threshold
		String administrationAuthorityJSON = administrationAuthorityRepository.find(adminBoardId);

		if (StringUtils.isEmpty(administrationAuthorityJSON) || JsonConstants.EMPTY_OBJECT.equals(administrationAuthorityJSON)) {
			throw new ResourceNotFoundException("Administration Authority not found");
		}

		return JsonUtils.getJsonObject(administrationAuthorityJSON);
	}

	/**
	 * Activate.
	 *
	 * @param adminBoardId the admin board id
	 * @return the activate output data
	 * @throws GeneralCryptoLibException the general crypto lib exception
	 */
	public ActivateOutputData activate(final String adminBoardId) throws GeneralCryptoLibException {
		LOGGER.info("Loading the admin board {} public key...", adminBoardId);
		PublicKey publicKey = loadAdminBoardPublicKey(adminBoardId);
		LOGGER.info("Admin board {} public key successfully loaded", adminBoardId);
		String publicKeyPEM = ch.post.it.evoting.cryptolib.certificates.utils.PemUtils.publicKeyToPem(publicKey);
		ActivateOutputData output = new ActivateOutputData();
		output.setIssuerPublicKeyPEM(publicKeyPEM);
		output.setSerializedSubjectPublicKey(publicKeyPEM);
		return output;
	}

	/**
	 * Read share.
	 *
	 * @param adminBoardId the admin board id
	 * @param shareNumber  the share number
	 * @param pin          the pin
	 * @param publicKeyPEM the public key pem
	 * @return the string
	 * @throws ResourceNotFoundException the resource not found exception
	 * @throws SharesException           the shares exception
	 */
	public String readShare(final String adminBoardId, final Integer shareNumber, final String pin, final String publicKeyPEM)
			throws ResourceNotFoundException, SharesException {

		String member = getMemberFromRepository(adminBoardId, shareNumber);
		LOGGER.info("Reading share of admin board {} and member {}...", adminBoardId, member);

		LOGGER.info("Checking that the smartcard corresponds to member {}...", member);
		String label;
		try {
			label = hashService.getHashValueForMember(member);
		} catch (HashServiceException e) {
			throw new AdminBoardServiceException("Failed to get hash value for admin board member", e);
		}
		if (label.length() > Constants.SMART_CARD_LABEL_MAX_LENGTH) {
			label = label.substring(0, Constants.SMART_CARD_LABEL_MAX_LENGTH);
		}
		if (!statelessReadSharesHandler.getSmartcardLabel().equals(label)) {
			throw new AdminBoardServiceException("The smartcard introduced does not correspond to the selected member: " + member);
		}

		PublicKey adminBoardPublicKey;
		try {
			adminBoardPublicKey = ch.post.it.evoting.cryptolib.certificates.utils.PemUtils.publicKeyFromPem(publicKeyPEM);
		} catch (GeneralCryptoLibException e) {
			throw new IllegalArgumentException("Public key is not in valid PEM format", e);
		}

		LOGGER.info("Reading share from smartcard...");
		String shareSerialized = statelessReadSharesHandler.readShareAndStringify(pin, adminBoardPublicKey);
		LOGGER.info("Share successfully read");

		return shareSerialized;
	}

	/**
	 * Reconstruct.
	 *
	 * @param adminBoardId     the admin board id
	 * @param serializedShares the serialized shares
	 * @param publicKeyPEM     the public key pem
	 * @return the string
	 * @throws SharesException           the shares exception
	 * @throws GeneralCryptoLibException the general crypto lib exception
	 */
	public String reconstruct(final String adminBoardId, final List<String> serializedShares, final String publicKeyPEM)
			throws SharesException, GeneralCryptoLibException {

		PublicKey adminBoardPublicKey = ch.post.it.evoting.cryptolib.certificates.utils.PemUtils.publicKeyFromPem(publicKeyPEM);

		LOGGER.info("Reconstructing private key of admin board {}...", adminBoardId);
		PrivateKey adminBoardPrivateKey = statelessReadSharesHandler
				.getPrivateKeyWithSerializedShares(new HashSet<>(serializedShares), adminBoardPublicKey);

		LOGGER.info("Private key successfully reconstructed");

		return ch.post.it.evoting.cryptolib.certificates.utils.PemUtils.privateKeyToPem(adminBoardPrivateKey);
	}

	/**
	 * Gets the member from repository.
	 *
	 * @param adminBoardId the admin board id
	 * @param shareNumber  the share number
	 * @return the member from repository
	 * @throws ResourceNotFoundException the resource not found exception
	 */
	private String getMemberFromRepository(final String adminBoardId, final Integer shareNumber) throws ResourceNotFoundException {
		JsonObject administrationAuthority = getAdminBoardJsonObject(adminBoardId);
		return getMemberFromRepository(administrationAuthority, shareNumber);
	}

	/**
	 * Gets the member from repository.
	 *
	 * @param administrationAuthority the administration authority
	 * @param shareNumber             the share number
	 * @return the member from repository
	 * @throws ResourceNotFoundException the resource not found exception
	 */
	private String getMemberFromRepository(final JsonObject administrationAuthority, final Integer shareNumber) throws ResourceNotFoundException {
		JsonArray administrationBoardMembers = administrationAuthority.getJsonArray("administrationBoard");

		String member = administrationBoardMembers.getString(shareNumber);
		if (member == null) {
			throw new ResourceNotFoundException("Administration board member for share number " + shareNumber + " is null.");
		}
		if (member.isEmpty()) {
			throw new ResourceNotFoundException("Administration board member for share number " + shareNumber + " is empty.");
		}

		return member;
	}

	/**
	 * Load admin board public key.
	 *
	 * @param adminBoardId the admin board id
	 * @return the public key
	 */
	private PublicKey loadAdminBoardPublicKey(final String adminBoardId) {
		return getAdminBoardCertificate(adminBoardId).getPublicKey();
	}

	private X509Certificate getAdminBoardCertificate(String adminBoardId) {
		Path adminBoardCertPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, Constants.CSR_FOLDER, adminBoardId + Constants.PEM);

		try {
			X509Certificate adminBoardCert = (X509Certificate) ch.post.it.evoting.cryptolib.certificates.utils.PemUtils
					.certificateFromPem(new String(Files.readAllBytes(adminBoardCertPath), StandardCharsets.UTF_8));

			CryptoAPIX509Certificate cryptoCert = new CryptoX509Certificate(adminBoardCert);
			if (!cryptoCert.getSubjectDn().getCommonName().contains(adminBoardId)) {
				throw new AdminBoardServiceException(
						"The loaded administration board certificate does not correspond to the board with id: " + adminBoardId);
			}

			return cryptoCert.getCertificate();
		} catch (GeneralCryptoLibException | IOException e) {
			throw new AdminBoardServiceException("An error occurred while loading the administration board public key", e);
		}
	}
}
