/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.commons;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * {@link PathResolver} implementation that resolves a path by treating it as absolute path
 */
public class AbsolutePathResolver implements PathResolver {

	private static final Path ROOT = Paths.get(File.separator);

	@Override
	public Path resolve(final String... pathStrings) {
		Path resolvedPath = Paths.get(pathStrings[0].trim());
		if (!resolvedPath.isAbsolute() && !resolvedPath.startsWith(File.separator)) {
			resolvedPath = ROOT.resolve(resolvedPath);
		}

		for (int i = 1; i < pathStrings.length; i++) {
			resolvedPath = Paths.get(resolvedPath.toString(), pathStrings[i].trim());
		}

		return resolvedPath;
	}
}
