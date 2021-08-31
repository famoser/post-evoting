/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.commons;

import java.nio.file.Path;

/**
 * Interface for resolving path.
 */
public interface PathResolver {

	/**
	 * Resolves a path, given one or more path strings, in the correct order.
	 *
	 * @param pathStrings One or more path strings, in the correct order.
	 * @return the resolved path.
	 */
	Path resolve(final String... pathStrings);
}
