/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.cryptolib.elgamal.exponentiation;

import java.math.BigInteger;
import java.util.List;

import javax.annotation.concurrent.ThreadSafe;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

/**
 * Exponentiation service.
 */
@ThreadSafe
public interface ExponentiationService {
	/**
	 * Exponentiate given cleartexts treating them as elements of the specified group.
	 *
	 * @param cleartexts the cleartexts
	 * @param exponent   the exponent
	 * @param group      the group
	 * @return the exponentiatedElements and exponentiationProof
	 * @throws GeneralCryptoLibException failed to exponentiate the cleartexts.
	 */
	ExponentiatedElementsAndProof<BigInteger> exponentiateCleartexts(List<BigInteger> cleartexts, Exponent exponent, ZpSubgroup group)
			throws GeneralCryptoLibException;

	/**
	 * Exponentiate given ElGamal cipertexts treating them as elements of the specified group.
	 *
	 * @param ciphertexts the ciphertexts
	 * @param exponent    the exponent
	 * @param group       the group
	 * @return the exponentiatedElements and exponentiationProof
	 * @throws GeneralCryptoLibException failed to exponentiate the cleartexts.
	 */
	ExponentiatedElementsAndProof<ElGamalCiphertext> exponentiateCiphertexts(List<ElGamalCiphertext> ciphertexts, Exponent exponent, ZpSubgroup group)
			throws GeneralCryptoLibException;
}
