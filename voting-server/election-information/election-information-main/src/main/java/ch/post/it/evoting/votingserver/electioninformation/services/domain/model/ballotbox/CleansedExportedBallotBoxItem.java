/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox;

public class CleansedExportedBallotBoxItem {

	private final String vote;

	private CleansedExportedBallotBoxItem(final String vote) {
		super();
		this.vote = vote;
	}

	public String getVote() {
		return vote;
	}

	public static class CleansedExportedBallotBoxItemBuilder {

		private String vote;

		public CleansedExportedBallotBoxItemBuilder setVote(final String vote) {
			this.vote = vote;
			return this;
		}

		public CleansedExportedBallotBoxItem build() {
			return new CleansedExportedBallotBoxItem(vote);
		}

	}

}
