/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.mathematical.primes;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Contains utility methods to work with prime numbers.
 */
public class PrimesUtils {

	private PrimesUtils() {
	}

	/**
	 * Obtains all of the prime values that are available from a source, and returns these values as a list of BigIntegers.
	 *
	 * <p>NOTE: this method attempts to read all of the primes in a file, and to return those primes
	 * in a list of type {@link BigInteger}. It is the responsibility of the caller of this method to ensure that the file to be read does not contain
	 * so much data that it affects performance.
	 *
	 * @return The list of primes.
	 */
	public static List<BigInteger> getPrimesList() {
		try (Stream<BigInteger> primes = getPrimes()) {
			return primes.collect(Collectors.toList());
		}
	}

	/**
	 * Returns the finite stream of the pre-computed primes.
	 *
	 * @return the primes.
	 */
	private static Stream<BigInteger> getPrimes() {
		InputStream stream = PrimesUtils.class.getResourceAsStream("/primes/primes.txt");
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
		return reader.lines().map(BigInteger::new);
	}
}
