/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.exception;

public class CleansingOutputsDownloadServiceException extends RuntimeException {

	private static final long serialVersionUID = 1739724214530186991L;

	public CleansingOutputsDownloadServiceException(Throwable cause) {
		super(cause);
	}

	public CleansingOutputsDownloadServiceException(String message) {
		super(message);
	}

	public CleansingOutputsDownloadServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
