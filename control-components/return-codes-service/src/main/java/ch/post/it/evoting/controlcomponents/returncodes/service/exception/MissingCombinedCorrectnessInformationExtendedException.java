/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.service.exception;

public class MissingCombinedCorrectnessInformationExtendedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MissingCombinedCorrectnessInformationExtendedException(final String electionEventId, final String verificationCardSetId) {
		super(String.format("No combined correctness information extended found for election event id %s and verification card set id %s.",
				electionEventId, verificationCardSetId));
	}

}
