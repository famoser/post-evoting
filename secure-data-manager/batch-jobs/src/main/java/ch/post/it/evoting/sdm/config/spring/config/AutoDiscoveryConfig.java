/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.config;

import org.springframework.batch.core.configuration.support.ApplicationContextFactory;
import org.springframework.batch.core.configuration.support.GenericApplicationContextFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ch.post.it.evoting.sdm.config.spring.batch.ConfigJobConfig;
import ch.post.it.evoting.sdm.config.spring.batch.ConfigJobConfigChallenge;
import ch.post.it.evoting.sdm.config.spring.batch.ConfigJobConfigStandard;

@Configuration
public class AutoDiscoveryConfig {

	@Bean
	@Profile({ "!standard", "!challenge" })
	public ApplicationContextFactory configContextProduct() {
		return new GenericApplicationContextFactory(ConfigJobConfig.class);
	}

	@Bean
	@Profile("standard")
	public ApplicationContextFactory configStandardContext() {
		return new GenericApplicationContextFactory(ConfigJobConfigStandard.class);
	}

	@Bean
	@Profile("challenge")
	public ApplicationContextFactory configChallengeContext() {
		return new GenericApplicationContextFactory(ConfigJobConfigChallenge.class);
	}

}
