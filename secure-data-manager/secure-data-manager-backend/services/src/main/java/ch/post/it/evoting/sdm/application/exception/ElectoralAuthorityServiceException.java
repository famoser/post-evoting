/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.exception;

public class ElectoralAuthorityServiceException extends RuntimeException {

	private static final long serialVersionUID = 7263023751982893102L;

	public ElectoralAuthorityServiceException(Throwable cause) {
		super(cause);
	}

	public ElectoralAuthorityServiceException(String message) {
		super(message);
	}

	public ElectoralAuthorityServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
