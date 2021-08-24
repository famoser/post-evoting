/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.exceptions.specific;

import ch.post.it.evoting.sdm.config.exceptions.CreateVotingCardSetException;

public class GenerateCredentialDataException extends CreateVotingCardSetException {

	private static final long serialVersionUID = 2706905178312822603L;

	public GenerateCredentialDataException(final String message) {
		super(message);
	}

	public GenerateCredentialDataException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public GenerateCredentialDataException(final Throwable cause) {
		super(cause);
	}
}
