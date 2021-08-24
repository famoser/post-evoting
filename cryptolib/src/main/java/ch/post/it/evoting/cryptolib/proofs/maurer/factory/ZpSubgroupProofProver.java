/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.factory;

import static ch.post.it.evoting.cryptolib.proofs.maurer.factory.ZpSubgroupProofHelper.validateGroup;
import static ch.post.it.evoting.cryptolib.proofs.maurer.factory.ZpSubgroupProofHelper.validateOrder;

import java.util.List;
import java.util.Objects;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.securerandom.CryptoAPIRandomInteger;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Ciphertext;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Witness;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.primitives.securerandom.factory.CryptoRandomInteger;
import ch.post.it.evoting.cryptolib.proofs.bean.ProofPreComputedValues;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofProverAPI;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;
import ch.post.it.evoting.cryptolib.proofs.utils.HashBuilder;

/**
 * Implementation of {@link ProofProverAPI}, which allows for the creation of several types of proofs.
 */
public final class ZpSubgroupProofProver implements ProofProverAPI {

	private static final String PRECOMPUTED_VALUES_OBJECT = "Pre-computed values object";
	private final ZpSubgroup group;
	private final CryptoAPIRandomInteger cryptoRandomInteger;
	private final HashBuilder hashBuilder;

	/**
	 * @param group               the Zp subgroup used for ElGamal encryption.
	 * @param hashBuilder         The hash builder used to generate and verify the proofs.
	 * @param cryptoRandomInteger the random number generator used to generate the proof.
	 */
	ZpSubgroupProofProver(final MathematicalGroup<?> group, HashBuilder hashBuilder, final CryptoRandomInteger cryptoRandomInteger) {
		Objects.requireNonNull(group, "A ℤₚ⃰ subgroup is required");
		Objects.requireNonNull(hashBuilder, "A hash builder is required");
		Objects.requireNonNull(cryptoRandomInteger, "A random integer generator is required");

		this.group = (ZpSubgroup) group;
		this.hashBuilder = hashBuilder;
		this.cryptoRandomInteger = cryptoRandomInteger;
	}

	private static void validateExponentiationProofGeneratorInput(final List<ZpGroupElement> exponentiatedElements,
			final List<ZpGroupElement> baseElements, final Witness witness) throws GeneralCryptoLibException {

		Validate.notNullOrEmptyAndNoNulls(exponentiatedElements, "List of exponentiated elements");
		Validate.notNullOrEmptyAndNoNulls(baseElements, "List of base elements");
		Validate.notNull(witness, "Witness");
		Validate.isEqual(exponentiatedElements.size(), baseElements.size(), "Number of exponentiated elements", "number of base elements");
	}

	private static void validatePlaintextEqualityProofGeneratorInput(final Ciphertext primaryCiphertext, final ElGamalPublicKey primaryPublicKey,
			final Witness primaryWitness, final Ciphertext secondaryCiphertext, final ElGamalPublicKey secondaryPublicKey,
			final Witness secondaryWitness) throws GeneralCryptoLibException {

		Validate.notNull(primaryCiphertext, "Primary ciphertext");
		Validate.notNull(primaryPublicKey, "Primary ElGamal public key");
		Validate.notNull(primaryWitness, "Primary witness");
		Validate.notNull(secondaryCiphertext, "Secondary ciphertext");
		Validate.notNull(secondaryPublicKey, "Secondary ElGamal public key");
		Validate.notNull(secondaryWitness, "Secondary witness");
		int primaryCiphertextLength = primaryCiphertext.size();
		int primaryPublicKeyLength = primaryPublicKey.getKeys().size();
		int secondaryCiphertextLength = secondaryCiphertext.size();
		int secondaryPublicKeyLength = secondaryPublicKey.getKeys().size();
		Validate.isEqual(primaryCiphertextLength, secondaryCiphertextLength, "Primary ciphertext length", "secondary ciphertext length");
		Validate.isEqual(primaryPublicKeyLength, secondaryPublicKeyLength, "Primary ElGamal public key length",
				"secondary ElGamal public key length");
		Validate.isEqual(primaryCiphertextLength, primaryPublicKeyLength + 1, "Ciphertext length", "ElGamal public key length plus 1");
	}

	@Override
	public Proof createExponentiationProof(final List<ZpGroupElement> exponentiatedElements, final List<ZpGroupElement> baseElements,
			final Witness witness) throws GeneralCryptoLibException {

		validateExponentiationProofGeneratorInput(exponentiatedElements, baseElements, witness);

		ProofPreComputedValues preComputedValues = new ExponentiationProofPreComputer<>(baseElements, group, hashBuilder)
				.preCompute(cryptoRandomInteger);

		return createExponentiationProof(exponentiatedElements, baseElements, witness, preComputedValues);
	}

	@Override
	public Proof createExponentiationProof(final List<ZpGroupElement> exponentiatedElements, final List<ZpGroupElement> baseElements,
			final Witness witness, final ProofPreComputedValues preComputedValues) throws GeneralCryptoLibException {

		validateExponentiationProofGeneratorInput(exponentiatedElements, baseElements, witness);

		Validate.notNull(preComputedValues, PRECOMPUTED_VALUES_OBJECT);

		ZpSubgroupProofHelper.validateGroup(group, exponentiatedElements.stream());
		ZpSubgroupProofHelper.validateGroup(group, baseElements.stream());
		validateOrder(group.getQ(), witness.getExponent());

		return new ExponentiationProofGenerator<>(exponentiatedElements, baseElements, witness.getExponent(), group, hashBuilder, preComputedValues)
				.generate();
	}

	@Override
	public Proof createPlaintextEqualityProof(final Ciphertext primaryCiphertext, final ElGamalPublicKey primaryPublicKey,
			final Witness primaryWitness, final Ciphertext secondaryCiphertext, final ElGamalPublicKey secondaryPublicKey,
			final Witness secondaryWitness) throws GeneralCryptoLibException {

		validatePlaintextEqualityProofGeneratorInput(primaryCiphertext, primaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey,
				secondaryWitness);

		ProofPreComputedValues preComputedValues = new PlaintextEqualityProofPreComputer<>(primaryPublicKey, secondaryPublicKey, group, hashBuilder)
				.preCompute(cryptoRandomInteger);

		return createPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey,
				secondaryWitness, preComputedValues);
	}

	@Override
	public Proof createPlaintextEqualityProof(final Ciphertext primaryCiphertext, final ElGamalPublicKey primaryPublicKey,
			final Witness primaryWitness, final Ciphertext secondaryCiphertext, final ElGamalPublicKey secondaryPublicKey,
			final Witness secondaryWitness, final ProofPreComputedValues preComputedValues) throws GeneralCryptoLibException {

		validatePlaintextEqualityProofGeneratorInput(primaryCiphertext, primaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey,
				secondaryWitness);

		Validate.notNull(preComputedValues, PRECOMPUTED_VALUES_OBJECT);

		Validate.notNull(primaryCiphertext, "Primary ciphertext");
		Validate.notNull(primaryPublicKey, "Primary ElGamal public key");
		Validate.notNull(primaryWitness, "Primary witness");
		Validate.notNull(secondaryCiphertext, "Secondary ciphertext");
		Validate.notNull(secondaryPublicKey, "Secondary ElGamal public key");
		Validate.notNull(secondaryWitness, "Secondary witness");

		validateGroup(group, primaryCiphertext.getElements().stream());
		validateGroup(group, primaryPublicKey.getKeys().stream());
		validateOrder(group.getQ(), primaryWitness.getExponent());
		validateGroup(group, secondaryCiphertext.getElements().stream());
		validateGroup(group, secondaryPublicKey.getKeys().stream());
		validateOrder(group.getQ(), secondaryWitness.getExponent());

		return new PlaintextEqualityProofGenerator<>(primaryCiphertext.getElements(), primaryPublicKey, primaryWitness.getExponent(),
				secondaryCiphertext.getElements(), secondaryPublicKey, secondaryWitness.getExponent(), group, hashBuilder, preComputedValues)
				.generate();
	}
}
