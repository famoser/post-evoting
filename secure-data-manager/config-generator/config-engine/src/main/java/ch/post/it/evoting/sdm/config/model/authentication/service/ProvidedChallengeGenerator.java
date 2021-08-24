/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.model.authentication.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import ch.post.it.evoting.sdm.config.model.authentication.ChallengeGenerator;
import ch.post.it.evoting.sdm.config.model.authentication.ExtraParams;
import ch.post.it.evoting.sdm.config.model.authentication.ProvidedChallenges;

public class ProvidedChallengeGenerator implements ChallengeGenerator {

	private final ProvidedChallengeSource providedChallengeSource;

	public ProvidedChallengeGenerator(final ProvidedChallengeSource providedChallengeSource) {
		this.providedChallengeSource = providedChallengeSource;
	}

	@Override
	public ExtraParams generateExtraParams() {

		ProvidedChallenges providedChallenges = providedChallengeSource.next();

		String alias = providedChallenges.getAlias();
		List<String> challenges = providedChallenges.getChallenges();

		String value = challenges.stream().collect(Collectors.joining());

		return ExtraParams.ofChallenges(Optional.of(value), Optional.of(alias));
	}
}
