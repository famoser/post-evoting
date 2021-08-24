/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.cryptoapi;

import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Ciphertext;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;

/**
 * Interface which defines an API that allows a number of Zero Knowledge Proofs (ZKPs) to be verified.
 *
 * <p>Note: some of the proofs that can be verified using this API have very similar names. Care
 * must be taken to ensure that the correct verify method is used to verify a proof of a particular type.
 */
public interface ProofVerifierAPI {

	/**
	 * Verifies an "exponentiation proof".
	 *
	 * @param exponentiatedElements the list of exponentiated elements.
	 * @param baseElements          the list of base elements.
	 * @param proof                 the proof to be verified.
	 * @return true if {@code proof} is verified as true, false otherwise.
	 * @throws GeneralCryptoLibException if the list of exponentiated elements or base elements is null, empty or contains one or more null elements,
	 *                                   or the exponentiation proof is null.
	 */
	boolean verifyExponentiationProof(final List<ZpGroupElement> exponentiatedElements, final List<ZpGroupElement> baseElements, final Proof proof)
			throws GeneralCryptoLibException;

	/**
	 * Verifies a "plaintext equality proof".
	 *
	 * @param primaryCiphertext   the primary ciphertext.
	 * @param primaryPublicKey    the primary public key.
	 * @param secondaryCiphertext the secondary ciphertext.
	 * @param secondaryPublicKey  the secondary public key.
	 * @param proof               the proof to be verified.
	 * @return true if {@code proof} is verified successfully, false otherwise.
	 * @throws GeneralCryptoLibException if the primary ciphertext, primary ElGamal public key, primary witness, secondary ciphertext, secondary
	 *                                   ElGamal public key or plaintext equality proof is null.
	 */
	boolean verifyPlaintextEqualityProof(final Ciphertext primaryCiphertext, final ElGamalPublicKey primaryPublicKey,
			final Ciphertext secondaryCiphertext, final ElGamalPublicKey secondaryPublicKey, final Proof proof) throws GeneralCryptoLibException;
}
