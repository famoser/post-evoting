/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.shares.exception;

public final class ConfigSharesException extends RuntimeException {

	private static final long serialVersionUID = 6718357803841022614L;

	public ConfigSharesException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public ConfigSharesException(final String cause) {
		super(cause);
	}
}
