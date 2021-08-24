/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore.PasswordProtection;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.controlcomponents.commons.keymanagement.exception.InvalidPasswordException;
import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.certificates.CertificatesServiceAPI;
import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.primitives.PrimitivesServiceAPI;
import ch.post.it.evoting.cryptolib.api.stores.StoresServiceAPI;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.certificates.bean.RootCertificateData;
import ch.post.it.evoting.cryptolib.certificates.bean.ValidityDates;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.service.CertificatesService;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;
import ch.post.it.evoting.cryptolib.primitives.service.PrimitivesService;
import ch.post.it.evoting.cryptolib.stores.service.StoresService;

/**
 * Tests of {@link Codec}.
 */
class CodecTest {
	private static final String ENCRYPTION_PARAMETERS_JSON = "{\"encryptionParams\":{\"p\":\"AMoeTsDNAOCZ1I+tv3/xjI/HVT8Ab3h8Ifm/d1623VoBQaL8mzFgtUerEiw/FcjyUnjwvu0cc2kPR6uM4eMgK6c7YXE/LSJ2BDgNMVLUgIIC+qVZLiEG/yPZAT+8akHS0Audz7XIxBnd1EzqsnkIFjsgtER3iVWpC7BTxJ1sT19pB+OaA2b+L6nVW0sZNwiHFCvvQ5rLTEc4uDEBcT6aNIXCK4Qkztc1hTfccn3k6158NNGMddvBB5GzqBoQMzjW35mKUBXOufRhQTYVQjNSoapfVfVKDsTkD1BDvdffJC/gP82m+/kud0x0p6RWfmfHgFoNmG6EjJkoMGfGLVJAMVs=\",\"q\":\"ZQ8nYGaAcEzqR9bfv/jGR+Oqn4A3vD4Q/N+7r1turQCg0X5NmLBao9WJFh+K5HkpPHhfdo45tIej1cZw8ZAV052wuJ+WkTsCHAaYqWpAQQF9UqyXEIN/keyAn941IOloBc7n2uRiDO7qJnVZPIQLHZBaIjvEqtSF2CniTrYnr7SD8c0Bs38X1OqtpYybhEOKFfehzWWmI5xcGIC4n00aQuEVwhJna5rCm+45PvJ1rz4aaMY67eCDyNnUDQgZnGtvzMUoCudc+jCgmwqhGalQ1S+q+qUHYnIHqCHe6++SF/Af5tN9/Jc7pjpT0is/M+PALQbMN0JGTJQYM+MWqSAYrQ==\",\"g\":\"Aw==\"}}";
	private static ElGamalServiceAPI elGamalService;
	private static Generator generator;
	private static Codec codec;
	private static NodeKeys nodeKeys;
	private static ElGamalEncryptionParameters parameters;
	private PasswordProtection pwd = new PasswordProtection("pwdpwd".toCharArray());

	@BeforeAll
	public static void beforeClass() throws GeneralCryptoLibException, KeyManagementException {

		StoresServiceAPI storesService = new StoresService();
		AsymmetricServiceAPI asymmetricService = new AsymmetricService();
		CertificatesServiceAPI certificatesService = new CertificatesService();
		elGamalService = new ElGamalService();
		PrimitivesServiceAPI primitivesService = new PrimitivesService();

		KeyPair pair = asymmetricService.getKeyPairForSigning();

		PrivateKey caPrivateKey = pair.getPrivate();

		RootCertificateData certificateData = new RootCertificateData();
		certificateData.setSubjectDn(new X509DistinguishedName.Builder("CA", "ES").build());
		certificateData.setSubjectPublicKey(pair.getPublic());
		certificateData.setValidityDates(new ValidityDates(new Date(), new Date(System.currentTimeMillis() + 1000)));
		X509Certificate certificate = certificatesService.createRootAuthorityX509Certificate(certificateData, caPrivateKey).getCertificate();

		X509Certificate[] caCertificateChain = new X509Certificate[] { certificate };

		codec = new Codec(storesService, asymmetricService);

		generator = new Generator(asymmetricService, certificatesService, elGamalService, primitivesService, codec, "nodeId");
		nodeKeys = generator.generateNodeKeys(caPrivateKey, caCertificateChain);

		parameters = ElGamalEncryptionParameters.fromJson(ENCRYPTION_PARAMETERS_JSON);
	}

	@BeforeEach
	public void initiatePassword() {
		pwd = new PasswordProtection("password".toCharArray());
	}

	@Test
	void testDecodeElectionSigningKeys() throws KeyManagementException {
		ElectionSigningKeys electionSigningKeys = generator
				.generateElectionSigningKeys("electionEventId", new Date(), new Date(System.currentTimeMillis() + 1000), nodeKeys);

		byte[] bytes = codec.encodeElectionSigningKeys(electionSigningKeys, pwd);
		ElectionSigningKeys electionSigningKeys2 = codec.decodeElectionSigningKeys(bytes, pwd);

		assertEquals(electionSigningKeys.privateKey(), electionSigningKeys2.privateKey());
		assertArrayEquals(electionSigningKeys.certificateChain(), electionSigningKeys2.certificateChain());
	}

	@Test
	void testDecodeElectionSigningKeysInvalidBytes() {
		byte[] bytes = { 1, 2, 3 };

		assertThrows(KeyManagementException.class, () -> codec.decodeElectionSigningKeys(bytes, pwd));
	}

	@Test
	void testDecodeElectionSigningKeysInvalidPassword() throws KeyManagementException {
		ElectionSigningKeys electionSigningKeys = generator
				.generateElectionSigningKeys("electionEventId", new Date(), new Date(System.currentTimeMillis() + 1000), nodeKeys);

		byte[] bytes = codec.encodeElectionSigningKeys(electionSigningKeys, pwd);

		assertThrows(InvalidPasswordException.class, () -> codec.decodeElectionSigningKeys(bytes, new PasswordProtection("pwdpwd2".toCharArray())));
	}

	@Test
	void testDecodeElGamalPrivateKey() throws GeneralCryptoLibException, KeyManagementException {
		ElGamalKeyPair pair = elGamalService.generateKeyPair(parameters, 3);
		ElGamalPrivateKey key = pair.getPrivateKeys();
		byte[] bytes = codec.encodeElGamalPrivateKey(key, nodeKeys.encryptionPublicKey());
		ElGamalPrivateKey key2 = codec.decodeElGamalPrivateKey(bytes, nodeKeys.encryptionPrivateKey());

		assertEquals(key, key2);
	}

	@Test
	void testDecodeElGamalPrivateKeyInvalidBytes() {
		assertThrows(KeyManagementException.class, () -> codec.decodeElGamalPrivateKey(new byte[257], nodeKeys.encryptionPrivateKey()));
	}

	@Test
	void testDecodeElGamalPublicKey() throws GeneralCryptoLibException, KeyManagementException {
		ElGamalKeyPair pair = elGamalService.generateKeyPair(parameters, 3);
		ElGamalPublicKey key = pair.getPublicKeys();
		byte[] bytes = codec.encodeElGamalPublicKey(key);
		ElGamalPublicKey key2 = codec.decodeElGamalPublicKey(bytes);

		assertEquals(key, key2);
	}

	@Test
	void testDecodeElGamalPublicKeyInvalidBytes() {
		byte[] bytes = { 1, 2, 3, 4 };

		assertThrows(KeyManagementException.class, () -> codec.decodeElGamalPublicKey(bytes));
	}

	@Test
	void testDecodeNodeKeys() throws KeyManagementException {
		byte[] bytes = codec.encodeNodeKeys(nodeKeys, pwd);
		NodeKeys nodeKeys2 = codec.decodeNodeKeys(bytes, pwd);

		assertEquals(nodeKeys.caPrivateKey(), nodeKeys2.caPrivateKey());
		assertArrayEquals(nodeKeys.caCertificateChain(), nodeKeys2.caCertificateChain());
		assertEquals(nodeKeys.encryptionPrivateKey(), nodeKeys2.encryptionPrivateKey());
		assertArrayEquals(nodeKeys.encryptionCertificateChain(), nodeKeys2.encryptionCertificateChain());
		assertEquals(nodeKeys.logEncryptionPrivateKey(), nodeKeys2.logEncryptionPrivateKey());
		assertArrayEquals(nodeKeys.logEncryptionCertificateChain(), nodeKeys2.logEncryptionCertificateChain());
		assertEquals(nodeKeys.logSigningPrivateKey(), nodeKeys2.logSigningPrivateKey());
		assertArrayEquals(nodeKeys.logSigningCertificateChain(), nodeKeys2.logSigningCertificateChain());
	}

	@Test
	void testDecodeNodeKeysInvalidBytes() {
		byte[] bytes = { 1, 2, 3 };

		assertThrows(KeyManagementException.class, () -> codec.decodeNodeKeys(bytes, pwd));
	}

	@Test
	void testDecodeNodeKeysInvalidPassword() throws KeyManagementException {
		byte[] bytes = codec.encodeNodeKeys(nodeKeys, pwd);

		assertThrows(InvalidPasswordException.class, () -> codec.decodeNodeKeys(bytes, new PasswordProtection("pwdpwd2".toCharArray())));
	}

	@Test
	void testDecodePassword() throws KeyManagementException {
		char[] password = pwd.getPassword();
		char[] guardedPassword = new char[password.length];
		System.arraycopy(password, 0, guardedPassword, 0, password.length);
		byte[] bytes = codec.encodePassword(pwd, nodeKeys.encryptionPublicKey());
		PasswordProtection password2 = codec.decodePassword(bytes, nodeKeys.encryptionPrivateKey());

		assertArrayEquals(guardedPassword, password2.getPassword());
	}

	@Test
	void testDecodePasswordInvalidBytes() {
		byte[] bytes = new byte[257];

		assertThrows(KeyManagementException.class, () -> codec.decodePassword(bytes, nodeKeys.encryptionPrivateKey()));
	}
}
