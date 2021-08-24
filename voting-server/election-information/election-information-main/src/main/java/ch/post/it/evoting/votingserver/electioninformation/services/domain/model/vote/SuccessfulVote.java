/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.vote;

import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.post.it.evoting.domain.election.model.constants.Constants;

/**
 * Entity representing a successful vote. One vote is considered successful if it passes all the
 * validations and it has been confirmed in case confirmation is required. Otherwise is considered a
 * failed vote.
 */
@Entity
@Table(name = "SUCCESSFUL_VOTES")
public class SuccessfulVote {

	@Id
	@Column(name = "ID", nullable = false)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "successfulVoteSeq")
	@SequenceGenerator(name = "successfulVoteSeq", sequenceName = "SUCCESSFUL_VOTES_SEQ", allocationSize = 1)
	@JsonIgnore
	private Integer id;

	@Column(name = "TENANT_ID", nullable = false)
	@NotNull
	@Size(max = Constants.COLUMN_LENGTH_100)
	private String tenantId;

	@Column(name = "ELECTION_EVENT_ID", nullable = false)
	@NotNull
	@Size(max = Constants.COLUMN_LENGTH_100)
	private String electionEventId;

	@Column(name = "BALLOT_BOX_ID", nullable = false)
	@NotNull
	@Size(max = Constants.COLUMN_LENGTH_100)
	private String ballotBoxId;

	@Column(name = "VOTING_CARD_ID", nullable = false)
	@NotNull
	@Size(max = Constants.COLUMN_LENGTH_100)
	private String votingCardId;

	@Column(name = "TIMESTAMP", nullable = false)
	@NotNull
	private ZonedDateTime timestamp;

	public Integer getId() {
		return id;
	}

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

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public void setElectionEventId(String electionEventId) {
		this.electionEventId = electionEventId;
	}

	public String getBallotBoxId() {
		return ballotBoxId;
	}

	public void setBallotBoxId(String ballotBoxId) {
		this.ballotBoxId = ballotBoxId;
	}
}
