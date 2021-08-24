/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.exception;

public class ElectionEventServiceException extends RuntimeException {

	private static final long serialVersionUID = -1380844286483578202L;

	public ElectionEventServiceException(Throwable cause) {
		super(cause);
	}

	public ElectionEventServiceException(String message) {
		super(message);
	}

	public ElectionEventServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
