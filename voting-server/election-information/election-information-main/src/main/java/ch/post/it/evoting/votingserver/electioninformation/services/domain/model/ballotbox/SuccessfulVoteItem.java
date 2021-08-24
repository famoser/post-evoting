/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox;

import java.time.ZonedDateTime;

public class SuccessfulVoteItem {

	private String votingCardId;
	private ZonedDateTime timestamp;

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

}
