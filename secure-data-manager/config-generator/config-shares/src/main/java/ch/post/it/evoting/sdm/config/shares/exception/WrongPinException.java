/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.shares.exception;

/**
 * Exception thrown when trying to read a smartcard with the wrong pin
 */
public final class WrongPinException extends SmartcardException {

	private static final long serialVersionUID = 7647658209631196258L;

	public WrongPinException() {
		super("The pin is not correct", null);
	}
}
