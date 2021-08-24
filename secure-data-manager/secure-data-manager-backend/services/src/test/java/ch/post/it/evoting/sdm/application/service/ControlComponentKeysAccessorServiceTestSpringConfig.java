/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.sdm.application.service;

import static org.mockito.Mockito.mock;

import java.nio.file.Paths;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.commons.PrefixPathResolver;
import ch.post.it.evoting.sdm.domain.model.electoralauthority.ElectoralAuthorityRepository;
import ch.post.it.evoting.sdm.domain.model.votingcardset.VotingCardSetRepository;

@Configuration
public class ControlComponentKeysAccessorServiceTestSpringConfig {

	private static final String TARGET_DIR = "target/ccKeysAccessorServiceTest/";

	@Bean
	public ControlComponentKeysAccessorService controlComponentKeysAccessorService() {
		return new ControlComponentKeysAccessorService();
	}

	@Bean
	public PathResolver pathResolver() {
		return new PrefixPathResolver(Paths.get(TARGET_DIR).toAbsolutePath().toString());
	}

	@Bean
	public ElectoralAuthorityRepository electoralAuthorityRepository() {
		return mock(ElectoralAuthorityRepository.class);
	}

	@Bean
	public VotingCardSetRepository votingCardSetRepository() {
		return mock(VotingCardSetRepository.class);
	}
}
