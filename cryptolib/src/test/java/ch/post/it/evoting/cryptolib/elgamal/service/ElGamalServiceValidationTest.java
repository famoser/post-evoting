/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncrypterValues;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.configuration.ElGamalPolicyFromProperties;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.CryptoAPIElGamalDecrypter;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.CryptoAPIElGamalEncrypter;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.CryptoAPIElGamalKeyPairGenerator;
import ch.post.it.evoting.cryptolib.elgamal.factory.ElGamalFactory;
import ch.post.it.evoting.cryptolib.elgamal.utils.ElGamalTestDataGenerator;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.mathematical.groups.utils.MathematicalTestDataGenerator;
import ch.post.it.evoting.cryptolib.primitives.securerandom.constants.SecureRandomConstants;
import ch.post.it.evoting.cryptolib.test.tools.utils.CommonTestDataGenerator;

class ElGamalServiceValidationTest {

	private static final int MIN_NUM_TEST_ELEMENTS = 3;
	private static final int MAX_NUM_TEST_ELEMENTS = 10;

	private static ElGamalService elGamalService;
	private static String whiteSpaceString;
	private static ElGamalEncryptionParameters encryptionParameters;
	private static int numElements;
	private static CryptoAPIElGamalKeyPairGenerator keyPairGenerator;
	private static ElGamalPrivateKey privateKey;
	private static CryptoAPIElGamalEncrypter encrypter;
	private static List<ZpGroupElement> plaintext;
	private static List<String> plaintextAsStrings;
	private static ElGamalEncrypterValues preComputedValues;
	private static CryptoAPIElGamalEncrypter encrypterForShortExponents;
	private static int numTooManyElements;
	private static ElGamalPublicKey publicKeyWithInvalidGroupElements;
	private static ElGamalPrivateKey privateKeyWithInvalidGroupElements;
	private static List<ZpGroupElement> emptyPlaintext;
	private static List<ZpGroupElement> plaintextWithNullElement;
	private static List<ZpGroupElement> plaintextWithTooManyElements;
	private static List<String> emptyPlaintextAsStrings;
	private static List<String> plaintextAsStringsWithNullElement;
	private static List<String> plaintextAsStringsWithEmptyElement;
	private static List<String> plaintextAsStringsWithWhiteSpaceElement;
	private static List<String> plaintextAsStringsWithTooManyElements;
	private static List<ZpGroupElement> plaintextForShortExponentsWithNullElement;
	private static List<ZpGroupElement> plaintextForShortExponentsWithTooManyElements;
	private static ElGamalCiphertext ciphertextWithTooManyElements;
	private static ElGamalCiphertext ciphertextWithInvalidGroupElement;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException {

		elGamalService = new ElGamalService();

		ElGamalPolicyFromProperties policy = new ElGamalPolicyFromProperties();
		ElGamalFactory elGamalFactory = new ElGamalFactory(policy);
		keyPairGenerator = elGamalFactory.createCryptoElGamalKeyPairGenerator();

		whiteSpaceString = CommonTestDataGenerator
				.getWhiteSpaceString(CommonTestDataGenerator.getInt(1, SecureRandomConstants.MAXIMUM_GENERATED_STRING_LENGTH));

		ZpSubgroup qrSubgroup = MathematicalTestDataGenerator.getQrSubgroup();

		encryptionParameters = ElGamalTestDataGenerator.getElGamalEncryptionParameters(qrSubgroup);

		numElements = CommonTestDataGenerator.getInt(MIN_NUM_TEST_ELEMENTS, MAX_NUM_TEST_ELEMENTS);

		ElGamalKeyPair elGamalKeyPair = keyPairGenerator.generateKeys(encryptionParameters, numElements);
		ElGamalPublicKey publicKey = elGamalKeyPair.getPublicKeys();
		privateKey = elGamalKeyPair.getPrivateKeys();

		encrypter = elGamalService.createEncrypter(publicKey);

		preComputedValues = encrypter.preCompute();

		encrypterForShortExponents = ElGamalTestDataGenerator.getEncrypterForShortExponents(publicKey);

		numTooManyElements = numElements + 1;

		ZpSubgroup invalidSubgroup = MathematicalTestDataGenerator.getOtherQrSubgroup();
		publicKeyWithInvalidGroupElements = new ElGamalPublicKey(publicKey.getKeys(), invalidSubgroup);
		privateKeyWithInvalidGroupElements = new ElGamalPrivateKey(privateKey.getKeys(), invalidSubgroup);

		plaintext = MathematicalTestDataGenerator.getZpGroupElements(qrSubgroup, numElements);
		plaintextAsStrings = MathematicalTestDataGenerator.zpGroupElementsToStrings(plaintext);

		emptyPlaintext = new ArrayList<>();
		plaintextWithNullElement = new ArrayList<>(plaintext);
		plaintextWithNullElement.set(0, null);
		plaintextWithTooManyElements = MathematicalTestDataGenerator.getZpGroupElements(qrSubgroup, numElements + 1);

		emptyPlaintextAsStrings = new ArrayList<>();
		plaintextAsStringsWithNullElement = new ArrayList<>(plaintextAsStrings);
		plaintextAsStringsWithNullElement.set(0, null);
		plaintextAsStringsWithEmptyElement = new ArrayList<>(plaintextAsStrings);
		plaintextAsStringsWithEmptyElement.set(0, "");
		plaintextAsStringsWithWhiteSpaceElement = new ArrayList<>(plaintextAsStrings);
		plaintextAsStringsWithWhiteSpaceElement.set(0, whiteSpaceString);
		plaintextAsStringsWithTooManyElements = MathematicalTestDataGenerator.zpGroupElementsToStrings(plaintextWithTooManyElements);

		plaintextForShortExponentsWithNullElement = new ArrayList<>(plaintext);
		plaintextForShortExponentsWithNullElement.set(0, null);
		plaintextForShortExponentsWithTooManyElements = MathematicalTestDataGenerator.getZpGroupElements(qrSubgroup, numElements + 1);

		elGamalKeyPair = elGamalService.generateKeyPair(encryptionParameters, numElements + 1);
		CryptoAPIElGamalEncrypter encrypter = elGamalService.createEncrypter(elGamalKeyPair.getPublicKeys());
		ciphertextWithTooManyElements = encrypter.encryptGroupElements(plaintextWithTooManyElements).getElGamalCiphertext();
		List<ZpGroupElement> plaintextForShortExponents = MathematicalTestDataGenerator.getZpGroupElements(invalidSubgroup, numElements);
		ciphertextWithInvalidGroupElement = new ElGamalCiphertext(plaintextForShortExponents);
	}

	static Stream<Arguments> createElGamalEncrypter() {

		return Stream.of(arguments(null, "ElGamal public key is null."), arguments(publicKeyWithInvalidGroupElements, "is not a group member"));
	}

	static Stream<Arguments> createElGamalDecrypter() {

		return Stream.of(arguments(null, "ElGamal private key is null."), arguments(privateKeyWithInvalidGroupElements, "is not a group member"));
	}

	static Stream<Arguments> createElGamalReEncrypter() {

		return Stream.of(arguments(null, "ElGamal public key is null."), arguments(publicKeyWithInvalidGroupElements, "is not a group member"));
	}

	static Stream<Arguments> generateKeyPair() {

		return Stream.of(arguments(null, numElements, "Mathematical group parameters object is null."),
				arguments(encryptionParameters, 0, "Length of ElGamal key pair to generate must be a positive integer; Found 0"));
	}

	static Stream<Arguments> encryptGroupElements() {

		return Stream.of(arguments(null, "List of mathematical group elements to ElGamal encrypt is null."),
				arguments(emptyPlaintext, "List of mathematical group elements to ElGamal encrypt is empty."),
				arguments(plaintextWithNullElement, "List of mathematical group elements to ElGamal encrypt contains one or more null elements."),
				arguments(plaintextWithTooManyElements,
						"Number of mathematical group elements to ElGamal encrypt must be less than or equal to encrypter public key length: "
								+ numElements + "; Found " + numTooManyElements));
	}

	static Stream<Arguments> encryptGroupElementsUsingPreComputedValues() {

		return Stream.of(arguments(null, preComputedValues, "List of mathematical group elements to ElGamal encrypt is null."),
				arguments(emptyPlaintext, preComputedValues, "List of mathematical group elements to ElGamal encrypt is empty."),
				arguments(plaintextWithNullElement, preComputedValues,
						"List of mathematical group elements to ElGamal encrypt contains one or more null elements."),
				arguments(plaintextWithTooManyElements, preComputedValues,
						"Number of mathematical group elements to ElGamal encrypt must be less than or equal to encrypter public key length: "
								+ numElements + "; Found " + numTooManyElements),
				arguments(plaintext, null, "ElGamal pre-computed values object is null."));
	}

	static Stream<Arguments> encryptStrings() {

		return Stream.of(arguments(null, "List of stringified mathematical group elements to ElGamal encrypt is null."),
				arguments(emptyPlaintextAsStrings, "List of stringified mathematical group elements to ElGamal encrypt is empty."),
				arguments(plaintextAsStringsWithNullElement, "A stringified mathematical group element to ElGamal encrypt is null."),
				arguments(plaintextAsStringsWithEmptyElement, "A stringified mathematical group element to ElGamal encrypt is blank."),
				arguments(plaintextAsStringsWithWhiteSpaceElement, "A stringified mathematical group element to ElGamal encrypt is blank."),
				arguments(plaintextAsStringsWithTooManyElements,
						"Number of mathematical group elements to ElGamal encrypt must be less than or equal to encrypter public key length: "
								+ numElements + "; Found " + numTooManyElements));
	}

	static Stream<Arguments> encryptStringsUsingPreComputedValues() {

		return Stream.of(arguments(null, preComputedValues, "List of stringified mathematical group elements to ElGamal encrypt is null."),
				arguments(emptyPlaintextAsStrings, preComputedValues,
						"List of stringified mathematical group elements to ElGamal encrypt is empty" + "."),
				arguments(plaintextAsStringsWithNullElement, preComputedValues,
						"A stringified mathematical group element to ElGamal encrypt is null."),
				arguments(plaintextAsStringsWithEmptyElement, preComputedValues,
						"A stringified mathematical group element to ElGamal encrypt is blank."),
				arguments(plaintextAsStringsWithWhiteSpaceElement, preComputedValues,
						"A stringified mathematical group element to ElGamal encrypt is blank."),
				arguments(plaintextAsStringsWithTooManyElements, preComputedValues,
						"Number of mathematical group elements to ElGamal encrypt must be less than or equal to encrypter public key length: "
								+ numElements + "; Found " + numTooManyElements),
				arguments(plaintextAsStrings, null, "ElGamal pre-computed values object is null."));
	}

	static Stream<Arguments> encryptGroupElementsUsingShortExponent() {

		return Stream.of(arguments(null, "List of mathematical group elements to ElGamal encrypt is null."),
				arguments(emptyPlaintext, "List of mathematical group elements to ElGamal encrypt is empty."),
				arguments(plaintextForShortExponentsWithNullElement,
						"List of mathematical group elements to ElGamal encrypt contains one or more null elements."),
				arguments(plaintextForShortExponentsWithTooManyElements,
						"Number of mathematical group elements to ElGamal encrypt must be less than or equal to encrypter public key length: "
								+ numElements + "; Found " + numTooManyElements));
	}

	static Stream<Arguments> encryptGroupElementsUsingShortExponentAndPreComputedValues() {

		ElGamalEncrypterValues preComputedValuesFromShortExponents = encrypterForShortExponents.preCompute();

		return Stream.of(arguments(null, preComputedValuesFromShortExponents, "List of mathematical group elements to ElGamal encrypt is null."),
				arguments(emptyPlaintext, preComputedValuesFromShortExponents, "List of mathematical group elements to ElGamal encrypt is empty."),
				arguments(plaintextForShortExponentsWithNullElement, preComputedValuesFromShortExponents,
						"List of mathematical group elements to ElGamal encrypt contains one or more null elements."),
				arguments(plaintextForShortExponentsWithTooManyElements, preComputedValuesFromShortExponents,
						"Number of mathematical group elements to ElGamal encrypt must be less than or equal to encrypter public key length: "
								+ numElements + "; Found " + numTooManyElements));
	}

	static Stream<Arguments> decryptCiphertext() {
		return Stream.of(arguments(null, "ElGamal ciphertext is null."), arguments(ciphertextWithTooManyElements,
				"ElGamal ciphertext length must be less than or equal to decrypter private key length: " + numElements + "; Found "
						+ numTooManyElements), arguments(ciphertextWithInvalidGroupElement,
				"ElGamal ciphertext contains one or more elements that do not belong to mathematical group of decrypter private key."));
	}

	static Stream<Arguments> reEncryptGroupElements() {

		return Stream.of(arguments(null, "ElGamal ciphertext is null."), arguments(ciphertextWithTooManyElements,
				"ElGamal ciphertext length must be less than or equal to re-encrypter public key length: " + numElements + "; Found "
						+ numTooManyElements));
	}

	static Stream<Arguments> deserializeElGamalEncryptionParameters() throws SecurityException, IllegalArgumentException {

		String encryptionParamsWithNullP = "{\"encryptionParams\":{\"g\":\"Ag==\",\"p\":null,\"q\":\"Cw==\"}}";
		String encryptionParamsWithNullQ = "{\"encryptionParams\":{\"g\":\"Ag==\",\"p\":\"Fw==\",\"q\":null}}";
		String encryptionParamsWithNullG = "{\"encryptionParams\":{\"g\":null,\"p\":\"Fw==\",\"q\":\"Cw==\"}}";
		String encryptionParamsWithIncorrectPQRelationship = "{\"encryptionParams\":{\"g\":\"Ag==\",\"q\":\"Fw==\",\"p\":\"Cw==\"}}";
		String encryptionParamsWithIncorrectPGRelationship = "{\"encryptionParams\":{\"g\":\"Fw==\",\"p\":\"Fw==\",\"q\":\"Cw==\"}}";

		return Stream.of(arguments(null, ElGamalEncryptionParameters.class.getSimpleName() + " JSON string is null."),
				arguments("", ElGamalEncryptionParameters.class.getSimpleName() + " JSON string is blank."),
				arguments(whiteSpaceString, ElGamalEncryptionParameters.class.getSimpleName() + " JSON string is blank."),
				arguments(encryptionParamsWithNullP, "Zp subgroup p parameter is null."),
				arguments(encryptionParamsWithNullQ, "Zp subgroup q parameter is null."),
				arguments(encryptionParamsWithNullG, "Zp subgroup generator is null."), arguments(encryptionParamsWithIncorrectPQRelationship,
						"Zp subgroup q parameter must be less than or equal to Zp subgroup p parameter minus 1"),
				arguments(encryptionParamsWithIncorrectPGRelationship,
						"Zp subgroup generator must be less than or equal to Zp subgroup p parameter minus 1"));
	}

	static Stream<Arguments> deserializeElGamalPublicKey() {

		String publicKeyWithNullZpSubgroup = "{\"publicKey\":{\"zpSubgroup\":null,\"elements\":[\"Ag==\",\"Ag==\"]}}";
		String publicKeyWithNullElementList = "{\"publicKey\":{\"zpSubgroup\":{\"p\":\"Fw==\",\"q\":\"Cw==\",\"g\":\"Ag==\"},\"elements\":null}}";
		String publicKeyWithEmptyElementList = "{\"publicKey\":{\"zpSubgroup\":{\"p\":\"Fw==\",\"q\":\"Cw==\",\"g\":\"Ag==\"},\"elements\":[]}}";
		String publicKeyWithNullInElementList =
				"{\"publicKey\":{\"zpSubgroup\":{\"p\":\"Fw==\",\"q\":\"Cw==\",\"g\":\"Ag==\"}," + "\"elements\":[\"Ag==\",null]}}";

		return Stream.of(arguments(null, ElGamalPublicKey.class.getSimpleName() + " JSON string is null."),
				arguments("", ElGamalPublicKey.class.getSimpleName() + " JSON string is blank."),
				arguments(whiteSpaceString, ElGamalPublicKey.class.getSimpleName() + " JSON string is blank."),
				arguments(publicKeyWithNullZpSubgroup, "Zp subgroup is null."),
				arguments(publicKeyWithNullElementList, "List of ElGamal public key elements is null."),
				arguments(publicKeyWithEmptyElementList, "List of ElGamal public key elements is empty."),
				arguments(publicKeyWithNullInElementList, "List of ElGamal public key elements contains one or more null elements."));
	}

	static Stream<Arguments> deserializeElGamalPrivateKey() {

		String privateKeyWithNullZpSubgroup = "{\"privateKey\":{\"zpSubgroup\":null,\"exponents\":[\"BA==\",\"BQ==\"]}}";
		String privateKeyWithNullElementList = "{\"privateKey\":{\"zpSubgroup\":{\"p\":\"Fw==\",\"q\":\"Cw==\",\"g\":\"Ag==\"},\"exponents\":null}}";
		String privateKeyWithEmptyElementList = "{\"privateKey\":{\"zpSubgroup\":{\"p\":\"Fw==\",\"q\":\"Cw==\",\"g\":\"Ag==\"},\"exponents\":[]}}";
		String privateKeyWithNullInElementList =
				"{\"privateKey\":{\"zpSubgroup\":{\"p\":\"Fw==\",\"q\":\"Cw==\",\"g\":\"Ag==\"}," + "\"exponents\":[\"BA==\",null]}}";

		return Stream.of(arguments(null, ElGamalPrivateKey.class.getSimpleName() + " JSON string is null."),
				arguments("", ElGamalPrivateKey.class.getSimpleName() + " JSON string is blank."),
				arguments(whiteSpaceString, ElGamalPrivateKey.class.getSimpleName() + " JSON string is blank."),
				arguments(privateKeyWithNullZpSubgroup, "Zp subgroup is null."),
				arguments(privateKeyWithNullElementList, "List of ElGamal private key exponents is null."),
				arguments(privateKeyWithEmptyElementList, "List of ElGamal private key exponents is empty."),
				arguments(privateKeyWithNullInElementList, "List of ElGamal private key exponents contains one or more null elements."));
	}

	static Stream<Arguments> deserializeElGamalCiphertext() {

		String ciphertextWithNullGamma = "{\"ciphertext\":{\"gamma\":null,\"phis\":[\"AQ==\",\"Ag==\"],\"p\":\"Fw==\",\"q\":\"Cw==\"}}";
		String ciphertextWithNullPhiList = "{\"ciphertext\":{\"gamma\":\"AQ==\",\"phis\":null,\"p\":\"Fw==\",\"q\":\"Cw==\"}}";
		String ciphertextWithEmptyPhiList = "{\"ciphertext\":{\"gamma\":\"AQ==\",\"phis\":[],\"p\":\"Fw==\",\"q\":\"Cw==\"}}";
		String ciphertextWithNullInPhiList = "{\"ciphertext\":{\"gamma\":\"AQ==\",\"phis\":[\"AQ==\",null],\"p\":\"Fw==\",\"q\":\"Cw==\"}}";
		String ciphertextWithNullP = "{\"ciphertext\":{\"gamma\":\"AQ==\",\"phis\":[\"AQ==\",\"Ag==\"],\"p\":null,\"q\":\"Cw==\"}}";
		String ciphertextWithNullQ = "{\"ciphertext\":{\"gamma\":\"AQ==\",\"phis\":[\"AQ==\",\"Ag==\"],\"p\":\"Fw==\",\"q\":null}}";

		return Stream.of(arguments(null, ElGamalCiphertext.class.getSimpleName() + " JSON string is null."),
				arguments("", ElGamalCiphertext.class.getSimpleName() + " JSON string is blank."),
				arguments(whiteSpaceString, ElGamalCiphertext.class.getSimpleName() + " JSON string is blank."),
				arguments(ciphertextWithNullGamma, "ElGamal gamma element is null."),
				arguments(ciphertextWithNullPhiList, "List of ElGamal phi elements is null."),
				arguments(ciphertextWithEmptyPhiList, "List of ElGamal phi elements is empty."),
				arguments(ciphertextWithNullInPhiList, "Zp group element value is null."),
				arguments(ciphertextWithNullP, "Zp subgroup p parameter is null."),
				arguments(ciphertextWithNullQ, "Zp subgroup q parameter is null."));
	}

	@ParameterizedTest
	@MethodSource("createElGamalEncrypter")
	void testElGamalEncrypterCreationValidation(ElGamalPublicKey publicKey, String errorMsg) {
		final Exception exception = assertThrows(Exception.class, () -> elGamalService.createEncrypter(publicKey));
		assertTrue(exception.getMessage().contains(errorMsg));
	}

	@ParameterizedTest
	@MethodSource("createElGamalDecrypter")
	void testElGamalDecrypterCreationValidation(ElGamalPrivateKey privateKey, String errorMsg) {
		final Exception exception = assertThrows(Exception.class, () -> elGamalService.createDecrypter(privateKey));
		assertTrue(exception.getMessage().contains(errorMsg));
	}

	@ParameterizedTest
	@MethodSource("generateKeyPair")
	void testKeyPairGenerationValidation(ElGamalEncryptionParameters encryptionParameters, int length, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> keyPairGenerator.generateKeys(encryptionParameters, length));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("encryptGroupElements")
	void testGroupElementsEncryptionValidation(List<ZpGroupElement> messages, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> encrypter.encryptGroupElements(messages));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("encryptGroupElementsUsingPreComputedValues")
	void testGroupElementsEncryptionUsingPreComputedValuesValidation(List<ZpGroupElement> messages, ElGamalEncrypterValues preComputedValues,
			String errorMsg) {

		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> encrypter.encryptGroupElements(messages, preComputedValues));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("encryptStrings")
	void testStringsEncryptionValidation(List<String> messages, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> encrypter.encryptStrings(messages));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("encryptStringsUsingPreComputedValues")
	void testStringsEncryptionUsingPreComputedValuesValidation(List<String> messages, ElGamalEncrypterValues preComputedValues, String errorMsg) {

		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> encrypter.encryptStrings(messages, preComputedValues));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("encryptGroupElementsUsingShortExponent")
	void testGroupElementsEncryptionUsingShortExponentValidation(List<ZpGroupElement> messages, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> encrypterForShortExponents.encryptGroupElementsWithShortExponent(messages));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("encryptGroupElementsUsingShortExponentAndPreComputedValues")
	void testGroupElementsEncryptionUsingShortExponentAndPreComputedValuesValidation(List<ZpGroupElement> messages,
			ElGamalEncrypterValues preComputedValues, String errorMsg) {

		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> encrypterForShortExponents.encryptGroupElements(messages, preComputedValues));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("decryptCiphertext")
	void testCiphertextDecryptionValidation(ElGamalCiphertext ciphertext, String errorMsg) throws GeneralCryptoLibException {
		final CryptoAPIElGamalDecrypter decrypter = elGamalService.createDecrypter(privateKey);
		// We explicitly expect an exception from decrypt() and not createDecrypter()
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> decrypter.decrypt(ciphertext, true));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("deserializeElGamalEncryptionParameters")
	void testElGamalEncryptionParametersDeserializationValidation(String jsonStr, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> ElGamalEncryptionParameters.fromJson(jsonStr));
		assertTrue(exception.getMessage().contains(errorMsg));
	}

	@ParameterizedTest
	@MethodSource("deserializeElGamalPublicKey")
	void testElGamalPublicKeyDeserializationValidation(String jsonStr, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> ElGamalPublicKey.fromJson(jsonStr));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("deserializeElGamalPrivateKey")
	void testElGamalPrivateKeyDeserializationValidation(String jsonStr, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> ElGamalPrivateKey.fromJson(jsonStr));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("deserializeElGamalCiphertext")
	void testElGamalCiphertextDeserializationValidation(String jsonStr, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> ElGamalCiphertext.fromJson(jsonStr));
		assertEquals(errorMsg, exception.getMessage());
	}
}
