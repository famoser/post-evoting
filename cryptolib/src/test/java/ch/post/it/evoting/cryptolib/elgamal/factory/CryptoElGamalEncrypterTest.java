/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.factory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.CryptoLibException;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncrypterValues;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.configuration.ElGamalPolicy;
import ch.post.it.evoting.cryptolib.elgamal.configuration.ElGamalPolicyFromProperties;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.test.tools.configuration.GroupLoader;

class CryptoElGamalEncrypterTest {

	private static final int SHORT_EXPONENT_LEN = 256;
	private static final ElGamalPolicy elGamalPolicy = new ElGamalPolicyFromProperties();
	private static CryptoElGamalEncrypter encrypterForSmallZpGroup;
	private static ZpSubgroup small_group;
	private static int numKeys;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException {
		final BigInteger p = new BigInteger("23");
		final BigInteger q = new BigInteger("11");
		final BigInteger g = new BigInteger("2");

		small_group = new ZpSubgroup(g, p, q);
		numKeys = 4;

		final ElGamalFactory elGamalFactoryForZpGroup = new ElGamalFactory(elGamalPolicy);

		final ElGamalEncryptionParameters elGamalEncryptionParameters = new ElGamalEncryptionParameters(p, q, g);
		final ElGamalKeyPair cryptoElGamalKeyPair = elGamalFactoryForZpGroup.createCryptoElGamalKeyPairGenerator()
				.generateKeys(elGamalEncryptionParameters, numKeys);

		final ElGamalPublicKey publicKey = cryptoElGamalKeyPair.getPublicKeys();

		encrypterForSmallZpGroup = elGamalFactoryForZpGroup.createEncrypter(publicKey);
	}

	@Test
	void given4Keys6CiphertextsWhenEncryptWith4PrecomputedValuesThenException() throws GeneralCryptoLibException {
		final ZpGroupElement element = new ZpGroupElement(new BigInteger("9"), small_group);

		int messageSize = 6;
		final List<ZpGroupElement> ciphertext = new ArrayList<>(messageSize);
		for (int i = 0; i < messageSize; i++) {
			ciphertext.add(element);
		}

		final List<ZpGroupElement> prephis = new ArrayList<>(numKeys);
		for (int i = 0; i < numKeys; i++) {
			prephis.add(element);
		}

		final ElGamalCiphertext preComputedValues = new ElGamalCiphertext(element, prephis);
		final Exponent randomExponent = new Exponent(small_group.getQ(), new BigInteger("2"));

		final ElGamalEncrypterValues values = new ElGamalEncrypterValues(randomExponent, preComputedValues);

		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> encrypterForSmallZpGroup.encryptGroupElements(ciphertext, values));
		assertEquals(
				"Number of mathematical group elements to ElGamal encrypt must be less than or equal to encrypter public key length: 4; Found" + " 6",
				exception.getMessage());
	}

	@Test
	void given4Keys4CiphertextsWhenEncryptWith6PrecomputerValuesThenException() throws GeneralCryptoLibException {
		final ZpGroupElement element = new ZpGroupElement(new BigInteger("9"), small_group);

		final List<ZpGroupElement> ciphertext = new ArrayList<>(numKeys);
		for (int i = 0; i < numKeys; i++) {
			ciphertext.add(element);
		}

		int phiSize = 6;
		final List<ZpGroupElement> prephis = new ArrayList<>(phiSize);
		for (int i = 0; i < 6; i++) {
			prephis.add(element);
		}

		final ElGamalCiphertext preComputedValues = new ElGamalCiphertext(element, prephis);
		final Exponent randomExponent = new Exponent(small_group.getQ(), new BigInteger("2"));

		final ElGamalEncrypterValues values = new ElGamalEncrypterValues(randomExponent, preComputedValues);

		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> encrypterForSmallZpGroup.encryptGroupElements(ciphertext, values));
		assertEquals("Number of ElGamal pre-computed phi elements  must be less than or equal to encrypter public key length: 4; Found 6",
				exception.getMessage());
	}

	@Test
	void whenEncryptThenGammaAndPhisAreGroupMembers() throws GeneralCryptoLibException {
		final ZpGroupElement element = new ZpGroupElement(BigInteger.ONE, small_group);

		final List<ZpGroupElement> ciphertext = new ArrayList<>();
		for (int i = 0; i < numKeys; i++) {
			ciphertext.add(element);
		}

		final ElGamalCiphertext encryptionValues = encrypterForSmallZpGroup.encryptGroupElements(ciphertext).getElGamalCiphertext();

		assertHasGammaAndPhis(encryptionValues);
		// Check that the number of generated phis is the same as the number of
		// elements to encrypt
		assertEquals(ciphertext.size(), encryptionValues.getPhis().size(),
				"The number of generated phis is the same that the number of elements to encrypt");
		assertEncryptionValuesAreGroupMembers(encryptionValues);
	}

	@Test
	void whenEncryptNullStringListThenException() {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> encrypterForSmallZpGroup.encryptStrings(null));
		assertEquals("List of stringified mathematical group elements to ElGamal encrypt is null.", exception.getMessage());
	}

	@Test
	void whenEncryptEmptyStringListThenException() {
		final List<String> ciphertext = new ArrayList<>();

		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> encrypterForSmallZpGroup.encryptStrings(ciphertext));
		assertEquals("List of stringified mathematical group elements to ElGamal encrypt is empty.", exception.getMessage());
	}

	@Test
	void whenEncryptNonNumericStringListThenException() {
		final List<String> ciphertext = new ArrayList<>();
		ciphertext.add("I am not a number");

		final CryptoLibException exception = assertThrows(CryptoLibException.class, () -> encrypterForSmallZpGroup.encryptStrings(ciphertext));
		assertEquals("'I am not a number 'is not a valid representation of a BigInteger.", exception.getMessage());
	}

	@Test
	void whenEncryptGroupElementListThenOk() {
		final List<String> ciphertext = new ArrayList<>();
		ciphertext.add("4");

		assertDoesNotThrow(() -> encrypterForSmallZpGroup.encryptStrings(ciphertext).getElGamalCiphertext());
	}

	@Test
	void whenEncryptListMultipleGroupElementListThenOk() {
		final List<String> ciphertext = new ArrayList<>();
		ciphertext.add("4");
		ciphertext.add("4");
		ciphertext.add("4");
		ciphertext.add("4");

		assertDoesNotThrow(() -> encrypterForSmallZpGroup.encryptStrings(ciphertext).getElGamalCiphertext());
	}

	@Test
	void whenEncryptListLargerThanKeyThenException() {
		final List<String> ciphertext = new ArrayList<>();
		ciphertext.add("4");
		ciphertext.add("4");
		ciphertext.add("4");
		ciphertext.add("4");
		ciphertext.add("4");

		assertThrows(GeneralCryptoLibException.class, () -> encrypterForSmallZpGroup.encryptStrings(ciphertext));
	}

	@Test
	void whenPreComputeThenGammaAndPrePhisAreGroupMembers() {
		final ElGamalCiphertext preComputedValues = encrypterForSmallZpGroup.preCompute().getElGamalCiphertext();

		assertHasGammaAndPrePhis(preComputedValues);
		assertPreComputeValuesAreGroupMembers(preComputedValues);
	}

	@Test
	void whenEncryptQrElementWithNotShortExponentThenExponentSizeCorrect() throws GeneralCryptoLibException {
		final ZpSubgroup group = obtainQrGroupFromFile();
		final Exponent exponent = encryptAndObtainRandomExponent(false, group, elGamalPolicy);

		assertTrue(exponent.getValue().bitLength() <= group.getQ().bitLength(), "Exponent was larger than expected");
	}

	@Test
	void whenEncryptQrElementWithShortExponentThenExponentSizeCorrect() throws GeneralCryptoLibException {
		ZpSubgroup group = obtainQrGroupFromFile();

		Exponent exponent = encryptAndObtainRandomExponent(true, group, elGamalPolicy);

		assertTrue(exponent.getValue().bitLength() <= SHORT_EXPONENT_LEN, "Short Exponent was not used!");
	}

	private Exponent encryptAndObtainRandomExponent(final boolean isShort, ZpSubgroup group, final ElGamalPolicy policy)
			throws GeneralCryptoLibException {

		List<ZpGroupElement> ciphertext = new ArrayList<>();

		// for test purpose the q value is used
		BigInteger randomGroupElementValue = group.getQ();
		ciphertext.add(new ZpGroupElement(randomGroupElementValue, group));

		List<ZpGroupElement> pubKeys = new ArrayList<>();
		pubKeys.add(new ZpGroupElement(randomGroupElementValue, group));

		CryptoElGamalEncrypter encrypter = new ElGamalFactory(policy).createEncrypter(new ElGamalPublicKey(pubKeys, group));

		if (isShort) {
			return encrypter.encryptGroupElementsWithShortExponent(ciphertext).getExponent();
		} else {
			return encrypter.encryptGroupElements(ciphertext).getExponent();
		}
	}

	private ZpSubgroup obtainQrGroupFromFile() throws GeneralCryptoLibException {

		GroupLoader qrGroupLoader = new GroupLoader();
		BigInteger p = qrGroupLoader.getP();
		BigInteger g = qrGroupLoader.getG();

		BigInteger q = p.subtract(BigInteger.ONE).divide(BigInteger.valueOf(2));

		return new ZpSubgroup(g, p, q);
	}

	private void assertPreComputeValuesAreGroupMembers(final ElGamalCiphertext preComputationValues) {

		String errorMessage = "All precomputed elements should be group members";

		assertTrue(small_group.isGroupMember(preComputationValues.getGamma()), errorMessage);

		for (ZpGroupElement element : preComputationValues.getPhis()) {
			assertTrue(small_group.isGroupMember(element), errorMessage);
		}
	}

	private void assertHasGammaAndPrePhis(final ElGamalCiphertext preComputationValues) {

		assertNotNull(preComputationValues.getGamma(), "The gamma should not be null");
		assertNotNull(preComputationValues.getPhis(), "The phis set should not be null");
	}

	private void assertEncryptionValuesAreGroupMembers(final ElGamalCiphertext encryptionValues) {

		String errorMessage = "All encrypted elements should be group members";

		assertTrue(small_group.isGroupMember(encryptionValues.getGamma()), errorMessage);

		for (ZpGroupElement element : encryptionValues.getPhis()) {
			assertTrue(small_group.isGroupMember(element), errorMessage);
		}
	}

	private void assertHasGammaAndPhis(final ElGamalCiphertext encryptionValues) {

		assertNotNull(encryptionValues.getGamma(), "The gamma should not be null");
		assertNotNull(encryptionValues.getPhis(), "The phis set should not be null");
	}
}
