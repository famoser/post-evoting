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
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

/**
 * Tests of BaseElGamalCiphertext.
 */
class BaseElGamalCiphertextTest {

	private static BigInteger _p;

	private static BigInteger _q;

	private static BigInteger _g;

	private static ZpSubgroup _group;

	@BeforeAll
	public static void setUp() throws GeneralCryptoLibException {

		_p = new BigInteger("23");

		_q = new BigInteger("11");

		_g = new BigInteger("2");

		_group = new ZpSubgroup(_g, _p, _q);
	}

	@Test
	void testGetPhis() throws GeneralCryptoLibException {
		ZpGroupElement ge1 = new ZpGroupElement(BigInteger.ONE, _group);
		ZpGroupElement ge2 = new ZpGroupElement(new BigInteger("2"), _group);

		ZpGroupElement gamma = createGamma();

		List<ZpGroupElement> phis = new ArrayList<>();
		phis.add(ge1);
		phis.add(ge2);

		BaseElGamalCiphertext<ZpGroupElement> values = new BaseElGamalCiphertext<>(gamma, phis);

		Assertions.assertEquals(phis, values.getPhis(), "The phis returned are not the phis expected");
	}

	@Test
	void testConstructorWithFullList() throws GeneralCryptoLibException {

		BigInteger p = BigInteger.TEN;

		List<ZpGroupElement> ciphertext = new ArrayList<>();
		ZpGroupElement gamma = new ZpGroupElement(BigInteger.ONE, _group);

		List<ZpGroupElement> phis = getSamplePhisList(p, new BigInteger("2"), new BigInteger("4"));

		ciphertext.add(gamma);
		ciphertext.addAll(phis);

		BaseElGamalCiphertext<ZpGroupElement> values = new BaseElGamalCiphertext<>(ciphertext);

		Assertions.assertEquals(gamma, values.getGamma(), "The gamma has not the expected value");
		Assertions.assertEquals(phis, values.getPhis(), "The list of phis is not the expected one");
	}

	@Test
	void testGetValuesMethod() throws GeneralCryptoLibException {

		List<ZpGroupElement> ciphertext = new ArrayList<>();

		List<ZpGroupElement> phis = createPhis();

		ZpGroupElement gamma = createGamma();

		ciphertext.add(gamma);
		ciphertext.addAll(phis);

		BaseElGamalCiphertext<ZpGroupElement> values = new BaseElGamalCiphertext<>(ciphertext);

		Assertions.assertEquals(gamma, values.getValues().get(0), "The gamma has not the expected value");
		Assertions.assertEquals(phis, values.getValues().subList(1, values.getValues().size()), "The list of phis is not the expected one");
		Assertions.assertEquals(values.getValues().size(), values.getValues().size(), "The values list does not have the expected value");
	}

	@Test
	void testGetGamma() throws GeneralCryptoLibException {

		List<ZpGroupElement> phis = createPhis();
		ZpGroupElement gamma = createGamma();

		BaseElGamalCiphertext<ZpGroupElement> preComputationValues = new BaseElGamalCiphertext<>(gamma, phis);
		Assertions.assertEquals(gamma, preComputationValues.getGamma(), "The gamma returned by the get method is not the gamma expected");
	}

	@Test
	void testGetPrePhis() throws GeneralCryptoLibException {

		List<ZpGroupElement> phis = createPhis();
		ZpGroupElement gamma = createGamma();

		BaseElGamalCiphertext<ZpGroupElement> preComputationValues = new BaseElGamalCiphertext<>(gamma, phis);

		Assertions.assertEquals(phis, preComputationValues.getPhis(), "The phis returned are not the phis expected");
	}

	private List<ZpGroupElement> getSamplePhisList(final BigInteger p, final BigInteger phi1Value, final BigInteger phi2Value)
			throws GeneralCryptoLibException {
		List<ZpGroupElement> phis = new ArrayList<>();

		ZpGroupElement phi1 = new ZpGroupElement(phi1Value, _group);
		ZpGroupElement phi2 = new ZpGroupElement(phi2Value, _group);

		phis.add(phi1);
		phis.add(phi2);

		return phis;
	}

	private List<ZpGroupElement> createPhis() throws GeneralCryptoLibException {
		ZpGroupElement ge1 = new ZpGroupElement(BigInteger.ONE, _group);
		ZpGroupElement ge2 = new ZpGroupElement(new BigInteger("2"), _group);

		List<ZpGroupElement> phis = new ArrayList<>();
		phis.add(ge1);
		phis.add(ge2);

		return phis;
	}

	private ZpGroupElement createGamma() throws GeneralCryptoLibException {
		return new ZpGroupElement(BigInteger.ONE, _group);
	}
}
