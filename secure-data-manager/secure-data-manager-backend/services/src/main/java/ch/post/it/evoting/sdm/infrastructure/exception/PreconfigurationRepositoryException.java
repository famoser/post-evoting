/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.exception;

public class PreconfigurationRepositoryException extends RuntimeException {

	private static final long serialVersionUID = -5271446720919937859L;

	public PreconfigurationRepositoryException(Throwable cause) {
		super(cause);
	}

	public PreconfigurationRepositoryException(String message) {
		super(message);
	}

	public PreconfigurationRepositoryException(String message, Throwable cause) {
		super(message, cause);
	}

}
