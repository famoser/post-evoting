/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.returncodes;

import java.util.UUID;

/**
 * Basic implementation of {@link Correlated}:
 */
public class CorrelatedSupport implements Correlated {
	private UUID correlationId;

	/**
	 * Constructor.
	 */
	public CorrelatedSupport() {
	}

	/**
	 * Constructor.
	 *
	 * @param correlationId the correlation identifier.
	 */
	public CorrelatedSupport(UUID correlationId) {
		setCorrelationId(correlationId);
	}

	@Override
	public UUID getCorrelationId() {
		return correlationId;
	}

	@Override
	public void setCorrelationId(UUID correlationId) {
		this.correlationId = correlationId;
	}
}
