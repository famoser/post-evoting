/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.factory;

import static ch.post.it.evoting.cryptolib.proofs.maurer.factory.ZpSubgroupProofHelper.validateGroup;
import static ch.post.it.evoting.cryptolib.proofs.maurer.factory.ZpSubgroupProofHelper.validateOrder;

import java.util.List;
import java.util.Objects;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Ciphertext;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofVerifierAPI;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;
import ch.post.it.evoting.cryptolib.proofs.utils.HashBuilder;

/**
 * Implementation of {@link ProofVerifierAPI}, which allows for the verification of several types of proofs.
 */
public final class ZpSubgroupProofVerifier implements ProofVerifierAPI {

	private final ZpSubgroup group;

	private final HashBuilder hashBuilder;

	/**
	 * @param group       the Zp subgroup used for ElGamal encryption.
	 * @param hashBuilder The hash builder used to generate and verify the proofs.
	 */
	ZpSubgroupProofVerifier(final MathematicalGroup<?> group, HashBuilder hashBuilder) {
		Objects.requireNonNull(group, "A ℤₚ⃰ subgroup is required");
		Objects.requireNonNull(hashBuilder, "A hash builder is required");

		this.group = (ZpSubgroup) group;
		this.hashBuilder = hashBuilder;
	}

	@Override
	public boolean verifyExponentiationProof(final List<ZpGroupElement> exponentiatedElements, final List<ZpGroupElement> baseElements,
			final Proof proof) throws GeneralCryptoLibException {

		validateExponentiationProofVerifierInput(exponentiatedElements, baseElements, proof);

		ExponentiationProofVerifier<ZpGroupElement> verifier = new ExponentiationProofVerifier<>(exponentiatedElements, baseElements, proof, group,
				hashBuilder);

		return verifier.verify();
	}

	@Override
	public boolean verifyPlaintextEqualityProof(final Ciphertext primaryCiphertext, final ElGamalPublicKey primaryPublicKey,
			final Ciphertext secondaryCiphertext, final ElGamalPublicKey secondaryPublicKey, final Proof proof) throws GeneralCryptoLibException {

		validatePlaintextEqualityProofVerifierInput(primaryCiphertext, primaryPublicKey, secondaryCiphertext, secondaryPublicKey, proof);

		PlaintextEqualityProofVerifier<ZpGroupElement> verifier = new PlaintextEqualityProofVerifier<>(primaryCiphertext.getElements(),
				primaryPublicKey, secondaryCiphertext.getElements(), secondaryPublicKey, proof, group, hashBuilder);

		return verifier.verify();
	}

	private void validateExponentiationProofVerifierInput(final List<ZpGroupElement> exponentiatedElements, final List<ZpGroupElement> baseElements,
			final Proof proof) throws GeneralCryptoLibException {

		Validate.notNullOrEmptyAndNoNulls(exponentiatedElements, "List of exponentiated elements");
		Validate.notNullOrEmptyAndNoNulls(baseElements, "List of base elements");
		Validate.notNull(proof, "Exponentiation proof");
		Validate.isEqual(exponentiatedElements.size(), baseElements.size(), "Number of exponentiated elements", "number of base elements");
		ZpSubgroupProofHelper.validateGroup(group, exponentiatedElements.stream());
		ZpSubgroupProofHelper.validateGroup(group, baseElements.stream());
		validateOrder(group.getQ(), proof.getValuesList().stream());
		validateOrder(group.getQ(), proof.getHashValue());
	}

	private void validatePlaintextEqualityProofVerifierInput(final Ciphertext primaryCiphertext, final ElGamalPublicKey primaryPublicKey,
			final Ciphertext secondaryCiphertext, final ElGamalPublicKey secondaryPublicKey, final Proof proof) throws GeneralCryptoLibException {

		Validate.notNull(primaryCiphertext, "Primary ciphertext");
		Validate.notNull(primaryPublicKey, "Primary ElGamal public key");
		Validate.notNull(secondaryCiphertext, "Secondary ciphertext");
		Validate.notNull(secondaryPublicKey, "Secondary ElGamal public key");
		Validate.notNull(proof, "Plaintext equality proof");
		int primaryCiphertextLength = primaryCiphertext.size();
		int primaryPublicKeyLength = primaryPublicKey.getKeys().size();
		int secondaryCiphertextLength = secondaryCiphertext.size();
		int secondaryPublicKeyLength = secondaryPublicKey.getKeys().size();
		Validate.isEqual(primaryCiphertextLength, secondaryCiphertextLength, "Primary ciphertext length", "secondary ciphertext length");
		Validate.isEqual(primaryPublicKeyLength, secondaryPublicKeyLength, "Primary ElGamal public key length",
				"secondary ElGamal public key length");
		Validate.isEqual(primaryCiphertextLength, primaryPublicKeyLength + 1, "Ciphertext length", "ElGamal public key length plus 1");
		validateGroup(group, primaryPublicKey.getKeys().stream());
		validateGroup(group, secondaryPublicKey.getKeys().stream());
		validateGroup(group, primaryCiphertext.getElements().stream());
		validateGroup(group, secondaryCiphertext.getElements().stream());
		validateOrder(group.getQ(), proof.getValuesList().stream());
		validateOrder(group.getQ(), proof.getHashValue());
	}
}
