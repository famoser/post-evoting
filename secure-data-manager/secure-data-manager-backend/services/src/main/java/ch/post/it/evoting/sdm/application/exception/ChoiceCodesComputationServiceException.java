/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.exception;

public class ChoiceCodesComputationServiceException extends RuntimeException {

	private static final long serialVersionUID = 4050170916637297678L;

	public ChoiceCodesComputationServiceException(Throwable cause) {
		super(cause);
	}

	public ChoiceCodesComputationServiceException(String message) {
		super(message);
	}

	public ChoiceCodesComputationServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
