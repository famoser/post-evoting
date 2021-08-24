/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.mathematical;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.stream.Stream;

import ch.post.it.evoting.cryptolib.mathematical.bigintegers.BigIntegers;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

/**
 * Exposes mathematical operations.
 */
public class MathematicalUtils {

	private MathematicalUtils() {
	}

	/**
	 * Gets the certainty level corresponding to the bit length of the number, according to FIPS-186-4 Table C.1.
	 *
	 * @param length length in bits.
	 * @return a certainty level.
	 */
	public static int getCertaintyForLength(final int length) {
		if (length <= 1024) {
			return 80;
		} else if (length <= 2048) {
			return 112;
		} else {
			return 128;
		}
	}

	/**
	 * Finds which members of a list of numbers belong to the supplied mathematical group.
	 *
	 * @param modulus    the modulus (P) of the multiplicative group to test for membership against.
	 * @param order      the order (Q) of the multiplicative group to test for membership against.
	 * @param candidates the numbers to test for membership.
	 * @return the numbers, out of the supplied list, that belong to the group.
	 * @throws NullPointerException if any input is null.
	 */
	public static Stream<BigInteger> findGroupMembers(final BigInteger modulus, final BigInteger order, final Stream<BigInteger> candidates) {
		checkNotNull(modulus);
		checkNotNull(order);
		checkNotNull(candidates);

		return candidates.filter(candidate -> BigInteger.ONE.equals(BigIntegers.modPow(candidate, order, modulus)));
	}

	/**
	 * Checks that an element belongs to a group.
	 *
	 * @param group   the group to verify against.
	 * @param element the element to verify.
	 * @throws IllegalArgumentException if the element is not a member of the group.
	 * @throws NullPointerException     if any input is null.
	 */
	public static void checkGroupMembership(final ZpSubgroup group, final ZpGroupElement element) {
		checkNotNull(group);
		checkNotNull(element);

		checkArgument(group.isGroupMember(element), String.format("Element %s is not a group member", element.getValue()));
	}

	/**
	 * Checks that an exponent belongs to a group.
	 *
	 * @param group    the group to verify against.
	 * @param exponent the exponent to verify.
	 * @throws IllegalArgumentException if the exponent is not a member of the group.
	 * @throws NullPointerException     if any input is null.
	 */
	public static void checkGroupMembership(final ZpSubgroup group, final Exponent exponent) {
		checkNotNull(group);
		checkNotNull(exponent);

		checkArgument(group.getQ().equals(exponent.getQ()), String.format("Exponent %s is not a group member", exponent.getValue()));
	}
}
