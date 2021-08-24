/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.factory;

import ch.post.it.evoting.cryptolib.CryptolibFactory;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;
import ch.post.it.evoting.cryptolib.primitives.securerandom.factory.CryptoRandomInteger;
import ch.post.it.evoting.cryptolib.primitives.securerandom.factory.SecureRandomFactory;
import ch.post.it.evoting.cryptolib.proofs.maurer.configuration.MaurerProofPolicy;
import ch.post.it.evoting.cryptolib.proofs.utils.HashBuilder;

/**
 * Factory class that can create prover, verifier and pre-computer objects, as well as implementations of the prover, verifier and pre-computer APIs.
 */
public final class MaurerUnifiedProofFactory extends CryptolibFactory {

	private final MaurerProofPolicy maurerProofPolicy;
	private final HashBuilder hashBuilder;

	/**
	 * Creates an instance of the class and initializes it with the provided policy.
	 *
	 * @param maurerProofPolicy the policy.
	 */
	public MaurerUnifiedProofFactory(final MaurerProofPolicy maurerProofPolicy, HashBuilder hashBuilder) {

		this.maurerProofPolicy = maurerProofPolicy;
		this.hashBuilder = hashBuilder;
	}

	/**
	 * Creates a {@code ZpSubgroupProofProver} that may be used for generating various proofs.
	 *
	 * @param group the mathematical group over which the proofs operate.
	 * @return a {@code ProofProverAPI} which provides methods for creating various proofs.
	 */
	public ZpSubgroupProofProver createProofCreationAPI(final MathematicalGroup<?> group) {

		CryptoRandomInteger cryptoRandomInteger = new SecureRandomFactory(maurerProofPolicy).createIntegerRandom();

		return new ZpSubgroupProofProver(group, hashBuilder, cryptoRandomInteger);
	}

	/**
	 * Creates a {@code ZpSubgroupProofVerifier} that may be used for verifying various proofs. Sets {@code group} as the mathematical group over
	 * which the proofs operate.
	 *
	 * @param group the mathematical group that should be set in the created {@code ProofVerifierAPI}.
	 * @return a {@code ZpSubgroupProofVerifier} which provides methods for verifying various proofs.
	 */
	public ZpSubgroupProofVerifier createProofVerificationAPI(final MathematicalGroup<?> group) {

		return new ZpSubgroupProofVerifier(group, hashBuilder);
	}

	/**
	 * Creates a {@code ZpSubgroupProofPreComputer} that may be used for pre-computing values needed to generate or verify various proofs. Sets {@code
	 * group} as the mathematical group over which the proofs operate.
	 *
	 * @param group the mathematical group that should be set in the created {@code ProofPreComputerAPI}.
	 * @return a {@code ZpSubgroupProofPreComputer} which provides methods for pre-computing the proof generation or verification values.
	 */
	public ZpSubgroupProofPreComputer createProofPreComputationAPI(final MathematicalGroup<?> group) {

		CryptoRandomInteger cryptoRandomInteger = new SecureRandomFactory(maurerProofPolicy).createIntegerRandom();

		return new ZpSubgroupProofPreComputer(group, hashBuilder, cryptoRandomInteger);
	}
}
