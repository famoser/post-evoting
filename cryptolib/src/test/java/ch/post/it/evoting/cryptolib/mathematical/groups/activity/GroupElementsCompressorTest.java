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
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

class GroupElementsCompressorTest {

	private static ZpSubgroup group;
	private static GroupElementsCompressor<ZpGroupElement> compressor;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException {

		BigInteger p = new BigInteger("23");
		BigInteger q = new BigInteger("11");
		BigInteger g = new BigInteger("2");

		group = new ZpSubgroup(g, p, q);

		compressor = new GroupElementsCompressor<>();
	}

	@Test
	void givenNullListWhenCompressThenExpectedValue() {
		assertThrows(GeneralCryptoLibException.class, () -> compressor.compress(null));
	}

	@Test
	void givenEmptyListWhenCompressThenExpectedValue() {
		List<ZpGroupElement> nullList = new ArrayList<>();

		assertThrows(GeneralCryptoLibException.class, () -> compressor.compress(nullList));
	}

	@Test
	void givenGroupAndListOfGroupElementsWhenCompressThenExpectedValue() throws GeneralCryptoLibException {
		List<ZpGroupElement> groupMembers = getSmallList();

		ZpGroupElement compressedValue = compressor.compress(groupMembers);

		ZpGroupElement expectedResult = new ZpGroupElement(new BigInteger("1"), group);

		String errorMsg = "The generated compressed value was not the expected value";
		assertEquals(expectedResult, compressedValue, errorMsg);
	}

	@Test
	void givenGroupAndListOfManyElementsWhenCompressThenExpectedValue() throws GeneralCryptoLibException {
		List<ZpGroupElement> groupMembers = getLargeList();

		ZpGroupElement compressedValue = compressor.compress(groupMembers);

		ZpGroupElement expectedResult = new ZpGroupElement(new BigInteger("12"), group);

		String errorMsg = "The generated compressed value was not the expected value";
		assertEquals(expectedResult, compressedValue, errorMsg);
	}

	@Test
	void givenNegativeNumRequiredElementsWhenBuildListThenException() throws GeneralCryptoLibException {
		List<ZpGroupElement> originalList = getLargeList();
		int numRequired = -1;

		assertThrows(GeneralCryptoLibException.class, () -> compressor.buildListWithCompressedFinalElement(numRequired, originalList));
	}

	@Test
	void givenNullListWhenBuildListThenException() {
		int numRequired = 2;

		assertThrows(GeneralCryptoLibException.class, () -> compressor.buildListWithCompressedFinalElement(numRequired, null));
	}

	@Test
	void givenEmptyListWhenBuildListThenException() {
		List<ZpGroupElement> emptyList = new ArrayList<>();
		int numRequired = 2;

		assertThrows(GeneralCryptoLibException.class, () -> compressor.buildListWithCompressedFinalElement(numRequired, emptyList));
	}

	@Test
	void givenListAndNumRequiredElementWhenBuildNewListThenOK() throws GeneralCryptoLibException {
		List<ZpGroupElement> originalList = getLargeList();
		int numRequired = 4;

		List<ZpGroupElement> newList = compressor.buildListWithCompressedFinalElement(numRequired, originalList);

		List<ZpGroupElement> expectedList = getExpectedCompressedList();

		String errorMsg = "The created compressed list was not the expected size, expected: " + numRequired + ", but was: " + newList.size();
		assertEquals(numRequired, newList.size(), errorMsg);

		errorMsg = "The created compressed did not have the expected values, expected: " + expectedList + ", but was: " + newList;
		assertEquals(expectedList, newList, errorMsg);
	}

	private List<ZpGroupElement> getSmallList() throws GeneralCryptoLibException {

		List<ZpGroupElement> groupMembers = new ArrayList<>();

		ZpGroupElement element_4 = new ZpGroupElement(new BigInteger("4"), group);
		ZpGroupElement element_6 = new ZpGroupElement(new BigInteger("6"), group);

		groupMembers.add(element_4);
		groupMembers.add(element_6);

		return groupMembers;
	}

	private List<ZpGroupElement> getLargeList() throws GeneralCryptoLibException {

		List<ZpGroupElement> groupMembers = new ArrayList<>();

		ZpGroupElement element_3 = new ZpGroupElement(new BigInteger("3"), group);
		ZpGroupElement element_4 = new ZpGroupElement(new BigInteger("4"), group);
		ZpGroupElement element_6 = new ZpGroupElement(new BigInteger("6"), group);
		ZpGroupElement element_8 = new ZpGroupElement(new BigInteger("8"), group);
		ZpGroupElement element_9 = new ZpGroupElement(new BigInteger("9"), group);
		ZpGroupElement element_12 = new ZpGroupElement(new BigInteger("12"), group);
		ZpGroupElement element_13 = new ZpGroupElement(new BigInteger("13"), group);
		ZpGroupElement element_16 = new ZpGroupElement(new BigInteger("16"), group);
		ZpGroupElement element_18 = new ZpGroupElement(new BigInteger("18"), group);

		groupMembers.add(element_3);
		groupMembers.add(element_4);
		groupMembers.add(element_6);
		groupMembers.add(element_8);
		groupMembers.add(element_9);
		groupMembers.add(element_12);
		groupMembers.add(element_13);
		groupMembers.add(element_16);
		groupMembers.add(element_18);

		return groupMembers;
	}

	private List<ZpGroupElement> getExpectedCompressedList() throws GeneralCryptoLibException {

		List<ZpGroupElement> groupMembers = new ArrayList<>();

		ZpGroupElement element_3 = new ZpGroupElement(new BigInteger("3"), group);
		ZpGroupElement element_4 = new ZpGroupElement(new BigInteger("4"), group);
		ZpGroupElement element_6 = new ZpGroupElement(new BigInteger("6"), group);

		groupMembers.add(element_3);
		groupMembers.add(element_4);
		groupMembers.add(element_6);
		groupMembers.add(element_4);

		return groupMembers;
	}
}
