/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.exceptions.specific;

import ch.post.it.evoting.sdm.config.exceptions.CreateBallotBoxesException;

public class GenerateBallotBoxesException extends CreateBallotBoxesException {

	private static final long serialVersionUID = 770571259291263625L;

	public GenerateBallotBoxesException(final String message) {
		super(message);
	}

	public GenerateBallotBoxesException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public GenerateBallotBoxesException(final Throwable cause) {
		super(cause);
	}

}
