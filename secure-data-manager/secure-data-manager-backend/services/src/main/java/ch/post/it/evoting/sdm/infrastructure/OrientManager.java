/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure;

import com.orientechnologies.orient.core.Orient;

/**
 * OrientManager is used to manage the life-cycle of {@link Orient}.
 * <p>
 * Implementation must be thread-safe.
 */
public interface OrientManager {
	/**
	 * Returns if the underlying {@link Orient} instance is active.
	 *
	 * @return the underlying {@link Orient} instance is active.
	 */
	boolean isActive();

	/**
	 * Shutdowns the underlying {@link Orient} instance.
	 */
	void shutdown();
}
