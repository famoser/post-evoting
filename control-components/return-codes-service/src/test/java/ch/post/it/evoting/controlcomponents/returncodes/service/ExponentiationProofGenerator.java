/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.WitnessImpl;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofProverAPI;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofVerifierAPI;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;

public class ExponentiationProofGenerator {

	private final ElGamalEncryptionParameters ep;

	private final ProofProverAPI proofProverAPI;

	private final ProofVerifierAPI proofVerifierAPI;

	public ExponentiationProofGenerator(ProofProverAPI proofProver, ProofVerifierAPI proofVerifier, ElGamalEncryptionParameters ep) {
		super();
		this.ep = ep;
		proofProverAPI = proofProver;
		proofVerifierAPI = proofVerifier;
	}

	public Proof createExponentiationProof(ElGamalKeyPair elGamalKeyPair, List<ZpGroupElement> zpGroupElements,
			List<ZpGroupElement> exponentiatedZpGroupElements) throws GeneralCryptoLibException {
		List<ZpGroupElement> modifiableZpGroupElements = new ArrayList<>(zpGroupElements);
		List<ZpGroupElement> modifiableExponentiateZpGroupElements = new ArrayList<>(exponentiatedZpGroupElements);

		final BigInteger generatorEncryptParamBigInteger = ep.getG();
		final ZpGroupElement groupElementGenerator = getZpGroupElement((ZpSubgroup) ep.getGroup(), generatorEncryptParamBigInteger);
		modifiableZpGroupElements.add(0, groupElementGenerator);

		ZpGroupElement verificationCardPublicKey = elGamalKeyPair.getPublicKeys().getKeys().get(0);
		Exponent exponent = elGamalKeyPair.getPrivateKeys().getKeys().get(0);
		assertEquals(verificationCardPublicKey, groupElementGenerator.exponentiate(exponent));
		modifiableExponentiateZpGroupElements.add(0, verificationCardPublicKey);

		Proof exponentiationProof = proofProverAPI
				.createExponentiationProof(modifiableExponentiateZpGroupElements, modifiableZpGroupElements, new WitnessImpl(exponent));

		assertTrue(proofVerifierAPI.verifyExponentiationProof(modifiableExponentiateZpGroupElements, modifiableZpGroupElements, exponentiationProof));

		return exponentiationProof;
	}

	private ZpGroupElement getZpGroupElement(final ZpSubgroup mathematicalGroup, final BigInteger value) throws GeneralCryptoLibException {
		return new ZpGroupElement(value, mathematicalGroup);
	}

}
