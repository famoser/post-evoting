/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.model.authentication.service;

import ch.post.it.evoting.sdm.config.model.authentication.AuthenticationKeyGenerator;
import ch.post.it.evoting.sdm.config.model.authentication.AuthenticationKeyGeneratorStrategyType;

/**
 * Factory class to get the instance of the specific strategy to generate authentication data of the voter
 */
public class AuthenticationGeneratorFactory {

	public AuthenticationKeyGenerator createStrategy(final AuthenticationKeyGeneratorStrategyType authGeneratorStrategy) {

		switch (authGeneratorStrategy) {
		case SIMPLE:
			return new SimpleAuthenticationKeyGenerator();
		case SINGLESECRET:
			return new SingleSecretAuthenticationKeyGenerator();
		}
		throw new UnsupportedOperationException();
	}
}
