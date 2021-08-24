/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.bean;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

/**
 * Tests of ElGamalEncrypterValues.
 */
class ElGamalEncrypterValuesTest {

	private static BigInteger p;

	private static BigInteger q;

	private static BigInteger g;

	private static ZpSubgroup group;

	private static ElGamalEncrypterValues elGamalEncrypterValues;

	private static Exponent r;

	@BeforeAll
	public static void setUp() throws GeneralCryptoLibException {

		p = new BigInteger("23");

		q = new BigInteger("11");

		g = new BigInteger("2");

		group = new ZpSubgroup(g, p, q);

		ElGamalCiphertext values = buildElGamalCiphertext();

		r = new Exponent(q, new BigInteger("4"));

		elGamalEncrypterValues = new ElGamalEncrypterValues(r, values);
	}

	private static ElGamalCiphertext buildElGamalCiphertext() throws GeneralCryptoLibException {

		ZpGroupElement ge1 = new ZpGroupElement(BigInteger.ONE, group);
		ZpGroupElement ge2 = new ZpGroupElement(new BigInteger("2"), group);
		List<ZpGroupElement> phis = new ArrayList<>();
		phis.add(ge1);
		phis.add(ge2);

		ZpGroupElement gamma = new ZpGroupElement(BigInteger.ONE, group);

		return new ElGamalCiphertext(gamma, phis);
	}

	@Test
	void testGetR() throws GeneralCryptoLibException {

		Exponent returnedR = elGamalEncrypterValues.getR();

		Exponent expectedR = new Exponent(q, new BigInteger("4"));

		String errorMsg = "The returned r was not the expected value";
		Assertions.assertEquals(expectedR, returnedR, errorMsg);
	}

	@Test
	void testGetValues() throws GeneralCryptoLibException {

		ElGamalCiphertext returnedValues = elGamalEncrypterValues.getElGamalCiphertext();

		ElGamalCiphertext expectedValues = buildElGamalCiphertext();

		String errorMsg = "The returned encryption values were not the expected values";
		Assertions.assertEquals(expectedValues.getValues(), returnedValues.getValues(), errorMsg);
	}
}
