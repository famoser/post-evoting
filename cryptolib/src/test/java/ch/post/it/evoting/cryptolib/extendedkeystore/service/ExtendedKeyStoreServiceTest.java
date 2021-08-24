/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.extendedkeystore.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore.PasswordProtection;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.utils.AsymmetricTestDataGenerator;
import ch.post.it.evoting.cryptolib.certificates.utils.X509CertificateTestDataGenerator;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.utils.ElGamalTestDataGenerator;
import ch.post.it.evoting.cryptolib.extendedkeystore.cryptoapi.CryptoAPIExtendedKeyStore;
import ch.post.it.evoting.cryptolib.extendedkeystore.utils.SksTestDataGenerator;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.mathematical.groups.utils.MathematicalTestDataGenerator;
import ch.post.it.evoting.cryptolib.symmetric.utils.SymmetricTestDataGenerator;
import ch.post.it.evoting.cryptolib.test.tools.utils.CommonTestDataGenerator;

class ExtendedKeyStoreServiceTest {

	private static final String EXTENDED_KEY_STORE_PATH = "target/keystore.sks";
	private static final int NUM_ELGAMAL_KEY_ELEMENTS = 5;

	private static ExtendedKeyStoreService keyStoreService;
	private static CryptoAPIExtendedKeyStore extendedKeyStore;
	private static String rootPrivateKeyAlias;
	private static String leafPrivateKeyAlias;
	private static String secretKey1Alias;
	private static String secretKey2Alias;
	private static String elGamalPrivateKey1Alias;
	private static String elGamalPrivateKey2Alias;
	private static char[] privateKeyPassword;
	private static char[] secretKeyPassword;
	private static char[] elGamalPrivateKeyPassword;
	private static PrivateKey rootPrivateKey;
	private static Certificate rootCertificate;
	private static Certificate[] rootCertificateChain;
	private static PrivateKey leafPrivateKey;
	private static Certificate leafCertificate;
	private static Certificate[] leafCertificateChain;
	private static SecretKey secretKey1;
	private static SecretKey secretKey2;
	private static ElGamalPrivateKey elGamalPrivateKey1;
	private static ElGamalPrivateKey elGamalPrivateKey2;
	private static char[] extendedKeyStorePassword;
	private static CryptoAPIExtendedKeyStore loadedExtendedKeyStore;
	private static String extendedKeyStoreAsJson;
	private static String aliasWithUnderscoreChar;
	private static String aliasWithHyphenChar;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException, IOException {

		keyStoreService = new ExtendedKeyStoreService();

		extendedKeyStore = keyStoreService.createKeyStore();

		rootPrivateKeyAlias = SksTestDataGenerator.getAlias();
		do {
			leafPrivateKeyAlias = SksTestDataGenerator.getAlias();
		} while (leafPrivateKeyAlias.equals(rootPrivateKeyAlias));

		secretKey1Alias = SksTestDataGenerator.getAlias();
		do {
			secretKey2Alias = SksTestDataGenerator.getAlias();
		} while (secretKey2Alias.equals(secretKey1Alias));

		elGamalPrivateKey1Alias = SksTestDataGenerator.getAlias();
		do {
			elGamalPrivateKey2Alias = SksTestDataGenerator.getAlias();
		} while (elGamalPrivateKey2Alias.equals(elGamalPrivateKey1Alias));

		privateKeyPassword = SksTestDataGenerator.getPassword();
		secretKeyPassword = SksTestDataGenerator.getPassword();
		elGamalPrivateKeyPassword = SksTestDataGenerator.getPassword();

		KeyPair rootKeyPair = AsymmetricTestDataGenerator.getKeyPairForSigning();
		rootPrivateKey = rootKeyPair.getPrivate();
		rootCertificate = X509CertificateTestDataGenerator.getRootAuthorityX509Certificate(rootKeyPair).getCertificate();
		rootCertificateChain = new Certificate[1];
		rootCertificateChain[0] = rootCertificate;

		KeyPair leafKeyPair = AsymmetricTestDataGenerator.getKeyPairForSigning();
		leafCertificate = X509CertificateTestDataGenerator.getSignX509Certificate(leafKeyPair, rootKeyPair).getCertificate();
		leafPrivateKey = leafKeyPair.getPrivate();
		leafCertificateChain = new Certificate[2];
		leafCertificateChain[0] = leafCertificate;
		leafCertificateChain[1] = rootCertificate;

		secretKey1 = SymmetricTestDataGenerator.getSecretKeyForEncryption();
		secretKey2 = SymmetricTestDataGenerator.getSecretKeyForEncryption();

		ZpSubgroup group = MathematicalTestDataGenerator.getQrSubgroup();
		elGamalPrivateKey1 = ElGamalTestDataGenerator.getKeyPair(group, NUM_ELGAMAL_KEY_ELEMENTS).getPrivateKeys();
		elGamalPrivateKey2 = ElGamalTestDataGenerator.getKeyPair(group, NUM_ELGAMAL_KEY_ELEMENTS).getPrivateKeys();

		extendedKeyStore.setPrivateKeyEntry(rootPrivateKeyAlias, rootPrivateKey, privateKeyPassword, rootCertificateChain);
		extendedKeyStore.setPrivateKeyEntry(leafPrivateKeyAlias, leafPrivateKey, privateKeyPassword, leafCertificateChain);

		extendedKeyStore.setSecretKeyEntry(secretKey1Alias, secretKey1, secretKeyPassword);
		extendedKeyStore.setSecretKeyEntry(secretKey2Alias, secretKey2, secretKeyPassword);

		extendedKeyStore.setElGamalPrivateKeyEntry(elGamalPrivateKey1Alias, elGamalPrivateKey1, elGamalPrivateKeyPassword);
		extendedKeyStore.setElGamalPrivateKeyEntry(elGamalPrivateKey2Alias, elGamalPrivateKey2, elGamalPrivateKeyPassword);

		extendedKeyStorePassword = SksTestDataGenerator.getPassword();

		try (OutputStream outStream = new FileOutputStream(EXTENDED_KEY_STORE_PATH)) {
			extendedKeyStore.store(outStream, extendedKeyStorePassword);
		} catch (IOException e) {
			throw new GeneralCryptoLibException("Could not store Extended key store to path " + EXTENDED_KEY_STORE_PATH, e);
		}

		loadedExtendedKeyStore = keyStoreService.loadKeyStore(Paths.get(EXTENDED_KEY_STORE_PATH), extendedKeyStorePassword);

		extendedKeyStoreAsJson = extendedKeyStore.toJSON(extendedKeyStorePassword);

		int index = CommonTestDataGenerator.getInt(0, (leafPrivateKeyAlias.length() - 1));
		StringBuilder builder = new StringBuilder(leafPrivateKeyAlias);
		builder.setCharAt(index, '_');
		aliasWithUnderscoreChar = builder.toString();
		builder = new StringBuilder(leafPrivateKeyAlias);
		builder.setCharAt(index, '-');
		aliasWithHyphenChar = builder.toString();
	}

	@Test
	void whenSetAndGetRootPrivateKeyFromKeyStoreThenOk() throws GeneralCryptoLibException {
		PrivateKey privateKey = extendedKeyStore.getPrivateKeyEntry(rootPrivateKeyAlias, privateKeyPassword);

		assertArrayEquals(privateKey.getEncoded(), rootPrivateKey.getEncoded());
	}

	@Test
	void whenSetAndGetRootCertificateChainFromKeyStoreThenOk() throws GeneralCryptoLibException, CertificateException {
		Certificate[] certificateChain = extendedKeyStore.getCertificateChain(rootPrivateKeyAlias);

		Certificate certificate = certificateChain[0];

		assertArrayEquals(certificate.getEncoded(), rootCertificate.getEncoded());
	}

	@Test
	void whenSetAndGetLeafPrivateKeyFromKeyStoreThenOk() throws GeneralCryptoLibException {
		PrivateKey privateKey = extendedKeyStore.getPrivateKeyEntry(leafPrivateKeyAlias, privateKeyPassword);

		assertArrayEquals(privateKey.getEncoded(), leafPrivateKey.getEncoded());
	}

	@Test
	void whenSetAndGetLeafCertificateChainFromKeyStoreThenOk() throws GeneralCryptoLibException, CertificateException {
		Certificate[] certificateChain = extendedKeyStore.getCertificateChain(leafPrivateKeyAlias);

		assertArrayEquals(certificateChain[0].getEncoded(), leafCertificate.getEncoded());
		assertArrayEquals(certificateChain[1].getEncoded(), rootCertificate.getEncoded());
	}

	@Test
	void whenSetAndGetSecretKeysForEncryptionFromKeyStoreThenOk() throws GeneralCryptoLibException {
		SecretKey secretKey = extendedKeyStore.getSecretKeyEntry(secretKey1Alias, secretKeyPassword);

		assertArrayEquals(secretKey.getEncoded(), secretKey1.getEncoded());

		secretKey = extendedKeyStore.getSecretKeyEntry(secretKey2Alias, secretKeyPassword);

		assertArrayEquals(secretKey.getEncoded(), secretKey2.getEncoded());
	}

	@Test
	void whenSetAndGetElGamalPrivateKeysFromKeyStoreThenOk() throws GeneralCryptoLibException {
		ElGamalPrivateKey elGamalPrivateKey = extendedKeyStore.getElGamalPrivateKeyEntry(elGamalPrivateKey1Alias, elGamalPrivateKeyPassword);

		assertEquals(elGamalPrivateKey, elGamalPrivateKey1);

		elGamalPrivateKey = extendedKeyStore.getElGamalPrivateKeyEntry(elGamalPrivateKey2Alias, elGamalPrivateKeyPassword);

		assertEquals(elGamalPrivateKey, elGamalPrivateKey2);
	}

	@Test
	void whenPrivateKeyGetAliasesFromKeyStoreThenOk() {
		List<String> privateKeyAliases = extendedKeyStore.getPrivateKeyAliases();

		assertEquals(privateKeyAliases.get(0), rootPrivateKeyAlias);
		assertEquals(privateKeyAliases.get(1), leafPrivateKeyAlias);
	}

	@Test
	void whenGetSecretAliasesFromKeyStoreThenOk() {
		List<String> secretKeyAliases = extendedKeyStore.getSecretKeyAliases();

		assertEquals(secretKeyAliases.get(0), secretKey1Alias);
		assertEquals(secretKeyAliases.get(1), secretKey2Alias);
	}

	@Test
	void whenGetElGamalPrivateKeyAliasesFromKeyStoreThenOk() {
		List<String> elGamalPrivateKeyAliases = extendedKeyStore.getElGamalPrivateKeyAliases();

		assertEquals(elGamalPrivateKeyAliases.get(0), elGamalPrivateKey1Alias);
		assertEquals(elGamalPrivateKeyAliases.get(1), elGamalPrivateKey2Alias);
	}

	@Test
	void whenSetKeyEntryUsingAliasWithUnderscoreCharThenOk() {
		assertAll(() -> assertDoesNotThrow(
				() -> extendedKeyStore.setPrivateKeyEntry(aliasWithUnderscoreChar, rootPrivateKey, privateKeyPassword, rootCertificateChain)),
				() -> assertDoesNotThrow(
						() -> extendedKeyStore.setPrivateKeyEntry(aliasWithUnderscoreChar, leafPrivateKey, privateKeyPassword, leafCertificateChain)),

				() -> assertDoesNotThrow(() -> extendedKeyStore.setSecretKeyEntry(aliasWithUnderscoreChar, secretKey1, secretKeyPassword)),
				() -> assertDoesNotThrow(() -> extendedKeyStore.setSecretKeyEntry(aliasWithUnderscoreChar, secretKey2, secretKeyPassword)),

				() -> assertDoesNotThrow(
						() -> extendedKeyStore.setElGamalPrivateKeyEntry(aliasWithUnderscoreChar, elGamalPrivateKey1, elGamalPrivateKeyPassword)),
				() -> assertDoesNotThrow(
						() -> extendedKeyStore.setElGamalPrivateKeyEntry(aliasWithUnderscoreChar, elGamalPrivateKey2, elGamalPrivateKeyPassword)));
	}

	@Test
	void whenSetKeyEntryUsingAliasWithHyphenCharThenOk() {
		assertAll(() -> assertDoesNotThrow(
				() -> extendedKeyStore.setPrivateKeyEntry(aliasWithHyphenChar, leafPrivateKey, privateKeyPassword, rootCertificateChain)),
				() -> assertDoesNotThrow(() -> extendedKeyStore.setSecretKeyEntry(aliasWithHyphenChar, secretKey1, secretKeyPassword)),
				() -> assertDoesNotThrow(
						() -> extendedKeyStore.setElGamalPrivateKeyEntry(aliasWithHyphenChar, elGamalPrivateKey1, elGamalPrivateKeyPassword)));
	}

	@Test
	void whenStoreAndLoadKeyStoreThenOk() throws GeneralCryptoLibException {
		PrivateKey privateKey = loadedExtendedKeyStore.getPrivateKeyEntry(rootPrivateKeyAlias, privateKeyPassword);
		assertArrayEquals(privateKey.getEncoded(), rootPrivateKey.getEncoded());
		privateKey = loadedExtendedKeyStore.getPrivateKeyEntry(leafPrivateKeyAlias, privateKeyPassword);
		assertArrayEquals(privateKey.getEncoded(), leafPrivateKey.getEncoded());

		SecretKey secretKey = loadedExtendedKeyStore.getSecretKeyEntry(secretKey1Alias, secretKeyPassword);
		assertArrayEquals(secretKey.getEncoded(), secretKey1.getEncoded());
		secretKey = loadedExtendedKeyStore.getSecretKeyEntry(secretKey2Alias, secretKeyPassword);
		assertArrayEquals(secretKey.getEncoded(), secretKey2.getEncoded());

		ElGamalPrivateKey elGamalPrivateKey = loadedExtendedKeyStore.getElGamalPrivateKeyEntry(elGamalPrivateKey1Alias, elGamalPrivateKeyPassword);
		assertEquals(elGamalPrivateKey, elGamalPrivateKey1);
		elGamalPrivateKey = loadedExtendedKeyStore.getElGamalPrivateKeyEntry(elGamalPrivateKey2Alias, elGamalPrivateKeyPassword);
		assertEquals(elGamalPrivateKey, elGamalPrivateKey2);
	}

	@Test
	void whenSerializeAndDeserializeKeyStoreThenOk() throws GeneralCryptoLibException, IOException {
		CryptoAPIExtendedKeyStore deserializedExtendedKeyStore;
		try (InputStream inStream = new ByteArrayInputStream(extendedKeyStoreAsJson.getBytes(StandardCharsets.UTF_8))) {
			deserializedExtendedKeyStore = keyStoreService.loadKeyStoreFromJSON(inStream, new PasswordProtection(extendedKeyStorePassword));
		}

		PrivateKey privateKey = deserializedExtendedKeyStore.getPrivateKeyEntry(rootPrivateKeyAlias, privateKeyPassword);
		assertArrayEquals(privateKey.getEncoded(), rootPrivateKey.getEncoded());
		privateKey = deserializedExtendedKeyStore.getPrivateKeyEntry(leafPrivateKeyAlias, privateKeyPassword);
		assertArrayEquals(privateKey.getEncoded(), leafPrivateKey.getEncoded());

		SecretKey secretKey = deserializedExtendedKeyStore.getSecretKeyEntry(secretKey1Alias, secretKeyPassword);
		assertArrayEquals(secretKey.getEncoded(), secretKey1.getEncoded());
		secretKey = deserializedExtendedKeyStore.getSecretKeyEntry(secretKey2Alias, secretKeyPassword);
		assertArrayEquals(secretKey.getEncoded(), secretKey2.getEncoded());

		ElGamalPrivateKey elGamalPrivateKey = deserializedExtendedKeyStore
				.getElGamalPrivateKeyEntry(elGamalPrivateKey1Alias, elGamalPrivateKeyPassword);
		assertEquals(elGamalPrivateKey, elGamalPrivateKey1);
		elGamalPrivateKey = deserializedExtendedKeyStore.getElGamalPrivateKeyEntry(elGamalPrivateKey2Alias, elGamalPrivateKeyPassword);
		assertEquals(elGamalPrivateKey, elGamalPrivateKey2);
	}
}
