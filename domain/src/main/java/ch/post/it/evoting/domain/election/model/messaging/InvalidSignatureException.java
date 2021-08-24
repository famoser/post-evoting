/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.model.messaging;

import ch.post.it.evoting.domain.election.model.vote.VoteSetId;

/**
 * Implementation of the invalid signature exception
 */
public class InvalidSignatureException extends Exception {

	private static final long serialVersionUID = 7006873886931014849L;

	private static final String ERROR_MESSAGE_TEMPLATE = "The signature of payload for %s %s does not match its contents";

	public InvalidSignatureException(VoteSetId voteSetId) {
		this("vote set", voteSetId.toString());
	}

	public InvalidSignatureException(String entityName, String entityId) {
		super(String.format(ERROR_MESSAGE_TEMPLATE, entityName, entityId));
	}
}
