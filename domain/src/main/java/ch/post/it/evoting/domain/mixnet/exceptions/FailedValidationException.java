/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.domain.mixnet.exceptions;

/**
 * Thrown to indicate that a validation failed.
 */
public class FailedValidationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public FailedValidationException(String message) {
		super(message);
	}
}
