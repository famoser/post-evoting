/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.mathematical.groups.activity;

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

class ExponentCompressorTest {

	private static BigInteger q;
	private static ExponentCompressor<ZpSubgroup> compressor;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException {

		BigInteger p = new BigInteger("23");
		q = new BigInteger("11");
		BigInteger g = new BigInteger("2");

		ZpSubgroup group = new ZpSubgroup(g, p, q);

		compressor = new ExponentCompressor<>(group);
	}

	@Test
	void attemptToCreateCompressorWithNullGroupThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> new ExponentCompressor<ZpSubgroup>(null));
	}

	@Test
	void attempToCompressNullListThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> compressor.compress(null));
	}

	@Test
	void attempToCompressEmptyListThenException() {
		List<Exponent> emptyListExponents = new ArrayList<>();

		assertThrows(GeneralCryptoLibException.class, () -> compressor.compress(emptyListExponents));
	}

	@Test
	void givenListExponentsTotalValueLessThanQWhenCompressThenOK() throws GeneralCryptoLibException {
		List<Exponent> listExponents = getListExponentsTotalValuesLessThenQ();

		Exponent result = compressor.compress(listExponents);

		Exponent expectedResult = new Exponent(q, new BigInteger("9"));

		String errorMsg = "The compressed exponent did not have the expected value";
		assertEquals(expectedResult, result, errorMsg);
	}

	@Test
	void givenListExponentsTotalValueGreaterThanQWhenCompressThenOK() throws GeneralCryptoLibException {
		List<Exponent> listExponents = getListExponentsTotalValuesGreaterThenQ();

		Exponent result = compressor.compress(listExponents);

		Exponent expectedResult = new Exponent(q, new BigInteger("3"));

		String errorMsg = "The compressed exponent did not have the expected value";
		assertEquals(expectedResult, result, errorMsg);
	}

	@Test
	void givenListContainingExponentFromOtherGroupWhenCompressThenException() throws GeneralCryptoLibException {
		BigInteger otherQ = new BigInteger("3");
		Exponent exponentOtherGroup = new Exponent(otherQ, new BigInteger("2"));

		List<Exponent> listExponents = getListExponentsTotalValuesGreaterThenQ();
		listExponents.add(exponentOtherGroup);

		assertThrows(GeneralCryptoLibException.class, () -> compressor.compress(listExponents));
	}

	@Test
	void givenNegativeNumRequiredExponentsWhenBuildNewListThenException() throws GeneralCryptoLibException {
		List<Exponent> originalList = getListExponents();
		int numRequired = -1;

		assertThrows(GeneralCryptoLibException.class, () -> compressor.buildListWithCompressedFinalElement(numRequired, originalList));
	}

	@Test
	void givenNullListWhenBuildNewListThenException() {
		int numRequired = 2;

		assertThrows(GeneralCryptoLibException.class, () -> compressor.buildListWithCompressedFinalElement(numRequired, null));
	}

	@Test
	void givenEmptyListWhenBuildNewListThenException() {
		List<Exponent> emptyList = new ArrayList<>();
		int numRequired = 2;

		assertThrows(GeneralCryptoLibException.class, () -> compressor.buildListWithCompressedFinalElement(numRequired, emptyList));
	}

	@Test
	void givenListAndNumRequiredExponentsWhenBuildNewListThenOK() throws GeneralCryptoLibException {
		List<Exponent> originalList = getListExponents();
		int numRequired = 2;

		List<Exponent> newList = compressor.buildListWithCompressedFinalElement(numRequired, originalList);

		List<Exponent> expectedList = getExpectedCompressedList();

		String errorMsg = "The created compressed list was not the expected size, expected: " + numRequired + ", but was: " + newList.size();
		assertEquals(numRequired, newList.size(), errorMsg);

		errorMsg = "The created compressed did not have the expected values, expected: " + expectedList + ", but was: " + newList;
		assertEquals(expectedList, newList, errorMsg);
	}

	@Test
	void moreElementsThanExisting() throws GeneralCryptoLibException {
		List<Exponent> originalList = getListExponents();
		int numRequired = 102;

		assertThrows(GeneralCryptoLibException.class, () -> compressor.buildListWithCompressedFinalElement(numRequired, originalList));
	}

	@Test
	void differentGroupElementTest() throws GeneralCryptoLibException {
		BigInteger otherQ = new BigInteger("4");
		Exponent exponentOtherGroup = new Exponent(otherQ, new BigInteger("2"));

		List<Exponent> lst = new ArrayList<>();

		List<Exponent> originalList = getListExponents();
		lst.add(exponentOtherGroup);
		lst.addAll(originalList);
		int numRequired = 2;

		assertThrows(GeneralCryptoLibException.class, () -> compressor.buildListWithCompressedFinalElement(numRequired, lst));
	}

	private List<Exponent> getListExponentsTotalValuesLessThenQ() throws GeneralCryptoLibException {

		Exponent exponent2 = new Exponent(q, new BigInteger("2"));
		Exponent exponent3 = new Exponent(q, new BigInteger("3"));
		Exponent exponent4 = new Exponent(q, new BigInteger("4"));

		List<Exponent> listExponents = new ArrayList<>();

		listExponents.add(exponent2);
		listExponents.add(exponent3);
		listExponents.add(exponent4);

		return listExponents;
	}

	private List<Exponent> getListExponentsTotalValuesGreaterThenQ() throws GeneralCryptoLibException {

		Exponent exponent2 = new Exponent(q, new BigInteger("2"));
		Exponent exponent3 = new Exponent(q, new BigInteger("3"));
		Exponent exponent4 = new Exponent(q, new BigInteger("4"));
		Exponent exponent5 = new Exponent(q, new BigInteger("5"));

		List<Exponent> listExponents = new ArrayList<>();

		listExponents.add(exponent2);
		listExponents.add(exponent3);
		listExponents.add(exponent4);
		listExponents.add(exponent5);

		return listExponents;
	}

	private List<Exponent> getListExponents() throws GeneralCryptoLibException {

		Exponent exponent2 = new Exponent(q, new BigInteger("2"));
		Exponent exponent3 = new Exponent(q, new BigInteger("3"));
		Exponent exponent4 = new Exponent(q, new BigInteger("4"));
		Exponent exponent5 = new Exponent(q, new BigInteger("5"));
		Exponent exponent6 = new Exponent(q, new BigInteger("6"));

		List<Exponent> listExponents = new ArrayList<>();

		listExponents.add(exponent2);
		listExponents.add(exponent3);
		listExponents.add(exponent4);
		listExponents.add(exponent5);
		listExponents.add(exponent6);

		return listExponents;
	}

	private List<Exponent> getExpectedCompressedList() throws GeneralCryptoLibException {

		Exponent exponent2 = new Exponent(q, new BigInteger("2"));
		Exponent exponent7 = new Exponent(q, new BigInteger("7"));

		List<Exponent> listExponents = new ArrayList<>();

		listExponents.add(exponent2);
		listExponents.add(exponent7);

		return listExponents;
	}
}
