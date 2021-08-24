/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.commons;

/**
 * Verification card set containing the election event identifier, the ballot box identifier, the voting card set identifier, the verification card
 * set identifier and the administration board identifier
 */
public class VerificationCardSet {
	private final String adminBoardId;
	private final String ballotBoxId;
	private final String electionEventId;
	private final String verificationCardSetId;
	private final String votingCardSetId;

	public VerificationCardSet(String adminBoardId, String ballotBoxId, String electionEventId, String verificationCardSetId,
			String votingCardSetId) {
		this.adminBoardId = adminBoardId;
		this.ballotBoxId = ballotBoxId;
		this.electionEventId = electionEventId;
		this.verificationCardSetId = verificationCardSetId;
		this.votingCardSetId = votingCardSetId;
	}

	public String getAdminBoardId() {
		return adminBoardId;
	}

	public String getBallotBoxId() {
		return ballotBoxId;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	public String getVotingCardSetId() {
		return votingCardSetId;
	}

}
