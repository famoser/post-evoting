/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.api.proofs;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofPreComputerAPI;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofProverAPI;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofVerifierAPI;

/**
 * Top-level API for functionality related to Zero Knowledge Proofs (ZKPs).
 *
 * <p>The primary functionalities that are required when working with ZKPs is the ability to
 * generate proofs and the ability to verify proofs. These functionalities are provided by the interfaces {@link ProofProverAPI} and {@link
 * ProofVerifierAPI} respectively, instances of these interfaces may be obtained using this interface.
 */
public interface ProofsServiceAPI {

	/**
	 * Creates a {@code ProofProverAPI} that may be used for generating various proofs. Sets {@code group} as the mathematical group over which the
	 * proofs operate.
	 *
	 * @param group the mathematical group that should be set in the created {@code ProofProverAPI}.
	 * @return a {@code ProofProverAPI} which provides methods for creating various proofs.
	 * @throws GeneralCryptoLibException if mathematical group is null.
	 */
	ProofProverAPI createProofProverAPI(MathematicalGroup<?> group) throws GeneralCryptoLibException;

	/**
	 * Creates a {@code ProofVerifierAPI} that may be used for verifying various proofs. Sets {@code group} as the mathematical group over which the
	 * proofs operate.
	 *
	 * @param group the mathematical group that should be set in the created {@code ProofVerifierAPI}.
	 * @return a {@code ProofVerifierAPI} which provides methods for verifying various proofs.
	 * @throws GeneralCryptoLibException if mathematical group is null.
	 */
	ProofVerifierAPI createProofVerifierAPI(MathematicalGroup<?> group) throws GeneralCryptoLibException;

	/**
	 * Creates a {@code ProofPreComputerAPI} that may be used for pre-computing the values needed for the generation of various proofs. Sets {@code
	 * group} as the mathematical group over which the proofs operate.
	 *
	 * @param group the mathematical group that should be set in the created {@code ProofPreComputerAPI}.
	 * @return a {@code ProofPreComputerAPI} which provides methods for pre-computing various proofs.
	 * @throws GeneralCryptoLibException if mathematical group is null.
	 */
	ProofPreComputerAPI createProofPreComputerAPI(MathematicalGroup<?> group) throws GeneralCryptoLibException;
}
