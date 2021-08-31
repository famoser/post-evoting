/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement.exception;

import java.security.KeyManagementException;

/**
 * Password is invalid.
 */
public final class InvalidPasswordException extends KeyManagementException {

	private static final long serialVersionUID = 1L;

	public InvalidPasswordException(final String message) {
		super(message);
	}

	public InvalidPasswordException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
