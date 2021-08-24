/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement.exception;

import java.security.KeyManagementException;

/**
 * Key has not been found.
 */
public final class KeyNotFoundException extends KeyManagementException {

	private static final long serialVersionUID = 1L;

	public KeyNotFoundException(final String message) {
		super(message);
	}

}
