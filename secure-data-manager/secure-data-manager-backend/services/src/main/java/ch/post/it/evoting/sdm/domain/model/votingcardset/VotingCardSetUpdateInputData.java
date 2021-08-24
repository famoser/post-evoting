/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.votingcardset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.post.it.evoting.sdm.domain.model.status.Status;

/**
 * Request body for updating a voting card set.
 */
public class VotingCardSetUpdateInputData {

	private final Status status;

	private final String privateKeyPEM;

	private final String adminBoardId;

	@JsonCreator
	public VotingCardSetUpdateInputData(
			@JsonProperty("status")
					Status status,
			@JsonProperty("privateKeyPEM")
					String privateKeyPEM,
			@JsonProperty("adminBoardId")
					String adminBoardId) {
		this.status = status;
		this.privateKeyPEM = privateKeyPEM;
		this.adminBoardId = adminBoardId;
	}

	public Status getStatus() {
		return status;
	}

	public String getPrivateKeyPEM() {
		return privateKeyPEM;
	}

	public String getAdminBoardId() {
		return adminBoardId;
	}
}
