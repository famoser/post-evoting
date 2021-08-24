/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.service.exception;

public class NotExponentiatedComputeVerificationCardException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NotExponentiatedComputeVerificationCardException(final String electionEventId, final String verificationCardId) {
		super(String
				.format("createLCCShare_j algorithm was not executed for verification card with election event id %s and verification card id %s.",
						electionEventId, verificationCardId));
	}

}
