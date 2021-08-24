/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.config.commands.primes;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptolib.mathematical.MathematicalUtils;
import ch.post.it.evoting.cryptolib.mathematical.primes.PrimesUtils;

/**
 * Allows the generation of ElGamal data parameters.
 */
@Service
public final class PrimeGroupMembersProvider {

	/**
	 * Generates an ElGamalDataParameters using the received encryption parameters.
	 *
	 * @param encryptionParameterP The ElGamal encryption parameter P.
	 * @param encryptionParameterQ The ElGamal encryption parameter Q.
	 * @return a list of prime group numbers.
	 */
	public List<BigInteger> generateVotingOptionRepresentations(final BigInteger encryptionParameterP, final BigInteger encryptionParameterQ,
			final BigInteger encryptionParameterG) {
		List<BigInteger> listOfPrimes = PrimesUtils.getPrimesList();
		listOfPrimes.remove(encryptionParameterG);

		// Check validity of primes list for the vote
		validateListOfPrimes(listOfPrimes, encryptionParameterP);

		return MathematicalUtils.findGroupMembers(encryptionParameterP, encryptionParameterQ, listOfPrimes.stream()).collect(Collectors.toList());
	}

	/**
	 * This method validates that:
	 * - all provided numbers in the list are effectively prime,
	 * - the product of the 120 largest primes is smaller than the ElGamal encryption parameter p.
	 * <p>
	 * Why 120 ?
	 * At most, we support elections where a voter can select 120 candidates (possibly out of > 1'000 candidates).
	 * Therefore, we need to check that multiplying the 120 largest primes yields a value smaller than p to prevent modulo overflow.
	 *
	 * @param listOfPrimes         Primes list to validate.
	 * @param encryptionParameterP The ElGamal encryption parameter P.
	 */
	void validateListOfPrimes(List<BigInteger> listOfPrimes, BigInteger encryptionParameterP) {
		// Check primality of each number in the list
		if (listOfPrimes.stream().anyMatch(
				primeCandidate -> !primeCandidate.isProbablePrime(MathematicalUtils.getCertaintyForLength(encryptionParameterP.bitLength())))) {
			throw new IllegalArgumentException("At least one number of the listOfPrimes is not prime");
		}

		// Validate that the product of largest primes is smaller than encryptionParameterP
		BigInteger primesProduct = listOfPrimes.stream().sorted(Comparator.reverseOrder()).limit(120).reduce(BigInteger.ONE, BigInteger::multiply);
		if (primesProduct.compareTo(encryptionParameterP) >= 0) {
			throw new IllegalArgumentException("The product of the 120 largest primes must be smaller than the ElGamal encryption parameter p");
		}
	}
}
