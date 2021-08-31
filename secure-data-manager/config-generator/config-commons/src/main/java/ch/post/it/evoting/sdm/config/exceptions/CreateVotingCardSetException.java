/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.exceptions;

public class CreateVotingCardSetException extends ConfigurationEngineException {

	private static final long serialVersionUID = 3797022295745939298L;

	public CreateVotingCardSetException(final String message) {
		super(message);
	}

	public CreateVotingCardSetException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public CreateVotingCardSetException(final Throwable cause) {
		super(cause);
	}

}
