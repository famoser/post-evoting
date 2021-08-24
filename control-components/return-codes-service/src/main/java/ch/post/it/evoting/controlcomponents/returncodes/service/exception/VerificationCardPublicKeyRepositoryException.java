/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.controlcomponents.returncodes.service.exception;

public class VerificationCardPublicKeyRepositoryException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public VerificationCardPublicKeyRepositoryException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
