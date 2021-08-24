/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.vote;

import java.time.ZonedDateTime;

public class FailedVote {
	private String votingCardId;
	private ZonedDateTime timestamp;
	private String validationError;

	public String getVotingCardId() {
		return votingCardId;
	}

	public void setVotingCardId(String votingCardId) {
		this.votingCardId = votingCardId;
	}

	public ZonedDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(ZonedDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public String getValidationError() {
		return validationError;
	}

	public void setValidationError(String validationError) {
		this.validationError = validationError;
	}
}
