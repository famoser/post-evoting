/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.exceptions.specific;

import ch.post.it.evoting.sdm.config.exceptions.CreateVotingCardSetException;

public class GenerateVerificationCardDataException extends CreateVotingCardSetException {

	private static final long serialVersionUID = -2311425072164493511L;

	public GenerateVerificationCardDataException(final String message) {
		super(message);
	}

	public GenerateVerificationCardDataException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public GenerateVerificationCardDataException(final Throwable cause) {
		super(cause);
	}
}
