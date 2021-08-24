/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.commons.domain.spring.batch;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * Tests of {@link SensitiveJobParameter}.
 */
class SensitiveJobParameterTest {
	@Test
	void testToString() {
		SensitiveJobParameter parameter = new SensitiveJobParameter("value", true);
		assertFalse(parameter.toString().contains("value"));
	}
}
