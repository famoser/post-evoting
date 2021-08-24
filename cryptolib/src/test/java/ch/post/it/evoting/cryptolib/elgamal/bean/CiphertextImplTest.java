/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.bean;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Ciphertext;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

class CiphertextImplTest {

	private static final BigInteger G = BigInteger.valueOf(2);
	private static final BigInteger P = BigInteger.valueOf(7);
	private static final BigInteger Q = BigInteger.valueOf(3);

	private ZpGroupElement gamma;
	private ZpGroupElement phi1;
	private ZpGroupElement phi2;

	@BeforeEach
	void setUp() throws GeneralCryptoLibException {
		ZpSubgroup group = new ZpSubgroup(G, P, Q);
		gamma = new ZpGroupElement(BigInteger.valueOf(2), group);
		phi1 = new ZpGroupElement(BigInteger.valueOf(4), group);
		phi2 = new ZpGroupElement(BigInteger.ONE, group);
	}

	@Test
	void whenCreateFromElementAndListThenOk() throws GeneralCryptoLibException {
		Ciphertext ciphertext = new CiphertextImpl(gamma, asList(phi1, phi2));
		assertEquals(gamma, ciphertext.getGamma());
		List<ZpGroupElement> phis = ciphertext.getPhis();
		assertEquals(2, phis.size());
		assertEquals(phi1, phis.get(0));
		assertEquals(phi2, phis.get(1));
	}

	@Test
	void whenCreateFromElementAndEmptyListThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> new CiphertextImpl(gamma, emptyList()));
	}

	@Test
	void whenCreateUsingListOfPhisThatIncludesInvalidGroupThenException() throws GeneralCryptoLibException {
		ZpSubgroup group = new ZpSubgroup(G, BigInteger.valueOf(11), BigInteger.valueOf(5));
		phi2 = new ZpGroupElement(BigInteger.valueOf(4), group);
		assertThrows(GeneralCryptoLibException.class, () -> new CiphertextImpl(gamma, asList(phi1, phi2)));
	}

	@Test
	void whenCreateUsingNullGammaAndPhiAsListThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> new CiphertextImpl(null, singletonList(phi1)));
	}

	@Test
	void whenCreateFromElementAndListWithNullPhiThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> new CiphertextImpl(gamma, asList(phi1, null)));
	}

	@Test
	void whenCreateUsingArrayOfPhisThatIncludesInvalidGroupThenException() throws GeneralCryptoLibException {
		ZpSubgroup group = new ZpSubgroup(G, BigInteger.valueOf(11), BigInteger.valueOf(5));
		phi2 = new ZpGroupElement(BigInteger.valueOf(4), group);
		assertThrows(GeneralCryptoLibException.class, () -> new CiphertextImpl(gamma, phi1, phi2));
	}

	@Test
	void whenCreateUsingNullGammaAndPhiAsElementThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> new CiphertextImpl(null, phi1));
	}

	@Test
	void whenCreateUsingPhiAsElementButIsNullThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> new CiphertextImpl(gamma, (ZpGroupElement) null));
	}

	@Test
	void whenCreateFromGammaAndOnePhiElementThenOk() throws GeneralCryptoLibException {
		Ciphertext ciphertext = new CiphertextImpl(gamma, phi1);
		assertEquals(gamma, ciphertext.getGamma());
		List<ZpGroupElement> phis = ciphertext.getPhis();
		assertEquals(1, phis.size());
		assertEquals(phi1, phis.get(0));
	}

	@Test
	void whenCreateFromGammaAndTwoPhiElementsThenOk() throws GeneralCryptoLibException {
		Ciphertext ciphertext = new CiphertextImpl(gamma, phi1, phi2);
		assertEquals(gamma, ciphertext.getGamma());
		List<ZpGroupElement> phis = ciphertext.getPhis();
		assertEquals(2, phis.size());
		assertEquals(phi1, phis.get(0));
		assertEquals(phi2, phis.get(1));
	}

	@Test
	void whenGetElementsThenOk() throws GeneralCryptoLibException {
		Ciphertext ciphertext = new CiphertextImpl(gamma, phi1, phi2);
		List<ZpGroupElement> elements = ciphertext.getElements();
		assertEquals(3, elements.size());
		assertEquals(gamma, elements.get(0));
		assertEquals(phi1, elements.get(1));
		assertEquals(phi2, elements.get(2));
	}

	@Test
	void whenGetSizeOfListOfElementsThenOk() throws GeneralCryptoLibException {
		Ciphertext ciphertext = new CiphertextImpl(gamma, phi1, phi2);
		assertEquals(3, ciphertext.size());
	}

	@Test
	void whenCompareTwoIdenticalInstancesThenOk() throws GeneralCryptoLibException {

		Ciphertext ciphertext1 = new CiphertextImpl(gamma, phi1, phi2);

		BigInteger gOther = BigInteger.valueOf(2);
		BigInteger pOther = BigInteger.valueOf(7);
		BigInteger qOther = BigInteger.valueOf(3);
		ZpSubgroup groupOther = new ZpSubgroup(gOther, pOther, qOther);
		ZpGroupElement gammaOther = new ZpGroupElement(BigInteger.valueOf(2), groupOther);
		ZpGroupElement phi1Other = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		ZpGroupElement phi2Other = new ZpGroupElement(BigInteger.ONE, groupOther);
		Ciphertext ciphertext2 = new CiphertextImpl(gammaOther, phi1Other, phi2Other);

		assertEquals(ciphertext2, ciphertext1);
	}

	@Test
	void whenCompareTwoNonIdenticalInstancesThenOk() throws GeneralCryptoLibException {

		Ciphertext ciphertext1 = new CiphertextImpl(gamma, phi1, phi2);

		BigInteger gOther = BigInteger.valueOf(2);
		BigInteger pOther = BigInteger.valueOf(7);
		BigInteger qOther = BigInteger.valueOf(3);
		ZpSubgroup groupOther = new ZpSubgroup(gOther, pOther, qOther);
		ZpGroupElement gammaOther = new ZpGroupElement(BigInteger.valueOf(2), groupOther);
		ZpGroupElement phi1Other = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		ZpGroupElement phi2Other = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		Ciphertext ciphertext2 = new CiphertextImpl(gammaOther, phi1Other, phi2Other);

		assertNotEquals(ciphertext2, ciphertext1);
	}

	@Test
	void whenCompareSimilarInstancesButPhisInDifferentOrderThenOk() throws GeneralCryptoLibException {

		Ciphertext ciphertext1 = new CiphertextImpl(gamma, phi1, phi2);

		BigInteger gOther = BigInteger.valueOf(2);
		BigInteger pOther = BigInteger.valueOf(7);
		BigInteger qOther = BigInteger.valueOf(3);
		ZpSubgroup groupOther = new ZpSubgroup(gOther, pOther, qOther);
		ZpGroupElement gammaOther = new ZpGroupElement(BigInteger.valueOf(2), groupOther);
		ZpGroupElement phi1Other = new ZpGroupElement(BigInteger.ONE, groupOther);
		ZpGroupElement phi2Other = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		Ciphertext ciphertext2 = new CiphertextImpl(gammaOther, phi1Other, phi2Other);

		assertNotEquals(ciphertext2, ciphertext1);
	}

	@Test
	void givenMorePhisInThisCiphertextWhenCompareThenOk() throws GeneralCryptoLibException {

		ZpSubgroup group = new ZpSubgroup(G, P, Q);
		ZpGroupElement phi3 = new ZpGroupElement(BigInteger.ONE, group);
		Ciphertext ciphertext1 = new CiphertextImpl(gamma, phi1, phi2, phi3);

		BigInteger gOther = BigInteger.valueOf(2);
		BigInteger pOther = BigInteger.valueOf(7);
		BigInteger qOther = BigInteger.valueOf(3);
		ZpSubgroup groupOther = new ZpSubgroup(gOther, pOther, qOther);
		ZpGroupElement gammaOther = new ZpGroupElement(BigInteger.valueOf(2), groupOther);
		ZpGroupElement phi1Other = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		ZpGroupElement phi2Other = new ZpGroupElement(BigInteger.ONE, groupOther);
		Ciphertext ciphertext2 = new CiphertextImpl(gammaOther, phi1Other, phi2Other);

		assertNotEquals(ciphertext2, ciphertext1);
	}

	@Test
	void givenMorePhisInOtherCiphertextWhenCompareThenOk() throws GeneralCryptoLibException {

		Ciphertext ciphertext1 = new CiphertextImpl(gamma, phi1, phi2);

		BigInteger gOther = BigInteger.valueOf(2);
		BigInteger pOther = BigInteger.valueOf(7);
		BigInteger qOther = BigInteger.valueOf(3);
		ZpSubgroup groupOther = new ZpSubgroup(gOther, pOther, qOther);
		ZpGroupElement gammaOther = new ZpGroupElement(BigInteger.valueOf(2), groupOther);
		ZpGroupElement phi1Other = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		ZpGroupElement phi2Other = new ZpGroupElement(BigInteger.ONE, groupOther);
		ZpGroupElement phi3Other = new ZpGroupElement(BigInteger.ONE, groupOther);
		Ciphertext ciphertext2 = new CiphertextImpl(gammaOther, phi1Other, phi2Other, phi3Other);

		assertNotEquals(ciphertext2, ciphertext1);
	}

	@Test
	void givenSimilarCiphertextsButDifferentGroupThenOk() throws GeneralCryptoLibException {

		Ciphertext ciphertext1 = new CiphertextImpl(gamma, phi1, phi2);

		BigInteger gOther = BigInteger.valueOf(2);
		BigInteger pOther = BigInteger.valueOf(23);
		BigInteger qOther = BigInteger.valueOf(11);
		ZpSubgroup groupOther = new ZpSubgroup(gOther, pOther, qOther);
		ZpGroupElement gammaOther = new ZpGroupElement(BigInteger.valueOf(2), groupOther);
		ZpGroupElement phi1Other = new ZpGroupElement(BigInteger.ONE, groupOther);
		ZpGroupElement phi2Other = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		Ciphertext ciphertext2 = new CiphertextImpl(gammaOther, phi1Other, phi2Other);

		assertNotEquals(ciphertext2, ciphertext1);
	}

	@Test
	void givenIdenticalCiphertextsThenHashcodesTheSame() throws GeneralCryptoLibException {

		Ciphertext ciphertext1 = new CiphertextImpl(gamma, phi1, phi2);

		BigInteger gOther = BigInteger.valueOf(2);
		BigInteger pOther = BigInteger.valueOf(7);
		BigInteger qOther = BigInteger.valueOf(3);
		ZpSubgroup groupOther = new ZpSubgroup(gOther, pOther, qOther);
		ZpGroupElement gammaOther = new ZpGroupElement(BigInteger.valueOf(2), groupOther);
		ZpGroupElement phi1Other = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		ZpGroupElement phi2Other = new ZpGroupElement(BigInteger.ONE, groupOther);
		Ciphertext ciphertext2 = new CiphertextImpl(gammaOther, phi1Other, phi2Other);

		assertEquals((ciphertext2.hashCode()), ciphertext1.hashCode());
	}

	@Test
	void whenExecuteHashcodeTwiceThenSameHashcode() throws GeneralCryptoLibException {

		Ciphertext ciphertext1 = new CiphertextImpl(gamma, phi1, phi2);

		int result1 = ciphertext1.hashCode();
		int result2 = ciphertext1.hashCode();

		assertEquals(result2, result1);
	}

	@Test
	void givenNonIdenticalCiphertextsThenHashcodesTheSame() throws GeneralCryptoLibException {

		Ciphertext ciphertext1 = new CiphertextImpl(gamma, phi1, phi2);

		BigInteger gOther = BigInteger.valueOf(2);
		BigInteger pOther = BigInteger.valueOf(7);
		BigInteger qOther = BigInteger.valueOf(3);
		ZpSubgroup groupOther = new ZpSubgroup(gOther, pOther, qOther);
		ZpGroupElement gammaOther = new ZpGroupElement(BigInteger.valueOf(2), groupOther);
		ZpGroupElement phi1Other = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		ZpGroupElement phi2Other = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		Ciphertext ciphertext2 = new CiphertextImpl(gammaOther, phi1Other, phi2Other);

		assertNotEquals(ciphertext1.hashCode(), ciphertext2.hashCode());
	}

	@Test
	void testToString() throws GeneralCryptoLibException {
		Ciphertext ciphertext = new CiphertextImpl(gamma, phi1, phi2);
		String toString = ciphertext.toString();
		assertTrue(toString.contains("=" + gamma.toString() + ","));
		assertTrue(toString.contains(phi1.toString()));
		assertTrue(toString.contains(phi2.toString()));
	}

	@Test
	void whenExponentiateThenExpectedValue() throws GeneralCryptoLibException {

		BigInteger g_2 = BigInteger.valueOf(2);
		BigInteger p_23 = BigInteger.valueOf(23);
		BigInteger q_11 = BigInteger.valueOf(11);
		ZpSubgroup groupOther = new ZpSubgroup(g_2, p_23, q_11);

		ZpGroupElement gamma = new ZpGroupElement(BigInteger.valueOf(2), groupOther);
		ZpGroupElement phi1 = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		ZpGroupElement phi2 = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		Ciphertext ciphertext = new CiphertextImpl(gamma, phi1, phi2);

		Exponent exponent_3 = new Exponent(q_11, BigInteger.valueOf(2));

		Ciphertext expected = performExponentiationManually(ciphertext, exponent_3);

		assertEquals(expected, ciphertext.exponentiate(exponent_3));
	}

	@Test
	void whenExponentFromOtherGroupThenException() throws GeneralCryptoLibException {

		BigInteger g_2 = BigInteger.valueOf(2);
		BigInteger p_23 = BigInteger.valueOf(23);
		BigInteger q_11 = BigInteger.valueOf(11);
		ZpSubgroup groupOther = new ZpSubgroup(g_2, p_23, q_11);

		ZpGroupElement gamma = new ZpGroupElement(BigInteger.valueOf(2), groupOther);
		ZpGroupElement phi1 = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		ZpGroupElement phi2 = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		Ciphertext ciphertext = new CiphertextImpl(gamma, phi1, phi2);

		BigInteger q_7 = BigInteger.valueOf(7);
		Exponent exponent_3 = new Exponent(q_7, BigInteger.valueOf(2));

		assertThrows(GeneralCryptoLibException.class, () -> ciphertext.exponentiate(exponent_3));
	}

	@Test
	void whenMultiplyCompatibleCiphertextsThenExpectedValue() throws GeneralCryptoLibException {

		BigInteger g_2 = BigInteger.valueOf(2);
		BigInteger p_23 = BigInteger.valueOf(23);
		BigInteger q_11 = BigInteger.valueOf(11);
		ZpSubgroup groupOther = new ZpSubgroup(g_2, p_23, q_11);

		ZpGroupElement c1_gamma = new ZpGroupElement(BigInteger.valueOf(2), groupOther);
		ZpGroupElement c1_phi1 = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		ZpGroupElement c1_phi2 = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		Ciphertext ciphertext1 = new CiphertextImpl(c1_gamma, c1_phi1, c1_phi2);

		ZpGroupElement c2_gamma = new ZpGroupElement(BigInteger.valueOf(2), groupOther);
		ZpGroupElement c2_phi1 = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		ZpGroupElement c2_phi2 = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		Ciphertext ciphertext2 = new CiphertextImpl(c2_gamma, c2_phi1, c2_phi2);

		Ciphertext expected = performMultiplicationManually(ciphertext1, ciphertext2);

		assertEquals(expected, ciphertext1.multiply(ciphertext2));
	}

	@Test
	void whenMultiplyNonCompatibleCiphertextsThenException() throws GeneralCryptoLibException {

		BigInteger g_2 = BigInteger.valueOf(2);
		BigInteger p_23 = BigInteger.valueOf(23);
		BigInteger q_11 = BigInteger.valueOf(11);
		ZpSubgroup groupOther = new ZpSubgroup(g_2, p_23, q_11);

		ZpGroupElement c1_gamma = new ZpGroupElement(BigInteger.valueOf(2), groupOther);
		ZpGroupElement c1_phi1 = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		ZpGroupElement c1_phi2 = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		Ciphertext ciphertext_2phis = new CiphertextImpl(c1_gamma, c1_phi1, c1_phi2);

		ZpGroupElement c2_gamma = new ZpGroupElement(BigInteger.valueOf(2), groupOther);
		ZpGroupElement c2_phi1 = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		ZpGroupElement c2_phi2 = new ZpGroupElement(BigInteger.valueOf(4), groupOther);
		ZpGroupElement c2_phi3 = new ZpGroupElement(BigInteger.valueOf(4), groupOther);

		Ciphertext ciphertext_3phis = new CiphertextImpl(c2_gamma, c2_phi1, c2_phi2, c2_phi3);

		assertThrows(GeneralCryptoLibException.class, () -> ciphertext_2phis.multiply(ciphertext_3phis));
	}

	private Ciphertext performExponentiationManually(Ciphertext ciphertext, Exponent exponent_3) throws GeneralCryptoLibException {

		BigInteger q = ciphertext.getGamma().getQ();
		BigInteger p = ciphertext.getGamma().getP();

		BigInteger gammExp = ciphertext.getGamma().getValue().modPow(exponent_3.getValue(), q);
		BigInteger phi1Exp = ciphertext.getPhis().get(0).getValue().modPow(exponent_3.getValue(), p);
		BigInteger phi2Exp = ciphertext.getPhis().get(1).getValue().modPow(exponent_3.getValue(), p);

		ZpGroupElement gammExpAsGroupElement = new ZpGroupElement(gammExp, p, q);
		ZpGroupElement phi1ExpAsGroupElement = new ZpGroupElement(phi1Exp, p, q);
		ZpGroupElement phi2ExpAsGroupElement = new ZpGroupElement(phi2Exp, p, q);

		return new CiphertextImpl(gammExpAsGroupElement, phi1ExpAsGroupElement, phi2ExpAsGroupElement);
	}

	private Ciphertext performMultiplicationManually(Ciphertext ciphertext1, Ciphertext ciphertext2) throws GeneralCryptoLibException {

		ZpGroupElement gammaResult = ciphertext1.getGamma().multiply(ciphertext2.getGamma());

		List<ZpGroupElement> phi1 = ciphertext1.getPhis();
		List<ZpGroupElement> phi2 = ciphertext2.getPhis();

		List<ZpGroupElement> phisResult = new ArrayList<>();

		for (int i = 0; i < phi1.size(); i++) {
			phisResult.add(phi1.get(i).multiply(phi2.get(i)));
		}

		return new CiphertextImpl(gammaResult, phisResult);
	}
}
