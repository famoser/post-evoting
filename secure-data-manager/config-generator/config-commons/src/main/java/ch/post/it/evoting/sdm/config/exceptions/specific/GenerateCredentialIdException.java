/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.exceptions.specific;

import ch.post.it.evoting.sdm.config.exceptions.CreateVotingCardSetException;

public class GenerateCredentialIdException extends CreateVotingCardSetException {

	private static final long serialVersionUID = 5449910856687285210L;

	public GenerateCredentialIdException(final String message) {
		super(message);
	}

	public GenerateCredentialIdException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public GenerateCredentialIdException(final Throwable cause) {
		super(cause);
	}
}
