/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.exception;

public class CheckedIllegalStateException extends Exception {
	private static final long serialVersionUID = 1;

	public CheckedIllegalStateException(Throwable cause) {
		super(cause);
	}

	public CheckedIllegalStateException(String message) {
		super(message);
	}

	public CheckedIllegalStateException(String message, Throwable cause) {
		super(message, cause);
	}
}

