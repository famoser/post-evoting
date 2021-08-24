/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.factory;

import java.util.List;
import java.util.Objects;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.primitives.securerandom.factory.CryptoRandomInteger;
import ch.post.it.evoting.cryptolib.proofs.bean.ProofPreComputedValues;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofPreComputerAPI;
import ch.post.it.evoting.cryptolib.proofs.utils.HashBuilder;

/**
 * Implementation of {@link ProofPreComputerAPI}, which allows for the pre-computation of values needed to generate various types of zero knowledge
 * proofs of knowledge.
 */
public class ZpSubgroupProofPreComputer implements ProofPreComputerAPI {

	private final ZpSubgroup zpSubgroup;

	private final CryptoRandomInteger cryptoRandomInteger;

	private final HashBuilder hashBuilder;

	/**
	 * @param group               the Zp subgroup used for ElGamal encryption.
	 * @param hashBuilder         The hash builder used to generate and verify the proofs.
	 * @param cryptoRandomInteger the random number generator used to generate the proof.
	 */
	ZpSubgroupProofPreComputer(final MathematicalGroup<?> group, HashBuilder hashBuilder, final CryptoRandomInteger cryptoRandomInteger) {
		Objects.requireNonNull(group, "A ℤₚ⃰ subgroup is required");
		Objects.requireNonNull(hashBuilder, "A hash builder is required");
		Objects.requireNonNull(cryptoRandomInteger, "A random integer generator is required");

		this.zpSubgroup = (ZpSubgroup) group;
		this.hashBuilder = hashBuilder;
		this.cryptoRandomInteger = cryptoRandomInteger;
	}

	@Override
	public ProofPreComputedValues preComputeExponentiationProof(final List<ZpGroupElement> baseElements) throws GeneralCryptoLibException {

		Validate.notNullOrEmptyAndNoNulls(baseElements, "List of base elements");

		return new ExponentiationProofPreComputer<>(baseElements, zpSubgroup, hashBuilder).preCompute(cryptoRandomInteger);
	}

	@Override
	public ProofPreComputedValues preComputePlaintextEqualityProof(final ElGamalPublicKey primaryPublicKey, final ElGamalPublicKey secondaryPublicKey)
			throws GeneralCryptoLibException {

		Validate.notNull(primaryPublicKey, "Primary ElGamal public key");
		Validate.notNull(secondaryPublicKey, "Secondary ElGamal public key");
		Validate.isEqual(primaryPublicKey.getKeys().size(), secondaryPublicKey.getKeys().size(), "Primary ElGamal public key length",
				"secondary ElGamal public key length");

		return new PlaintextEqualityProofPreComputer<>(primaryPublicKey, secondaryPublicKey, zpSubgroup, hashBuilder).preCompute(cryptoRandomInteger);
	}
}
