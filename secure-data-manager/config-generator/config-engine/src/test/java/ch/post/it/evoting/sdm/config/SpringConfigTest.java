/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.primitives.PrimitivesServiceAPI;
import ch.post.it.evoting.cryptolib.api.symmetric.SymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.primitives.service.PrimitivesService;
import ch.post.it.evoting.cryptolib.symmetric.service.SymmetricService;
import ch.post.it.evoting.sdm.config.actions.ExtendedAuthenticationService;
import ch.post.it.evoting.sdm.config.model.authentication.AuthKeyGeneratorBeyondLimit;
import ch.post.it.evoting.sdm.config.model.authentication.AuthenticationKeyGenerator;
import ch.post.it.evoting.sdm.config.model.authentication.AuthenticationKeyGeneratorStrategyType;
import ch.post.it.evoting.sdm.config.model.authentication.ChallengeGenerator;
import ch.post.it.evoting.sdm.config.model.authentication.ChallengeGeneratorStrategyType;
import ch.post.it.evoting.sdm.config.model.authentication.LongSecretsAuthKeyGenerator;
import ch.post.it.evoting.sdm.config.model.authentication.service.AuthenticationGeneratorFactory;
import ch.post.it.evoting.sdm.config.model.authentication.service.AuthenticationKeyCryptoService;
import ch.post.it.evoting.sdm.config.model.authentication.service.ChallengeGeneratorFactory;
import ch.post.it.evoting.sdm.config.model.authentication.service.ChallengeService;
import ch.post.it.evoting.sdm.config.model.authentication.service.ChallengeServiceAPI;
import ch.post.it.evoting.sdm.config.model.authentication.service.ProvidedChallengeSource;
import ch.post.it.evoting.sdm.config.model.authentication.service.SequentialProvidedChallengeSource;
import ch.post.it.evoting.sdm.config.model.authentication.service.StartVotingKeyService;

@Configuration
public class SpringConfigTest {

	@Bean
	SymmetricServiceAPI symmetricServiceAPI() throws GeneralCryptoLibException {
		return new SymmetricService();
	}

	@Bean
	PrimitivesServiceAPI primitivesServiceAPI() throws GeneralCryptoLibException {
		return new PrimitivesService();
	}

	@Bean
	AuthenticationKeyCryptoService authKeyService() {
		return new AuthenticationKeyCryptoService();
	}

	@Bean
	public ProvidedChallengeSource providedChallengeSource() throws URISyntaxException {
		final URL url = this.getClass().getResource("/aliasDataSample.csv");
		Path aliasesPath = new File(url.toURI()).toPath();
		return new SequentialProvidedChallengeSource(aliasesPath);
	}

	@Bean
	ChallengeGeneratorFactory challengeGeneratorFactory() {
		return new ChallengeGeneratorFactory();
	}

	@Bean
	ChallengeGenerator challengeGenerator(final ChallengeGeneratorFactory challengeGeneratorFactory) {
		return challengeGeneratorFactory.createStrategy(ChallengeGeneratorStrategyType.NONE);
	}

	@Bean
	ChallengeServiceAPI challengeService(final PrimitivesServiceAPI primitivesService, final ChallengeGenerator challengeGenerator) {
		return new ChallengeService(primitivesService, challengeGenerator);
	}

	@Bean
	AuthenticationGeneratorFactory authenticationGeneratorFactory() {
		return new AuthenticationGeneratorFactory();
	}

	@Bean
	@Qualifier("SIMPLE")
	AuthenticationKeyGenerator simpleAuthenticationKeyGeneratorStrategy(final AuthenticationGeneratorFactory authenticationGeneratorFactory) {
		return authenticationGeneratorFactory.createStrategy(AuthenticationKeyGeneratorStrategyType.SIMPLE);
	}

	@Bean
	@Qualifier("LONGSECRETS")
	AuthenticationKeyGenerator longSecretsAuthenticationKeyGeneratorStrategy() {
		return new LongSecretsAuthKeyGenerator();
	}

	@Bean
	@Qualifier("BEYONDLIMIT")
	AuthenticationKeyGenerator authenticationKeyGeneratorBeyondLimit() {
		return new AuthKeyGeneratorBeyondLimit();
	}

	@Bean
	ExtendedAuthenticationService createAndHandleAuthKey(final AuthenticationKeyCryptoService authKeyService,
			@Qualifier("SIMPLE")
			final AuthenticationKeyGenerator authenticationKeyGenerator, final ChallengeServiceAPI challengeService) {
		return new ExtendedAuthenticationService(authKeyService, authenticationKeyGenerator, challengeService);
	}

	@Bean
	@Qualifier("LONGSECRETS")
	StartVotingKeyService startVotingKeyServiceWithLongSecrets(
			@Qualifier("LONGSECRETS")
			final AuthenticationKeyGenerator authenticationKeyGenerator) {
		return new StartVotingKeyService(authenticationKeyGenerator);
	}

	@Bean
	@Qualifier("BEYONDLIMIT")
	StartVotingKeyService startVotingKeyServiceBeyondLimit(
			@Qualifier("BEYONDLIMIT")
			final AuthenticationKeyGenerator authenticationKeyGenerator) {
		return new StartVotingKeyService(authenticationKeyGenerator);
	}

	@Bean
	AuthenticationKeyCryptoService authenticationKeyCryptoService() {
		return new AuthenticationKeyCryptoService();
	}

}
