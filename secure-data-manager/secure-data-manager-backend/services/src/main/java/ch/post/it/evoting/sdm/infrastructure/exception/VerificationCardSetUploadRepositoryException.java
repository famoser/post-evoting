/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.exception;

public class VerificationCardSetUploadRepositoryException extends RuntimeException {

	private static final long serialVersionUID = 1907863331648478803L;

	public VerificationCardSetUploadRepositoryException(Throwable cause) {
		super(cause);
	}

	public VerificationCardSetUploadRepositoryException(String message) {
		super(message);
	}

	public VerificationCardSetUploadRepositoryException(String message, Throwable cause) {
		super(message, cause);
	}

}
