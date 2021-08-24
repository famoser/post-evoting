/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.mathematical;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class MathematicalUtilsTest {

	@Test
	void findGroupMembers() {
		Collection<BigInteger> expectedMembers = new ArrayList<>();
		// 12, 23, 34, 45
		expectedMembers.add(BigInteger.valueOf(12));
		expectedMembers.add(BigInteger.valueOf(34));
		expectedMembers.add(BigInteger.valueOf(23));
		expectedMembers.add(BigInteger.valueOf(45));

		Stream<BigInteger> candidates = IntStream.range(2, 50).mapToObj(BigInteger::valueOf);

		Set<BigInteger> members = MathematicalUtils.findGroupMembers(BigInteger.valueOf(11), BigInteger.valueOf(9), candidates)
				.collect(Collectors.toSet());
		assertTrue(members.containsAll(expectedMembers));

		Set<BigInteger> remainingMembers = new HashSet<>(members);
		expectedMembers.forEach(remainingMembers::remove);
		assertTrue(remainingMembers.isEmpty());
	}

}
