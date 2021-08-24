/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain;

import static ch.post.it.evoting.domain.Validations.validateUUID;

import java.math.BigInteger;

import ch.post.it.evoting.cryptolib.proofs.proof.Proof;

/**
 * Encapsulates an exponentiation proof for the exponentiation of the hashed and squared confirmation key with the voter vote cast return code
 * generation secret key. This object also includes the bases and exponentiated elements. However to avoid redundancy, object does not contain the
 * public key and the context information that is necessary to verify the proofs.
 */
public class LongVoteCastReturnCodesShareExponentiationProof {

	private String verificationCardId;
	private int confirmationAttempt;
	private BigInteger hashedConfirmationKey;
	private BigInteger ccrjLongVoteCastReturnCodeShare;
	private Proof exponentiationProof;

	public LongVoteCastReturnCodesShareExponentiationProof(String verificationCardId, int confirmationAttempt, BigInteger hashedConfirmationKey,
			BigInteger ccrjLongVoteCastReturnCodeShare, Proof exponentiationProof) {
		validateUUID(verificationCardId);
		this.verificationCardId = verificationCardId;
		this.confirmationAttempt = confirmationAttempt;
		this.hashedConfirmationKey = hashedConfirmationKey;
		this.ccrjLongVoteCastReturnCodeShare = ccrjLongVoteCastReturnCodeShare;
		this.exponentiationProof = exponentiationProof;
	}

	public String getVerificationCardId() {
		return verificationCardId;
	}

	public void setVerificationCardId(String verificationCardId) {
		this.verificationCardId = verificationCardId;
	}

	public int getConfirmationAttempt() {
		return confirmationAttempt;
	}

	public void setConfirmationAttempt(int confirmationAttempt) {
		this.confirmationAttempt = confirmationAttempt;
	}

	public BigInteger getHashedConfirmationKey() {
		return hashedConfirmationKey;
	}

	public void setHashedConfirmationKey(BigInteger hashedConfirmationKey) {
		this.hashedConfirmationKey = hashedConfirmationKey;
	}

	public BigInteger getCcrjLongVoteCastReturnCodeShare() {
		return ccrjLongVoteCastReturnCodeShare;
	}

	public void setCcrjLongVoteCastReturnCodeShare(BigInteger ccrjLongVoteCastReturnCodeShare) {
		this.ccrjLongVoteCastReturnCodeShare = ccrjLongVoteCastReturnCodeShare;
	}

	public Proof getExponentiationProof() {
		return exponentiationProof;
	}

	public void setExponentiationProof(Proof exponentiationProof) {
		this.exponentiationProof = exponentiationProof;
	}
}
