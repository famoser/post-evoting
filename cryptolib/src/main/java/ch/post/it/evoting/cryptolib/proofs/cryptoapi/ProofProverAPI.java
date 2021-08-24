/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.cryptoapi;

import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Ciphertext;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Witness;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.proofs.bean.ProofPreComputedValues;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;

/**
 * Interface which defines an API that allows a number of Zero Knowledge Proofs (ZKPs) to be generated.
 *
 * <p>Note: some of the proofs that may be created using this API have very similar names, and they
 * are used for similar, but slightly different reasons, i.e. to prove different facts. Care must be taken to ensure that the correct type of proof is
 * being created.
 *
 * <p>Later, when a proof is being verified, care must be taken to ensure that the correct verify
 * method is used.
 */
public interface ProofProverAPI {

	/**
	 * Creates an "exponentiation proof".
	 *
	 * @param exponentiatedElements the list of exponentiated elements.
	 * @param baseElements          the list of base elements.
	 * @param witness               the witness.
	 * @return the created {@code Proof}.
	 * @throws GeneralCryptoLibException if the list of exponentiated elements or base elements is null, empty or contains one or more null elements,
	 *                                   or the witness is null.
	 */
	Proof createExponentiationProof(final List<ZpGroupElement> exponentiatedElements, final List<ZpGroupElement> baseElements, final Witness witness)
			throws GeneralCryptoLibException;

	/**
	 * Creates an "exponentiation proof", using pre-computed values.
	 *
	 * @param exponentiatedElements the list of exponentiated elements.
	 * @param baseElements          the list of base elements.
	 * @param witness               the witness.
	 * @param preComputedValues     the proof pre-computed values.
	 * @return the created {@code Proof}.
	 * @throws GeneralCryptoLibException if the list of exponentiated elements or base elements is null, empty or contains one or more null elements,
	 *                                   the witness is null, or the pre-computed values object is null.
	 */
	Proof createExponentiationProof(final List<ZpGroupElement> exponentiatedElements, final List<ZpGroupElement> baseElements, final Witness witness,
			final ProofPreComputedValues preComputedValues) throws GeneralCryptoLibException;

	/**
	 * Creates a "plaintext equality proof".
	 *
	 * @param primaryCiphertext   the primary ciphertext.
	 * @param primaryPublicKey    the primary public key.
	 * @param primaryWitness      the primary witness.
	 * @param secondaryCiphertext the secondary ciphertext.
	 * @param secondaryPublicKey  the secondary public key.
	 * @param secondaryWitness    the secondary witness.
	 * @return the created {@code Proof}.
	 * @throws GeneralCryptoLibException if the primary ciphertext, primary ElGamal public key, primary witness, secondary ciphertext, secondary
	 *                                   ElGamal public key or secondary witness is null.
	 */
	Proof createPlaintextEqualityProof(final Ciphertext primaryCiphertext, final ElGamalPublicKey primaryPublicKey, final Witness primaryWitness,
			final Ciphertext secondaryCiphertext, final ElGamalPublicKey secondaryPublicKey, final Witness secondaryWitness)
			throws GeneralCryptoLibException;

	/**
	 * Creates a "plaintext equality proof", using pre-computed values.
	 *
	 * @param primaryCiphertext   the primary ciphertext.
	 * @param primaryPublicKey    the primary public key.
	 * @param primaryWitness      the primary witness.
	 * @param secondaryCiphertext the secondary ciphertext.
	 * @param secondaryPublicKey  the secondary public key.
	 * @param secondaryWitness    the secondary witness.
	 * @param preComputedValues   the proof pre-computed values.
	 * @return the created {@code Proof}.
	 * @throws GeneralCryptoLibException if the primary ciphertext, primary ElGamal public key, primary witness, secondary ciphertext, secondary
	 *                                   ElGamal public key, secondary witness or pre-computed values object is null.
	 */
	Proof createPlaintextEqualityProof(final Ciphertext primaryCiphertext, final ElGamalPublicKey primaryPublicKey, final Witness primaryWitness,
			final Ciphertext secondaryCiphertext, final ElGamalPublicKey secondaryPublicKey, final Witness secondaryWitness,
			final ProofPreComputedValues preComputedValues) throws GeneralCryptoLibException;
}
