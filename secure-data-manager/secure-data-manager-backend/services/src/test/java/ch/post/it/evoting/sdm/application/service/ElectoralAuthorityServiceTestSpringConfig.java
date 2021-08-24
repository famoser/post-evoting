/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.sdm.application.service;

import static org.mockito.Mockito.mock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.post.it.evoting.sdm.application.config.SmartCardConfig;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.config.shares.handler.CreateSharesHandler;
import ch.post.it.evoting.sdm.config.shares.handler.StatelessReadSharesHandler;
import ch.post.it.evoting.sdm.domain.model.ballot.BallotRepository;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.electionevent.ElectionEventRepository;
import ch.post.it.evoting.sdm.domain.model.electoralauthority.ElectoralAuthorityRepository;
import ch.post.it.evoting.sdm.domain.model.votingcardset.VotingCardSetRepository;
import ch.post.it.evoting.sdm.domain.service.impl.CCPublicKeySignatureValidator;
import ch.post.it.evoting.sdm.domain.service.impl.ElectoralAuthorityDataGeneratorServiceImpl;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;

@Configuration
public class ElectoralAuthorityServiceTestSpringConfig {

	@Bean
	public ElectoralAuthorityService electoralAuthorityService() {
		return new ElectoralAuthorityService();
	}

	@Bean
	public SmartCardConfig smartCardConfig() {
		return SmartCardConfig.SMART_CARD;
	}

	@Bean
	public ElectoralAuthorityRepository electoralAuthorityRepository() {
		return mock(ElectoralAuthorityRepository.class);
	}

	@Bean
	public ElectionEventRepository electionEventRepository() {
		return mock(ElectionEventRepository.class);
	}

	@Bean
	public CreateSharesHandler createSharesHandler() {
		return mock(CreateSharesHandler.class);
	}

	@Bean
	public StatelessReadSharesHandler statelessReadSharesHandler() {
		return mock(StatelessReadSharesHandler.class);
	}

	@Bean
	public ConfigurationEntityStatusService configurationEntityStatusService() {
		return mock(ConfigurationEntityStatusService.class);
	}

	@Bean
	public BallotRepository ballotRepository() {
		return mock(BallotRepository.class);
	}

	@Bean
	public BallotBoxRepository ballotBoxRepository() {
		return mock(BallotBoxRepository.class);
	}

	@Bean
	public VotingCardSetRepository votingCardSetRepository() {
		return mock(VotingCardSetRepository.class);
	}

	@Bean
	public ElectoralAuthorityDataGeneratorServiceImpl electoralAuthorityDataGeneratorServiceImpl() {
		return mock(ElectoralAuthorityDataGeneratorServiceImpl.class);
	}

	@Bean
	public PathResolver pathResolver() {
		return mock(PathResolver.class);
	}

	@Bean
	public PlatformRootCAService platformRootCAService() {
		return mock(PlatformRootCAService.class);
	}

	@Bean
	public CCPublicKeySignatureValidator cCPublicKeySignatureValidator() {
		return mock(CCPublicKeySignatureValidator.class);
	}

	@Bean
	public ControlComponentKeysAccessorService controlComponentKeysAccessorService() {
		return mock(ControlComponentKeysAccessorService.class);
	}

	@Bean
	public SignaturesVerifierService signaturesVerifierService() {
		return mock(SignaturesVerifierService.class);
	}

	@Bean
	public HashService hashService() {
		return mock(HashService.class);
	}
}
