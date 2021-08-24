/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.exceptions.specific;

public class VerificationCardIdsGenerationException extends Exception {

	private static final long serialVersionUID = 4191302688176860484L;

	public VerificationCardIdsGenerationException(final String message) {
		super(message);
	}

	public VerificationCardIdsGenerationException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public VerificationCardIdsGenerationException(final Throwable cause) {
		super(cause);
	}
}
