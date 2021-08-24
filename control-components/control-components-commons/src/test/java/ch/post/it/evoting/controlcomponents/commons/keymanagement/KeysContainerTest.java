/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.awaitility.Durations;
import org.junit.jupiter.api.Test;

/**
 * Tests of {@link KeysContainer}.
 */
class KeysContainerTest {
	private static final Object KEYS = new Object();

	@Test
	void testIsExpired() {
		KeysContainer container = new KeysContainer(KEYS, Duration.ofMillis(500));
		assertFalse(container.isExpired());

		await().atMost(Durations.ONE_SECOND).untilAsserted(() -> assertTrue(container.isExpired()));

		container.getKeys();
		assertFalse(container.isExpired());

		await().atMost(Durations.ONE_SECOND).untilAsserted(() -> assertTrue(container.isExpired()));
	}

	@Test
	void testGetKeys() {
		KeysContainer container = new KeysContainer(KEYS, Duration.ofSeconds(1));
		assertEquals(KEYS, container.getKeys());
	}
}
