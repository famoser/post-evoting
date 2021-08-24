/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.model.authentication.service;

import org.springframework.beans.factory.annotation.Autowired;

import ch.post.it.evoting.sdm.config.model.authentication.ChallengeGenerator;
import ch.post.it.evoting.sdm.config.model.authentication.ChallengeGeneratorStrategyType;

/**
 * Factory class to get the instance of the specific strategy to generate authentication challenge data of the voter
 */
public class ChallengeGeneratorFactory {

	@Autowired
	private ProvidedChallengeSource providedChallengeSource;

	public ChallengeGenerator createStrategy(final ChallengeGeneratorStrategyType challengeGeneratorStrategy) {

		switch (challengeGeneratorStrategy) {
		case NONE:
			return new NoneChallengeGenerator();
		case PROVIDED:
			return new ProvidedChallengeGenerator(providedChallengeSource);
		}
		throw new UnsupportedOperationException();
	}
}
