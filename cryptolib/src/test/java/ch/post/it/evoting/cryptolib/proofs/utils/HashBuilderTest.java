/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.mathematical.groups.GroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.primitives.service.PrimitivesService;
import ch.post.it.evoting.cryptolib.proofs.maurer.configuration.ConfigProofHashCharset;

class HashBuilderTest {

	private static final BigInteger q = BigInteger.valueOf(11);

	private static String shortString;
	private static HashBuilder hashBuilder;
	private static List<ZpGroupElement> list1;
	private static List<ZpGroupElement> list2;
	private static List<ZpGroupElement> list3;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException {

		shortString = "I am a short String";

		ConfigProofHashCharset charset = ConfigProofHashCharset.UTF8;

		hashBuilder = new HashBuilder(new PrimitivesService(), charset.getCharset());

		BigInteger g = BigInteger.valueOf(2);
		BigInteger p = BigInteger.valueOf(23);

		ZpSubgroup group = new ZpSubgroup(g, p, q);

		ZpGroupElement element2 = new ZpGroupElement(BigInteger.valueOf(2), group);
		ZpGroupElement element3 = new ZpGroupElement(BigInteger.valueOf(3), group);
		ZpGroupElement element4 = new ZpGroupElement(BigInteger.valueOf(4), group);

		// Create 3 lists of ZpGroupElement

		list1 = new ArrayList<>();
		list1.add(element2);
		list1.add(element3);

		list2 = new ArrayList<>();
		list2.add(element4);

		list3 = new ArrayList<>();
		list3.add(element2);
	}

	/**
	 * Test that the method getCharset method returns the expected charset for a given HashBuilder object.
	 */
	@Test
	void givenHashBuilderWhenGetCharsetThenExpectedString() {
		assertEquals(StandardCharsets.UTF_8, hashBuilder.getCharset(), "The returned has name was not what was expected");
	}

	/**
	 * Test that the hash generated using the method buildHashForProofs, is the expected value. In this case, the lists contain objects of type
	 * QuadraticResidueGroupElement.
	 *
	 * <p>Performs the test with different sets of inputs to confirm different outputs.
	 */
	@Test
	void buildHashFromCompositionTest() throws GeneralCryptoLibException {

		BigInteger expectedHashValue1 = new BigInteger("30263310936975873378154824001080525837420285024442893690443810172422546936995");

		BigInteger expectedHashValue2 = new BigInteger("28954822680231466116306325775302326763740937530052903310420811346913265572468");

		getHashFromObjectsUsingToStringAndAssertValue(expectedHashValue1, list1, list2, shortString);

		getHashFromObjectsUsingToStringAndAssertValue(expectedHashValue2, list1, list3, shortString);
	}

	/**
	 * Test that when an object which is null is passed to the method buildHashForProofs, then a CryptoLibException is thrown.
	 */
	@Test
	void givenNullFirstListWhenBuildHashFromStringThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> hashBuilder.buildHashForProofs(null, new ArrayList<>(), "Data"));
	}

	/**
	 * Test that when an object which is null is passed to the method buildHashForProofs, then a CryptoLibException is thrown.
	 */
	@Test
	void givenNullSecondListWhenBuildHashFromStringThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> hashBuilder.buildHashForProofs(new ArrayList<>(), null, "Data"));
	}

	private void getHashFromObjectsUsingToStringAndAssertValue(final BigInteger expectedHashValue, final List<? extends GroupElement> publicValues,
			final List<? extends GroupElement> generatedValues, final String data) throws GeneralCryptoLibException {

		BigInteger hash = new BigInteger(1, hashBuilder.buildHashForProofs(publicValues, generatedValues, data));

		assertHashMatchesExpectedValue(expectedHashValue, hash);
	}

	@Test
	void getHashTwiceFromObjectsUsingToStringAndAssertEqualsValue() throws GeneralCryptoLibException {

		BigInteger hash1 = new BigInteger(1, hashBuilder.buildHashForProofs(list1, list2, shortString));

		BigInteger hash2 = new BigInteger(1, hashBuilder.buildHashForProofs(list1, list2, shortString));

		assertHashesMatch(hash1, hash2);
	}

	@Test
	void generateHashDoesNotReturnANullValueTest() throws GeneralCryptoLibException {
		assertNotNull(hashBuilder.generateHash(q, list1, list2, shortString));
	}

	private void assertHashMatchesExpectedValue(final BigInteger expectedValue, final BigInteger hash) {

		String errorMsg = "Generated hash value does not match expected value";

		assertEquals(expectedValue, hash, errorMsg);
	}

	private void assertHashesMatch(final BigInteger hash1, final BigInteger hash2) {

		String errorMsg = "Expected that two hash values generated from the same inputs would be the same, but they are not";

		assertEquals(hash1, hash2, errorMsg);
	}

}
