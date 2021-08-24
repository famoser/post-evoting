/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.exceptions;

public class InvalidInputException extends ConfigurationEngineException {

	private static final long serialVersionUID = 3706414159718245813L;

	public InvalidInputException(final String message) {
		super(message);
	}

	public InvalidInputException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public InvalidInputException(final Throwable cause) {
		super(cause);
	}

}
