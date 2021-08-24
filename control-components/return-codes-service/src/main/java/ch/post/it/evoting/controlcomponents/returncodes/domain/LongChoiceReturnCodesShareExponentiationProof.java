/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain;

import static ch.post.it.evoting.domain.Validations.validateUUID;

import java.math.BigInteger;
import java.util.List;

import ch.post.it.evoting.cryptolib.proofs.proof.Proof;

/**
 * Encapsulates an exponentiation proof for the exponentiation of the hashed and squared partial choice return codes with the voter choice return
 * code generation secret key. This object also includes the bases and exponentiated elements. However to avoid redundancy, object does not contain
 * the public key and the context information that is necessary to verify the proofs.
 */
public class LongChoiceReturnCodesShareExponentiationProof {

	private String verificationCardId;
	private List<BigInteger> hashedPartialChoiceReturnCodes;
	private List<BigInteger> ccrjLongChoiceReturnCodeShare;
	private Proof exponentiationProof;

	public LongChoiceReturnCodesShareExponentiationProof(String verificationCardId, List<BigInteger> hashedPartialChoiceReturnCodes,
			List<BigInteger> ccrjLongChoiceReturnCodeShare, Proof exponentiationProof) {
		validateUUID(verificationCardId);
		this.verificationCardId = verificationCardId;
		this.hashedPartialChoiceReturnCodes = hashedPartialChoiceReturnCodes;
		this.ccrjLongChoiceReturnCodeShare = ccrjLongChoiceReturnCodeShare;
		this.exponentiationProof = exponentiationProof;
	}

	public String getVerificationCardId() {
		return verificationCardId;
	}

	public void setVerificationCardId(String verificationCardId) {
		this.verificationCardId = verificationCardId;
	}

	public List<BigInteger> getHashedPartialChoiceReturnCodes() {
		return hashedPartialChoiceReturnCodes;
	}

	public void setHashedPartialChoiceReturnCodes(List<BigInteger> hashedPartialChoiceReturnCodes) {
		this.hashedPartialChoiceReturnCodes = hashedPartialChoiceReturnCodes;
	}

	public List<BigInteger> getCcrjLongChoiceReturnCodeShare() {
		return ccrjLongChoiceReturnCodeShare;
	}

	public void setCcrjLongChoiceReturnCodeShare(List<BigInteger> ccrjLongChoiceReturnCodeShare) {
		this.ccrjLongChoiceReturnCodeShare = ccrjLongChoiceReturnCodeShare;
	}

	public Proof getExponentiationProof() {
		return exponentiationProof;
	}

	public void setExponentiationProof(Proof exponentiationProof) {
		this.exponentiationProof = exponentiationProof;
	}
}
