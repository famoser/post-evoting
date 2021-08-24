/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.mixdec.domain.model;

import ch.post.it.evoting.domain.election.model.vote.VoteSetId;

public class VoteSetStatus {

	private VoteSetId voteSetId;

	private MixDecStatus processStatus = MixDecStatus.UNKNOWN;

	private String errorMessage;

	public VoteSetStatus(VoteSetId voteSetId, MixDecStatus processStatus) {
		this.setVoteSetId(voteSetId);
		this.setProcessStatus(processStatus);
	}

	public VoteSetId getVoteSetId() {
		return voteSetId;
	}

	public void setVoteSetId(VoteSetId voteSetId) {
		this.voteSetId = voteSetId;
	}

	public MixDecStatus getProcessStatus() {
		return processStatus;
	}

	public void setProcessStatus(MixDecStatus processStatus) {
		this.processStatus = processStatus;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

}


