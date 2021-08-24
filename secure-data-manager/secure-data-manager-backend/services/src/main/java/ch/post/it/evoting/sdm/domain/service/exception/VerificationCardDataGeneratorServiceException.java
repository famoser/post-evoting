/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service.exception;

public class VerificationCardDataGeneratorServiceException extends RuntimeException {

	private static final long serialVersionUID = 262169831436423910L;

	public VerificationCardDataGeneratorServiceException(Throwable cause) {
		super(cause);
	}

	public VerificationCardDataGeneratorServiceException(String message) {
		super(message);
	}

	public VerificationCardDataGeneratorServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}
