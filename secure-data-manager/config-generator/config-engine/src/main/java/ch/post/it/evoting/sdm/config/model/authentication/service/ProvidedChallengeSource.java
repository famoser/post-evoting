/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.model.authentication.service;

import ch.post.it.evoting.sdm.config.model.authentication.ProvidedChallenges;

public interface ProvidedChallengeSource {

	ProvidedChallenges next();

}
