/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.mathematical.groups.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;

class ExponentTest extends ExponentTestBase {

	@Test
	void givenAJsonStringThenCreateExponent() throws GeneralCryptoLibException {
		String jsonStr = "{\"exponent\":{\"q\":\"Cw==\",\"value\":\"Cg==\"}}";

		Exponent exponent = Exponent.fromJson(jsonStr);

		assertEquals(BigInteger.TEN, exponent.getValue());
	}

	@Test
	void givenAMissingJsonStringFieldThenException() {
		String jsonStr = "{\"exponent\":{\"q\":\"Cw==\"}}";

		assertThrows(GeneralCryptoLibException.class, () -> Exponent.fromJson(jsonStr));
	}

	@Test
	void givenNullNullExponentWhenAttemptToCreateExponentThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> new Exponent(_smallQ, null));
	}

	@Test
	void givenNullQAndValidValueWhenAttemptToCreateExponentThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> new Exponent(null, BigInteger.TEN));
	}

	@Test
	void givenANonRandomExponentValueLessThanQGetExponentValue() throws GeneralCryptoLibException {
		BigInteger exponentValue = new BigInteger("1");

		createExponentAndAssertExponentValue(_smallQ, exponentValue, exponentValue);
	}

	@Test
	void givenANonRandomExponentValueGreaterThanQGetExponentValue() throws GeneralCryptoLibException {
		BigInteger exponentValue = new BigInteger("113");
		BigInteger expectedExponentValue = new BigInteger("3");

		createExponentAndAssertExponentValue(_smallQ, exponentValue, expectedExponentValue);
	}

	@Test
	void givenANonRandomExponentValueEqualToQGetExponentValue() throws GeneralCryptoLibException {
		BigInteger exponentValue = new BigInteger("11");
		BigInteger expectedExponentValue = BigInteger.ZERO;

		createExponentAndAssertExponentValue(_smallQ, exponentValue, expectedExponentValue);
	}

	@Test
	void givenANonRandomNegativeExponentGetExponentValue() throws GeneralCryptoLibException {
		BigInteger exponentValue = new BigInteger("-111");
		BigInteger expectedExponentValue = BigInteger.TEN;

		createExponentAndAssertExponentValue(_smallQ, exponentValue, expectedExponentValue);
	}

	@Test
	void givenAnExponentWhenGetQThenExpectedQReturned() throws GeneralCryptoLibException {
		BigInteger exponentValue = new BigInteger("2");
		BigInteger expectedQ = new BigInteger("11");

		createExponentAndAssertQ(_smallQ, _smallP, _smallG, expectedQ, exponentValue);
	}

	@Test
	void givenExponentsDifferentGroupsWhenAddThenException() throws GeneralCryptoLibException {
		Exponent exponent1 = new Exponent(_smallQ, _smallG);
		Exponent exponent2 = new Exponent(_largeQ, _largeG);

		assertThrows(GeneralCryptoLibException.class, () -> exponent1.add(exponent2));
	}

	@Test
	void givenNullExponentsWhenAddThenException() throws GeneralCryptoLibException {
		Exponent exponent1 = new Exponent(_smallQ, _smallG);

		assertThrows(GeneralCryptoLibException.class, () -> exponent1.add(null));
	}

	@Test
	void givenTwoExponentsWhenAddedThenLessThanQ() throws GeneralCryptoLibException {
		BigInteger exponent1Value = new BigInteger("2");
		BigInteger exponent2Value = new BigInteger("3");
		BigInteger expectedResult = new BigInteger("5");

		addExponentsAndAssert(exponent1Value, exponent2Value, expectedResult);
	}

	@Test
	void givenTwoExponentsWhenAddedThenEqualsToQ() throws GeneralCryptoLibException {
		BigInteger exponent1Value = new BigInteger("5");
		BigInteger exponent2Value = new BigInteger("6");
		BigInteger expectedResult = BigInteger.ZERO;

		addExponentsAndAssert(exponent1Value, exponent2Value, expectedResult);
	}

	@Test
	void givenTwoExponentsWhenAddedThenGreaterThanQ() throws GeneralCryptoLibException {
		BigInteger exponent1Value = BigInteger.TEN;
		BigInteger exponent2Value = new BigInteger("2");
		BigInteger expectedResult = BigInteger.ONE;

		addExponentsAndAssert(exponent1Value, exponent2Value, expectedResult);
	}

	@Test
	void givenTwoEqualExponentsWhenAddedThenGreaterThanQ() throws GeneralCryptoLibException {
		BigInteger exponent1Value = BigInteger.TEN;
		BigInteger exponent2Value = BigInteger.TEN;
		BigInteger expectedResult = new BigInteger("9");

		addExponentsAndAssert(exponent1Value, exponent2Value, expectedResult);
	}

	@Test
	void givenTwoExponentsOneEqualToZeroWhenAddedThenSucceeds() throws GeneralCryptoLibException {
		BigInteger exponent1Value = BigInteger.ZERO;
		BigInteger exponent2Value = new BigInteger("4");
		BigInteger expectedResult = new BigInteger("4");

		addExponentsAndAssert(exponent1Value, exponent2Value, expectedResult);
	}

	@Test
	void givenAnExponentWithValueZeroWhenNegatedThenResultIsZero() throws GeneralCryptoLibException {
		BigInteger exponentValue = BigInteger.ZERO;
		BigInteger expectedResult = BigInteger.ZERO;

		negateExponentAndAssert(exponentValue, expectedResult);
	}

	@Test
	void givenAnExponentLessThanQWhenNegatedThenSucceeds() throws GeneralCryptoLibException {
		BigInteger exponentValue = new BigInteger("9");
		BigInteger expectedResult = new BigInteger("2");

		negateExponentAndAssert(exponentValue, expectedResult);
	}

	@Test
	void givenTwoExponentsWhenSubtractedResultIsPositive() throws GeneralCryptoLibException {
		BigInteger exponent1Value = new BigInteger("3");
		BigInteger exponent2Value = new BigInteger("2");
		BigInteger expectedResult = BigInteger.ONE;

		subtractExponentsAndAssert(exponent1Value, exponent2Value, expectedResult);
	}

	@Test
	void givenTwoExponentsWhenSubtractedResultIsZero() throws GeneralCryptoLibException {
		BigInteger exponent1Value = BigInteger.TEN;
		BigInteger exponent2Value = BigInteger.TEN;
		BigInteger expectedResult = BigInteger.ZERO;

		subtractExponentsAndAssert(exponent1Value, exponent2Value, expectedResult);
	}

	@Test
	void givenTwoExponentsWhenSubtractedResultIsNegative() throws GeneralCryptoLibException {
		BigInteger exponent1Value = new BigInteger("2");
		BigInteger exponent2Value = new BigInteger("3");
		BigInteger expectedResult = BigInteger.TEN;

		subtractExponentsAndAssert(exponent1Value, exponent2Value, expectedResult);
	}

	@Test
	void givenAnExponentWhenSubtractedZeroThenResultIsTheExponent() throws GeneralCryptoLibException {
		BigInteger exponent1Value = new BigInteger("4");
		BigInteger exponent2Value = BigInteger.ZERO;
		BigInteger expectedResult = new BigInteger("4");

		subtractExponentsAndAssert(exponent1Value, exponent2Value, expectedResult);
	}

	@Test
	void givenAnExponentWhenMultipliedSmallThenResultIsCorrect() throws GeneralCryptoLibException {
		BigInteger exponent1Value = BigInteger.valueOf(2);
		BigInteger exponent2Value = BigInteger.valueOf(3);
		BigInteger expectedResult = BigInteger.valueOf(6);

		multiplyExponentsAndAssert(exponent1Value, exponent2Value, expectedResult);
	}

	@Test
	void givenAnExponentWhenMultipliedBigThenResultIsCorrect() throws GeneralCryptoLibException {
		BigInteger exponent1Value = BigInteger.valueOf(2);
		BigInteger exponent2Value = BigInteger.valueOf(6);
		BigInteger expectedResult = BigInteger.ONE;

		multiplyExponentsAndAssert(exponent1Value, exponent2Value, expectedResult);
	}

	@Test
	void givenAnExponentWhenMultipliedOneThenResultIsZero() throws GeneralCryptoLibException {
		BigInteger exponent1Value = BigInteger.valueOf(2);
		BigInteger exponent2Value = BigInteger.ONE;
		BigInteger expectedResult = BigInteger.valueOf(2);

		multiplyExponentsAndAssert(exponent1Value, exponent2Value, expectedResult);
	}

	@Test
	void givenAnExponentWhenMultipliedZeroThenResultIsZero() throws GeneralCryptoLibException {
		BigInteger exponent1Value = BigInteger.valueOf(2);
		BigInteger exponent2Value = BigInteger.ZERO;
		BigInteger expectedResult = BigInteger.ZERO;

		multiplyExponentsAndAssert(exponent1Value, exponent2Value, expectedResult);
	}

	/**
	 * Creates an exponent and then calls the toJson() method on that exponent. Asserts that the returned string can be used to reconstruct the
	 * exponent.
	 */
	@Test
	void givenAnExponentWhenToJsonCanRecoverExponent() throws GeneralCryptoLibException {
		Exponent exponent = new Exponent(_smallQ, BigInteger.TEN);

		String jsonStr = exponent.toJson();

		Exponent reconstructedExponent = Exponent.fromJson(jsonStr);

		String errorMessage = "The Exponent could not be reconstructed from a JSON string.";
		assertEquals(reconstructedExponent, exponent, errorMessage);
	}

	@Test
	void testEquals() throws GeneralCryptoLibException {
		ZpSubgroup mathematicalGroup_q11 = new ZpSubgroup(new BigInteger("2"), new BigInteger("23"), new BigInteger("11"));
		ZpSubgroup mathematicalGroup_q3 = new ZpSubgroup(new BigInteger("2"), new BigInteger("23"), new BigInteger("3"));

		Exponent exponent1_q11_value10 = new Exponent(mathematicalGroup_q11.getQ(), BigInteger.TEN);
		Exponent exponent2_q11_value10 = new Exponent(mathematicalGroup_q11.getQ(), BigInteger.TEN);

		Exponent exponent3_q13_value4 = new Exponent(mathematicalGroup_q3.getQ(), new BigInteger("4"));

		Exponent exponent4_q11_value9 = new Exponent(mathematicalGroup_q11.getQ(), new BigInteger("9"));

		assertAll(() -> assertEquals(exponent1_q11_value10, exponent2_q11_value10),
				() -> assertNotEquals(exponent1_q11_value10, exponent3_q13_value4),
				() -> assertNotEquals(exponent1_q11_value10, exponent4_q11_value9),
				() -> assertNotEquals(exponent3_q13_value4, exponent4_q11_value9));
	}

	@Test
	void testToString() throws GeneralCryptoLibException {
		Exponent element = new Exponent(BigInteger.TEN, BigInteger.valueOf(2));
		String toString = element.toString();
		assertTrue(toString.contains("=2"));
		assertTrue(toString.contains("=" + BigInteger.TEN.toString()));
	}

	/**
	 * @param q                     the Zp subgroup q parameter.
	 * @param exponentValue         The desired exponent value.
	 * @param expectedExponentValue The expected exponent value.
	 */
	private void createExponentAndAssertExponentValue(final BigInteger q, final BigInteger exponentValue, final BigInteger expectedExponentValue)
			throws GeneralCryptoLibException {

		Exponent exponent = new Exponent(q, exponentValue);

		assertEquals(expectedExponentValue, exponent.getValue(), "The exponent value is not the expected one");
	}

	/**
	 * @param q             The q parameter to be used when creating the exponent.
	 * @param expectedQ     The q that is expected to be returned when the getQ() method is called.
	 * @param exponentValue The exponent value to be used when creating the exponent.
	 */
	private void createExponentAndAssertQ(final BigInteger q, final BigInteger p, final BigInteger g, final BigInteger expectedQ,
			final BigInteger exponentValue) throws GeneralCryptoLibException {

		Exponent exponent = new Exponent(q, exponentValue);

		assertEquals(expectedQ, exponent.getQ(), "The q is not the expected one");
	}

	/**
	 * Create the exponents with the given values, and add them. Then assert that the exponent result has the expected value.
	 *
	 * @param exponent1Value The exponent1 value.
	 * @param exponent2Value The exponent2 value.
	 * @param expectedResult The expected value of the result of adding those two exponents.
	 */
	private void addExponentsAndAssert(final BigInteger exponent1Value, final BigInteger exponent2Value, final BigInteger expectedResult)
			throws GeneralCryptoLibException {

		Exponent exponent1 = new Exponent(_smallQ, exponent1Value);
		Exponent exponent2 = new Exponent(_smallQ, exponent2Value);

		exponent1 = exponent1.add(exponent2);

		assertEquals(expectedResult, exponent1.getValue(), "The operation result is invalid");
	}

	/**
	 * Create the exponents with the given values, and subtract them: {@code (exponent1 - exponent2)}. Then assert that the exponent result has the
	 * expected value
	 *
	 * @param exponent1Value The exponent1 value.
	 * @param exponent2Value The exponent2 value.
	 * @param expectedResult The expected value of the result of subtracting those two exponents: {@code (exponent1 - exponent2)}.
	 * @throws GeneralCryptoLibException if arguments are invalid.
	 */
	private void subtractExponentsAndAssert(final BigInteger exponent1Value, final BigInteger exponent2Value, final BigInteger expectedResult)
			throws GeneralCryptoLibException {

		Exponent exponent1 = new Exponent(_smallQ, exponent1Value);

		Exponent exponent2 = new Exponent(_smallQ, exponent2Value);

		exponent1 = exponent1.subtract(exponent2);

		assertEquals(expectedResult, exponent1.getValue(), "The operation result is invalid");
	}

	/**
	 * Create the exponents with the given values, and multiplies them: {@code (exponent1 * exponent2)}. Then assert that the exponent result has the
	 * expected value
	 *
	 * @param exponent1Value The exponent1 value.
	 * @param exponent2Value The exponent2 value.
	 * @param expectedResult The expected value of the result of multiplying those two exponents: {@code (exponent1 * exponent2)}.
	 * @throws GeneralCryptoLibException if arguments are invalid.
	 */
	private void multiplyExponentsAndAssert(final BigInteger exponent1Value, final BigInteger exponent2Value, final BigInteger expectedResult)
			throws GeneralCryptoLibException {
		Exponent exponent1 = new Exponent(_smallQ, exponent1Value);

		Exponent exponent2 = new Exponent(_smallQ, exponent2Value);

		exponent1 = exponent1.multiply(exponent2);

		assertEquals(expectedResult, exponent1.getValue(), "The operation result is invalid");
	}

	/**
	 * Creates the exponent with the {@code exponentValue}, and negate it. Asserts that the negated exponent has the {@code expectedValue}.
	 *
	 * @param exponentValue The value for the exponent.
	 * @param expectedValue The expected value for the negated exponent.
	 * @throws GeneralCryptoLibException if arguments are invalid.
	 */
	private void negateExponentAndAssert(final BigInteger exponentValue, final BigInteger expectedValue) throws GeneralCryptoLibException {
		Exponent exponent = new Exponent(_smallQ, exponentValue);
		Exponent negated = exponent.negate();

		assertEquals(expectedValue, negated.getValue(), "The negated exponent has not the expected value");
	}
}
