/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.model.authentication.service;

import java.util.Optional;

import ch.post.it.evoting.sdm.config.model.authentication.ChallengeGenerator;
import ch.post.it.evoting.sdm.config.model.authentication.ExtraParams;

/**
 * Strategy in which there is no challenge.
 */
public class NoneChallengeGenerator implements ChallengeGenerator {

	@Override
	public ExtraParams generateExtraParams() {
		return ExtraParams.ofChallenges(Optional.empty(), Optional.empty());
	}
}
