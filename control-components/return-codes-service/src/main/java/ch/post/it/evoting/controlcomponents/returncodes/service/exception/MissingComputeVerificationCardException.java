/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.service.exception;

public class MissingComputeVerificationCardException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MissingComputeVerificationCardException(final String electionEventId, final String verificationCardId) {
		super(String
				.format("No existing computed verification card entry found for election event id %s and verification card id %s.", electionEventId,
						verificationCardId));
	}

}
