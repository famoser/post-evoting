/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncrypterValues;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.WitnessImpl;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Ciphertext;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Witness;
import ch.post.it.evoting.cryptolib.mathematical.groups.activity.GroupElementsCompressor;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofProverAPI;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofVerifierAPI;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;

public class PlainTextEqualityProofGenerator {

	private final ZpSubgroup group_g2_q11;

	private final ProofProverAPI proofProverAPI;

	private final ProofVerifierAPI proofVerifierAPI;

	private final ElGamalKeyPair elGamalKeyPair;

	public PlainTextEqualityProofGenerator(ProofProverAPI proofProver, ProofVerifierAPI proofVerifier, ZpSubgroup group_g2_q11,
			ElGamalKeyPair elGamalKeyPair) {
		super();
		this.elGamalKeyPair = elGamalKeyPair;
		proofProverAPI = proofProver;
		proofVerifierAPI = proofVerifier;
		this.group_g2_q11 = group_g2_q11;
	}

	public Proof createPlaintextEqualityProof(String cipherTextExponentiations, ElGamalEncrypterValues encryptGroupElements)
			throws GeneralCryptoLibException {
		Exponent exponent1 = encryptGroupElements.getExponent();
		Exponent exponent2 = elGamalKeyPair.getPrivateKeys().getKeys().get(0);
		BigInteger finalExponentInteger = exponent1.getValue().multiply(exponent2.getValue()).mod(group_g2_q11.getQ());
		Exponent finalExponent = new Exponent(group_g2_q11.getQ(), finalExponentInteger);
		Witness witness = new WitnessImpl(finalExponent);

		String encryptedPartialChoiceCodes = cipherTextExponentiations;
		ElGamalPublicKey elGamalPublicKey = elGamalKeyPair.getPublicKeys();

		final Ciphertext primaryCiphertext = getPrimaryCipherText(group_g2_q11, cipherTextExponentiations);
		final Ciphertext secondaryCiphertext = getSecondaryCiphertext(group_g2_q11, encryptedPartialChoiceCodes);

		Proof proof = proofProverAPI
				.createPlaintextEqualityProof(primaryCiphertext, elGamalPublicKey, witness, secondaryCiphertext, elGamalPublicKey, witness);
		assertTrue(proofVerifierAPI.verifyPlaintextEqualityProof(primaryCiphertext, elGamalPublicKey, secondaryCiphertext, elGamalPublicKey, proof));
		return proof;
	}

	private List<ZpGroupElement> getZpGroupElements(String encryptedChoiceCodes, ZpSubgroup mathematicalGroup) throws GeneralCryptoLibException {
		List<ZpGroupElement> elements = new ArrayList<>();
		for (String partial : encryptedChoiceCodes.split(";")) {
			elements.add(new ZpGroupElement(new BigInteger(partial), mathematicalGroup));
		}
		return elements;
	}

	public Ciphertext getPrimaryCipherText(final ZpSubgroup mathematicalGroup, String cipherTextExponentiations) throws GeneralCryptoLibException {
		final List<ZpGroupElement> primaryZpGroupElements = new ArrayList<>();
		primaryZpGroupElements.add(getZpGroupElementFromExponentiatedOptions(cipherTextExponentiations, mathematicalGroup, 0));
		primaryZpGroupElements.add(getZpGroupElementFromExponentiatedOptions(cipherTextExponentiations, mathematicalGroup, 1));

		return getCiphertextFromElements(mathematicalGroup, primaryZpGroupElements);
	}

	public Ciphertext getSecondaryCiphertext(final ZpSubgroup mathematicalGroup, final String encryptedChoiceCodes) throws GeneralCryptoLibException {
		// Retrieval of D0 and D'1 as result of compressing the D1-Dn elements
		// of the partial choice codes.
		final List<ZpGroupElement> zpGroupElements = getZpGroupElements(encryptedChoiceCodes, mathematicalGroup);
		final ZpGroupElement D0 = zpGroupElements.get(0);
		final ZpGroupElement D1prima = getCompressedList(mathematicalGroup, zpGroupElements.subList(1, zpGroupElements.size()));
		final List<ZpGroupElement> secondaryZpGroupElements = new ArrayList<>();
		secondaryZpGroupElements.add(D0);
		secondaryZpGroupElements.add(D1prima);

		return getCiphertextFromElements(mathematicalGroup, secondaryZpGroupElements);
	}

	private Ciphertext getCiphertextFromElements(final ZpSubgroup mathematicalGroup, List<ZpGroupElement> zpGroupElements)
			throws GeneralCryptoLibException {
		ZpGroupElement gamma = zpGroupElements.get(0);
		List<ZpGroupElement> phis = new ArrayList<>();
		phis.add(zpGroupElements.get(1));
		ElGamalCiphertext elGamalCiphertext = new ElGamalCiphertext(gamma, phis);

		return new ElGamalEncrypterValues(new Exponent(mathematicalGroup.getQ(), BigInteger.ONE), elGamalCiphertext);
	}

	private ZpGroupElement getCompressedList(ZpSubgroup mathematicalGroup, List<ZpGroupElement> elements) throws GeneralCryptoLibException {
		GroupElementsCompressor<ZpGroupElement> compressor = new GroupElementsCompressor<>();
		return compressor.compress(elements);
	}

	public ZpGroupElement getZpGroupElement(final ZpSubgroup mathematicalGroup, final int ck, String encryptedOptions)
			throws GeneralCryptoLibException {
		final BigInteger Ck = new BigInteger(getCkFromEncryptedOptions(encryptedOptions, ck));
		return new ZpGroupElement(Ck, mathematicalGroup);
	}

	public ZpGroupElement getZpGroupElementFromExponentiatedOptions(final String cipherTextExponentiations, final ZpSubgroup mathematicalGroup,
			final int ck) throws GeneralCryptoLibException {
		final BigInteger Ck = new BigInteger(getCkFromEncryptedOptions(cipherTextExponentiations, ck));
		return new ZpGroupElement(Ck, mathematicalGroup);
	}

	private String getCkFromEncryptedOptions(String encryptedOptions, int ck) {
		return encryptedOptions.split(";")[ck];
	}

}
