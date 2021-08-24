/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.cryptolib.elgamal.exponentiation;

import static java.util.Objects.requireNonNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.proofs.ProofsServiceAPI;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;
import ch.post.it.evoting.cryptolib.elgamal.bean.WitnessImpl;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Witness;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofProverAPI;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;

public final class ExponentiationServiceImpl implements ExponentiationService {
	private final ProofsServiceAPI proofsService;

	public ExponentiationServiceImpl(ProofsServiceAPI proofsService) {
		this.proofsService = proofsService;
	}

	@Override
	public ExponentiatedElementsAndProof<BigInteger> exponentiateCleartexts(List<BigInteger> cleartexts, Exponent exponent, ZpSubgroup group)
			throws GeneralCryptoLibException {
		requireNonNull(cleartexts, "Cleartexts is null.");
		requireNonNull(exponent, "Exponent is null.");
		requireNonNull(group, "Group is null.");

		List<BigInteger> powers = new ArrayList<>(cleartexts.size());
		List<ZpGroupElement> baseElements = new ArrayList<>(cleartexts.size() + 1);
		baseElements.add(group.getGenerator());
		List<ZpGroupElement> powerElements = new ArrayList<>(cleartexts.size() + 1);
		powerElements.add(group.getGenerator().exponentiate(exponent));
		for (BigInteger cleartext : cleartexts) {
			ZpGroupElement baseElement = new ZpGroupElement(cleartext, group);
			ZpGroupElement powerElement = baseElement.exponentiate(exponent);
			powers.add(powerElement.getValue());
			baseElements.add(baseElement);
			powerElements.add(powerElement);
		}

		Witness witness = new WitnessImpl(exponent);
		ProofProverAPI prover = proofsService.createProofProverAPI(group);
		Proof proof = prover.createExponentiationProof(powerElements, baseElements, witness);
		return new ExponentiatedElementsAndProof<>(powers, proof);
	}

	@Override
	public ExponentiatedElementsAndProof<ElGamalCiphertext> exponentiateCiphertexts(List<ElGamalCiphertext> ciphertexts, Exponent exponent,
			ZpSubgroup group) throws GeneralCryptoLibException {
		requireNonNull(ciphertexts, "Ciphertexts is null.");
		requireNonNull(exponent, "Exponent is null.");
		requireNonNull(group, "Group is null.");

		List<ElGamalCiphertext> powers = new ArrayList<>();
		List<ZpGroupElement> baseElements = new ArrayList<>();
		baseElements.add(group.getGenerator());
		List<ZpGroupElement> powerElements = new ArrayList<>();
		powerElements.add(group.getGenerator().exponentiate(exponent));
		for (ElGamalCiphertext ciphertext : ciphertexts) {
			List<ZpGroupElement> baseValues = ciphertext.getValues();
			List<ZpGroupElement> powerValues = new ArrayList<>(baseValues.size());
			for (ZpGroupElement baseValue : baseValues) {
				powerValues.add(baseValue.exponentiate(exponent));
			}
			powers.add(new ElGamalCiphertext(powerValues));
			baseElements.addAll(baseValues);
			powerElements.addAll(powerValues);
		}

		Witness witness = new WitnessImpl(exponent);
		ProofProverAPI prover = proofsService.createProofProverAPI(group);
		Proof proof = prover.createExponentiationProof(powerElements, baseElements, witness);
		return new ExponentiatedElementsAndProof<>(powers, proof);
	}
}
