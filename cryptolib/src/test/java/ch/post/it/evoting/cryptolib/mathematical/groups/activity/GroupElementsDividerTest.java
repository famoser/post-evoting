/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.mathematical.groups.activity;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

class GroupElementsDividerTest {

	private static ZpSubgroup group;
	private static GroupElementsDivider divider;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException {

		BigInteger p = new BigInteger("23");
		BigInteger q = new BigInteger("11");
		BigInteger g = new BigInteger("2");

		group = new ZpSubgroup(g, p, q);

		divider = new GroupElementsDivider();
	}

	@Test
	void givenGroupAndListOfGroupElementsWhenCompressThenExpectedValue() throws GeneralCryptoLibException {
		List<ZpGroupElement> list1 = getList1_4elements();
		List<ZpGroupElement> list2 = getList2_4elements();
		List<ZpGroupElement> combinedList = divider.divide(list1, list2, group);
		List<ZpGroupElement> expectedList = buildExpectedList();

		String errorMsg = "The created list is not the expected list";
		Assertions.assertEquals(expectedList, combinedList, errorMsg);
	}

	@Test
	void givenNullGroupWhenCompressThenException() throws GeneralCryptoLibException {
		List<ZpGroupElement> list1 = getList1_4elements();
		List<ZpGroupElement> list2 = getList2_4elements();

		assertThrows(GeneralCryptoLibException.class, () -> divider.divide(list1, list2, null));
	}

	@Test
	void givenNullList1WhenCompressThenException() throws GeneralCryptoLibException {
		List<ZpGroupElement> list = getList_5elements();

		assertThrows(GeneralCryptoLibException.class, () -> divider.divide(null, list, group));
	}

	@Test
	void givenEmptyList1WhenCompressThenException() throws GeneralCryptoLibException {
		List<ZpGroupElement> list1 = new ArrayList<>();
		List<ZpGroupElement> list2 = getList_5elements();

		assertThrows(GeneralCryptoLibException.class, () -> divider.divide(list1, list2, group));
	}

	@Test
	void givenNullList2WhenCompressThenException() throws GeneralCryptoLibException {
		List<ZpGroupElement> list = getList_5elements();

		assertThrows(GeneralCryptoLibException.class, () -> divider.divide(list, null, group));
	}

	@Test
	void givenEmptyList2WhenCompressThenException() throws GeneralCryptoLibException {
		List<ZpGroupElement> list1 = getList_5elements();
		List<ZpGroupElement> list2 = new ArrayList<>();

		assertThrows(GeneralCryptoLibException.class, () -> divider.divide(list1, list2, group));
	}

	@Test
	void givenListsOfDifferentSizesWhenCompressThenException() throws GeneralCryptoLibException {
		List<ZpGroupElement> list1 = getList1_4elements();
		List<ZpGroupElement> list2 = getList_5elements();

		assertThrows(GeneralCryptoLibException.class, () -> divider.divide(list1, list2, group));
	}

	private List<ZpGroupElement> buildExpectedList() throws GeneralCryptoLibException {

		List<ZpGroupElement> groupMembers = new ArrayList<>();

		ZpGroupElement element_8 = new ZpGroupElement(new BigInteger("8"), group);
		ZpGroupElement element_4 = new ZpGroupElement(new BigInteger("4"), group);
		ZpGroupElement element_12 = new ZpGroupElement(new BigInteger("12"), group);

		groupMembers.add(element_8);
		groupMembers.add(element_4);
		groupMembers.add(element_12);
		groupMembers.add(element_12);

		return groupMembers;
	}

	private List<ZpGroupElement> getList1_4elements() throws GeneralCryptoLibException {

		List<ZpGroupElement> groupMembers = new ArrayList<>();

		ZpGroupElement element_4 = new ZpGroupElement(new BigInteger("4"), group);
		ZpGroupElement element_6 = new ZpGroupElement(new BigInteger("6"), group);
		ZpGroupElement element_8 = new ZpGroupElement(new BigInteger("8"), group);
		ZpGroupElement element_9 = new ZpGroupElement(new BigInteger("9"), group);

		groupMembers.add(element_4);
		groupMembers.add(element_6);
		groupMembers.add(element_8);
		groupMembers.add(element_9);

		return groupMembers;
	}

	private List<ZpGroupElement> getList2_4elements() throws GeneralCryptoLibException {

		List<ZpGroupElement> groupMembers = new ArrayList<>();

		ZpGroupElement element_12 = new ZpGroupElement(new BigInteger("12"), group);
		ZpGroupElement element_13 = new ZpGroupElement(new BigInteger("13"), group);
		ZpGroupElement element_16 = new ZpGroupElement(new BigInteger("16"), group);
		ZpGroupElement element_18 = new ZpGroupElement(new BigInteger("18"), group);

		groupMembers.add(element_12);
		groupMembers.add(element_13);
		groupMembers.add(element_16);
		groupMembers.add(element_18);

		return groupMembers;
	}

	private List<ZpGroupElement> getList_5elements() throws GeneralCryptoLibException {

		List<ZpGroupElement> groupMembers = new ArrayList<>();

		ZpGroupElement element_12 = new ZpGroupElement(new BigInteger("12"), group);
		ZpGroupElement element_13 = new ZpGroupElement(new BigInteger("13"), group);
		ZpGroupElement element_16 = new ZpGroupElement(new BigInteger("16"), group);
		ZpGroupElement element_18 = new ZpGroupElement(new BigInteger("18"), group);
		ZpGroupElement element_9 = new ZpGroupElement(new BigInteger("9"), group);

		groupMembers.add(element_12);
		groupMembers.add(element_13);
		groupMembers.add(element_16);
		groupMembers.add(element_18);
		groupMembers.add(element_9);

		return groupMembers;
	}
}
