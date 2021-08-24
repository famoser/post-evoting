/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;

@Service
public class PlatformRootCAService extends FileRootCAService {
	public PlatformRootCAService(PathResolver pathResolver) {
		super(pathResolver, Constants.CONFIG_FILE_NAME_PLATFORM_ROOT_CA);
	}
}
