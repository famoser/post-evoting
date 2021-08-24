/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.commons.domain;

public class CreateVerificationCardIdsInput {

	private String verificationCardSetId;

	private int numberOfVerificationCardIds;

	private String electionEventId;

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	public void setVerificationCardSetId(String verificationCardSetId) {
		this.verificationCardSetId = verificationCardSetId;
	}

	public int getNumberOfVerificationCardIds() {
		return numberOfVerificationCardIds;
	}

	public void setNumberOfVerificationCardIds(int numberOfVerificationCardIds) {
		this.numberOfVerificationCardIds = numberOfVerificationCardIds;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public void setElectionEventId(String electionEventId) {
		this.electionEventId = electionEventId;
	}
}
