/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class IDsParserTest {

	private final IDsParser target = new IDsParser();

	@Test
	void parseIllegalArgument() {
		assertThrows(IllegalArgumentException.class, () -> target.parse("3232,3232"));
	}

	@Test
	void parsePositiveCase() {

		String testCase = "[08b82ffc12e84dd6973ffd7b9feadeee,451a6ffc3e214ca8ae5c451d82e7fbe4,17ccbe962cf341bc93208c26e911090c,4b35ae490b2a495a98e709fb004e22a1]";
		List<String> idsArray = target.parse(testCase);

		assertEquals(4, idsArray.size());
		assertEquals("08b82ffc12e84dd6973ffd7b9feadeee", idsArray.get(0));
		assertEquals("451a6ffc3e214ca8ae5c451d82e7fbe4", idsArray.get(1));
		assertEquals("17ccbe962cf341bc93208c26e911090c", idsArray.get(2));
		assertEquals("4b35ae490b2a495a98e709fb004e22a1", idsArray.get(3));
	}
}
