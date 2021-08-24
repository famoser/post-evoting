/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.config;

import static org.mockito.Mockito.mock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.client.RestTemplate;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.sdm.application.service.BallotBoxService;
import ch.post.it.evoting.sdm.application.service.VotingCardSetService;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.commons.PrefixPathResolver;
import ch.post.it.evoting.sdm.domain.service.BallotBoxDataGeneratorService;
import ch.post.it.evoting.sdm.domain.service.BallotDataGeneratorService;
import ch.post.it.evoting.sdm.domain.service.ElectionEventDataGeneratorService;
import ch.post.it.evoting.sdm.domain.service.VotingCardSetDataGeneratorService;
import ch.post.it.evoting.sdm.domain.service.utils.PublicKeyLoader;
import ch.post.it.evoting.sdm.domain.service.utils.SystemTenantPublicKeyLoader;

/**
 * MVC Configuration
 */
@Configuration
@ComponentScan(basePackages = { "ch.post.it.evoting.sdm.infrastructure" })
@PropertySources({ @PropertySource("classpath:config/sdm_db_test_application.properties"), @PropertySource("${upload.config.source}"),
		@PropertySource("${download.config.source}"), @PropertySource("${sdm.config.source}") })
@Profile("test")
public class SdmDatabaseConfig {

	@Value("${user.home}")
	private String prefix;

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertiesResolver() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public PathResolver getPrefixPathResolver() {
		return new PrefixPathResolver(prefix);
	}

	@Bean
	public SystemTenantPublicKeyLoader getSystemTenantPublicKeyLoader() {
		return new SystemTenantPublicKeyLoader();
	}

	@Bean
	public PublicKeyLoader getPublicKeyLoader() {
		return new PublicKeyLoader();
	}

	@Bean
	public BallotBoxService getBallotBoxService() {
		return new BallotBoxService();
	}

	@Bean
	public VotingCardSetService getVotingCardSetService() {
		return new VotingCardSetService();
	}

	@Bean
	public BallotBoxDataGeneratorService getBallotBoxDataGeneratorService() {
		return mock(BallotBoxDataGeneratorService.class);
	}

	@Bean
	public BallotDataGeneratorService getBallotDataGeneratorService() {
		return mock(BallotDataGeneratorService.class);
	}

	@Bean
	public ElectionEventDataGeneratorService getElectionEventDataGeneratorService() {
		return mock(ElectionEventDataGeneratorService.class);
	}

	@Bean
	public VotingCardSetDataGeneratorService getVotingCardSetDataGeneratorService() {
		return mock(VotingCardSetDataGeneratorService.class);
	}

	@Bean
	public RestTemplate getRestTemplate() {
		return mock(RestTemplate.class);
	}

	@Bean
	AsymmetricServiceAPI asymmetricServiceAPI() {
		return new AsymmetricService();
	}

}
