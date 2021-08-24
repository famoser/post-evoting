/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.factory;

import static ch.post.it.evoting.cryptolib.proofs.maurer.configuration.Constants.PLAINTEXT_EQUALITY_PROOF_AUXILIARY_DATA;

import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.GroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;
import ch.post.it.evoting.cryptolib.mathematical.groups.activity.GroupElementsDivider;
import ch.post.it.evoting.cryptolib.proofs.maurer.function.PhiFunctionPlaintextEquality;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;
import ch.post.it.evoting.cryptolib.proofs.utils.HashBuilder;

/**
 * Verifier of the zero knowledge proof for two ciphertexts that were each generated with a different key pair and random exponent from the same
 * plaintext.
 *
 * @param <E> the type of the group elements of the {@link MathematicalGroup} that will be used during this exchange. E must be a type which extends
 *            GroupElement.
 */
public class PlaintextEqualityProofVerifier<E extends GroupElement> {

	private final E primaryCiphertextGamma;

	private final List<E> primaryCiphertextPhis;

	private final ElGamalPublicKey primaryPublicKey;

	private final E secondaryCiphertextGamma;

	private final List<E> secondaryCiphertextPhis;

	private final ElGamalPublicKey secondaryPublicKey;

	private final Proof proof;

	private final MathematicalGroup<E> group;

	private final HashBuilder hashBuilder;

	private final GroupElementsDivider divider;

	/**
	 * Default constructor.
	 *
	 * @param primaryCiphertext   the primary ElGamal encrypted plaintext.
	 * @param primaryPublicKey    the primary ElGamal public key.
	 * @param secondaryCiphertext the secondary ElGamal encrypted plaintext.
	 * @param secondaryPublicKey  the secondary ElGamal public key.
	 * @param proof               the proof to be verified.
	 * @param group               the Zp subgroup used for ElGamal encryption.
	 * @param hashBuilder         A helper that calculates hashes for proofs.
	 */
	PlaintextEqualityProofVerifier(final List<E> primaryCiphertext, final ElGamalPublicKey primaryPublicKey, final List<E> secondaryCiphertext,
			final ElGamalPublicKey secondaryPublicKey, final Proof proof, final MathematicalGroup<E> group, final HashBuilder hashBuilder) {

		primaryCiphertextGamma = primaryCiphertext.get(0);
		primaryCiphertextPhis = new ArrayList<>(primaryCiphertext.subList(1, primaryCiphertext.size()));
		this.primaryPublicKey = primaryPublicKey;

		secondaryCiphertextGamma = secondaryCiphertext.get(0);
		secondaryCiphertextPhis = new ArrayList<>(secondaryCiphertext.subList(1, secondaryCiphertext.size()));
		this.secondaryPublicKey = secondaryPublicKey;

		this.proof = proof;
		this.group = group;

		this.hashBuilder = hashBuilder;

		divider = new GroupElementsDivider();
	}

	/**
	 * Verifies the plaintext equality zero knowledge proof of knowledge.
	 *
	 * @return true if the proof is verified as true, false otherwise.
	 * @throws GeneralCryptoLibException if {@code PlaintextEqualityProofVerifier} was initialized by the invalid arguments.
	 */
	public boolean verify() throws GeneralCryptoLibException {

		PhiFunctionPlaintextEquality phiFunctionPlaintextEquality = new PhiFunctionPlaintextEquality(group, buildListBaseElements());

		Verifier<E> verifier = new Verifier<>(group, phiFunctionPlaintextEquality, hashBuilder);

		return verifier.verify(buildListPublicValues(), proof, PLAINTEXT_EQUALITY_PROOF_AUXILIARY_DATA);
	}

	@SuppressWarnings("unchecked")
	private List<E> buildListBaseElements() {

		List<E> invertedSecondaryPublicKey = buildSecondaryInvertedPublicKey();

		List<E> baseElements = new ArrayList<>();
		baseElements.add(group.getGenerator());
		baseElements.addAll((List<? extends E>) primaryPublicKey.getKeys());
		baseElements.addAll(invertedSecondaryPublicKey);

		return baseElements;
	}

	@SuppressWarnings("unchecked")
	private List<E> buildSecondaryInvertedPublicKey() {

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
}
