/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.mixdec.infrastructure.persistence;

public class MixDecNodeOutputRepositoryException extends Exception {

	private static final long serialVersionUID = 6828919932084028638L;

	public MixDecNodeOutputRepositoryException(Throwable cause) {
		super(cause);
	}

	public MixDecNodeOutputRepositoryException(String message) {
		super(message);
	}

	public MixDecNodeOutputRepositoryException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
