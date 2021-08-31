/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.model.messaging;

/**
 * Implementation of the safe stream deserialization
 */
public class SafeStreamDeserializationException extends Exception {

	private static final long serialVersionUID = -140165307911190422L;

	public SafeStreamDeserializationException(Throwable cause) {
		super(cause);
	}
}
