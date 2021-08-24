/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;

@Service
public class TenantCAService extends FileRootCAService {

	public TenantCAService(PathResolver pathResolver,
			@Value("${tenantID}")
					String tenantId) {
		super(pathResolver, String.format(Constants.CONFIG_FILE_NAME_TENANT_CA_PATTERN, tenantId));
	}
}
