/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.returncodes;

public class PartialChoiceReturnCodesVerificationInput {

	private String vote;

	private String electionPublicKeyJwt;

	private String verificationCardSetDataJwt;

	public String getVote() {
		return vote;
	}

	public void setVote(String vote) {
		this.vote = vote;
	}

	public String getElectionPublicKeyJwt() {
		return electionPublicKeyJwt;
	}

	public void setElectionPublicKeyJwt(String electionPublicKeyJwt) {
		this.electionPublicKeyJwt = electionPublicKeyJwt;
	}

	public String getVerificationCardSetDataJwt() {
		return verificationCardSetDataJwt;
	}

	public void setVerificationCardSetDataJwt(String verificationSetEntityJwt) {
		this.verificationCardSetDataJwt = verificationSetEntityJwt;
	}
}
