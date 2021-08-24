/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.factory;

import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.GroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;
import ch.post.it.evoting.cryptolib.mathematical.groups.activity.GroupElementsDivider;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.proofs.bean.ProofPreComputedValues;
import ch.post.it.evoting.cryptolib.proofs.maurer.configuration.Constants;
import ch.post.it.evoting.cryptolib.proofs.maurer.function.PhiFunctionPlaintextEquality;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;
import ch.post.it.evoting.cryptolib.proofs.utils.HashBuilder;

/**
 * Generator of the zero knowledge proof for two ciphertexts that were each generated with a different key pair and random exponent from the same
 * plaintext.
 *
 * @param <E> the type of the mathematical group elements that will be used during this exchange. E must be a type which extends GroupElement.
 */
public final class PlaintextEqualityProofGenerator<E extends GroupElement> {

	private final E primaryCiphertextGamma;

	private final List<E> primaryCiphertextPhis;

	private final ElGamalPublicKey primaryPublicKey;

	private final Exponent primaryExponent;

	private final E secondaryCiphertextGamma;

	private final List<E> secondaryCiphertextPhis;

	private final ElGamalPublicKey secondaryPublicKey;

	private final Exponent secondaryExponent;

	private final MathematicalGroup<E> group;

	private final HashBuilder hashBuilder;

	private final ProofPreComputedValues preComputedValues;

	private final GroupElementsDivider divider;

	/**
	 * Default constructor.
	 *
	 * @param primaryCiphertext   the primary ElGamal encrypted plaintext.
	 * @param primaryPublicKey    the primary ElGamal public key.
	 * @param primaryExponent     the primary ElGamal encryption exponent, which acts as a witness for this proof.
	 * @param secondaryCiphertext the secondary ElGamal encrypted plaintext.
	 * @param secondaryPublicKey  the secondary ElGamal public key.
	 * @param secondaryExponent   the primary ElGamal encryption exponent, which acts as a witness for this proof.
	 * @param group               the Zp subgroup used for ElGamal encryption.
	 * @param hashBuilder         A helper that calculates hashes for proofs.
	 * @param preComputedValues   the pre-computed values used to generate the proof.
	 */
	PlaintextEqualityProofGenerator(final List<E> primaryCiphertext, final ElGamalPublicKey primaryPublicKey, final Exponent primaryExponent,
			final List<E> secondaryCiphertext, final ElGamalPublicKey secondaryPublicKey, final Exponent secondaryExponent,
			final MathematicalGroup<E> group, final HashBuilder hashBuilder, final ProofPreComputedValues preComputedValues) {

		primaryCiphertextGamma = primaryCiphertext.get(0);
		primaryCiphertextPhis = new ArrayList<>(primaryCiphertext.subList(1, primaryCiphertext.size()));
		this.primaryPublicKey = primaryPublicKey;
		this.primaryExponent = primaryExponent;

		secondaryCiphertextGamma = secondaryCiphertext.get(0);
		secondaryCiphertextPhis = new ArrayList<>(secondaryCiphertext.subList(1, secondaryCiphertext.size()));
		this.secondaryPublicKey = secondaryPublicKey;
		this.secondaryExponent = secondaryExponent;

		this.group = group;

		this.hashBuilder = hashBuilder;

		this.preComputedValues = preComputedValues;

		divider = new GroupElementsDivider();
	}

	/**
	 * Generates the plaintext equality zero knowledge proof of knowledge.
	 *
	 * @return the plaintext equality zero knowledge proof of knowledge, as an object of type {@link Proof}.
	 * @throws GeneralCryptoLibException PlaintextEqualityProofGenerator if {@code PlaintextEqualityProofGenerator} was initialized by invalid
	 *                                   arguments.
	 */
	public Proof generate() throws GeneralCryptoLibException {

		PhiFunctionPlaintextEquality phiFunctionPlaintextEquality = new PhiFunctionPlaintextEquality(group, buildListBaseElements());

		Prover<E> prover = new Prover<>(group, phiFunctionPlaintextEquality, hashBuilder);

		return prover.prove(buildListPublicValues(), buildListExponents(), Constants.PLAINTEXT_EQUALITY_PROOF_AUXILIARY_DATA, preComputedValues);
	}

	@SuppressWarnings("unchecked")
	private List<E> buildListBaseElements() {

		List<E> invertedSecondaryPublicKey = buildInvertedSecondaryPublicKey();

		List<E> baseElements = new ArrayList<>();
		baseElements.add(group.getGenerator());
		baseElements.addAll((List<? extends E>) primaryPublicKey.getKeys());
		baseElements.addAll(invertedSecondaryPublicKey);

		return baseElements;
	}

	@SuppressWarnings("unchecked")
	private List<E> buildInvertedSecondaryPublicKey() {

		List<E> invertedSecondaryPublicKey = new ArrayList<>();
		List<? extends E> secondaryPublicKeyElements = (List<? extends E>) secondaryPublicKey.getKeys();
		for (E element : secondaryPublicKeyElements) {
			invertedSecondaryPublicKey.add((E) element.invert());
		}

		return invertedSecondaryPublicKey;
	}

	@SuppressWarnings("unchecked")
	private List<E> buildListPublicValues() throws GeneralCryptoLibException {

		List<E> dividedSubCiphertext = divider.divide(primaryCiphertextPhis, secondaryCiphertextPhis, group);

		List<E> publicKeys = new ArrayList<>();
		primaryPublicKey.getKeys().forEach(value -> publicKeys.add((E) value));
		secondaryPublicKey.getKeys().forEach(value -> publicKeys.add((E) value));

		List<E> publicValues = new ArrayList<>();
		publicValues.add(primaryCiphertextGamma);
		publicValues.add(secondaryCiphertextGamma);
		publicValues.addAll(dividedSubCiphertext);
		publicValues.addAll(primaryCiphertextPhis);
		publicValues.addAll(secondaryCiphertextPhis);
		publicValues.addAll(publicKeys);
		publicValues.add(group.getGenerator());

		return publicValues;
	}

	private List<Exponent> buildListExponents() {

		List<Exponent> exponents = new ArrayList<>();
		exponents.add(primaryExponent);
		exponents.add(secondaryExponent);

		return exponents;
	}
}
