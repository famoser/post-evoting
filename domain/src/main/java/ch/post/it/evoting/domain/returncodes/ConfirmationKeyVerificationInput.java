/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.returncodes;

public class ConfirmationKeyVerificationInput {

	private String confirmationMessage;

	private String votingCardId;

	public String getConfirmationMessage() {
		return confirmationMessage;
	}

	public void setConfirmationMessage(String confirmationMessage) {
		this.confirmationMessage = confirmationMessage;
	}

	public String getVotingCardId() {
		return votingCardId;
	}

	public void setVotingCardId(String votingCardId) {
		this.votingCardId = votingCardId;
	}

}
