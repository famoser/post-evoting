/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement.exception;

import java.security.KeyManagementException;

/**
 * Node CA keys and certificates are invalid.
 */
public final class InvalidNodeCAException extends KeyManagementException {

	private static final long serialVersionUID = 1L;

	public InvalidNodeCAException(final String message) {
		super(message);
	}

	public InvalidNodeCAException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
