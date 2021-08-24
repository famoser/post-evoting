/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.service.exception;

public class MissingVerificationCardPublicKeyExtendedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MissingVerificationCardPublicKeyExtendedException(final String verificationCardId) {
		super(String.format("No verification card public key extended entry found for verification card id %s.", verificationCardId));
	}

}
