/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.sdm.application.service;

import static org.mockito.Mockito.mock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.votingcardset.VotingCardSetRepository;
import ch.post.it.evoting.sdm.domain.service.BallotBoxDataGeneratorService;
import ch.post.it.evoting.sdm.domain.service.BallotDataGeneratorService;
import ch.post.it.evoting.sdm.domain.service.VotingCardSetDataGeneratorService;
import ch.post.it.evoting.sdm.infrastructure.cc.ReturnCodeGenerationRequestPayloadRepository;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;

@Configuration
public class VotingCardSetServiceTestSpringConfig {

	@Bean
	public VotingCardSetService votingCardSetService() {
		return new VotingCardSetService();
	}

	@Bean
	public SignatureService signatureService() {
		return new SignatureService();
	}

	@Bean
	public ExtendedAuthenticationService extendedAuthenticationService() {
		return new ExtendedAuthenticationService();
	}

	@Bean
	public PathResolver pathResolver() {
		return mock(PathResolver.class);
	}

	@Bean
	public VotingCardSetRepository votingCardSetRepository() {
		return mock(VotingCardSetRepository.class);
	}

	@Bean
	public ObjectMapper objectMapper() {
		return mock(ObjectMapper.class);
	}

	@Bean
	public IdleStatusService idleStatusService() {
		return mock(IdleStatusService.class);
	}

	@Bean
	public BallotBoxRepository ballotBoxRepository() {
		return mock(BallotBoxRepository.class);
	}

	@Bean
	public BallotBoxDataGeneratorService ballotBoxDataGeneratorService() {
		return mock(BallotBoxDataGeneratorService.class);
	}

	@Bean
	public BallotDataGeneratorService ballotDataGeneratorService() {
		return mock(BallotDataGeneratorService.class);
	}

	@Bean
	public VotingCardSetDataGeneratorService votingCardSetDataGeneratorService() {
		return mock(VotingCardSetDataGeneratorService.class);
	}

	@Bean
	public ConfigurationEntityStatusService configurationEntityStatusService() {
		return mock(ConfigurationEntityStatusService.class);
	}

	@Bean
	public VotingCardSetChoiceCodesService votingCardSetChoiceCodesService() {
		return mock(VotingCardSetChoiceCodesService.class);
	}

	@Bean
	public ReturnCodeGenerationRequestPayloadRepository returnCodeGenerationRequestPayloadRepository() {
		return mock(ReturnCodeGenerationRequestPayloadRepository.class);
	}

}
