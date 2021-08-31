/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.status;

/*
 * An entity has been attempted to be transitioned to a status that cannot be reached from its
 * current status.
 */
public class InvalidStatusTransitionException extends Exception {

	private static final long serialVersionUID = -5773020954555377570L;

	public InvalidStatusTransitionException(Status from, Status to) {

		super(String.format("Status %s could not be transitioned to status %s", from, to));
	}
}
