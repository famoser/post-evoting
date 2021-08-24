/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.exceptions.specific;

import ch.post.it.evoting.sdm.config.exceptions.CreateElectionEventException;

public class GenerateElectionEventCAException extends CreateElectionEventException {

	private static final long serialVersionUID = 7632515886602651521L;

	public GenerateElectionEventCAException(final String message) {
		super(message);
	}

	public GenerateElectionEventCAException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public GenerateElectionEventCAException(final Throwable cause) {
		super(cause);
	}
}
