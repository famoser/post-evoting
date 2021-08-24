/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.logging.api.exceptions;

public class LoggingException extends RuntimeException {

	private static final long serialVersionUID = 530152086440252559L;

	public LoggingException(final String message) {
		super(message);
	}

	public LoggingException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public LoggingException(final Throwable cause) {
		super(cause);
	}
}
