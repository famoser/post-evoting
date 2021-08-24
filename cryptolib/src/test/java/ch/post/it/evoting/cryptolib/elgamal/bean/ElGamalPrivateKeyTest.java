/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.bean;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

class ElGamalPrivateKeyTest {

	private static BigInteger p;
	private static BigInteger q;
	private static BigInteger g;
	private static ZpSubgroup group;
	private static int numKeys;
	private static List<Exponent> privKeys;
	private static ElGamalPrivateKey elGamalPrivateKey;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException {

		p = new BigInteger("23");
		q = new BigInteger("11");
		g = new BigInteger("2");

		group = new ZpSubgroup(g, p, q);

		numKeys = 2;

		privKeys = new ArrayList<>();
		privKeys.add(new Exponent(q, new BigInteger("4")));
		privKeys.add(new Exponent(q, new BigInteger("5")));

		elGamalPrivateKey = new ElGamalPrivateKey(privKeys, group);
	}

	@Test
	void givenNullKeysListWhenCreatePrivateKeyThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> new ElGamalPrivateKey(null, group));
	}

	@Test
	void givenEmptyKeysListWhenCreatePrivateKeyThenException() {
		List<Exponent> nullKeysList = new ArrayList<>();

		assertThrows(GeneralCryptoLibException.class, () -> new ElGamalPrivateKey(nullKeysList, group));
	}

	@Test
	void givenNullGroupWhenCreatePrivateKeyThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> new ElGamalPrivateKey(privKeys, null));
	}

	@Test
	void givenPrivateKeyWhenGetKeysThenExpectedKeys() {
		List<Exponent> returnedKeys = elGamalPrivateKey.getKeys();

		String errorMsg = "The created private key does not have the expected number of elements";
		assertEquals(numKeys, returnedKeys.size(), errorMsg);

		errorMsg = "The private does not have the expected list of keys";
		assertEquals(privKeys, returnedKeys, errorMsg);
	}

	@Test
	void givenPrivateKeyWhenGetGroupThenExpectedGroup() {
		String errorMsg = "The created private key does not have the expected group";

		assertEquals(group, elGamalPrivateKey.getGroup(), errorMsg);
	}

	@Test
	void givenJsonStringWhenReconstructThenEqualToOriginalPrivateKey() throws GeneralCryptoLibException {
		BigInteger p = new BigInteger("23");
		BigInteger q = new BigInteger("11");
		BigInteger g = new BigInteger("2");
		ZpSubgroup smallGroup = new ZpSubgroup(g, p, q);

		List<Exponent> privKeys = new ArrayList<>();
		privKeys.add(new Exponent(q, new BigInteger("4")));
		privKeys.add(new Exponent(q, new BigInteger("5")));
		ElGamalPrivateKey expectedElGamalPrivateKey = new ElGamalPrivateKey(privKeys, smallGroup);

		String jsonStr = elGamalPrivateKey.toJson();

		ElGamalPrivateKey reconstructedPrivateKey = ElGamalPrivateKey.fromJson(jsonStr);

		String errorMsg = "The reconstructed ElGamal private key is not equal to the expected key";

		assertEquals(expectedElGamalPrivateKey, reconstructedPrivateKey, errorMsg);
	}

	@Test
	void givenKeyWhenMultiplyThenOK() throws GeneralCryptoLibException {
		List<Exponent> exponents = singletonList(new Exponent(q, BigInteger.valueOf(13)));
		ElGamalPrivateKey other = new ElGamalPrivateKey(exponents, group);
		ElGamalPrivateKey product = elGamalPrivateKey.multiply(other);

		assertEquals(group, product.getGroup());
		List<Exponent> productExponents = product.getKeys();

		assertEquals(1, productExponents.size());
		assertEquals(new Exponent(q, BigInteger.valueOf(17)), productExponents.get(0));
	}

	@Test
	void givenNullWhenMultiplyThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> elGamalPrivateKey.multiply((ElGamalPrivateKey) null));
	}

	@Test
	void givenKeyWithDifferentGroupWhenMultiplyThenException() throws GeneralCryptoLibException {
		BigInteger g = BigInteger.valueOf(2);
		BigInteger p = BigInteger.valueOf(7);
		BigInteger q = BigInteger.valueOf(3);
		ZpSubgroup zpSubgroup = new ZpSubgroup(g, p, q);
		List<Exponent> exponents = singletonList(new Exponent(q, BigInteger.valueOf(2)));
		ElGamalPrivateKey other = new ElGamalPrivateKey(exponents, zpSubgroup);

		assertThrows(GeneralCryptoLibException.class, () -> elGamalPrivateKey.multiply(other));
	}

	@Test
	void givenArrayOfKeysWhenMultiplyThenOK() throws GeneralCryptoLibException {
		List<Exponent> exponents = singletonList(new Exponent(q, BigInteger.valueOf(6)));
		ElGamalPrivateKey first = new ElGamalPrivateKey(exponents, group);
		exponents = singletonList(new Exponent(q, BigInteger.valueOf(7)));
		ElGamalPrivateKey other = new ElGamalPrivateKey(exponents, group);
		ElGamalPrivateKey product = ElGamalPrivateKey.multiply(elGamalPrivateKey, first, other);

		assertEquals(group, product.getGroup());
		List<Exponent> productExponents = product.getKeys();

		assertEquals(1, productExponents.size());
		assertEquals(new Exponent(q, BigInteger.valueOf(17)), productExponents.get(0));
	}

	@Test
	void givenCollectionOfKeysWhenMultiplyThenOK() throws GeneralCryptoLibException {
		List<Exponent> exponents = singletonList(new Exponent(q, BigInteger.valueOf(13)));
		ElGamalPrivateKey other = new ElGamalPrivateKey(exponents, group);
		ElGamalPrivateKey product = ElGamalPrivateKey.multiply(asList(elGamalPrivateKey, other));

		assertEquals(group, product.getGroup());
		List<Exponent> productExponents = product.getKeys();

		assertEquals(1, productExponents.size());
		assertEquals(new Exponent(q, BigInteger.valueOf(17)), productExponents.get(0));
	}

	@Test
	void givenEmptyCollectionOfKeysWhenMultiplyThenException() throws GeneralCryptoLibException {
		assertThrows(GeneralCryptoLibException.class, () -> ElGamalPrivateKey.multiply(emptyList()));
	}

	@Test
	void givenCollectionOfKeysWithNullWhenMultiplyThenException() throws GeneralCryptoLibException {
		assertThrows(GeneralCryptoLibException.class, () -> ElGamalPrivateKey.multiply(asList(elGamalPrivateKey, null)));
	}

	@Test
	void givenCollectionOfKeysWithDifferentGroupWhenMultiplyThenException() throws GeneralCryptoLibException {
		List<Exponent> exponents = singletonList(new Exponent(q, BigInteger.valueOf(13)));
		BigInteger g = BigInteger.valueOf(2);
		BigInteger p = BigInteger.valueOf(7);
		BigInteger q = BigInteger.valueOf(3);
		ZpSubgroup zpSubgroup = new ZpSubgroup(g, p, q);
		ElGamalPrivateKey other = new ElGamalPrivateKey(exponents, zpSubgroup);

		assertThrows(GeneralCryptoLibException.class, () -> ElGamalPrivateKey.multiply(asList(elGamalPrivateKey, other)));
	}

	@Test
	void testBinarySerialization() throws GeneralCryptoLibException {
		byte[] bytes = elGamalPrivateKey.toBytes();
		ElGamalPrivateKey other = ElGamalPrivateKey.fromBytes(bytes);

		assertEquals(other, elGamalPrivateKey);
	}

	@Test
	void testInvert() throws GeneralCryptoLibException {
		ElGamalPrivateKey inverted = elGamalPrivateKey.invert();
		assertEquals(group, inverted.getGroup());

		List<Exponent> invertedExponents = inverted.getKeys();
		assertEquals(2, invertedExponents.size());
		assertEquals(new Exponent(q, BigInteger.valueOf(7)), invertedExponents.get(0));
		assertEquals(new Exponent(q, BigInteger.valueOf(6)), invertedExponents.get(1));
	}

	@Test
	void testDivide() throws GeneralCryptoLibException {
		List<Exponent> exponents = singletonList(new Exponent(q, BigInteger.valueOf(6)));
		ElGamalPrivateKey other = new ElGamalPrivateKey(exponents, group);
		ElGamalPrivateKey quotient = elGamalPrivateKey.divide(other);

		assertEquals(group, quotient.getGroup());

		List<Exponent> quotientExponents = quotient.getKeys();
		assertEquals(1, quotientExponents.size());
		assertEquals(new Exponent(q, BigInteger.valueOf(9)), quotientExponents.get(0));
	}
}
