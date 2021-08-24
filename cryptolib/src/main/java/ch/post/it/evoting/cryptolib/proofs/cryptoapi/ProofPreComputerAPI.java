/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.cryptoapi;

import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.proofs.bean.ProofPreComputedValues;

/**
 * Interface that allows for the pre-computation of values needed to generate a zero knowledge proof. Since the pre-computation process is independent
 * of the user data and since it dominates the generation times, performing this process ahead of time can result in considerable savings in time.
 */
public interface ProofPreComputerAPI {

	/**
	 * Pre-computes the values needed to generate an exponentiation zero knowledge proof.
	 *
	 * @param baseElements the list of base elements used for the exponentiation.
	 * @return the proof pre-computed values.
	 * @throws GeneralCryptoLibException if the list of base elements is null, empty or contains one or more null elements, or if the pre-computation
	 *                                   process fails.
	 */
	ProofPreComputedValues preComputeExponentiationProof(final List<ZpGroupElement> baseElements) throws GeneralCryptoLibException;

	/**
	 * Pre-computes the values needed to generate a plaintext equality zero knowledge proof.
	 *
	 * @param primaryPublicKey   the ElGamal public key used to encrypt the primary plaintext.
	 * @param secondaryPublicKey the ElGamal public key used to encrypt the secondary plaintext.
	 * @return the proof pre-computed values.
	 * @throws GeneralCryptoLibException if the primary or secondary ElGamal public key is null, or if the pre-computation process fails.
	 */
	ProofPreComputedValues preComputePlaintextEqualityProof(final ElGamalPublicKey primaryPublicKey, final ElGamalPublicKey secondaryPublicKey)
			throws GeneralCryptoLibException;
}
