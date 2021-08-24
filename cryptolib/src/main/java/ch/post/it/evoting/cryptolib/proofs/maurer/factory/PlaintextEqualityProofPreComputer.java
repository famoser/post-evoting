/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.factory;

import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.securerandom.CryptoAPIRandomInteger;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.GroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;
import ch.post.it.evoting.cryptolib.proofs.bean.ProofPreComputedValues;
import ch.post.it.evoting.cryptolib.proofs.maurer.function.PhiFunctionPlaintextEquality;
import ch.post.it.evoting.cryptolib.proofs.utils.HashBuilder;

/**
 * Plaintext equality proof pre-computer.
 *
 * @param <E> the type of the group elements of the {@link MathematicalGroup} that will be used during this exchange. E must be a type which extends
 *            GroupElement.
 */
public class PlaintextEqualityProofPreComputer<E extends GroupElement> {

	private final ElGamalPublicKey primaryPublicKey;

	private final ElGamalPublicKey secondaryPublicKey;

	private final MathematicalGroup<E> group;

	private final HashBuilder hashBuilder;

	private final PhiFunctionPlaintextEquality phiFunctionPlaintextEquality;

	/**
	 * Default constructor.
	 *
	 * @param primaryPublicKey   the primary ElGamal public key.
	 * @param secondaryPublicKey the secondary ElGamal public key.
	 * @param group              the Zp subgroup used for ElGamal encryption.
	 * @param hashBuilder        A helper that calculates hashes for proofs.
	 * @throws GeneralCryptoLibException if the phi function cannot be created.
	 */
	PlaintextEqualityProofPreComputer(final ElGamalPublicKey primaryPublicKey, final ElGamalPublicKey secondaryPublicKey,
			final MathematicalGroup<E> group, final HashBuilder hashBuilder) throws GeneralCryptoLibException {

		this.primaryPublicKey = primaryPublicKey;
		this.secondaryPublicKey = secondaryPublicKey;
		this.group = group;

		this.hashBuilder = hashBuilder;

		phiFunctionPlaintextEquality = new PhiFunctionPlaintextEquality(this.group, buildListBaseElements());
	}

	/**
	 * Pre-computes the values needed for proof generation.
	 *
	 * @param cryptoRandomInteger the random number generator used to create the proof secrets.
	 * @return the pre-computed values.
	 * @throws GeneralCryptoLibException if an error occurs during the pre-computation process.
	 */
	public ProofPreComputedValues preCompute(final CryptoAPIRandomInteger cryptoRandomInteger) throws GeneralCryptoLibException {

		Prover<E> prover = new Prover<>(group, phiFunctionPlaintextEquality, hashBuilder);

		return prover.preCompute(cryptoRandomInteger);
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
}
