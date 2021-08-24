/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.bean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.test.tools.configuration.GroupLoader;

class ElGamalBeanValidationTest {

	private static ZpSubgroup zpSubgroup;
	private static List<Exponent> exponentList;
	private static List<ZpGroupElement> elementList;
	private static List<Exponent> emptyExponentList;
	private static List<ZpGroupElement> emptyElementList;
	private static List<ZpGroupElement> elementListWithNullValue;
	private static ZpGroupElement gamma;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException {

		GroupLoader zpGroupLoader = new GroupLoader();
		zpSubgroup = new ZpSubgroup(zpGroupLoader.getG(), zpGroupLoader.getP(), zpGroupLoader.getQ());

		exponentList = new ArrayList<>();
		exponentList.add(new Exponent(zpSubgroup.getQ(), BigInteger.TEN));

		elementList = new ArrayList<>();
		elementList.add(new ZpGroupElement(BigInteger.ONE, zpSubgroup));

		emptyExponentList = new ArrayList<>();

		emptyElementList = new ArrayList<>();

		elementListWithNullValue = new ArrayList<>(elementList);
		elementListWithNullValue.add(null);

		gamma = elementList.get(0);
	}

	static Stream<Arguments> createElGamalEncryptionParameters() {

		BigInteger p = zpSubgroup.getP();
		BigInteger q = zpSubgroup.getQ();
		BigInteger g = zpSubgroup.getG();

		return Stream.of(arguments(null, q, g, "Zp subgroup p parameter is null."), arguments(p, null, g, "Zp subgroup q parameter is null."),
				arguments(p, q, null, "Zp subgroup generator is null."),
				arguments(p, BigInteger.ZERO, g, "Zp subgroup q parameter must be greater than or equal to : 1; Found 0"), arguments(p, p, g,
						"Zp subgroup q parameter must be less than or equal to Zp subgroup p parameter minus 1: " + p.subtract(BigInteger.ONE)
								+ "; Found " + p),
				arguments(p, q, BigInteger.valueOf(1), "Zp subgroup generator must be greater than or equal to : 2; Found 1"), arguments(p, q, p,
						"Zp subgroup generator must be less than or equal to Zp subgroup p parameter minus 1: " + p.subtract(BigInteger.ONE)
								+ "; Found " + p));
	}

	static Stream<Arguments> createElGamalKeyPair() throws GeneralCryptoLibException {

		ElGamalPrivateKey privateKey = new ElGamalPrivateKey(exponentList, zpSubgroup);
		ElGamalPublicKey publicKey = new ElGamalPublicKey(elementList, zpSubgroup);

		List<Exponent> exponentsLarger = new ArrayList<>(exponentList);
		exponentsLarger.add(new Exponent(zpSubgroup.getQ(), BigInteger.TEN));
		ElGamalPrivateKey privateKeyWithMoreExponents = new ElGamalPrivateKey(exponentsLarger, zpSubgroup);

		GroupLoader qrGroupLoader = new GroupLoader(2);
		ZpSubgroup qrSubgroup = new ZpSubgroup(qrGroupLoader.getG(), qrGroupLoader.getP(), qrGroupLoader.getQ());
		ElGamalPrivateKey privateKeyForOtherGroup = new ElGamalPrivateKey(exponentList, qrSubgroup);

		return Stream.of(arguments(null, publicKey, "ElGamal private key is null."), arguments(privateKey, null, "ElGamal public key is null."),
				arguments(privateKeyWithMoreExponents, publicKey,
						"ElGamal private key length must be equal to ElGamal public key length: " + publicKey.getKeys().size() + "; Found "
								+ privateKeyWithMoreExponents.getKeys().size()),
				arguments(privateKeyForOtherGroup, publicKey, "ElGamal public and private keys must belong to same mathematical group."));
	}

	static Stream<Arguments> createElGamalPrivateKey() {

		List<Exponent> exponentsWithNullValue = new ArrayList<>(exponentList);
		exponentsWithNullValue.add(null);

		return Stream.of(arguments(null, zpSubgroup, "List of ElGamal private key exponents is null."),
				arguments(emptyExponentList, zpSubgroup, "List of ElGamal private key exponents is empty."),
				arguments(exponentsWithNullValue, zpSubgroup, "List of ElGamal private key exponents contains one or more null elements."),
				arguments(exponentList, null, "Zp subgroup is null."));
	}

	static Stream<Arguments> createElGamalPublicKey() {

		return Stream.of(arguments(null, zpSubgroup, "List of ElGamal public key elements is null."),
				arguments(emptyExponentList, zpSubgroup, "List of ElGamal public key elements is empty."),
				arguments(elementListWithNullValue, zpSubgroup, "List of ElGamal public key elements contains one or more null elements."),
				arguments(elementList, null, "Zp subgroup is null."));
	}

	static Stream<Arguments> createElGamalCiphertextFromElements() {

		return Stream.of(arguments(null, elementList, "ElGamal gamma element is null."),
				arguments(gamma, null, "List of ElGamal phi elements is null" + "."),
				arguments(gamma, emptyElementList, "List of ElGamal phi elements is empty."),
				arguments(gamma, elementListWithNullValue, "List of ElGamal phi elements contains one or more null elements."));
	}

	static Stream<Arguments> createElGamalCiphertextFromCiphertext() {

		return Stream.of(arguments(null, "ElGamal ciphertext is null."), arguments(emptyElementList, "ElGamal ciphertext is empty."),
				arguments(elementListWithNullValue, "ElGamal ciphertext contains one or more null elements."),
				arguments(elementList, "ElGamal ciphertext length must be greater than or equal to : 2; Found 1"));
	}

	static Stream<Arguments> createElGamalEncrypterValues() throws GeneralCryptoLibException {
		ElGamalCiphertext values = new ElGamalCiphertext(gamma, elementList);

		return Stream.of(arguments(null, values, "Random exponent is null."), arguments(exponentList.get(0), null, "ElGamal ciphertext is null."));
	}

	static Stream<Arguments> createWitnessImpl() {
		return Stream.of(arguments(null, "Exponent is null."));
	}

	@ParameterizedTest
	@MethodSource("createElGamalEncryptionParameters")
	void testElGamalEncryptionParametersCreationValidation(BigInteger p, BigInteger q, BigInteger g, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> new ElGamalEncryptionParameters(p, q, g));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("createElGamalKeyPair")
	void testElGamalKeyPairCreationValidation(ElGamalPrivateKey privateKey, ElGamalPublicKey publicKey, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> new ElGamalKeyPair(privateKey, publicKey));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("createElGamalPrivateKey")
	void testElGamalPrivateKeyCreationValidation(List<Exponent> exponents, ZpSubgroup zpSubgroup, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> new ElGamalPrivateKey(exponents, zpSubgroup));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("createElGamalPublicKey")
	void testElGamalPublicKeyCreationValidation(List<ZpGroupElement> elements, ZpSubgroup zpSubgroup, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> new ElGamalPublicKey(elements, zpSubgroup));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("createElGamalCiphertextFromElements")
	void testElGamalCiphertextCreationFromElementsValidation(ZpGroupElement gammaElement, List<ZpGroupElement> phiElements, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> new ElGamalCiphertext(gammaElement, phiElements));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("createElGamalCiphertextFromCiphertext")
	void testElGamalCiphertextCreationFromCiphertextValidation(List<ZpGroupElement> ciphertext, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> new ElGamalCiphertext(ciphertext));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("createElGamalEncrypterValues")
	void testElGamalEncrypterValuesValidation(Exponent exponent, ElGamalCiphertext values, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> new ElGamalEncrypterValues(exponent, values));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("createWitnessImpl")
	void testWitnessImplCreationValidation(Exponent exponent, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> new WitnessImpl(exponent));
		assertEquals(errorMsg, exception.getMessage());
	}
}
