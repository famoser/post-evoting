/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.exception;

public class AdminBoardUploadServiceException extends RuntimeException {

	private static final long serialVersionUID = 8026276349431237116L;

	public AdminBoardUploadServiceException(Throwable cause) {
		super(cause);
	}

	public AdminBoardUploadServiceException(String message) {
		super(message);
	}

	public AdminBoardUploadServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
