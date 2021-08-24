/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain;

import static ch.post.it.evoting.domain.Validations.validateUUID;

import java.math.BigInteger;
import java.util.List;

import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;

/**
 * Encapsulates an exponentiation proof for the partial decryption of the partial choice return codes. This object also includes the bases and
 * exponentiated elements. However to avoid redundancy, object does not contain the public key and the context information that is necessary to
 * verify the proofs.
 */
public class PartialDecryptPccExponentiationProof {

	private String verificationCardId;
	private BigInteger gammaEncryptedPartialChoiceReturnCodes;
	private List<ZpGroupElement> exponentiatedGammaElements;
	private Proof exponentiationProof;

	public PartialDecryptPccExponentiationProof(String verificationCardId, BigInteger gammaEncryptedPartialChoiceReturnCodes,
			List<ZpGroupElement> exponentiatedGammaElements, Proof exponentiationProof) {
		validateUUID(verificationCardId);
		this.verificationCardId = verificationCardId;
		this.gammaEncryptedPartialChoiceReturnCodes = gammaEncryptedPartialChoiceReturnCodes;
		this.exponentiatedGammaElements = exponentiatedGammaElements;
		this.exponentiationProof = exponentiationProof;
	}

	public String getVerificationCardId() {
		return verificationCardId;
	}

	public void setVerificationCardId(String verificationCardId) {
		this.verificationCardId = verificationCardId;
	}

	public BigInteger getGammaEncryptedPartialChoiceReturnCodes() {
		return gammaEncryptedPartialChoiceReturnCodes;
	}

	public void setGammaEncryptedPartialChoiceReturnCodes(BigInteger gammaEncryptedPartialChoiceReturnCodes) {
		this.gammaEncryptedPartialChoiceReturnCodes = gammaEncryptedPartialChoiceReturnCodes;
	}

	public List<ZpGroupElement> getExponentiatedGammaElements() {
		return exponentiatedGammaElements;
	}

	public void setExponentiatedGammaElements(List<ZpGroupElement> exponentiatedGammaElements) {
		this.exponentiatedGammaElements = exponentiatedGammaElements;
	}

	public Proof getExponentiationProof() {
		return exponentiationProof;
	}

	public void setExponentiationProof(Proof exponentiationProof) {
		this.exponentiationProof = exponentiationProof;
	}
}
