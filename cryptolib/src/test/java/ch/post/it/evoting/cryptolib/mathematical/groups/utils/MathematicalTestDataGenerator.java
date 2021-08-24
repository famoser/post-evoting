/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.mathematical.groups.utils;

import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.securerandom.CryptoAPIRandomInteger;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponents;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.primitives.securerandom.factory.CryptoRandomInteger;
import ch.post.it.evoting.cryptolib.primitives.service.PrimitivesService;
import ch.post.it.evoting.cryptolib.test.tools.configuration.GroupLoader;

/**
 * Utility to generate various types of mathematical data needed by tests.
 */
public class MathematicalTestDataGenerator {

	/**
	 * Retrieves a pre-generated quadratic residue subgroup.
	 *
	 * @return the pre-generated quadratic residue subgroup.
	 * @throws GeneralCryptoLibException if the quadratic residue subgroup cannot be retrieved.
	 */
	public static ZpSubgroup getQrSubgroup() throws GeneralCryptoLibException {

		GroupLoader qrGroupLoader = new GroupLoader();

		return new ZpSubgroup(qrGroupLoader.getG(), qrGroupLoader.getP(), qrGroupLoader.getQ());
	}

	/**
	 * Retrieves another pre-generated quadratic residue subgroup.
	 *
	 * @return the pre-generated quadratic residue subgroup.
	 * @throws GeneralCryptoLibException if the quadratic residue subgroup cannot be retrieved.
	 */
	public static ZpSubgroup getOtherQrSubgroup() throws GeneralCryptoLibException {

		GroupLoader qrGroupLoader = new GroupLoader(2);

		return new ZpSubgroup(qrGroupLoader.getG(), qrGroupLoader.getP(), qrGroupLoader.getQ());
	}

	/**
	 * Randomly generates an exponent for a specified Zp subgroup.
	 *
	 * @param zpSubgroup the Zp subgroup to which the generated exponent is to belong.
	 * @return the generated exponent.
	 * @throws GeneralCryptoLibException if the exponent generation process fails.
	 */
	public static Exponent getExponent(final ZpSubgroup zpSubgroup) throws GeneralCryptoLibException {

		CryptoAPIRandomInteger cryptoRandomInteger = new PrimitivesService().getCryptoRandomInteger();

		return Exponents.random(zpSubgroup, cryptoRandomInteger);
	}

	/**
	 * Randomly generates a list of exponents for a specified Zp subgroup.
	 *
	 * @param zpSubgroup   the Zp subgroup to which the generated exponents are to belong.
	 * @param numExponents the number of exponents to generate.
	 * @return the randomly generated list of exponents.
	 * @throws GeneralCryptoLibException if the exponent generation process fails.
	 */
	public static List<Exponent> getExponents(final ZpSubgroup zpSubgroup, final int numExponents) throws GeneralCryptoLibException {

		List<Exponent> exponents = new ArrayList<>();
		for (int i = 0; i < numExponents; i++) {
			exponents.add(getExponent(zpSubgroup));
		}

		return exponents;
	}

	/**
	 * Randomly generates a short exponent for a specified Zp subgroup.
	 *
	 * @param zpSubgroup the Zp subgroup to which the generated short exponent is to belong.
	 * @return the generated short exponent.
	 * @throws GeneralCryptoLibException if the short exponent generation process fails.
	 */
	public static Exponent getShortExponent(final ZpSubgroup zpSubgroup) throws GeneralCryptoLibException {

		CryptoAPIRandomInteger cryptoRandomInteger = new PrimitivesService().getCryptoRandomInteger();

		return Exponents.shortRandom(zpSubgroup, (CryptoRandomInteger) cryptoRandomInteger);
	}

	/**
	 * Randomly generates a Zp group element for a specified Zp subgroup.
	 *
	 * @param zpSubgroup the Zp subgroup to which the generated Zp group element is to belong.
	 * @return the generated Zp group element.
	 * @throws GeneralCryptoLibException if the Zp group element generation process fails.
	 */
	public static ZpGroupElement getZpGroupElement(final ZpSubgroup zpSubgroup) throws GeneralCryptoLibException {

		return zpSubgroup.getGenerator().exponentiate(getExponent(zpSubgroup));
	}

	/**
	 * Randomly generates a list of Zp group elements belonging to a specified Zp subgroup.
	 *
	 * @param zpSubgroup  the Zp subgroup to which the generated Zp group elements are to belong.
	 * @param numElements the number of Zp group elements to generate.
	 * @return the randomly generated list of Zp group elements.
	 * @throws GeneralCryptoLibException if the Zp group element generation process fails.
	 */
	public static List<ZpGroupElement> getZpGroupElements(final ZpSubgroup zpSubgroup, final int numElements) throws GeneralCryptoLibException {

		List<ZpGroupElement> zpGroupElements = new ArrayList<>();
		for (int i = 0; i < numElements; i++) {
			zpGroupElements.add(getZpGroupElement(zpSubgroup));
		}

		return zpGroupElements;
	}

	/**
	 * Converts a list of Zp group elements to a list of their corresponding string representations.
	 *
	 * @param zpGroupElements the Zp group elements to convert.
	 * @return the list of Zp group elements as strings.
	 * @throws GeneralCryptoLibException if the Zp group element to string conversion process fails.
	 */
	public static List<String> zpGroupElementsToStrings(final List<ZpGroupElement> zpGroupElements) throws GeneralCryptoLibException {

		List<String> zpGroupElementsAsStrings = new ArrayList<>();
		for (ZpGroupElement element : zpGroupElements) {
			zpGroupElementsAsStrings.add(element.getValue().toString());
		}

		return zpGroupElementsAsStrings;
	}

	/**
	 * Exponentiates a specified list of Zp group elements with a specified exponent.
	 *
	 * @param baseElements the list of Zp group elements to exponentiate.
	 * @param exponent     the exponent to use for the exponentiation.
	 * @return the list of exponentiated Zp group elements.
	 * @throws GeneralCryptoLibException if the exponentiation process fails.
	 */
	public static List<ZpGroupElement> exponentiateZpGroupElements(final List<ZpGroupElement> baseElements, final Exponent exponent)
			throws GeneralCryptoLibException {

		List<ZpGroupElement> exponentiatedElements = new ArrayList<>();
		for (ZpGroupElement baseElement : baseElements) {
			exponentiatedElements.add(baseElement.exponentiate(exponent));
		}

		return exponentiatedElements;
	}
}
