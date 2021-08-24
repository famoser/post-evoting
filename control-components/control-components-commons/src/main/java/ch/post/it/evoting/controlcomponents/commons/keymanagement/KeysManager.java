/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.Files.newInputStream;
import static java.text.MessageFormat.format;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.security.auth.x500.X500Principal;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponents.commons.keymanagement.exception.InvalidKeyStoreException;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.exception.InvalidNodeCAException;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.exception.InvalidPasswordException;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.exception.KeyNotFoundException;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.log.ControlComponentsCommonsLogConstants;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.log.ControlComponentsCommonsLogEvents;
import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.stores.StoresServiceAPI;
import ch.post.it.evoting.cryptolib.api.stores.bean.KeyStoreType;
import ch.post.it.evoting.cryptolib.certificates.constants.X509CertificateConstants;
import ch.post.it.evoting.cryptolib.certificates.utils.LdapHelper;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.logging.api.domain.LogContent;
import ch.post.it.evoting.logging.api.domain.LogEvent;

/**
 * <p>
 * This implementation uses a relational database as persistence storage of the keys. Clients should respect the following rules while implementing
 * the transaction management:
 * <ul>
 * <li>The supplied {@link DataSource} must be configured with the auto-commit switched off.</li>
 * <li>All the methods like
 * {@code activateNodeKeys, createAndActivateNodeKeys, createXXX, getXXX, hasValidElectionSigningKeys}
 * must be invoked in transaction.</li>
 * <li>All the checked exceptions but the generic {@link KeyManagementException} indicates some
 * precondition check failure. They are thrown before any data is actually written to the database.
 * Thus in such a case the client can proceed with the current transaction without a risk to damage
 * the data.</li>
 * </ul>
 */
@Service
public class KeysManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(KeysManager.class);

	private static final char SEPARATOR = '|';
	private static final String PIPE_REPLACEMENT = "(p)";
	private static final char ADDITIONAL_VALUES = '"';
	private static final char BLANK = ' ';
	private static final char EQUAL = '=';
	private static final char DEFAULT_VALUE = '-';
	private static final Pattern PIPE_ESCAPED_PATTERN = Pattern.compile("\\|");

	private final AsymmetricServiceAPI asymmetricService;
	private final StoresServiceAPI storesService;
	private final Generator generator;
	private final KeysRepository keysRepository;
	private final Cache cache;
	private final String controlComponentId;

	private AtomicReference<NodeKeys> nodeKeys;

	public KeysManager(final AsymmetricServiceAPI asymmetricService, final StoresServiceAPI storesService, final Generator generator,
			final KeysRepository keysRepository, final Cache cache,
			@Value("${key.node.id}")
			final String controlComponentId) {
		this.asymmetricService = asymmetricService;
		this.storesService = storesService;
		this.generator = generator;
		this.keysRepository = keysRepository;
		this.cache = cache;
		this.controlComponentId = controlComponentId;
	}

	private static String getSubjectCommonName(final X509Certificate certificate) {
		return getPrincipalCommonName(certificate.getSubjectX500Principal());
	}

	private static String getIssuerCommonName(final X509Certificate certificate) {
		return getPrincipalCommonName(certificate.getIssuerX500Principal());
	}

	private static String getPrincipalCommonName(final X500Principal principal) {
		return new LdapHelper().getAttributeFromDistinguishedName(principal.getName(), X509CertificateConstants.COMMON_NAME_ATTRIBUTE_NAME);
	}

	private static String escapePipe(final String info) {
		if (info != null) {
			return PIPE_ESCAPED_PATTERN.matcher(info).replaceAll(PIPE_REPLACEMENT);
		}
		return null;
	}

	public synchronized void activateNodeKeys(final PasswordProtection password) throws KeyManagementException {
		checkNotNull(password);

		activateNodeKeys(keysRepository.loadNodeKeys(password));
	}

	public void createAndActivateNodeKeys(
			@javax.annotation.WillNotClose
			final InputStream stream, final String alias, final PasswordProtection password) throws KeyManagementException, IOException {

		checkNotNull(stream);

		final KeyStore keyStore;
		try {
			keyStore = storesService.loadKeyStore(KeyStoreType.PKCS12, stream, password.getPassword());
		} catch (GeneralCryptoLibException e) {
			if (e.getCause().getCause() instanceof UnrecoverableEntryException) {
				throw new InvalidPasswordException("Password is invalid.", e);
			} else if (e.getCause() instanceof IOException) {
				throw (IOException) e.getCause();
			} else {
				throw new KeyManagementException("Failed to create and activate node keys.", e);
			}
		}

		createAndActivateNodeKeys(keyStore, alias, password);
	}

	public void createAndActivateNodeKeys(final KeyStore keyStore, final String alias, final PasswordProtection password)
			throws KeyManagementException {

		checkNotNull(keyStore);
		checkNotNull(alias);
		checkNotNull(password);

		final PrivateKeyEntry entry;
		try {
			if (!keyStore.entryInstanceOf(alias, PrivateKeyEntry.class)) {
				throw new InvalidKeyStoreException(format("Invalid CCN CA alias ''{0}''", alias));
			}
			entry = (PrivateKeyEntry) keyStore.getEntry(alias, password);
		} catch (UnrecoverableEntryException e) {
			throw new InvalidPasswordException("Password is invalid.", e);
		} catch (KeyStoreException | NoSuchAlgorithmException e) {
			throw new KeyManagementException("Failed to create and activate node keys.", e);
		}

		final Certificate[] certificateChain = entry.getCertificateChain();
		for (final Certificate certificate : certificateChain) {
			if (!(certificate instanceof X509Certificate)) {
				throw new InvalidNodeCAException("Certificate chain contains non-X509 certificates.");
			}
		}

		createAndActivateNodeKeys(entry.getPrivateKey(), (X509Certificate[]) certificateChain, password);
	}

	public void createAndActivateNodeKeys(final Path file, final String alias, final PasswordProtection password)
			throws KeyManagementException, IOException {

		checkNotNull(file);

		try (final InputStream stream = newInputStream(file)) {
			createAndActivateNodeKeys(stream, alias, password);
		}
	}

	public void createAndActivateNodeKeys(final PrivateKey nodeCAPrivateKey, final X509Certificate[] nodeCACertificateChain,
			final PasswordProtection password) throws KeyManagementException {

		checkNotNull(nodeCAPrivateKey);
		checkNotNull(nodeCACertificateChain);
		checkArgument(nodeCACertificateChain.length != 0, "CCN CA certificate chain is empty.");
		checkNotNull(password);
		checkNodeCAKeysMatch(nodeCAPrivateKey, nodeCACertificateChain[0].getPublicKey());

		synchronized (this) {
			final NodeKeys generatedNodeKeys = generator.generateNodeKeys(nodeCAPrivateKey, nodeCACertificateChain);
			keysRepository.saveNodeKeys(generatedNodeKeys, password);
			activateNodeKeys(generatedNodeKeys);
		}
	}

	public void createCcrjReturnCodesKeys(final CcrjReturnCodesKeysSpec ccrjReturnCodesKeysSpec) throws KeyManagementException {

		checkNotNull(ccrjReturnCodesKeysSpec);

		checkNodeKeysActivated();

		final ElectionSigningKeys electionSigningKeys = keysRepository.loadElectionSigningKeys(ccrjReturnCodesKeysSpec.getElectionEventId());

		final CcrjReturnCodesKeys ccrjReturnCodesKeys;
		try {
			ccrjReturnCodesKeys = generator.generateCcrjReturnCodesKeys(ccrjReturnCodesKeysSpec, electionSigningKeys);
		} catch (KeyManagementException e) {
			logErrorGeneratingKeyPairCEK(ccrjReturnCodesKeysSpec);
			throw e;
		}

		final String keyPairsGeneratedMessage = buildMessage(
				new LogContent.LogContentBuilder().logEvent(ControlComponentsCommonsLogEvents.CCR_J_RETURN_CODES_KEY_PAIRS_GENERATED)
						.electionEvent(ccrjReturnCodesKeysSpec.getElectionEventId()).objectId(ccrjReturnCodesKeysSpec.getVerificationCardSetId())
						.additionalInfo(ControlComponentsCommonsLogConstants.CONTROL_COMPONENT_ID, controlComponentId).createLogInfo());
		LOGGER.info(keyPairsGeneratedMessage);

		final String keyPairsSignedMessage = buildMessage(
				new LogContent.LogContentBuilder().logEvent(ControlComponentsCommonsLogEvents.CCR_J_RETURN_CODES_KEY_PAIRS_SIGNED)
						.electionEvent(ccrjReturnCodesKeysSpec.getElectionEventId())
						.objectId(ccrjReturnCodesKeysSpec.getVerificationCardSetId())
						.additionalInfo(ControlComponentsCommonsLogConstants.CONTROL_COMPONENT_ID, controlComponentId).createLogInfo());
		LOGGER.info(keyPairsSignedMessage);

		try {

			keysRepository.saveCcrjReturnCodesKeys(ccrjReturnCodesKeysSpec.getElectionEventId(), ccrjReturnCodesKeysSpec.getVerificationCardSetId(),
					ccrjReturnCodesKeys);

			final String keyPairsStoredMessage = buildMessage(
					new LogContent.LogContentBuilder().logEvent(ControlComponentsCommonsLogEvents.CCR_J_RETURN_CODES_KEY_PAIRS_STORED)
							.electionEvent(ccrjReturnCodesKeysSpec.getElectionEventId())
							.objectId(ccrjReturnCodesKeysSpec.getVerificationCardSetId())
							.additionalInfo(ControlComponentsCommonsLogConstants.CONTROL_COMPONENT_ID, controlComponentId).createLogInfo());
			LOGGER.info(keyPairsStoredMessage);

		} catch (KeyManagementException e) {
			logErrorStoringKeyPairCEK(ccrjReturnCodesKeysSpec);
			throw e;
		}
	}

	private void logErrorStoringKeyPairCEK(final CcrjReturnCodesKeysSpec spec) {
		final String errorStoringKeyPairsMessage = buildMessage(
				new LogContent.LogContentBuilder().logEvent(ControlComponentsCommonsLogEvents.ERROR_STORING_CCR_J_RETURN_CODES_KEY_PAIRS)
						.objectId(spec.getVerificationCardSetId())
						.electionEvent(spec.getElectionEventId())
						.additionalInfo(ControlComponentsCommonsLogConstants.CONTROL_COMPONENT_ID, controlComponentId)
						.additionalInfo(ControlComponentsCommonsLogConstants.ERR_DESC,
								ControlComponentsCommonsLogEvents.ERROR_STORING_CCR_J_RETURN_CODES_KEY_PAIRS.getInfo()).createLogInfo());
		LOGGER.error(errorStoringKeyPairsMessage);
	}

	private void logErrorGeneratingKeyPairCEK(final CcrjReturnCodesKeysSpec spec) {
		final String errorGenerationKeyPairs = buildMessage(
				new LogContent.LogContentBuilder().logEvent(ControlComponentsCommonsLogEvents.ERROR_GENERATION_CCR_J_RETURN_CODES_KEY_PAIRS)
						.electionEvent(spec.getElectionEventId())
						.objectId(spec.getVerificationCardSetId())
						.additionalInfo(ControlComponentsCommonsLogConstants.CONTROL_COMPONENT_ID, controlComponentId)
						.additionalInfo(ControlComponentsCommonsLogConstants.ERR_DESC,
								ControlComponentsCommonsLogEvents.ERROR_GENERATION_CCR_J_RETURN_CODES_KEY_PAIRS.getInfo()).createLogInfo());
		LOGGER.error(errorGenerationKeyPairs);
	}

	public void createElectionSigningKeys(final String electionEventId, final Date validFrom, final Date validTo) throws KeyManagementException {
		checkNotNull(electionEventId);
		checkNotNull(validFrom);
		checkNotNull(validTo);

		checkNodeKeysActivated();

		final ElectionSigningKeys electionSigningkeys;
		try {
			electionSigningkeys = generator.generateElectionSigningKeys(electionEventId, validFrom, validTo, nodeKeys.get());
		} catch (KeyManagementException e) {
			logErrorSigningCertificateGenerated(electionEventId);
			throw e;
		}

		logSigningCertificateGenerated(electionEventId, electionSigningkeys);

		keysRepository.saveElectionSigningKeys(electionEventId, electionSigningkeys);

		logStoredGeneratedSigningKeyPair(electionEventId);
	}

	private void logStoredGeneratedSigningKeyPair(final String electionEventId) {
		final String keyPairsGeneratedStored = buildMessage(
				new LogContent.LogContentBuilder().logEvent(ControlComponentsCommonsLogEvents.KEY_PAIR_GENERATED_STORED)
						.electionEvent(electionEventId)
						.additionalInfo(ControlComponentsCommonsLogConstants.CONTROL_COMPONENT_ID, controlComponentId).createLogInfo());
		LOGGER.info(keyPairsGeneratedStored);
	}

	private void logSigningCertificateGenerated(final String electionEventId, final ElectionSigningKeys electionSigningkeys) {
		// Generate Control Component Nodes signing key pair -- Control Component Signing Certificate successfully generated

		final X509Certificate certificate = electionSigningkeys.certificate();
		final String certCommonName = getSubjectCommonName(certificate);
		final String issuerCommonName = getIssuerCommonName(certificate);
		final BigInteger certSerialNumber = certificate.getSerialNumber();

		final String signingCertificateGenerated = buildMessage(
				new LogContent.LogContentBuilder().logEvent(ControlComponentsCommonsLogEvents.CONTROL_COMPONENT_SIGNING_CERTIFICATED_GENERATED)
						.electionEvent(electionEventId)
						.additionalInfo(ControlComponentsCommonsLogConstants.CERT_COMMON_NAME, certCommonName)
						.additionalInfo(ControlComponentsCommonsLogConstants.CERT_SERIAL_NUMBER, certSerialNumber.toString())
						.additionalInfo(ControlComponentsCommonsLogConstants.ISSUER_COMMON_NAME, issuerCommonName)
						.additionalInfo(ControlComponentsCommonsLogConstants.CONTROL_COMPONENT_ID, controlComponentId).createLogInfo());
		LOGGER.info(signingCertificateGenerated);
	}

	private void logErrorSigningCertificateGenerated(final String electionEventId) {
		final String errorGeneratingSigningCertificate = buildMessage(
				new LogContent.LogContentBuilder().logEvent(ControlComponentsCommonsLogEvents.ERROR_CONTROL_COMPONENT_SIGNING_CERTIFICATED)
						.electionEvent(electionEventId)
						.additionalInfo(ControlComponentsCommonsLogConstants.CONTROL_COMPONENT_ID, controlComponentId)
						.additionalInfo(ControlComponentsCommonsLogConstants.ERR_DESC,
								ControlComponentsCommonsLogEvents.ERROR_CONTROL_COMPONENT_SIGNING_CERTIFICATED.getInfo()).createLogInfo());
		LOGGER.error(errorGeneratingSigningCertificate);
	}

	public void createElectionSigningKeys(final String electionEventId, final ZonedDateTime validFrom, final ZonedDateTime validTo)
			throws KeyManagementException {

		checkNotNull(validFrom);
		checkNotNull(validTo);

		createElectionSigningKeys(electionEventId, Date.from(validFrom.toInstant()), Date.from(validTo.toInstant()));
	}

	public void createCcmElectionKey(final CcmjElectionKeysSpec ccmjElectionKeysSpec) throws KeyManagementException {
		checkNotNull(ccmjElectionKeysSpec);

		checkNodeKeysActivated();

		final ElectionSigningKeys electionSigningKeys = keysRepository.loadElectionSigningKeys(ccmjElectionKeysSpec.getElectionEventId());

		final CcmjElectionKeys ccmjElectionKeys;
		try {
			ccmjElectionKeys = generator.generateCcmjElectionKeys(ccmjElectionKeysSpec, electionSigningKeys);
		} catch (KeyManagementException e) {
			logErrorGeneratingKpELK(ccmjElectionKeysSpec);
			throw e;
		}

		final String keyPairGeneratedMessage = buildMessage(
				new LogContent.LogContentBuilder().logEvent(ControlComponentsCommonsLogEvents.CCM_J_ELECTION_KEY_PAIR_GENERATED)
						.electionEvent(ccmjElectionKeysSpec.getElectionEventId())
						.objectId(ccmjElectionKeysSpec.getElectoralAuthorityId())
						.additionalInfo(ControlComponentsCommonsLogConstants.CONTROL_COMPONENT_ID, controlComponentId).createLogInfo());
		LOGGER.info(keyPairGeneratedMessage);

		final String keyPairSignedMessage = buildMessage(
				new LogContent.LogContentBuilder().logEvent(ControlComponentsCommonsLogEvents.CCM_J_ELECTION_KEY_PAIR_SIGNED)
						.electionEvent(ccmjElectionKeysSpec.getElectionEventId())
						.additionalInfo(ControlComponentsCommonsLogConstants.CONTROL_COMPONENT_ID, controlComponentId).createLogInfo());
		LOGGER.info(keyPairSignedMessage);

		try {
			keysRepository.saveCcmjElectionKeys(ccmjElectionKeysSpec.getElectionEventId(), ccmjElectionKeys);
		} catch (KeyManagementException e) {
			logErrorStoringKpELK(ccmjElectionKeysSpec);
			throw e;
		}

		// Generate Control Component Nodes election key -- Key pair successfully stored
		final String keyPairStoredMessage = buildMessage(
				new LogContent.LogContentBuilder().logEvent(ControlComponentsCommonsLogEvents.CCM_J_ELECTION_KEY_PAIR_STORED)
						.electionEvent(ccmjElectionKeysSpec.getElectionEventId())
						.objectId(ccmjElectionKeysSpec.getElectoralAuthorityId())
						.additionalInfo(ControlComponentsCommonsLogConstants.CONTROL_COMPONENT_ID, controlComponentId).createLogInfo());
		LOGGER.info(keyPairStoredMessage);
	}

	private void logErrorStoringKpELK(final CcmjElectionKeysSpec ccmjElectionKeysSpec) {
		final String errorKeyPairStoringMessage = buildMessage(
				new LogContent.LogContentBuilder().logEvent(ControlComponentsCommonsLogEvents.ERROR_STORING_CCM_J_ELECTION_KEY_PAIR)
						.electionEvent(ccmjElectionKeysSpec.getElectionEventId())
						.objectId(ccmjElectionKeysSpec.getElectoralAuthorityId())
						.additionalInfo(ControlComponentsCommonsLogConstants.CONTROL_COMPONENT_ID, controlComponentId)
						.additionalInfo(ControlComponentsCommonsLogConstants.ERR_DESC,
								ControlComponentsCommonsLogEvents.ERROR_STORING_CCM_J_ELECTION_KEY_PAIR.getInfo()).createLogInfo());
		LOGGER.error(errorKeyPairStoringMessage);
	}

	private void logErrorGeneratingKpELK(final CcmjElectionKeysSpec ccmjElectionKeysSpec) {
		final String errorKeyPairGenerationMessage = buildMessage(
				new LogContent.LogContentBuilder().logEvent(ControlComponentsCommonsLogEvents.ERROR_GENERATION_CCM_J_ELECTION_KEY_PAIR)
						.electionEvent(ccmjElectionKeysSpec.getElectionEventId())
						.objectId(ccmjElectionKeysSpec.getElectoralAuthorityId())
						.additionalInfo(ControlComponentsCommonsLogConstants.CONTROL_COMPONENT_ID, controlComponentId)
						.additionalInfo(ControlComponentsCommonsLogConstants.ERR_DESC,
								ControlComponentsCommonsLogEvents.ERROR_GENERATION_CCM_J_ELECTION_KEY_PAIR.getInfo()).createLogInfo());
		LOGGER.error(errorKeyPairGenerationMessage);
	}

	public ElGamalPrivateKey getCcrjChoiceReturnCodesEncryptionSecretKey(final String electionEventId, final String verificationCardSetId)
			throws KeyManagementException {
		return getCcrjReturnCodesKeys(electionEventId, verificationCardSetId).getCcrjChoiceReturnCodesEncryptionSecretKey();
	}

	public ElGamalPublicKey getCcrjChoiceReturnCodesEncryptionPublicKey(final String electionEventId, final String verificationCardSetId)
			throws KeyManagementException {
		return getCcrjReturnCodesKeys(electionEventId, verificationCardSetId).getCcrjChoiceReturnCodesEncryptionPublicKey();
	}

	public byte[] getCcrjChoiceReturnCodesEncryptionPublicKeySignature(final String electionEventId, final String verificationCardSetId)
			throws KeyManagementException {
		return getCcrjReturnCodesKeys(electionEventId, verificationCardSetId).getCcrjChoiceReturnCodesEncryptionPublicKeySignature();
	}

	public ElGamalEncryptionParameters getEncryptionParameters(final String electionEventId, final String verificationCardSetId)
			throws KeyManagementException {
		return getCcrjReturnCodesKeys(electionEventId, verificationCardSetId).getEncryptionParameters();
	}

	public ElGamalPrivateKey getCcrjReturnCodesGenerationSecretKey(final String electionEventId, final String verificationCardSetId)
			throws KeyManagementException {
		return getCcrjReturnCodesKeys(electionEventId, verificationCardSetId).getCcrjReturnCodesGenerationSecretKey();
	}

	public ElGamalPublicKey getCcrjReturnCodesGenerationPublicKey(final String electionEventId, final String verificationCardSetId)
			throws KeyManagementException {
		return getCcrjReturnCodesKeys(electionEventId, verificationCardSetId).getCcrjReturnCodesGenerationPublicKey();
	}

	public byte[] getCcrjReturnCodesGenerationPublicKeySignature(final String electionEventId, final String verificationCardSetId)
			throws KeyManagementException {
		return getCcrjReturnCodesKeys(electionEventId, verificationCardSetId).getCcrjReturnCodesGenerationPublicKeySignature();
	}

	public X509Certificate getElectionSigningCertificate(final String electionEventId) throws KeyManagementException {
		return getElectionSigningKeys(electionEventId).certificate();
	}

	public X509Certificate[] getElectionSigningCertificateChain(final String electionEventId) throws KeyManagementException {
		return getElectionSigningKeys(electionEventId).certificateChain();
	}

	public PrivateKey getElectionSigningPrivateKey(final String electionEventId) throws KeyManagementException {
		return getElectionSigningKeys(electionEventId).privateKey();
	}

	public PublicKey getElectionSigningPublicKey(final String electionEventId) throws KeyManagementException {
		return getElectionSigningKeys(electionEventId).publicKey();
	}

	public ElGamalPrivateKey getCcmjElectionSecretKey(final String electionEventId) throws KeyManagementException {
		return getCcmjElectionKeys(electionEventId).getCcmjElectionSecretKey();
	}

	public ElGamalPublicKey getCcmjElectionPublicKey(final String electionEventId) throws KeyManagementException {
		return getCcmjElectionKeys(electionEventId).getCcmjElectionPublicKey();
	}

	public byte[] getCcmjElectionPublicKeySignature(final String electionEventId) throws KeyManagementException {
		return getCcmjElectionKeys(electionEventId).getCcmjElectionPublicKeySignature();
	}

	public X509Certificate getPlatformCACertificate() {
		checkNodeKeysActivated();

		// The existence of a non-empty certificate chain is mandatory, as the object cannot be built otherwise.
		return nodeKeys.get().caCertificateChain()[nodeKeys.get().caCertificateChain().length - 1];
	}

	public boolean hasCcrjReturnCodesKeys(final String electionEventId, final String verificationCardSetId) {
		return keysRepository.hasCcrjReturnCodesKeys(electionEventId, verificationCardSetId);
	}

	public boolean hasCcmjElectionKeys(final String electionEventId) {
		return keysRepository.hasCcmjElectionKeys(electionEventId);
	}

	public boolean hasNodeKeysActivated() {
		return nodeKeys != null;
	}

	public boolean hasValidElectionSigningKeys(final String electionEventId, final Date validFrom, final Date validTo) throws KeyManagementException {
		checkNotNull(validFrom);
		checkNotNull(validTo);

		final X509Certificate certificate;
		try {
			certificate = getElectionSigningCertificate(electionEventId);
		} catch (KeyNotFoundException e) {
			return false;
		}

		return !certificate.getNotBefore().after(validFrom) && !certificate.getNotAfter().before(validTo);
	}

	public boolean hasValidElectionSigningKeys(final String electionEventId, final ZonedDateTime validFrom, final ZonedDateTime validTo)
			throws KeyManagementException {
		checkNotNull(validFrom);
		checkNotNull(validTo);

		return hasValidElectionSigningKeys(electionEventId, Date.from(validFrom.toInstant()), Date.from(validTo.toInstant()));
	}

	public X509Certificate nodeCACertificate() {
		checkNodeKeysActivated();

		return nodeKeys.get().caCertificate();
	}

	public PrivateKey nodeCAPrivateKey() {
		checkNodeKeysActivated();

		return nodeKeys.get().caPrivateKey();
	}

	public PublicKey nodeCAPublicKey() {
		return nodeKeys.get().caPublicKey();
	}

	public X509Certificate nodeEncryptionCertificate() {
		checkNodeKeysActivated();

		return nodeKeys.get().encryptionCertificate();
	}

	public PrivateKey nodeEncryptionPrivateKey() {
		checkNodeKeysActivated();

		return nodeKeys.get().encryptionPrivateKey();
	}

	public PublicKey nodeEncryptionPublicKey() {
		return nodeKeys.get().encryptionPublicKey();
	}

	public X509Certificate nodeLogEncryptionCertificate() {
		checkNodeKeysActivated();

		return nodeKeys.get().logEncryptionCertificate();
	}

	public PrivateKey nodeLogEncryptionPrivateKey() {
		checkNodeKeysActivated();

		return nodeKeys.get().logEncryptionPrivateKey();
	}

	public PublicKey nodeLogEncryptionPublicKey() {
		return nodeKeys.get().logEncryptionPublicKey();
	}

	public X509Certificate nodeLogSigningCertificate() {
		checkNodeKeysActivated();

		return nodeKeys.get().logSigningCertificate();
	}

	public PrivateKey nodeLogSigningPrivateKey() {
		checkNodeKeysActivated();

		return nodeKeys.get().logSigningPrivateKey();
	}

	public PublicKey nodeLogSigningPublicKey() {
		return nodeKeys.get().logSigningPublicKey();
	}

	@PreDestroy
	public void shutdown() {
		cache.shutdown();
	}

	@PostConstruct
	public void startup() {
		cache.startup();
	}

	private void activateNodeKeys(final NodeKeys nodeKeys) {

		final PrivateKey privateKey = nodeKeys.encryptionPrivateKey();
		final PublicKey publicKey = nodeKeys.encryptionPublicKey();
		keysRepository.setEncryptionKeys(privateKey, publicKey);

		this.nodeKeys = new AtomicReference<>(nodeKeys);
	}

	private void checkNodeCAKeysMatch(final PrivateKey nodeCAPrivateKey, final PublicKey nodeCAPublicKey) throws InvalidNodeCAException {

		final byte[] bytes = nodeCAPublicKey.getEncoded();

		try {
			final byte[] signature = asymmetricService.sign(nodeCAPrivateKey, bytes);
			if (!asymmetricService.verifySignature(signature, nodeCAPublicKey, bytes)) {
				throw new InvalidNodeCAException("CCN CA keys do not match.");
			}
		} catch (GeneralCryptoLibException e) {
			throw new InvalidNodeCAException("CCN CA keys are invalid.", e);
		}
	}

	private void checkNodeKeysActivated() {
		if (!hasNodeKeysActivated()) {
			throw new IllegalStateException("Node keys are not activated.");
		}
	}

	private CcrjReturnCodesKeys getCcrjReturnCodesKeys(final String electionEventId, final String verificationCardSetId)
			throws KeyManagementException {
		checkNotNull(electionEventId);
		checkNotNull(verificationCardSetId);

		checkNodeKeysActivated();

		CcrjReturnCodesKeys ccrjReturnCodesKeys = cache.getCcrjReturnCodesKeys(electionEventId, verificationCardSetId);
		if (ccrjReturnCodesKeys == null) {
			ccrjReturnCodesKeys = keysRepository.loadCcrjReturnCodesKeys(electionEventId, verificationCardSetId);
			cache.putCcrjReturnCodesKeys(electionEventId, verificationCardSetId, ccrjReturnCodesKeys);
		}

		return ccrjReturnCodesKeys;
	}

	private ElectionSigningKeys getElectionSigningKeys(final String electionEventId) throws KeyManagementException {
		checkNotNull(electionEventId);

		checkNodeKeysActivated();

		ElectionSigningKeys electionSigningKeys = cache.getElectionSigningKeys(electionEventId);
		if (electionSigningKeys == null) {
			electionSigningKeys = keysRepository.loadElectionSigningKeys(electionEventId);
			cache.putElectionSigningKeys(electionEventId, electionSigningKeys);
		}

		return electionSigningKeys;
	}

	private CcmjElectionKeys getCcmjElectionKeys(final String electionEventId) throws KeyManagementException {
		checkNotNull(electionEventId);
		checkNodeKeysActivated();

		CcmjElectionKeys ccmjElectionKeys = cache.getCcmjElectionKeys(electionEventId);
		if (ccmjElectionKeys == null) {
			ccmjElectionKeys = keysRepository.loadCcmjElectionKeys(electionEventId);
			cache.putCcmjElectionKeys(electionEventId, ccmjElectionKeys);
		}
		return ccmjElectionKeys;
	}

	private String buildMessage(final LogContent logContent) {

		final StringBuilder message = new StringBuilder();

		final LogEvent logEvent = logContent.getLogEvent();

		message.append(SEPARATOR);
		message.append(escapePipe(logEvent.getLayer()));
		message.append(SEPARATOR);
		message.append(escapePipe(logEvent.getAction()));
		message.append(SEPARATOR);
		appendOptionalField(escapePipe(logContent.getObjectType()), message);
		message.append(SEPARATOR);

		appendOptionalField(escapePipe(logContent.getObjectId()), message);
		message.append(SEPARATOR);

		message.append(escapePipe(logEvent.getOutcome()));
		message.append(SEPARATOR);

		appendOptionalField(escapePipe(logContent.getUser()), message);
		message.append(SEPARATOR);

		appendOptionalField(escapePipe(logContent.getElectionEvent()), message);
		message.append(SEPARATOR);

		message.append(escapePipe(logEvent.getInfo()));
		message.append(SEPARATOR);

		final Map<String, String> additionalInfoMap = logContent.getAdditionalInfo();

		if (additionalInfoMap != null) {

			for (Map.Entry<String, String> pair : additionalInfoMap.entrySet()) {

				message.append(escapePipe(pair.getKey())).append(EQUAL);
				message.append(ADDITIONAL_VALUES).append(escapePipe(pair.getValue())).append(ADDITIONAL_VALUES);
				message.append(BLANK);
			}
		}

		return message.toString();
	}

	private void appendOptionalField(final String fieldValue, final StringBuilder sb) {
		if (fieldValue == null) {
			sb.append(DEFAULT_VALUE);
		} else {
			sb.append(fieldValue);
		}
	}
}
