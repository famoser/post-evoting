/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.model.authentication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.sdm.config.model.authentication.service.ProvidedChallengeGenerator;

class ProvidedChallengeGeneratorTest {

	private final ChallengeGenerator challengeGenerator = new ProvidedChallengeGenerator(
			() -> new ProvidedChallenges(RandomStringUtils.random(10), Collections.emptyList()));

	@Test
	void generateNonNullParams() {
		final ExtraParams extraParams = challengeGenerator.generateExtraParams();
		assertNotNull(extraParams);
	}

	@Test
	void generatesNonNullValue() {
		final ExtraParams extraParams = challengeGenerator.generateExtraParams();
		assertTrue(extraParams.getValue().isPresent());
	}

	@Test
	void generatesNonNullAlias() {
		final ExtraParams extraParams = challengeGenerator.generateExtraParams();
		assertTrue(extraParams.getAlias().isPresent());
	}

}
