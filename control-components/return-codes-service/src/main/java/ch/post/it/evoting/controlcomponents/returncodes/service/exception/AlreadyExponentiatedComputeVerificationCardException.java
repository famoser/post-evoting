/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.service.exception;

public class AlreadyExponentiatedComputeVerificationCardException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AlreadyExponentiatedComputeVerificationCardException(final String electionEventId, final String verificationCardId) {
		super(String
				.format("Exponentiation already done for verification card with election event id %s and verification card id %s.", electionEventId,
						verificationCardId));
	}

}
