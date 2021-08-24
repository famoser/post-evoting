/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncrypterValues;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.VerifiableElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.CryptoAPIElGamalDecrypter;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.CryptoAPIElGamalEncrypter;
import ch.post.it.evoting.cryptolib.elgamal.utils.ElGamalTestDataGenerator;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.mathematical.groups.utils.MathematicalTestDataGenerator;
import ch.post.it.evoting.cryptolib.test.tools.utils.CommonTestDataGenerator;

class ElGamalServiceTest {

	private static final int MIN_NUM_TEST_ELEMENTS = 3;
	private static final int MAX_NUM_TEST_ELEMENTS = 10;

	private static ElGamalEncryptionParameters encryptionParameters;
	private static int numElements;
	private static ElGamalPublicKey publicKey;
	private static ElGamalPrivateKey privateKey;
	private static CryptoAPIElGamalEncrypter encrypter;
	private static CryptoAPIElGamalDecrypter decrypter;
	private static List<ZpGroupElement> plaintext;
	private static List<String> plaintextAsStrings;
	private static ElGamalCiphertext ciphertext;
	private static ElGamalEncrypterValues preComputedValues;
	private static CryptoAPIElGamalEncrypter encrypterForShortExponents;
	private static CryptoAPIElGamalDecrypter decrypterForShortExponents;
	private static List<ZpGroupElement> plaintextForShortExponents;
	private static ElGamalEncrypterValues preComputedValuesFromShortExponent;
	private static List<ZpGroupElement> plaintextWithLessElements;
	private static ElGamalCiphertext ciphertextWithLessElements;
	private static List<String> plaintextAsStringsWithLessElements;
	private static List<ZpGroupElement> plaintextForShortExponentsWithLessElements;
	private static ElGamalService elGamalService;
	private static ZpSubgroup qr2048Group;
	private static ZpSubgroup invalidGroup;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException, IOException {

		elGamalService = new ElGamalService();

		ZpSubgroup zpSubgroup = MathematicalTestDataGenerator.getQrSubgroup();
		encryptionParameters = ElGamalTestDataGenerator.getElGamalEncryptionParameters(zpSubgroup);

		numElements = CommonTestDataGenerator.getInt(MIN_NUM_TEST_ELEMENTS, MAX_NUM_TEST_ELEMENTS);

		ElGamalKeyPair elGamalKeyPair = elGamalService.generateKeyPair(encryptionParameters, numElements);
		publicKey = elGamalKeyPair.getPublicKeys();
		privateKey = elGamalKeyPair.getPrivateKeys();

		encrypter = elGamalService.createEncrypter(publicKey);
		decrypter = elGamalService.createDecrypter(privateKey);

		plaintext = MathematicalTestDataGenerator.getZpGroupElements(zpSubgroup, numElements);
		plaintextAsStrings = MathematicalTestDataGenerator.zpGroupElementsToStrings(plaintext);

		ciphertext = encrypter.encryptGroupElements(plaintext).getElGamalCiphertext();
		preComputedValues = encrypter.preCompute();

		ZpSubgroup qrSubgroup = MathematicalTestDataGenerator.getQrSubgroup();

		encrypterForShortExponents = ElGamalTestDataGenerator.getEncrypterForShortExponents(publicKey);
		decrypterForShortExponents = ElGamalTestDataGenerator.getDecrypterForShortExponents(privateKey);

		plaintextForShortExponents = MathematicalTestDataGenerator.getZpGroupElements(qrSubgroup, numElements);

		preComputedValuesFromShortExponent = encrypterForShortExponents.preCompute();

		plaintextWithLessElements = MathematicalTestDataGenerator.getZpGroupElements(zpSubgroup, numElements - 2);
		plaintextAsStringsWithLessElements = MathematicalTestDataGenerator.zpGroupElementsToStrings(plaintextWithLessElements);

		ciphertextWithLessElements = encrypter.encryptGroupElements(plaintextWithLessElements).getElGamalCiphertext();

		plaintextForShortExponentsWithLessElements = MathematicalTestDataGenerator.getZpGroupElements(qrSubgroup, numElements - 2);

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		// A group matching the QR_2048 policy.
		qr2048Group = new ObjectMapper().readValue(classLoader.getResource("qr_2048-zpsubgroup.json"), ZpSubgroup.class);
		// A group with an invalid policy.
		invalidGroup = new ZpSubgroup(new BigInteger("3"), new BigInteger("11"), new BigInteger("5"));
	}

	@Test
	void testWhenGenerateKeyPairThenKeysHaveExpectedLengths() {
		assertEquals(publicKey.getKeys().size(), numElements);
		assertEquals(privateKey.getKeys().size(), numElements);
	}

	@Test
	void testWhenEncryptAndDecryptThenOk() throws GeneralCryptoLibException {
		List<ZpGroupElement> decryptedPlaintext = decrypter.decrypt(ciphertext, true);

		assertEquals(decryptedPlaintext, plaintext);
	}

	@Test
	void testWhenEncryptUsingPreComputedValuesAndDecryptThenOk() throws GeneralCryptoLibException {
		List<ZpGroupElement> decryptedPlaintext = decrypter
				.decrypt(encrypter.encryptGroupElements(plaintext, preComputedValues).getElGamalCiphertext(), true);

		assertEquals(decryptedPlaintext, plaintext);
	}

	@Test
	void testWhenEncryptPlaintextAsStringsAndDecryptThenOk() throws GeneralCryptoLibException {
		List<ZpGroupElement> decryptedPlaintext = decrypter.decrypt(encrypter.encryptStrings(plaintextAsStrings).getElGamalCiphertext(), true);

		assertEquals(decryptedPlaintext, plaintext);
	}

	@Test
	void testWhenEncryptPlaintextAsStringsUsingPreComputedValuesAndDecryptThenOk() throws GeneralCryptoLibException {
		List<ZpGroupElement> decryptedPlaintext = decrypter
				.decrypt(encrypter.encryptStrings(plaintextAsStrings, preComputedValues).getElGamalCiphertext(), true);

		assertEquals(decryptedPlaintext, plaintext);
	}

	@Test
	void testWhenEncryptUsingShortExponentAndDecryptThenOk() throws GeneralCryptoLibException {
		List<ZpGroupElement> decryptedPlaintext = decrypterForShortExponents
				.decrypt(encrypterForShortExponents.encryptGroupElementsWithShortExponent(plaintextForShortExponents).getElGamalCiphertext(), true);

		assertEquals(decryptedPlaintext, plaintextForShortExponents);
	}

	@Test
	void testWhenEncryptUsingShortExponentAndPreComputedValuesAndDecryptThenOk() throws GeneralCryptoLibException {
		List<ZpGroupElement> decryptedPlaintext = decrypterForShortExponents.decrypt(
				encrypterForShortExponents.encryptGroupElements(plaintextForShortExponents, preComputedValuesFromShortExponent)
						.getElGamalCiphertext(), true);

		assertEquals(decryptedPlaintext, plaintextForShortExponents);
	}

	@Test
	void testWhenEncryptPlaintextSmallerThanKeyAndDecryptThenOk() throws GeneralCryptoLibException {
		List<ZpGroupElement> decryptedPlaintext = decrypter.decrypt(ciphertextWithLessElements, true);

		assertEquals(decryptedPlaintext, plaintextWithLessElements);
	}

	@Test
	void testWhenEncryptPlaintextSmallerThanKeyUsingPreComputedValuesAndDecryptThenOk() throws GeneralCryptoLibException {
		List<ZpGroupElement> decryptedPlaintext = decrypter
				.decrypt(encrypter.encryptGroupElements(plaintextWithLessElements, preComputedValues).getElGamalCiphertext(), true);

		assertEquals(decryptedPlaintext, plaintextWithLessElements);
	}

	@Test
	void testWhenEncryptPlaintextSmallerThanKeyAsStringsAndDecryptThenOk() throws GeneralCryptoLibException {
		List<ZpGroupElement> decryptedPlaintext = decrypter
				.decrypt(encrypter.encryptStrings(plaintextAsStringsWithLessElements).getElGamalCiphertext(), true);

		assertEquals(decryptedPlaintext, plaintextWithLessElements);
	}

	@Test
	void testWhenEncryptPlaintextSmallerThanKeyAsStringsUsingPreComputedValuesAndDecryptThenOk() throws GeneralCryptoLibException {
		List<ZpGroupElement> decryptedPlaintext = decrypter
				.decrypt(encrypter.encryptStrings(plaintextAsStringsWithLessElements, preComputedValues).getElGamalCiphertext(), true);

		assertEquals(decryptedPlaintext, plaintextWithLessElements);
	}

	@Test
	void testWhenEncryptPlaintextShorterThanKeyUsingShortExponentAndDecryptThenOk() throws GeneralCryptoLibException {
		List<ZpGroupElement> decryptedPlaintext = decrypterForShortExponents.decrypt(
				encrypterForShortExponents.encryptGroupElementsWithShortExponent(plaintextForShortExponentsWithLessElements).getElGamalCiphertext(),
				true);

		assertEquals(decryptedPlaintext, plaintextForShortExponentsWithLessElements);
	}

	@Test
	void testWhenEncryptPlaintextShorterThanKeyUsingShortExponentAndPreComputedValuesAndDecryptThenOk() throws GeneralCryptoLibException {
		List<ZpGroupElement> decryptedPlaintext = decrypterForShortExponents.decrypt(
				encrypterForShortExponents.encryptGroupElements(plaintextForShortExponentsWithLessElements, preComputedValuesFromShortExponent)
						.getElGamalCiphertext(), true);

		assertEquals(decryptedPlaintext, plaintextForShortExponentsWithLessElements);
	}

	@Test
	void testWhenSerializeAndDeserializeEncryptionParametersThenOk() throws GeneralCryptoLibException {
		ElGamalEncryptionParameters deserializedEncryptionParameters = ElGamalEncryptionParameters.fromJson(encryptionParameters.toJson());

		assertEquals(deserializedEncryptionParameters, encryptionParameters);
	}

	@Test
	void testWhenSerializeAndDeserializePublicKeyThenOk() throws GeneralCryptoLibException {
		ElGamalPublicKey deserializedPublicKey = ElGamalPublicKey.fromJson(publicKey.toJson());

		assertEquals(deserializedPublicKey, publicKey);
	}

	@Test
	void testWhenSerializeAndDeserializePrivateKeyThenOk() throws GeneralCryptoLibException {
		ElGamalPrivateKey deserializedPrivateKey = ElGamalPrivateKey.fromJson(privateKey.toJson());

		assertEquals(deserializedPrivateKey, privateKey);
	}

	@Test
	void testWhenSerializeAndDeserializeCiphertextThenOk() throws GeneralCryptoLibException {
		ElGamalCiphertext deserializedCiphertext = ElGamalCiphertext.fromJson(ciphertext.toJson());

		assertEquals(deserializedCiphertext, ciphertext);
	}

	@Test
	void testGroupPolicyValidation() {
		assertDoesNotThrow(() -> elGamalService.checkGroupPolicy(qr2048Group));
	}

	@Test
	void failQR2048GroupPolicyValidation() {
		assertThrows(IllegalArgumentException.class, () -> elGamalService.checkGroupPolicy(invalidGroup));
	}

	@Test
	void generateKeyPair() throws GeneralCryptoLibException {
		ElGamalKeyPair keypair = elGamalService.generateKeyPair(encryptionParameters, numElements);
		assertEquals(numElements, keypair.getPublicKeys().getKeys().size());
		assertEquals(numElements, keypair.getPrivateKeys().getKeys().size());
	}

	@Test
	void failToGenerateKeyPairWithWrongGroup() {
		Properties properties = new Properties();
		properties.setProperty("elgamal.grouptype", "QR_3072");
		ElGamalServiceAPI service = new ElGamalService(properties);

		assertThrows(IllegalArgumentException.class, () -> service.generateKeyPair(encryptionParameters, numElements));
	}

	@Test
	void generateKeyPairFromVerifiableElGamalEncryptionParameters() throws GeneralCryptoLibException, IOException {
		ElGamalServiceAPI service = new ElGamalService();

		VerifiableElGamalEncryptionParameters params = ElGamalTestDataGenerator.getElGamalVerifiableEncryptionParameters();

		ElGamalKeyPair keyPair = service.generateKeyPair(params, numElements);

		assertEquals(numElements, keyPair.getPublicKeys().getKeys().size());
		assertEquals(numElements, keyPair.getPrivateKeys().getKeys().size());
	}

	@Test
	void failToGenerateKeyPairFromVerifiableParamsWithWrongGroup() throws GeneralCryptoLibException, IOException {
		Properties properties = new Properties();
		properties.setProperty("elgamal.grouptype", "QR_3072");
		ElGamalServiceAPI service = new ElGamalService(properties);

		// These parameters have a group of type QR_2048
		VerifiableElGamalEncryptionParameters params = ElGamalTestDataGenerator.getElGamalVerifiableEncryptionParameters();

		assertThrows(IllegalArgumentException.class, () -> service.generateKeyPair(params, numElements));
	}
}
