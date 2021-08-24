/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.votingcard;

public class VotingCard {

	private final String votingCardId;

	public VotingCard(String votingCardId) {
		super();
		this.votingCardId = votingCardId;
	}

	public String getVotingCardId() {
		return votingCardId;
	}

}
