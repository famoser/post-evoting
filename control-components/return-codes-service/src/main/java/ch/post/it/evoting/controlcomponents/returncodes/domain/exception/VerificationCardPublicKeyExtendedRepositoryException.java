/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.controlcomponents.returncodes.domain.exception;

public class VerificationCardPublicKeyExtendedRepositoryException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public VerificationCardPublicKeyExtendedRepositoryException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
