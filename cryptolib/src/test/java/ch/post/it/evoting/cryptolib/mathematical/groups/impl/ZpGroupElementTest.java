/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.mathematical.groups.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;

class ZpGroupElementTest {

	private static BigInteger q;
	private static BigInteger p;
	private static BigInteger g;
	private static ZpSubgroup groupG2Q11;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException {

		q = new BigInteger("11");
		p = new BigInteger("23");
		g = new BigInteger("2");

		groupG2Q11 = new ZpSubgroup(g, p, q);
	}

	@Test
	void givenToJsonStringThenCanReconstructElement() throws GeneralCryptoLibException {
		ZpGroupElement element = new ZpGroupElement(new BigInteger("2"), p, q);

		String jsonStr = element.toJson();

		ZpGroupElement reconstructedElement = ZpGroupElement.fromJson(jsonStr);

		String errorMsg = "Reconstructed Zp group element does not equal original element.";
		assertEquals(reconstructedElement, element, errorMsg);
	}

	@Test
	void givenAValueWhenAGroupElementIsCreatedWithThatValueThenHasThatValue() throws GeneralCryptoLibException {
		BigInteger value = new BigInteger("2");

		ZpGroupElement element = new ZpGroupElement(value, groupG2Q11);

		assertEquals(value, element.getValue(), "The returned element value is not the expected one");
	}

	@Test
	void whenCreateAnElementWithValueZeroThenError() {
		BigInteger value = BigInteger.ZERO;

		assertThrows(GeneralCryptoLibException.class, () -> new ZpGroupElement(value, groupG2Q11));
	}

	@Test
	void whenCreateAnElementWithNegativeValueThenError() {
		BigInteger value = new BigInteger("-1");

		assertThrows(GeneralCryptoLibException.class, () -> new ZpGroupElement(value, groupG2Q11));
	}

	@Test
	void whenCreateAnElementWithValueGreaterThanPThenError() {
		BigInteger value = new BigInteger("24");

		assertThrows(GeneralCryptoLibException.class, () -> new ZpGroupElement(value, groupG2Q11));
	}

	@Test
	void whenCreateAnElementWithNullValueThenError() {
		assertThrows(GeneralCryptoLibException.class, () -> new ZpGroupElement(null, groupG2Q11));
	}

	@Test
	void whenCreateAnElementWithNullGroupThenError() {
		BigInteger value = BigInteger.ONE;

		assertThrows(GeneralCryptoLibException.class, () -> new ZpGroupElement(value, null));
	}

	@Test
	void whenCreateAnElement() {
		BigInteger value = new BigInteger("17");

		assertDoesNotThrow(() -> new ZpGroupElement(value, groupG2Q11));
	}

	@Test
	void whenCreateAnElementWithValueOneThenResultHasValueOne() throws GeneralCryptoLibException {
		BigInteger value = BigInteger.ONE;

		ZpGroupElement element = new ZpGroupElement(value, groupG2Q11);

		assertEquals(value, element.getValue(), "The result has a wrong value");
	}

	@Test
	void givenAnElementWhenInvertedThenSucceeds() throws GeneralCryptoLibException {
		BigInteger value = new BigInteger("16");
		BigInteger expectedInverseValue = new BigInteger("13");

		invertAndAssert(value, expectedInverseValue);
	}

	@Test
	void givenAnElementWithValueOneWhenInvertedThenResultIsOne() throws GeneralCryptoLibException {
		BigInteger value = BigInteger.ONE;
		BigInteger expectedInverseValue = BigInteger.ONE;

		invertAndAssert(value, expectedInverseValue);
	}

	@Test
	void givenNullElementWhenMultiplyThenException() throws GeneralCryptoLibException {
		BigInteger value1 = new BigInteger("3");
		ZpGroupElement element1 = new ZpGroupElement(value1, groupG2Q11);

		assertThrows(GeneralCryptoLibException.class, () -> element1.multiply(null));
	}

	@Test
	void givenTwoElementsFromDifferentGroupsWhenMultiplyThenException() throws GeneralCryptoLibException {
		BigInteger value1 = new BigInteger("3");
		BigInteger value2 = new BigInteger("3");

		ZpGroupElement element1 = new ZpGroupElement(value1, groupG2Q11);
		ZpGroupElement element2 = new ZpGroupElement(value2, new ZpSubgroup(g, new BigInteger("7"), new BigInteger("3")));

		assertThrows(GeneralCryptoLibException.class, () -> element1.multiply(element2));
	}

	@Test
	void givenTwoElementsWhenMultipliedThenSucceeds() throws GeneralCryptoLibException {
		BigInteger value1 = new BigInteger("3");
		BigInteger value2 = new BigInteger("4");
		BigInteger expectedResult = new BigInteger("12");

		multiplyAndAssert(value1, value2, expectedResult);
	}

	@Test
	void givenAnElementWithValueOneWhenMultipliedWithASecondElementThenTheResultIsSecondElement() throws GeneralCryptoLibException {
		BigInteger value1 = new BigInteger("2");
		BigInteger value2 = BigInteger.ONE;
		BigInteger expectedResult = new BigInteger("2");

		multiplyAndAssert(value1, value2, expectedResult);
	}

	@Test
	void givenTwoElementWhenMultipliedThenTheResultIsGreaterThanP() throws GeneralCryptoLibException {
		BigInteger value1 = new BigInteger("12");
		BigInteger value2 = new BigInteger("13");
		BigInteger expectedResult = new BigInteger("18");

		multiplyAndAssert(value1, value2, expectedResult);
	}

	@Test
	void givenElementAndNullExponentWhenExponentiateThenException() throws GeneralCryptoLibException {
		BigInteger value1 = new BigInteger("3");
		ZpGroupElement element = new ZpGroupElement(value1, groupG2Q11);

		assertThrows(GeneralCryptoLibException.class, () -> element.exponentiate(null));
	}

	@Test
	void givenElementAndExponentWithNullValueWhenExponentiateThenException() throws GeneralCryptoLibException {
		BigInteger value1 = new BigInteger("3");
		ZpGroupElement element = new ZpGroupElement(value1, groupG2Q11);

		final Exponent mockExponent = mock(Exponent.class);
		when(mockExponent.getValue()).thenReturn(null);

		assertThrows(GeneralCryptoLibException.class, () -> element.exponentiate(mockExponent));
	}

	@Test
	void givenElementAndExponentFromDifferentGroupsWhenExponentiateThenException() throws GeneralCryptoLibException {
		BigInteger value1 = new BigInteger("3");
		ZpGroupElement element = new ZpGroupElement(value1, groupG2Q11);

		ZpSubgroup exponentGroup = new ZpSubgroup(g, new BigInteger("7"), new BigInteger("3"));
		BigInteger exponentValue = new BigInteger("3");
		Exponent exponent = new Exponent(exponentGroup.getQ(), exponentValue);

		assertThrows(GeneralCryptoLibException.class, () -> element.exponentiate(exponent));
	}

	@Test
	void givenAnExponentWithValueZeroWhenExponentiateWithItThenResultIsOne() throws GeneralCryptoLibException {
		BigInteger value = new BigInteger("16");
		BigInteger exponentValue = BigInteger.ZERO;
		BigInteger expectedResult = BigInteger.ONE;

		exponentiateAndAssert(value, exponentValue, expectedResult);
	}

	@Test
	void givenElementAndExponentWhenExponentiateThenSucceeds() throws GeneralCryptoLibException {
		BigInteger value = new BigInteger("2");
		BigInteger exponentValue = new BigInteger("4");
		BigInteger expectedResult = new BigInteger("16");

		exponentiateAndAssert(value, exponentValue, expectedResult);
	}

	@Test
	void givenElementAndExponentWhenExponentiationThenResultGreaterThanQ() throws GeneralCryptoLibException {
		BigInteger value = new BigInteger("13");
		BigInteger exponentValue = new BigInteger("5");
		BigInteger expectedResult = new BigInteger("4");

		exponentiateAndAssert(value, exponentValue, expectedResult);
	}

	@Test
	void testExponentiateWithANullElement() throws Exception {
		ZpGroupElement element = new ZpGroupElement(BigInteger.TEN, groupG2Q11);

		assertThrows(GeneralCryptoLibException.class, () -> element.exponentiate(null));
	}

	@Test
	void testEquals() throws GeneralCryptoLibException {
		ZpGroupElement element1_value1_q11 = new ZpGroupElement(BigInteger.ONE, groupG2Q11);
		ZpGroupElement element2_value1_q11 = new ZpGroupElement(BigInteger.ONE, groupG2Q11);

		ZpGroupElement element3_value2_q11 = new ZpGroupElement(new BigInteger("2"), groupG2Q11);

		ZpSubgroup otherGroup_g4_q3 = new ZpSubgroup(new BigInteger("2"), new BigInteger("7"), new BigInteger("3"));
		ZpGroupElement element4_value1_q13 = new ZpGroupElement(BigInteger.ONE, otherGroup_g4_q3);

		assertAll(() -> assertEquals(element1_value1_q11, element2_value1_q11), () -> assertNotEquals(element1_value1_q11, element3_value2_q11),
				() -> assertNotEquals(element1_value1_q11, element4_value1_q13), () -> assertNotEquals(element3_value2_q11, element4_value1_q13));
	}

	/**
	 * Creates a ZpGroupElement and then calls the toJson() method on that ZpGroupElement. Asserts that the returned string can be used to reconstruct
	 * the ZpGroupElement.
	 */
	@Test
	void testToJson() throws GeneralCryptoLibException {
		ZpGroupElement element = new ZpGroupElement(new BigInteger("2"), groupG2Q11);

		String jsonStr = element.toJson();

		ZpGroupElement reconstructedElement = ZpGroupElement.fromJson(jsonStr);

		String errorMessage = "The reconstructed Zp group element does not equal the original element.";
		assertEquals(reconstructedElement, element, errorMessage);
	}

	@Test
	void testToString() throws GeneralCryptoLibException {
		ZpGroupElement element = new ZpGroupElement(new BigInteger("2"), groupG2Q11);
		String toString = element.toString();

		assertAll(() -> assertTrue(toString.contains("=2,")), () -> assertTrue(toString.contains("=" + groupG2Q11.getP().toString())),
				() -> assertTrue(toString.contains("=" + groupG2Q11.getQ().toString())));
	}

	@Test
	final void givenNullZpSubgroupPParamThenJsonDeserializationThrowsException() {
		String jsonStr = "{\"zpGroupElement\":{\"q\":\"Cw==\",\"p\":null,\"value\":\"Ag==\"}}";

		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> ZpGroupElement.fromJson(jsonStr));
		assertEquals("Zp subgroup p parameter is null.", exception.getMessage());
	}

	@Test
	final void givenNullZpSubgroupQParamThenJsonDeserializationThrowsException() {
		String jsonStr = "{\"zpGroupElement\":{\"q\":null,\"p\":\"Fw==\",\"value\":\"Ag==\"}}";

		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> ZpGroupElement.fromJson(jsonStr));
		assertEquals("Zp subgroup q parameter is null.", exception.getMessage());
	}

	/**
	 * Exponentiates an element by an exponent and asserts the expected result.
	 *
	 * @param elementValue   The group element value to set.
	 * @param exponentValue  The exponent value to set.
	 * @param expectedResult The expected result of the exponentiation.
	 */
	private void exponentiateAndAssert(final BigInteger elementValue, final BigInteger exponentValue, final BigInteger expectedResult)
			throws GeneralCryptoLibException {

		ZpGroupElement element = new ZpGroupElement(elementValue, groupG2Q11);

		Exponent exponent = new Exponent(groupG2Q11.getQ(), exponentValue);

		ZpGroupElement result = element.exponentiate(exponent);

		assertEquals(expectedResult, result.getValue(), "The result of the exponentiation is not the expected.");
	}

	/**
	 * Multiply two group elements with the values {@code value1} and {@code value2}. Then asserts that the result has the value {@code
	 * expectedResult}.
	 *
	 * @param value1         First element to multiply.
	 * @param value2         Second element to multiply.
	 * @param expectedResult The expected result of the {@code value1 * value2}.
	 */
	private void multiplyAndAssert(final BigInteger value1, final BigInteger value2, final BigInteger expectedResult)
			throws GeneralCryptoLibException {
		ZpGroupElement element1 = new ZpGroupElement(value1, groupG2Q11);
		ZpGroupElement element2 = new ZpGroupElement(value2, groupG2Q11);

		ZpGroupElement result = element1.multiply(element2);

		assertEquals(expectedResult, result.getValue(), "The multiplication result is not the expected one");
	}

	/**
	 * Inverts the element with the value {@code elementValue}, and checks whether the result is the {@code expectedInverseValue}.
	 *
	 * @param elementValue         The value of the element to invert.
	 * @param expectedInverseValue The expected result of the invert operation of the element with value {@code elementValue}.
	 */
	private void invertAndAssert(final BigInteger elementValue, final BigInteger expectedInverseValue) throws GeneralCryptoLibException {
		ZpGroupElement element = new ZpGroupElement(elementValue, groupG2Q11);

		ZpGroupElement inverse = element.invert();

		assertEquals(expectedInverseValue, inverse.getValue(), "The returned element is not the inverse");
	}
}
