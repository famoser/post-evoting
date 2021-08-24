/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.exception;

public class BallotBoxServiceException extends RuntimeException {

	private static final long serialVersionUID = -188943882949381942L;

	public BallotBoxServiceException(Throwable cause) {
		super(cause);
	}

	public BallotBoxServiceException(String message) {
		super(message);
	}

	public BallotBoxServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
