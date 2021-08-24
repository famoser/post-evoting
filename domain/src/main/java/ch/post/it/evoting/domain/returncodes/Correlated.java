/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.returncodes;

import java.util.UUID;

/**
 * Correlated entity.
 */
public interface Correlated {
	/**
	 * Returns the correlation identifier.
	 *
	 * @return the correlation identifier.
	 */
	UUID getCorrelationId();

	/**
	 * Sets the correlation identifier.
	 *
	 * @param correlationId the correlation identifier.
	 */
	void setCorrelationId(UUID correlationId);
}
