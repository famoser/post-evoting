/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.exception;

public class KeyStoreServiceException extends RuntimeException {

	private static final long serialVersionUID = 3880324231542129840L;

	public KeyStoreServiceException(Throwable cause) {
		super(cause);
	}

	public KeyStoreServiceException(String message) {
		super(message);
	}

	public KeyStoreServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
