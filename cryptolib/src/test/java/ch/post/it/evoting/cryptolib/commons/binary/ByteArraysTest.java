/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.commons.binary;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests of {@link ByteArrays}
 */
class ByteArraysTest {

	@Test
	void testConstantTimeEquals() {
		byte[] array1 = { 1, 2, 3 };
		byte[] array2 = { 4, 5, 6 };
		byte[] array3 = { 7, 8 };
		byte[] array4 = {};

		Assertions.assertTrue(ByteArrays.constantTimeEquals(array1, array1));
		Assertions.assertTrue(ByteArrays.constantTimeEquals(array4, array4));
		Assertions.assertFalse(ByteArrays.constantTimeEquals(array1, array2));
		Assertions.assertFalse(ByteArrays.constantTimeEquals(array1, array3));
		Assertions.assertFalse(ByteArrays.constantTimeEquals(array1, null));
		Assertions.assertFalse(ByteArrays.constantTimeEquals(null, array1));
		Assertions.assertFalse(ByteArrays.constantTimeEquals(null, null));
	}

	@Test
	void testConcatenateTwo() {
		byte[] array1 = { 1, 2, 3 };
		byte[] array2 = { 4, 5, };
		byte[] array3 = {};

		Assertions.assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteArrays.concatenate(array1, array2));
		Assertions.assertArrayEquals(new byte[] { 1, 2, 3 }, ByteArrays.concatenate(array1, array3));
		Assertions.assertArrayEquals(new byte[0], ByteArrays.concatenate(array3, array3));
	}

	@Test
	void testConcatenateMoreThanTwo() {
		byte[] array1 = { 1, 2, 3 };
		byte[] array2 = { 4, 5, 6 };
		byte[] array3 = { 7, 8 };
		byte[] array4 = {};
		Assertions.assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }, ByteArrays.concatenate(array1, array2, array3, array4));
	}
}
