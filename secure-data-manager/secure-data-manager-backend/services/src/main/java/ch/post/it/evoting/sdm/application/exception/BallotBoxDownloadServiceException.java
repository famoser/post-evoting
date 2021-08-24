/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.exception;

public class BallotBoxDownloadServiceException extends RuntimeException {

	private static final long serialVersionUID = -5397322233066334805L;

	public BallotBoxDownloadServiceException(Throwable cause) {
		super(cause);
	}

	public BallotBoxDownloadServiceException(String message) {
		super(message);
	}

	public BallotBoxDownloadServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
