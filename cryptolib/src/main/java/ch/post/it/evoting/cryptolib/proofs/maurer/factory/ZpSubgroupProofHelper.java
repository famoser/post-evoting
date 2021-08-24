/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.factory;

import java.math.BigInteger;
import java.util.stream.Stream;

import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

final class ZpSubgroupProofHelper {

	private ZpSubgroupProofHelper() {
	}

	/**
	 * Ensure that all supplied group elements expose the same ℤₚ⃰ subgroup. The first element found in violation of the group membership stops the
	 * search.
	 *
	 * @param zpSubgroup the reference ℤₚ⃰ subgroup
	 * @param elements   the elements to be checked.
	 * @throws IllegalArgumentException if any element is not in the expected ℤₚ⃰ subgroup
	 */
	static void validateGroup(ZpSubgroup zpSubgroup, Stream<ZpGroupElement> elements) {
		elements.forEach(element -> validateGroup(zpSubgroup, element));
	}

	/**
	 * Ensure that the supplied group element exposes the specified ℤₚ⃰ subgroup.
	 *
	 * @param zpSubgroup the reference ℤₚ⃰ subgroup
	 * @param element    the element to be checked.
	 * @throws IllegalArgumentException if the element is not in the expected ℤₚ⃰ subgroup
	 */
	static void validateGroup(ZpSubgroup zpSubgroup, ZpGroupElement element) {
		if (!(element.getP().equals(zpSubgroup.getP()) && element.getQ().equals(zpSubgroup.getQ()))) {
			throw new IllegalArgumentException("Not all elements belong to the same ℤₚ⃰ subgroup");
		}
	}

	/**
	 * Ensure that all supplied group elements expose the same ℤₚ⃰ subgroup's order.
	 *
	 * @param order     the reference ℤₚ⃰ subgroup's order
	 * @param exponents the exponents to be checked.
	 * @throws IllegalArgumentException if any exponent diverges from the ℤₚ⃰ subgroup's order
	 */
	static void validateOrder(BigInteger order, Stream<Exponent> exponents) {
		exponents.forEach(exponent -> validateOrder(order, exponent));
	}

	/**
	 * Ensure that the supplied group elements's order is the same as the supplied ℤₚ⃰ subgroup's.
	 *
	 * @param order    the reference ℤₚ⃰ subgroup's order
	 * @param exponent the exponent to be checked.
	 * @throws IllegalArgumentException if the exponent's order is not the same as the ℤₚ⃰ subgroup's
	 */
	static void validateOrder(BigInteger order, Exponent exponent) {
		if (!exponent.getQ().equals(order)) {
			throw new IllegalArgumentException("Not all exponents share the same ℤₚ⃰ subgroup order");
		}
	}
}
