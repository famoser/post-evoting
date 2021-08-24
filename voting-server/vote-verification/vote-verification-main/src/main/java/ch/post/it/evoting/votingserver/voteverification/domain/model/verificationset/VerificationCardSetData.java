/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset;

import javax.validation.constraints.NotNull;

/**
 * Data structure used to upload the verification card set related data.
 */
public class VerificationCardSetData {

	@NotNull
	private String verificationCardSetData;

	@NotNull
	private String voteVerificationContextData;

	/**
	 * Returns the current value of the field verificationCardSetData.
	 *
	 * @return Returns the verificationCardSetData.
	 */
	public String getVerificationCardSetData() {
		return verificationCardSetData;
	}

	/**
	 * Sets the value of the field verificationCardSetData.
	 *
	 * @param verificationCardSetData The verificationCardSetData to set.
	 */
	public void setVerificationCardSetData(String verificationCardSetData) {
		this.verificationCardSetData = verificationCardSetData;
	}

	/**
	 * Returns the current value of the field voteVerificationContextData.
	 *
	 * @return Returns the voteVerificationContextData.
	 */
	public String getVoteVerificationContextData() {
		return voteVerificationContextData;
	}

	/**
	 * Sets the value of the field voteVerificationContextData.
	 *
	 * @param voteVerificationContextData The voteVerificationContextData to set.
	 */
	public void setVoteVerificationContextData(String voteVerificationContextData) {
		this.voteVerificationContextData = voteVerificationContextData;
	}

}
