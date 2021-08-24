/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.deleteIfExists;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import javax.security.auth.x500.X500Principal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.controlcomponents.commons.keymanagement.exception.InvalidKeyStoreException;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.exception.InvalidNodeCAException;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.exception.InvalidPasswordException;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.exception.KeyAlreadyExistsException;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.exception.KeyNotFoundException;
import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.stores.StoresServiceAPI;
import ch.post.it.evoting.cryptolib.api.stores.bean.KeyStoreType;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

/**
 * Tests of {@link KeysManager}.
 */
class KeysManagerTest {
	private static final PasswordProtection PASSWORD_PROTECTION = new PasswordProtection("password".toCharArray());

	private static final String ELECTION_EVENT_ID = "electionEventId";

	private static final String ELECTORAL_AUTHORITY_ID = "electoralAuthorityId";

	private static final String VERIFICATION_CARD_SET_ID = "verificationCardSetId";

	private static final String ENCRYPTION_PARAMETERS_JSON = "{\"encryptionParams\":{\"p\":\"AMoJqsCm6UG4B+FGO1Vh6qJcCJcMAVyZFIbZnTIZyD3XAQSTLdxVZxrXm5ZNQMajKAmjUfb3/BV84OgDZaNMXQziV2Uw6hQ70jUcswZ6ZfHAQBy+kwSozDM1LA6ndqp0JIGmPD7LqFIhv9ly7427ksgX1LjNR1tUInVaCS26dfP5wkZyCxwa9/3TapFpFugek+WSJqozaDSd89WAuqXrbBu0FMJWvBVvJapsjfJLEOXTHzdeWef9X5M4FY4Uk+/0WaDVZXy5JhcdoxuNU6Oy0/9buDrPfzlg5a26gpV5ei7XHfykYxdBIPUQoAmraSyUp/byhSDcQu6GJxkfknfAORU=\",\"q\":\"AJT/piLUjl6zN9D/Pc4GYsAG0KAS57H7xJ/mqb+ycbIl\",\"g\":\"ALNkOUZfTDbTMsCYUAKBAAzPdl7422rYGhca6GUAwEPQMlexuIZfIoft+ww54NDqGrUb4TFyw8Yy+y+9Ryb7AXNockro4XYfUjN6eiwOrPxfWG3r9BxqReWZRp74uQ2Ec1toVBrpHaXQNETOJpPBThMKKqrgIuPeAdMRM9U9viku8X0IDmHcqoNaI2o2fvFLFaxlzt5h4kzeJ0A5Twn8wwp5Uln0KcTBJf3H904Icq1u9WTvCTa1jQkoSzkNDQ1VABNFWBa/hW/RGAG7YqTurYHQB+9sMRpOiN10X+6ihdF5VgGpCgXfquKCSqa7I44vq2Vr9+Bm34Gjy+uDOJWrilg=\"}}";

	private static final String ALIAS = "alias";

	private AsymmetricServiceAPI asymmetricService;

	private PrivateKey caPrivateKey;

	private X509Certificate[] caCertificateChain;

	private NodeKeys nodeKeys;

	private ElectionSigningKeys electionSigningKeys;

	private CcmjElectionKeys ccmjElectionKeys;

	private CcrjReturnCodesKeys ccrjReturnCodesKeys;

	private KeyStoreSpi keyStoreSpi;

	private StoresServiceAPI storesService;

	private Generator generator;

	private KeysRepository keysRepository;

	private Cache cache;

	private KeysManager manager;

	@BeforeEach
	public void setUp()
			throws GeneralCryptoLibException, NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException, KeyManagementException,
			CertificateException, IOException {
		caPrivateKey = mock(PrivateKey.class);
		when(caPrivateKey.getAlgorithm()).thenReturn("RSA");
		PublicKey caPublicKey = mock(PublicKey.class);
		when(caPublicKey.getAlgorithm()).thenReturn("RSA");
		when(caPublicKey.getEncoded()).thenReturn(new byte[0]);
		X509Certificate caCertificate = mock(X509Certificate.class);
		when(caCertificate.getPublicKey()).thenReturn(caPublicKey);
		caCertificateChain = new X509Certificate[] { caCertificate };

		PrivateKey encryptionPrivateKey = mock(PrivateKey.class);
		PublicKey encryptionPublicKey = mock(PublicKey.class);
		X509Certificate encryptionCertificate = mock(X509Certificate.class);
		when(encryptionCertificate.getPublicKey()).thenReturn(encryptionPublicKey);
		X509Certificate[] encryptionCertificateChain = { encryptionCertificate, caCertificate };

		PrivateKey logSigningPrivateKey = mock(PrivateKey.class);
		PublicKey logSigningPublicKey = mock(PublicKey.class);
		X509Certificate logSigningCertificate = mock(X509Certificate.class);
		when(logSigningCertificate.getPublicKey()).thenReturn(logSigningPublicKey);
		X509Certificate[] logSigningCertificateChain = { logSigningCertificate, caCertificate };

		PrivateKey logEncryptionPrivateKey = mock(PrivateKey.class);
		PublicKey logEncryptionPublicKey = mock(PublicKey.class);
		X509Certificate logEncryptionCertificate = mock(X509Certificate.class);
		when(logEncryptionCertificate.getPublicKey()).thenReturn(logEncryptionPublicKey);
		X509Certificate[] logEncryptionCertificateChain = { logEncryptionCertificate, caCertificate };

		nodeKeys = new NodeKeys.Builder().setCAKeys(caPrivateKey, caCertificateChain)
				.setEncryptionKeys(encryptionPrivateKey, encryptionCertificateChain)
				.setLogSigningKeys(logSigningPrivateKey, logSigningCertificateChain)
				.setLogEncryptionKeys(logEncryptionPrivateKey, logEncryptionCertificateChain).build();

		PrivateKey electionSigingPrivateKey = mock(PrivateKey.class);
		PublicKey electionSigingPublicKey = mock(PublicKey.class);
		X509Certificate electionSigningCertificate = mock(X509Certificate.class);
		when(electionSigningCertificate.getPublicKey()).thenReturn(electionSigingPublicKey);
		X500Principal subjectPrincipal = new X500Principal("");
		when(electionSigningCertificate.getSubjectX500Principal()).thenReturn(subjectPrincipal);
		X500Principal issuerPrincipal = new X500Principal("");
		when(electionSigningCertificate.getIssuerX500Principal()).thenReturn(issuerPrincipal);
		when(electionSigningCertificate.getIssuerDN()).thenReturn(issuerPrincipal);
		when(electionSigningCertificate.getSerialNumber()).thenReturn(BigInteger.ONE);
		when(electionSigningCertificate.getEncoded()).thenReturn("encoded".getBytes(StandardCharsets.UTF_8));

		X509Certificate[] electionSigningCertificateChain = { electionSigningCertificate, caCertificate };
		electionSigningKeys = new ElectionSigningKeys(electionSigingPrivateKey, electionSigningCertificateChain);

		ZpSubgroup group = new ZpSubgroup(BigInteger.valueOf(2), BigInteger.valueOf(7), BigInteger.valueOf(3));

		List<Exponent> electionExponents = singletonList(new Exponent(group.getQ(), BigInteger.valueOf(2)));
		ElGamalPrivateKey electionPrivateKey = new ElGamalPrivateKey(electionExponents, group);
		List<ZpGroupElement> electionElements = singletonList(new ZpGroupElement(BigInteger.valueOf(2), group));
		ElGamalPublicKey electionPublicKey = new ElGamalPublicKey(electionElements, group);
		byte[] electionPublicKeySignature = { 1, 2, 3 };
		ccmjElectionKeys = new CcmjElectionKeys(electionPrivateKey, electionPublicKey, electionPublicKeySignature);

		List<Exponent> generationExponents = singletonList(new Exponent(group.getQ(), BigInteger.valueOf(3)));
		ElGamalPrivateKey generationPrivateKey = new ElGamalPrivateKey(generationExponents, group);
		List<ZpGroupElement> generationElements = singletonList(new ZpGroupElement(BigInteger.valueOf(3), group));
		ElGamalPublicKey generationPublicKey = new ElGamalPublicKey(generationElements, group);
		byte[] generationPublicKeySignature = { 4, 5, 6 };
		List<Exponent> decryptionExponents = singletonList(new Exponent(group.getQ(), BigInteger.valueOf(4)));
		ElGamalPrivateKey decryptionPrivateKey = new ElGamalPrivateKey(decryptionExponents, group);
		List<ZpGroupElement> decryptionElements = singletonList(new ZpGroupElement(BigInteger.valueOf(4), group));
		ElGamalPublicKey decryptionPublicKey = new ElGamalPublicKey(decryptionElements, group);
		byte[] decryptionPublicKeySignature = { 7, 8, 9 };
		ccrjReturnCodesKeys = new CcrjReturnCodesKeys.Builder()
				.setCcrjReturnCodesGenerationKeys(generationPrivateKey, generationPublicKey, generationPublicKeySignature)
				.setCcrjChoiceReturnCodesEncryptionKeys(decryptionPrivateKey, decryptionPublicKey, decryptionPublicKeySignature).build();

		asymmetricService = mock(AsymmetricServiceAPI.class);
		when(asymmetricService.sign(eq(caPrivateKey), any(byte[].class))).thenReturn(new byte[0]);
		when(asymmetricService.verifySignature(any(byte[].class), eq(caPublicKey), any(byte[].class))).thenReturn(true);

		keyStoreSpi = mock(KeyStoreSpi.class);
		doReturn(new PrivateKeyEntry(caPrivateKey, caCertificateChain)).when(keyStoreSpi).engineGetEntry(ALIAS, PASSWORD_PROTECTION);
		doReturn(true).when(keyStoreSpi).engineEntryInstanceOf(ALIAS, PrivateKeyEntry.class);

		KeyStoreDouble store = new KeyStoreDouble(keyStoreSpi);
		store.load(null, null);

		storesService = mock(StoresServiceAPI.class);
		when(storesService.loadKeyStore(eq(KeyStoreType.PKCS12), any(InputStream.class), eq(PASSWORD_PROTECTION.getPassword()))).thenReturn(store);

		generator = mock(Generator.class);
		when(generator.generateNodeKeys(caPrivateKey, caCertificateChain)).thenReturn(nodeKeys);
		when(generator.generateElectionSigningKeys(eq(ELECTION_EVENT_ID), any(Date.class), any(Date.class), eq(nodeKeys)))
				.thenReturn(electionSigningKeys);
		when(generator.generateCcmjElectionKeys(any(CcmjElectionKeysSpec.class), eq(electionSigningKeys))).thenReturn(ccmjElectionKeys);
		when(generator.generateCcrjReturnCodesKeys(any(CcrjReturnCodesKeysSpec.class), eq(electionSigningKeys))).thenReturn(ccrjReturnCodesKeys);

		keysRepository = mock(KeysRepository.class);
		when(keysRepository.loadNodeKeys(PASSWORD_PROTECTION)).thenReturn(nodeKeys);
		when(keysRepository.loadElectionSigningKeys(ELECTION_EVENT_ID)).thenReturn(electionSigningKeys);
		when(keysRepository.loadCcmjElectionKeys(ELECTION_EVENT_ID)).thenReturn(ccmjElectionKeys);
		when(keysRepository.loadCcrjReturnCodesKeys(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID)).thenReturn(ccrjReturnCodesKeys);

		cache = new Cache(new Timer(), Duration.ofHours(1));

		manager = new KeysManager(asymmetricService, storesService, generator, keysRepository, cache, "nodeId");
	}

	@AfterEach
	public void tearDown() {
		cache.shutdown();
	}

	@Test
	void testActivateNodeKeys() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertTrue(manager.hasNodeKeysActivated());
		assertEquals(nodeKeys.caPrivateKey(), manager.nodeCAPrivateKey());
		verify(keysRepository).loadNodeKeys(PASSWORD_PROTECTION);
		verify(keysRepository).setEncryptionKeys(nodeKeys.encryptionPrivateKey(), nodeKeys.encryptionPublicKey());
	}

	@Test
	void testActivateNodeKeysError() throws KeyManagementException {
		manager.startup();
		when(keysRepository.loadNodeKeys(PASSWORD_PROTECTION)).thenThrow(new KeyManagementException("test"));
		assertThrows(KeyManagementException.class, () -> manager.activateNodeKeys(PASSWORD_PROTECTION));
		assertFalse(manager.hasNodeKeysActivated());
	}

	@Test
	void testActivateNodeKeysInvalidPassword() throws KeyManagementException {
		manager.startup();
		when(keysRepository.loadNodeKeys(PASSWORD_PROTECTION)).thenThrow(new InvalidPasswordException("test"));
		assertThrows(InvalidPasswordException.class, () -> manager.activateNodeKeys(PASSWORD_PROTECTION));
		assertFalse(manager.hasNodeKeysActivated());
	}

	@Test
	void testActivateNodeKeysNotFound() throws KeyManagementException {
		manager.startup();
		when(keysRepository.loadNodeKeys(PASSWORD_PROTECTION)).thenThrow(new KeyNotFoundException("test"));
		assertThrows(KeyNotFoundException.class, () -> manager.activateNodeKeys(PASSWORD_PROTECTION));
		assertFalse(manager.hasNodeKeysActivated());
	}

	@Test
	void testCcrjReturnCodesKeysCaching() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(ccrjReturnCodesKeys.getCcrjChoiceReturnCodesEncryptionSecretKey(),
				manager.getCcrjChoiceReturnCodesEncryptionSecretKey(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID));

		assertEquals(ccrjReturnCodesKeys, cache.getCcrjReturnCodesKeys(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID));

		assertEquals(ccrjReturnCodesKeys.getCcrjChoiceReturnCodesEncryptionSecretKey(),
				manager.getCcrjChoiceReturnCodesEncryptionSecretKey(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID));

		verify(keysRepository, times(1)).loadCcrjReturnCodesKeys(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID);
	}

	@Test
	void testCreateAndActivateNodeKeysInputStreamStringPasswordProtection() throws IOException, KeyManagementException {
		manager.startup();
		try (InputStream stream = new ByteArrayInputStream(new byte[0])) {
			manager.createAndActivateNodeKeys(stream, ALIAS, PASSWORD_PROTECTION);
		}
		assertTrue(manager.hasNodeKeysActivated());
		assertEquals(nodeKeys.caPrivateKey(), manager.nodeCAPrivateKey());
		verify(keysRepository).saveNodeKeys(nodeKeys, PASSWORD_PROTECTION);
		verify(keysRepository).setEncryptionKeys(nodeKeys.encryptionPrivateKey(), nodeKeys.encryptionPublicKey());
	}

	@Test
	void testCreateAndActivateNodeKeysInputStreamStringPasswordProtectionAlreadyExist() throws KeyManagementException, IOException {
		manager.startup();
		doThrow(new KeyAlreadyExistsException("test")).when(keysRepository).saveNodeKeys(nodeKeys, PASSWORD_PROTECTION);
		try (InputStream stream = new ByteArrayInputStream(new byte[0])) {
			assertThrows(KeyAlreadyExistsException.class, () -> manager.createAndActivateNodeKeys(stream, ALIAS, PASSWORD_PROTECTION));
		} finally {
			assertFalse(manager.hasNodeKeysActivated());
		}
	}

	@Test
	void testCreateAndActivateNodeKeysInputStreamStringPasswordProtectionDatabaseError() throws KeyManagementException, IOException {
		manager.startup();
		doThrow(new KeyManagementException("test")).when(keysRepository).saveNodeKeys(nodeKeys, PASSWORD_PROTECTION);
		try (InputStream stream = new ByteArrayInputStream(new byte[0])) {
			assertThrows(KeyManagementException.class, () -> manager.createAndActivateNodeKeys(stream, ALIAS, PASSWORD_PROTECTION));
			assertFalse(manager.hasNodeKeysActivated());
		}
	}

	@Test
	void testCreateAndActivateNodeKeysInputStreamStringPasswordProtectionGeneratorError() throws KeyManagementException, IOException {
		manager.startup();
		when(generator.generateNodeKeys(caPrivateKey, caCertificateChain)).thenThrow(new KeyManagementException("test"));
		try (InputStream stream = new ByteArrayInputStream(new byte[0])) {
			assertThrows(KeyManagementException.class, () -> manager.createAndActivateNodeKeys(stream, ALIAS, PASSWORD_PROTECTION));
			assertFalse(manager.hasNodeKeysActivated());
		}
	}

	@Test
	void testCreateAndActivateNodeKeysInputStreamStringPasswordProtectionInvalidKeyStore() throws IOException {
		manager.startup();
		doReturn(false).when(keyStoreSpi).engineEntryInstanceOf(ALIAS, PrivateKeyEntry.class);
		try (InputStream stream = new ByteArrayInputStream(new byte[0])) {
			assertThrows(InvalidKeyStoreException.class, () -> manager.createAndActivateNodeKeys(stream, ALIAS, PASSWORD_PROTECTION));
			assertFalse(manager.hasNodeKeysActivated());
		}
	}

	@Test
	void testCreateAndActivateNodeKeysInputStreamStringPasswordProtectionInvalidNodeCA() throws IOException, GeneralCryptoLibException {
		manager.startup();
		when(asymmetricService.verifySignature(any(byte[].class), any(PublicKey.class), any(byte[].class))).thenReturn(false);
		try (InputStream stream = new ByteArrayInputStream(new byte[0])) {
			assertThrows(InvalidNodeCAException.class, () -> manager.createAndActivateNodeKeys(stream, ALIAS, PASSWORD_PROTECTION));
			assertFalse(manager.hasNodeKeysActivated());
		}
	}

	@Test
	void testCreateAndActivateNodeKeysInputStreamStringPasswordProtectionInvalidPassword() throws IOException, GeneralCryptoLibException {
		manager.startup();
		when(storesService.loadKeyStore(any(), any(), any()))
				.thenThrow(new GeneralCryptoLibException(new IOException(new UnrecoverableKeyException("test"))));
		try (InputStream stream = new ByteArrayInputStream(new byte[0])) {
			assertThrows(InvalidPasswordException.class, () -> manager.createAndActivateNodeKeys(stream, ALIAS, PASSWORD_PROTECTION));
			assertFalse(manager.hasNodeKeysActivated());
		}
	}

	@Test
	void testCreateAndActivateNodeKeysInputStreamStringPasswordProtectionIOException() throws GeneralCryptoLibException, IOException {
		manager.startup();
		when(storesService.loadKeyStore(any(), any(), any())).thenThrow(new GeneralCryptoLibException(new IOException("test")));
		try (InputStream stream = new ByteArrayInputStream(new byte[0])) {
			assertThrows(IOException.class, () -> manager.createAndActivateNodeKeys(stream, ALIAS, PASSWORD_PROTECTION));
			assertFalse(manager.hasNodeKeysActivated());
		}
	}

	@Test
	void testCreateAndActivateNodeKeysKeyStoreStringPasswordProtection()
			throws NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException {
		manager.startup();
		KeyStore keyStore = new KeyStoreDouble(keyStoreSpi);
		keyStore.load(null, null);
		manager.createAndActivateNodeKeys(keyStore, ALIAS, PASSWORD_PROTECTION);
		assertTrue(manager.hasNodeKeysActivated());
		assertEquals(nodeKeys.caPrivateKey(), manager.nodeCAPrivateKey());
		verify(keysRepository).saveNodeKeys(nodeKeys, PASSWORD_PROTECTION);
		verify(keysRepository).setEncryptionKeys(nodeKeys.encryptionPrivateKey(), nodeKeys.encryptionPublicKey());
	}

	@Test
	void testCreateAndActivateNodeKeysKeyStoreStringPasswordProtectionAlreadyExist()
			throws NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException {
		manager.startup();
		doThrow(new KeyAlreadyExistsException("test")).when(keysRepository).saveNodeKeys(nodeKeys, PASSWORD_PROTECTION);
		KeyStore keyStore = new KeyStoreDouble(keyStoreSpi);
		keyStore.load(null, null);
		assertThrows(KeyAlreadyExistsException.class, () -> manager.createAndActivateNodeKeys(keyStore, ALIAS, PASSWORD_PROTECTION));
		assertFalse(manager.hasNodeKeysActivated());
	}

	@Test
	void testCreateAndActivateNodeKeysKeyStoreStringPasswordProtectionDataBaseError()
			throws KeyManagementException, NoSuchAlgorithmException, CertificateException, IOException {
		manager.startup();
		doThrow(new KeyManagementException("test")).when(keysRepository).saveNodeKeys(nodeKeys, PASSWORD_PROTECTION);
		KeyStore keyStore = new KeyStoreDouble(keyStoreSpi);
		keyStore.load(null, null);
		assertThrows(KeyManagementException.class, () -> manager.createAndActivateNodeKeys(keyStore, ALIAS, PASSWORD_PROTECTION));
		assertFalse(manager.hasNodeKeysActivated());
	}

	@Test
	void testCreateAndActivateNodeKeysKeyStoreStringPasswordProtectionGeneratorError()
			throws KeyManagementException, NoSuchAlgorithmException, CertificateException, IOException {
		manager.startup();
		when(generator.generateNodeKeys(caPrivateKey, caCertificateChain)).thenThrow(new KeyManagementException("test"));
		KeyStore keyStore = new KeyStoreDouble(keyStoreSpi);
		keyStore.load(null, null);
		assertThrows(KeyManagementException.class, () -> manager.createAndActivateNodeKeys(keyStore, ALIAS, PASSWORD_PROTECTION));
		assertFalse(manager.hasNodeKeysActivated());
	}

	@Test
	void testCreateAndActivateNodeKeysKeyStoreStringPasswordProtectionInvalidKeyStore()
			throws NoSuchAlgorithmException, CertificateException, IOException {
		manager.startup();
		doReturn(false).when(keyStoreSpi).engineEntryInstanceOf(ALIAS, PrivateKeyEntry.class);
		KeyStore keyStore = new KeyStoreDouble(keyStoreSpi);
		keyStore.load(null, null);
		assertThrows(InvalidKeyStoreException.class, () -> manager.createAndActivateNodeKeys(keyStore, ALIAS, PASSWORD_PROTECTION));
		assertFalse(manager.hasNodeKeysActivated());
	}

	@Test
	void testCreateAndActivateNodeKeysKeyStoreStringPasswordProtectionInvalidNodeCA()
			throws GeneralCryptoLibException, NoSuchAlgorithmException, CertificateException, IOException {
		manager.startup();
		when(asymmetricService.verifySignature(any(byte[].class), any(PublicKey.class), any(byte[].class))).thenReturn(false);
		KeyStore keyStore = new KeyStoreDouble(keyStoreSpi);
		keyStore.load(null, null);
		assertThrows(InvalidNodeCAException.class, () -> manager.createAndActivateNodeKeys(keyStore, ALIAS, PASSWORD_PROTECTION));
		assertFalse(manager.hasNodeKeysActivated());
	}

	@Test
	void testCreateAndActivateNodeKeysKeyStoreStringPasswordProtectionInvalidPassword()
			throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableEntryException, CertificateException, IOException {
		manager.startup();
		doThrow(new UnrecoverableEntryException("test")).when(keyStoreSpi).engineGetEntry(ALIAS, PASSWORD_PROTECTION);
		KeyStore keyStore = new KeyStoreDouble(keyStoreSpi);
		keyStore.load(null, null);
		assertThrows(InvalidPasswordException.class, () -> manager.createAndActivateNodeKeys(keyStore, ALIAS, PASSWORD_PROTECTION));
		assertFalse(manager.hasNodeKeysActivated());
	}

	@Test
	void testCreateAndActivateNodeKeysPathStringPasswordProtection() throws IOException, KeyManagementException {
		manager.startup();
		Path file = createTempFile("keys", ".p12");
		try {
			manager.createAndActivateNodeKeys(file, ALIAS, PASSWORD_PROTECTION);
			assertTrue(manager.hasNodeKeysActivated());
			assertEquals(nodeKeys.caPrivateKey(), manager.nodeCAPrivateKey());
			verify(keysRepository).saveNodeKeys(nodeKeys, PASSWORD_PROTECTION);
			verify(keysRepository).setEncryptionKeys(nodeKeys.encryptionPrivateKey(), nodeKeys.encryptionPublicKey());
		} finally {
			deleteIfExists(file);
		}
	}

	@Test
	void testCreateAndActivateNodeKeysPathStringPasswordProtectionAlreadyExist() throws IOException, KeyManagementException {
		manager.startup();
		Path file = createTempFile("keys", ".p12");
		try {
			doThrow(new KeyAlreadyExistsException("test")).when(keysRepository).saveNodeKeys(nodeKeys, PASSWORD_PROTECTION);
			assertThrows(KeyAlreadyExistsException.class, () -> manager.createAndActivateNodeKeys(file, ALIAS, PASSWORD_PROTECTION));
			assertFalse(manager.hasNodeKeysActivated());
		} finally {
			deleteIfExists(file);
		}
	}

	@Test
	void testCreateAndActivateNodeKeysPathStringPasswordProtectionDatabaseError() throws IOException, KeyManagementException {
		manager.startup();
		Path file = createTempFile("keys", ".p12");
		try {
			doThrow(new KeyManagementException("test")).when(keysRepository).saveNodeKeys(nodeKeys, PASSWORD_PROTECTION);
			assertThrows(KeyManagementException.class, () -> manager.createAndActivateNodeKeys(file, ALIAS, PASSWORD_PROTECTION));
			assertFalse(manager.hasNodeKeysActivated());
		} finally {
			deleteIfExists(file);
		}
	}

	@Test
	void testCreateAndActivateNodeKeysPathStringPasswordProtectionGeneratorError() throws IOException, KeyManagementException {
		manager.startup();
		Path file = createTempFile("keys", ".p12");
		try {
			when(generator.generateNodeKeys(caPrivateKey, caCertificateChain)).thenThrow(new KeyManagementException("test"));
			assertThrows(KeyManagementException.class, () -> manager.createAndActivateNodeKeys(file, ALIAS, PASSWORD_PROTECTION));
			assertFalse(manager.hasNodeKeysActivated());
		} finally {
			deleteIfExists(file);
		}
	}

	@Test
	void testCreateAndActivateNodeKeysPathStringPasswordProtectionInvalidKeyStore() throws IOException {
		manager.startup();
		Path file = createTempFile("keys", ".p12");
		try {
			doReturn(false).when(keyStoreSpi).engineEntryInstanceOf(ALIAS, PrivateKeyEntry.class);
			assertThrows(InvalidKeyStoreException.class, () -> manager.createAndActivateNodeKeys(file, ALIAS, PASSWORD_PROTECTION));
			assertFalse(manager.hasNodeKeysActivated());
		} finally {
			deleteIfExists(file);
		}
	}

	@Test
	void testCreateAndActivateNodeKeysPathStringPasswordProtectionInvalidNodeCA() throws IOException, GeneralCryptoLibException {
		manager.startup();
		Path file = createTempFile("keys", ".p12");
		try {
			when(asymmetricService.verifySignature(any(), any(), any(byte[].class))).thenReturn(false);
			assertThrows(InvalidNodeCAException.class, () -> manager.createAndActivateNodeKeys(file, ALIAS, PASSWORD_PROTECTION));
			assertFalse(manager.hasNodeKeysActivated());
		} finally {
			deleteIfExists(file);
		}
	}

	@Test
	void testCreateAndActivateNodeKeysPathStringPasswordProtectionInvalidPassword() throws IOException, GeneralCryptoLibException {
		manager.startup();
		Path file = createTempFile("keys", ".p12");
		try {
			when(storesService.loadKeyStore(any(), any(), any()))
					.thenThrow(new GeneralCryptoLibException(new IOException(new UnrecoverableEntryException("test"))));
			assertThrows(InvalidPasswordException.class, () -> manager.createAndActivateNodeKeys(file, ALIAS, PASSWORD_PROTECTION));
			assertFalse(manager.hasNodeKeysActivated());
		} finally {
			deleteIfExists(file);
		}
	}

	@Test
	void testCreateAndActivateNodeKeysPathStringPasswordProtectionIOError() throws IOException, GeneralCryptoLibException {
		manager.startup();
		Path file = createTempFile("keys", ".p12");
		try {
			when(storesService.loadKeyStore(any(), any(), any())).thenThrow(new GeneralCryptoLibException(new IOException("test")));
			assertThrows(IOException.class, () -> manager.createAndActivateNodeKeys(file, ALIAS, PASSWORD_PROTECTION));
			assertFalse(manager.hasNodeKeysActivated());
		} finally {
			deleteIfExists(file);
		}
	}

	@Test
	void testCreateAndActivateNodeKeysPathStringPasswordProtectionNoSuchFile() throws IOException {
		manager.startup();
		Path file = createTempFile("keys", ".p12");
		try {
			delete(file);
			assertThrows(NoSuchFileException.class, () -> manager.createAndActivateNodeKeys(file, ALIAS, PASSWORD_PROTECTION));
			assertFalse(manager.hasNodeKeysActivated());
		} finally {
			deleteIfExists(file);
		}
	}

	@Test
	void testCreateAndActivateNodeKeysPrivateKeyX509CertificateArrayPasswordProtection() throws KeyManagementException {
		manager.startup();
		manager.createAndActivateNodeKeys(caPrivateKey, caCertificateChain, PASSWORD_PROTECTION);
		assertTrue(manager.hasNodeKeysActivated());
		assertEquals(nodeKeys.caPrivateKey(), manager.nodeCAPrivateKey());
		verify(keysRepository).saveNodeKeys(nodeKeys, PASSWORD_PROTECTION);
		verify(keysRepository).setEncryptionKeys(nodeKeys.encryptionPrivateKey(), nodeKeys.encryptionPublicKey());
	}

	@Test
	void testCreateAndActivateNodeKeysPrivateKeyX509CertificateArrayPasswordProtectionAlreadyExist() throws KeyManagementException {
		manager.startup();
		doThrow(new KeyAlreadyExistsException("test")).when(keysRepository).saveNodeKeys(nodeKeys, PASSWORD_PROTECTION);
		assertThrows(KeyAlreadyExistsException.class, () -> manager.createAndActivateNodeKeys(caPrivateKey, caCertificateChain, PASSWORD_PROTECTION));
		assertFalse(manager.hasNodeKeysActivated());
	}

	@Test
	void testCreateAndActivateNodeKeysPrivateKeyX509CertificateArrayPasswordProtectionDatabaseError() throws KeyManagementException {
		manager.startup();
		doThrow(new KeyManagementException("test")).when(keysRepository).saveNodeKeys(nodeKeys, PASSWORD_PROTECTION);
		assertThrows(KeyManagementException.class, () -> manager.createAndActivateNodeKeys(caPrivateKey, caCertificateChain, PASSWORD_PROTECTION));
		assertFalse(manager.hasNodeKeysActivated());
	}

	@Test
	void testCreateAndActivateNodeKeysPrivateKeyX509CertificateArrayPasswordProtectionGeneratorError() throws KeyManagementException {
		manager.startup();
		when(generator.generateNodeKeys(caPrivateKey, caCertificateChain)).thenThrow(new KeyManagementException("test"));
		assertThrows(KeyManagementException.class, () -> manager.createAndActivateNodeKeys(caPrivateKey, caCertificateChain, PASSWORD_PROTECTION));
		assertFalse(manager.hasNodeKeysActivated());
	}

	@Test
	void testCreateAndActivateNodeKeysPrivateKeyX509CertificateArrayPasswordProtectionInvalidNodeCA() throws GeneralCryptoLibException {
		manager.startup();
		when(asymmetricService.verifySignature(any(), any(), any(byte[].class))).thenReturn(false);
		assertThrows(InvalidNodeCAException.class, () -> manager.createAndActivateNodeKeys(caPrivateKey, caCertificateChain, PASSWORD_PROTECTION));
		assertFalse(manager.hasNodeKeysActivated());
	}

	@Test
	void testCreateCcrjReturnCodesKeys() throws KeyManagementException, GeneralCryptoLibException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		ElGamalEncryptionParameters parameters = ElGamalEncryptionParameters.fromJson(ENCRYPTION_PARAMETERS_JSON);
		CcrjReturnCodesKeysSpec spec = new CcrjReturnCodesKeysSpec.Builder().setElectionEventId(ELECTION_EVENT_ID)
				.setVerificationCardSetId(VERIFICATION_CARD_SET_ID).setParameters(parameters).setCcrjReturnCodesGenerationKeyLength(1)
				.setCcrjChoiceReturnCodesEncryptionKeyLength(1).build();
		manager.createCcrjReturnCodesKeys(spec);
		verify(keysRepository).loadElectionSigningKeys(ELECTION_EVENT_ID);
		verify(generator).generateCcrjReturnCodesKeys(spec, electionSigningKeys);
		verify(keysRepository).saveCcrjReturnCodesKeys(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, ccrjReturnCodesKeys);
	}

	@Test
	void testCreateCcrjReturnCodesKeysElectionSigningKeysAlreadyExist() throws KeyManagementException, GeneralCryptoLibException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		ElGamalEncryptionParameters parameters = ElGamalEncryptionParameters.fromJson(ENCRYPTION_PARAMETERS_JSON);
		CcrjReturnCodesKeysSpec spec = new CcrjReturnCodesKeysSpec.Builder().setElectionEventId(ELECTION_EVENT_ID)
				.setVerificationCardSetId(VERIFICATION_CARD_SET_ID).setParameters(parameters).setCcrjReturnCodesGenerationKeyLength(1)
				.setCcrjChoiceReturnCodesEncryptionKeyLength(1).build();
		doThrow(new KeyAlreadyExistsException("test")).when(keysRepository)
				.saveCcrjReturnCodesKeys(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, ccrjReturnCodesKeys);
		assertThrows(KeyAlreadyExistsException.class, () -> manager.createCcrjReturnCodesKeys(spec));
	}

	@Test
	void testCreateCcrjReturnCodesKeysElectionSigningKeysDatabaseError() throws KeyManagementException, GeneralCryptoLibException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		ElGamalEncryptionParameters parameters = ElGamalEncryptionParameters.fromJson(ENCRYPTION_PARAMETERS_JSON);
		CcrjReturnCodesKeysSpec spec = new CcrjReturnCodesKeysSpec.Builder().setElectionEventId(ELECTION_EVENT_ID)
				.setVerificationCardSetId(VERIFICATION_CARD_SET_ID).setParameters(parameters).setCcrjReturnCodesGenerationKeyLength(1)
				.setCcrjChoiceReturnCodesEncryptionKeyLength(1).build();
		doThrow(new KeyManagementException("test")).when(keysRepository)
				.saveCcrjReturnCodesKeys(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, ccrjReturnCodesKeys);
		assertThrows(KeyManagementException.class, () -> manager.createCcrjReturnCodesKeys(spec));
	}

	@Test
	void testCreateCcrjReturnCodesKeysElectionSigningKeysGeneratorError() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		ElGamalEncryptionParameters parameters = mock(ElGamalEncryptionParameters.class);
		CcrjReturnCodesKeysSpec spec = new CcrjReturnCodesKeysSpec.Builder().setElectionEventId(ELECTION_EVENT_ID)
				.setVerificationCardSetId(VERIFICATION_CARD_SET_ID).setParameters(parameters).setCcrjReturnCodesGenerationKeyLength(1)
				.setCcrjChoiceReturnCodesEncryptionKeyLength(1).build();
		when(generator.generateCcrjReturnCodesKeys(spec, electionSigningKeys)).thenThrow(new KeyManagementException("test"));
		assertThrows(KeyManagementException.class, () -> manager.createCcrjReturnCodesKeys(spec));
	}

	@Test
	void testCreateCcrjReturnCodesKeysElectionSigningKeysNotFound() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		ElGamalEncryptionParameters parameters = mock(ElGamalEncryptionParameters.class);
		CcrjReturnCodesKeysSpec spec = new CcrjReturnCodesKeysSpec.Builder().setElectionEventId(ELECTION_EVENT_ID)
				.setVerificationCardSetId(VERIFICATION_CARD_SET_ID).setParameters(parameters).setCcrjReturnCodesGenerationKeyLength(1)
				.setCcrjChoiceReturnCodesEncryptionKeyLength(1).build();
		when(keysRepository.loadElectionSigningKeys(ELECTION_EVENT_ID)).thenThrow(new KeyNotFoundException("test"));
		assertThrows(KeyNotFoundException.class, () -> manager.createCcrjReturnCodesKeys(spec));
	}

	@Test
	void testCreateElectionSigningKeysStringDateDate() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		Date validFrom = new Date();
		Date validTo = new Date(validFrom.getTime() + 1000);
		manager.createElectionSigningKeys(ELECTION_EVENT_ID, validFrom, validTo);
		verify(generator).generateElectionSigningKeys(ELECTION_EVENT_ID, validFrom, validTo, nodeKeys);
		verify(keysRepository).saveElectionSigningKeys(ELECTION_EVENT_ID, electionSigningKeys);
	}

	@Test
	void testCreateElectionSigningKeysStringDateDateAlreadyExist() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		doThrow(new KeyAlreadyExistsException("test")).when(keysRepository).saveElectionSigningKeys(ELECTION_EVENT_ID, electionSigningKeys);
		Date validFrom = new Date();
		Date validTo = new Date(validFrom.getTime() + 1000);
		assertThrows(KeyAlreadyExistsException.class, () -> manager.createElectionSigningKeys(ELECTION_EVENT_ID, validFrom, validTo));
	}

	@Test
	void testCreateElectionSigningKeysStringDateDateDatabaseError() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		doThrow(new KeyManagementException("test")).when(keysRepository).saveElectionSigningKeys(ELECTION_EVENT_ID, electionSigningKeys);
		Date validFrom = new Date();
		Date validTo = new Date(validFrom.getTime() + 1000);
		assertThrows(KeyManagementException.class, () -> manager.createElectionSigningKeys(ELECTION_EVENT_ID, validFrom, validTo));
	}

	@Test
	void testCreateElectionSigningKeysStringDateDateGeneratorError() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		Date validFrom = new Date();
		Date validTo = new Date(validFrom.getTime() + 1000);
		when(generator.generateElectionSigningKeys(ELECTION_EVENT_ID, validFrom, validTo, nodeKeys)).thenThrow(new KeyManagementException("test"));
		assertThrows(KeyManagementException.class, () -> manager.createElectionSigningKeys(ELECTION_EVENT_ID, validFrom, validTo));
	}

	@Test
	void testCreateElectionSigningKeysStringZonedDateTimeZonedDateTime() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		ZonedDateTime validFrom = ZonedDateTime.now();
		ZonedDateTime validTo = validFrom.plusSeconds(1);
		manager.createElectionSigningKeys(ELECTION_EVENT_ID, validFrom, validTo);
		verify(generator).generateElectionSigningKeys(ELECTION_EVENT_ID, Date.from(validFrom.toInstant()), Date.from(validTo.toInstant()), nodeKeys);
	}

	@Test
	void testCreateCcmjElectionKeys() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		ElGamalEncryptionParameters parameters = mock(ElGamalEncryptionParameters.class);
		CcmjElectionKeysSpec spec = new CcmjElectionKeysSpec.Builder().setElectionEventId(ELECTION_EVENT_ID)
				.setElectoralAuthorityId(ELECTORAL_AUTHORITY_ID).setParameters(parameters).setLength(1).build();
		manager.createCcmElectionKey(spec);
		verify(keysRepository).loadElectionSigningKeys(ELECTION_EVENT_ID);
		verify(generator).generateCcmjElectionKeys(spec, electionSigningKeys);
		verify(keysRepository).saveCcmjElectionKeys(ELECTION_EVENT_ID, ccmjElectionKeys);
	}

	@Test
	void testCreateCcmjElectionKeysAlreadyElectionSigningKeysNotFound() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		ElGamalEncryptionParameters parameters = mock(ElGamalEncryptionParameters.class);
		CcmjElectionKeysSpec spec = new CcmjElectionKeysSpec.Builder().setElectionEventId(ELECTION_EVENT_ID)
				.setElectoralAuthorityId(ELECTORAL_AUTHORITY_ID).setParameters(parameters).setLength(1).build();
		when(keysRepository.loadElectionSigningKeys(ELECTION_EVENT_ID)).thenThrow(new KeyNotFoundException("test"));
		assertThrows(KeyNotFoundException.class, () -> manager.createCcmElectionKey(spec));
	}

	@Test
	void testCreateCcmjElectionKeysAlreadyExist() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		ElGamalEncryptionParameters parameters = mock(ElGamalEncryptionParameters.class);
		CcmjElectionKeysSpec spec = new CcmjElectionKeysSpec.Builder().setElectionEventId(ELECTION_EVENT_ID)
				.setElectoralAuthorityId(ELECTORAL_AUTHORITY_ID).setParameters(parameters).setLength(1).build();
		doThrow(new KeyAlreadyExistsException("test")).when(keysRepository).saveCcmjElectionKeys(ELECTION_EVENT_ID, ccmjElectionKeys);
		assertThrows(KeyAlreadyExistsException.class, () -> manager.createCcmElectionKey(spec));
	}

	@Test
	void testCreateCcmjElectionKeysDatabaseError() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		ElGamalEncryptionParameters parameters = mock(ElGamalEncryptionParameters.class);
		CcmjElectionKeysSpec spec = new CcmjElectionKeysSpec.Builder().setElectionEventId(ELECTION_EVENT_ID)
				.setElectoralAuthorityId(ELECTORAL_AUTHORITY_ID).setParameters(parameters).setLength(1).build();
		doThrow(new KeyManagementException("test")).when(keysRepository).saveCcmjElectionKeys(ELECTION_EVENT_ID, ccmjElectionKeys);
		assertThrows(KeyManagementException.class, () -> manager.createCcmElectionKey(spec));
	}

	@Test
	void testCreateCcmjElectionKeysGeneratorError() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		ElGamalEncryptionParameters parameters = mock(ElGamalEncryptionParameters.class);
		CcmjElectionKeysSpec spec = new CcmjElectionKeysSpec.Builder().setElectionEventId(ELECTION_EVENT_ID)
				.setElectoralAuthorityId(ELECTORAL_AUTHORITY_ID).setParameters(parameters).setLength(1).build();
		when(generator.generateCcmjElectionKeys(spec, electionSigningKeys)).thenThrow(new KeyNotFoundException("test"));
		assertThrows(KeyManagementException.class, () -> manager.createCcmElectionKey(spec));
	}

	@Test
	void testElectionSigningKeysCaching() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(electionSigningKeys.privateKey(), manager.getElectionSigningPrivateKey(ELECTION_EVENT_ID));

		assertEquals(electionSigningKeys, cache.getElectionSigningKeys(ELECTION_EVENT_ID));

		assertEquals(electionSigningKeys.privateKey(), manager.getElectionSigningPrivateKey(ELECTION_EVENT_ID));

		verify(keysRepository, times(1)).loadElectionSigningKeys(ELECTION_EVENT_ID);
	}

	@Test
	void testGetCcrjChoiceReturnCodesEncryptionSecretKey() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(ccrjReturnCodesKeys.getCcrjChoiceReturnCodesEncryptionSecretKey(),
				manager.getCcrjChoiceReturnCodesEncryptionSecretKey(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID));
	}

	@Test
	void testGetCcrjChoiceReturnCodesEncryptionPublicKey() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(ccrjReturnCodesKeys.getCcrjChoiceReturnCodesEncryptionPublicKey(),
				manager.getCcrjChoiceReturnCodesEncryptionPublicKey(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID));
	}

	@Test
	void testGetCcrjChoiceReturnCodesEncryptionPublicKeySignature() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertArrayEquals(ccrjReturnCodesKeys.getCcrjChoiceReturnCodesEncryptionPublicKeySignature(),
				manager.getCcrjChoiceReturnCodesEncryptionPublicKeySignature(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID));
	}

	@Test
	void testGetEncryptionParameters() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(ccrjReturnCodesKeys.getEncryptionParameters(), manager.getEncryptionParameters(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID));
	}

	@Test
	void testGetCcrjReturnCodesGenerationSecretKey() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(ccrjReturnCodesKeys.getCcrjReturnCodesGenerationSecretKey(),
				manager.getCcrjReturnCodesGenerationSecretKey(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID));
	}

	@Test
	void testGetCcrjReturnCodesGenerationPublicKey() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(ccrjReturnCodesKeys.getCcrjReturnCodesGenerationPublicKey(),
				manager.getCcrjReturnCodesGenerationPublicKey(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID));
	}

	@Test
	void testGetCcrjReturnCodesGenerationPublicKeySignature() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertArrayEquals(ccrjReturnCodesKeys.getCcrjReturnCodesGenerationPublicKeySignature(),
				manager.getCcrjReturnCodesGenerationPublicKeySignature(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID));
	}

	@Test
	void testGetElectionSigningCertificate() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(electionSigningKeys.certificate(), manager.getElectionSigningCertificate(ELECTION_EVENT_ID));
	}

	@Test
	void testGetElectionSigningPrivateKey() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(electionSigningKeys.privateKey(), manager.getElectionSigningPrivateKey(ELECTION_EVENT_ID));
	}

	@Test
	void testGetElectionSigningPublicKey() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(electionSigningKeys.publicKey(), manager.getElectionSigningPublicKey(ELECTION_EVENT_ID));
	}

	@Test
	void testGetCcmjElectionSecretKey() throws KeyManagementException {
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(ccmjElectionKeys.getCcmjElectionSecretKey(), manager.getCcmjElectionSecretKey(ELECTION_EVENT_ID));
	}

	@Test
	void testGetCcmjElectionPublicKey() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(ccmjElectionKeys.getCcmjElectionPublicKey(), manager.getCcmjElectionPublicKey(ELECTION_EVENT_ID));
	}

	@Test
	void testGetCcmjElectionPublicKeySignature() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertArrayEquals(ccmjElectionKeys.getCcmjElectionPublicKeySignature(), manager.getCcmjElectionPublicKeySignature(ELECTION_EVENT_ID));
	}

	@Test
	void testGetPlatformCACertificate() throws Exception {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		X509Certificate platformCACertificate = nodeKeys.caCertificateChain()[nodeKeys.caCertificateChain().length - 1];
		assertEquals(platformCACertificate, manager.getPlatformCACertificate());
	}

	@Test
	void testHasValidElectionSigningKeysStringDateDate() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		Date notBefore = new Date();
		Date notAfter = new Date(notBefore.getTime() + 1000);
		X509Certificate certificate = electionSigningKeys.certificate();
		when(certificate.getNotBefore()).thenReturn(notBefore);
		when(certificate.getNotAfter()).thenReturn(notAfter);

		Date validFrom = notBefore;
		Date validTo = notAfter;
		assertTrue(manager.hasValidElectionSigningKeys(ELECTION_EVENT_ID, validFrom, validTo));

		validFrom = new Date(notBefore.getTime() - 1);
		// validTo = notAfter;
		assertFalse(manager.hasValidElectionSigningKeys(ELECTION_EVENT_ID, validFrom, validTo));

		validFrom = notBefore;
		validTo = new Date(notAfter.getTime() + 1);
		assertFalse(manager.hasValidElectionSigningKeys(ELECTION_EVENT_ID, validFrom, validTo));
	}

	@Test
	void testHasValidElectionSigningKeysStringZonedDateTimeZonedDateTime() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		Date notBefore = new Date();
		Date notAfter = new Date(notBefore.getTime() + 1000);
		X509Certificate certificate = electionSigningKeys.certificate();
		when(certificate.getNotBefore()).thenReturn(notBefore);
		when(certificate.getNotAfter()).thenReturn(notAfter);

		ZoneId zoneId = ZoneId.of("UTC");
		ZonedDateTime validFrom = ZonedDateTime.ofInstant(notBefore.toInstant(), zoneId);
		ZonedDateTime validTo = ZonedDateTime.ofInstant(notAfter.toInstant(), zoneId);
		assertTrue(manager.hasValidElectionSigningKeys(ELECTION_EVENT_ID, validFrom, validTo));

		validFrom = validFrom.minusSeconds(1);
		assertFalse(manager.hasValidElectionSigningKeys(ELECTION_EVENT_ID, validFrom, validTo));

		validFrom = validFrom.plusSeconds(1);
		validTo = validTo.plusSeconds(1);
		assertFalse(manager.hasValidElectionSigningKeys(ELECTION_EVENT_ID, validFrom, validTo));
	}

	@Test
	void testCcmjElectionKeysCaching() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(ccmjElectionKeys.getCcmjElectionSecretKey(), manager.getCcmjElectionSecretKey(ELECTION_EVENT_ID));

		assertEquals(ccmjElectionKeys, cache.getCcmjElectionKeys(ELECTION_EVENT_ID));

		assertEquals(ccmjElectionKeys.getCcmjElectionSecretKey(), manager.getCcmjElectionSecretKey(ELECTION_EVENT_ID));

		verify(keysRepository, times(1)).loadCcmjElectionKeys(ELECTION_EVENT_ID);
	}

	@Test
	void testNodeCACertificate() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(nodeKeys.caCertificate(), manager.nodeCACertificate());
	}

	@Test
	void testNodeCAPrivateKey() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(nodeKeys.caPrivateKey(), manager.nodeCAPrivateKey());
	}

	@Test
	void testNodeCAPublicKey() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(nodeKeys.caPublicKey(), manager.nodeCAPublicKey());
	}

	@Test
	void testNodeEncryptionCertificate() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(nodeKeys.encryptionCertificate(), manager.nodeEncryptionCertificate());
	}

	@Test
	void testNodeEncryptionPrivateKey() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(nodeKeys.encryptionPrivateKey(), manager.nodeEncryptionPrivateKey());
	}

	@Test
	void testNodeEncryptionPublicKey() throws KeyManagementException {
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(nodeKeys.encryptionPublicKey(), manager.nodeEncryptionPublicKey());
	}

	@Test
	void testNodeLogEncryptionCertificate() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(nodeKeys.logEncryptionCertificate(), manager.nodeLogEncryptionCertificate());
	}

	@Test
	void testNodeLogEncryptionPrivateKey() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(nodeKeys.logEncryptionPrivateKey(), manager.nodeLogEncryptionPrivateKey());
	}

	@Test
	void testNodeLogEncryptionPublicKey() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(nodeKeys.logEncryptionPublicKey(), manager.nodeLogEncryptionPublicKey());
	}

	@Test
	void testNodeLogSigningCertificate() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(nodeKeys.logSigningCertificate(), manager.nodeLogSigningCertificate());
	}

	@Test
	void testNodeLogSigningPrivateKey() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(nodeKeys.logSigningPrivateKey(), manager.nodeLogSigningPrivateKey());
	}

	@Test
	void testNodeLogSigningPublicKey() throws KeyManagementException {
		manager.startup();
		manager.activateNodeKeys(PASSWORD_PROTECTION);
		assertEquals(nodeKeys.logSigningPublicKey(), manager.nodeLogSigningPublicKey());
	}

	@Test
	void testShutdown() {
		cache.shutdown();
		cache = mock(Cache.class);
		manager = new KeysManager(asymmetricService, storesService, generator, keysRepository, cache, "nodeId");
		manager.startup();
		manager.shutdown();
		verify(cache).shutdown();
	}

	@Test
	void testStartup() {
		cache.shutdown();
		cache = mock(Cache.class);
		manager = new KeysManager(asymmetricService, storesService, generator, keysRepository, cache, "nodeId");
		manager.startup();
		verify(cache).startup();
	}

	private static class KeyStoreDouble extends KeyStore {
		public KeyStoreDouble(KeyStoreSpi keyStoreSpi) {
			super(keyStoreSpi, null, "PkCSS12");
		}
	}
}
