/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.asymmetric.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.asymmetric.utils.KeyPairConverterAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.keypair.configuration.ConfigSigningKeyPairAlgorithmAndSpec;
import ch.post.it.evoting.cryptolib.commons.configuration.PolicyFromPropertiesHelper;
import ch.post.it.evoting.cryptolib.primitives.primes.utils.PrimitivesTestDataGenerator;
import ch.post.it.evoting.cryptolib.primitives.securerandom.constants.SecureRandomConstants;
import ch.post.it.evoting.cryptolib.test.tools.utils.CommonTestDataGenerator;

/**
 * Tests of the asymmetric service API.
 */
class AsymmetricServiceTest {

	private static final int MAXIMUM_DATA_ARRAY_LENGTH = 10;

	private static PolicyFromPropertiesHelper defaultPolicy;

	private static AsymmetricService asymmetricServiceForDefaultPolicy;

	private static PublicKey publicKeyForEncryption;

	private static PrivateKey privateKeyForDecryption;

	private static PublicKey publicKeyForVerification;

	private static PrivateKey privateKeyForSigning;

	private static int dataByteLength;

	private static byte[] data;

	private static KeyPairConverterAPI keyPairConverter;

	@BeforeAll
	public static void setUp() throws GeneralCryptoLibException {

		Properties properties = new Properties();
		properties.setProperty("asymmetric.signingkeypair", "RSA_2048_F4_SUN_RSA_SIGN");
		properties.setProperty("asymmetric.encryptionkeypair", "RSA_2048_F4_SUN_RSA_SIGN");
		defaultPolicy = new PolicyFromPropertiesHelper(properties);

		asymmetricServiceForDefaultPolicy = new AsymmetricService();

		KeyPair keyPairForEncryption = asymmetricServiceForDefaultPolicy.getKeyPairForEncryption();
		publicKeyForEncryption = keyPairForEncryption.getPublic();
		privateKeyForDecryption = keyPairForEncryption.getPrivate();

		KeyPair keyPairForSigning = asymmetricServiceForDefaultPolicy.getKeyPairForSigning();
		publicKeyForVerification = keyPairForSigning.getPublic();
		privateKeyForSigning = keyPairForSigning.getPrivate();

		dataByteLength = CommonTestDataGenerator.getInt(1, SecureRandomConstants.MAXIMUM_GENERATED_BYTE_ARRAY_LENGTH);

		data = PrimitivesTestDataGenerator.getByteArray(dataByteLength);

		keyPairConverter = asymmetricServiceForDefaultPolicy.getKeyPairConverter();
	}

	@Test
	void testWhenCreateEncryptionCryptoKeyPairThenExpectedAlgorithm() {

		String publicKeyAlgorithm = publicKeyForEncryption.getAlgorithm();
		ConfigSigningKeyPairAlgorithmAndSpec encryptionKeyPairAlgorithmAndSpec = ConfigSigningKeyPairAlgorithmAndSpec
				.valueOf(defaultPolicy.getPropertyValue("asymmetric.encryptionkeypair"));

		Assertions.assertEquals(encryptionKeyPairAlgorithmAndSpec.getAlgorithm(), publicKeyAlgorithm, "The algorithm was not the expected one.");
	}

	@Test
	void testWhenCreateSigningCryptoKeyPairThenExpectedAlgorithm() {

		String publicKeyAlgorithm = publicKeyForVerification.getAlgorithm();
		ConfigSigningKeyPairAlgorithmAndSpec signingKeyPairAlgorithmAndSpec = ConfigSigningKeyPairAlgorithmAndSpec
				.valueOf(defaultPolicy.getPropertyValue("asymmetric.signingkeypair"));

		Assertions.assertEquals(signingKeyPairAlgorithmAndSpec.getAlgorithm(), publicKeyAlgorithm, "The algorithm was not the expected one.");
	}

	@Test
	void testWhenAsymmetricallyEncryptAndDecryptThenOk() throws GeneralCryptoLibException {

		byte[] encryptedData = asymmetricServiceForDefaultPolicy.encrypt(publicKeyForEncryption, data);

		byte[] decryptedData = asymmetricServiceForDefaultPolicy.decrypt(privateKeyForDecryption, encryptedData);

		Assertions.assertArrayEquals(decryptedData, data);
	}

	@Test
	void testWhenSignAndVerifySignatureThenOk() throws GeneralCryptoLibException {

		byte[] signature = asymmetricServiceForDefaultPolicy.sign(privateKeyForSigning, data);

		boolean verified = asymmetricServiceForDefaultPolicy.verifySignature(signature, publicKeyForVerification, data);

		Assertions.assertTrue(verified);
	}

	@Test
	void testWhenSignAndVerifySignatureForMultipleDataElementsThenOk() throws GeneralCryptoLibException {

		byte[][] dataArray = PrimitivesTestDataGenerator
				.getByteArrayArray(dataByteLength, CommonTestDataGenerator.getInt(2, MAXIMUM_DATA_ARRAY_LENGTH));

		byte[] signature = asymmetricServiceForDefaultPolicy.sign(privateKeyForSigning, dataArray);

		boolean verified = asymmetricServiceForDefaultPolicy.verifySignature(signature, publicKeyForVerification, dataArray);

		Assertions.assertTrue(verified);
	}

	@Test
	void testWhenSignAndVerifySignatureForDataInputStreamThenOk() throws GeneralCryptoLibException, IOException {

		boolean verified;
		try (InputStream dataInputStream = new ByteArrayInputStream(data)) {

			byte[] signature = asymmetricServiceForDefaultPolicy.sign(privateKeyForSigning, dataInputStream);

			dataInputStream.reset();

			verified = asymmetricServiceForDefaultPolicy.verifySignature(signature, publicKeyForVerification, dataInputStream);
		}

		Assertions.assertTrue(verified);
	}

	@Test
	void testWhenConvertKeyPairForEncryptionToAndFromPemThenOk() throws GeneralCryptoLibException {

		String publicKeyPem = keyPairConverter.exportPublicKeyForEncryptingToPem(publicKeyForEncryption);
		PublicKey publicKey = keyPairConverter.getPublicKeyForEncryptingFromPem(publicKeyPem);
		Assertions.assertArrayEquals(publicKey.getEncoded(), publicKeyForEncryption.getEncoded());

		String privateKeyPem = keyPairConverter.exportPrivateKeyForEncryptingToPem(privateKeyForDecryption);
		PrivateKey privateKey = keyPairConverter.getPrivateKeyForEncryptingFromPem(privateKeyPem);
		Assertions.assertArrayEquals(privateKey.getEncoded(), privateKeyForDecryption.getEncoded());
	}

	@Test
	void testWhenConvertKeyPairForSigningToAndFromPemThenOk() throws GeneralCryptoLibException {

		String publicKeyPem = keyPairConverter.exportPublicKeyForEncryptingToPem(publicKeyForVerification);
		PublicKey publicKey = keyPairConverter.getPublicKeyForEncryptingFromPem(publicKeyPem);
		Assertions.assertArrayEquals(publicKey.getEncoded(), publicKeyForVerification.getEncoded());

		String privateKeyPem = keyPairConverter.exportPrivateKeyForEncryptingToPem(privateKeyForSigning);
		PrivateKey privateKey = keyPairConverter.getPrivateKeyForEncryptingFromPem(privateKeyPem);
		Assertions.assertArrayEquals(privateKey.getEncoded(), privateKeyForSigning.getEncoded());
	}
}
