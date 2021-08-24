/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.exception;

public class VotingCardSetChoiceCodesServiceException extends RuntimeException {

	private static final long serialVersionUID = 8069770207692371130L;

	public VotingCardSetChoiceCodesServiceException(Throwable cause) {
		super(cause);
	}

	public VotingCardSetChoiceCodesServiceException(String message) {
		super(message);
	}

	public VotingCardSetChoiceCodesServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
