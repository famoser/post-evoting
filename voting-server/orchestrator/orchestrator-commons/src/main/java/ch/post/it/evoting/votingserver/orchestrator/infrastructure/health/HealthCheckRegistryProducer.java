/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.infrastructure.health;

import javax.annotation.Resource;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.sql.DataSource;

import ch.post.it.evoting.votingserver.commons.infrastructure.health.CachingHealthCheck;
import ch.post.it.evoting.votingserver.commons.infrastructure.health.DatabaseHealthCheck;
import ch.post.it.evoting.votingserver.commons.infrastructure.health.HealthCheckRegistry;
import ch.post.it.evoting.votingserver.commons.infrastructure.health.HealthCheckValidationType;
import ch.post.it.evoting.votingserver.commons.util.PropertiesFileReader;

@Dependent
public class HealthCheckRegistryProducer {

	@Resource(name = "or")
	DataSource dataSource;

	@Produces
	public HealthCheckRegistry getHealthCheckRegistry() {
		final PropertiesFileReader properties = PropertiesFileReader.getInstance();
		final String validationQuery = properties.getPropertyValue("health.check.db.validation.query");
		final int cacheTtl = Integer.parseInt(properties.getPropertyValue("health.check.db.validation.cache.ttl"));

		final HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
		healthCheckRegistry
				.register(HealthCheckValidationType.DATABASE, new CachingHealthCheck(new DatabaseHealthCheck(dataSource, validationQuery), cacheTtl));

		return healthCheckRegistry;
	}
}
