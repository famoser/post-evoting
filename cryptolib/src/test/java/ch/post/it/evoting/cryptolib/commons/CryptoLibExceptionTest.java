/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.commons;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.CryptoLibException;

/**
 * Tests of {@link CryptoLibException}.
 */
class CryptoLibExceptionTest {

	private static final String INVALID_CAUSE = "The cause should be: ";

	private static final String INVALID_MESSAGE = "The message should be: ";

	@Test
	void testMessageConstructor() {
		String expectedMessage = "errorMessage";

		CryptoLibException e = new CryptoLibException(expectedMessage);

		Assertions.assertEquals(expectedMessage, e.getMessage(), INVALID_MESSAGE);
	}

	@Test
	void testCauseConstructor() {
		Throwable expectedCause = new IllegalArgumentException();

		CryptoLibException e = new CryptoLibException(expectedCause);

		Assertions.assertEquals(expectedCause, e.getCause(), INVALID_CAUSE);
	}

	@Test
	void testMessageAndCauseConstructor() {
		String expectedMessage = "errorMessage";
		Throwable expectedCause = new IllegalArgumentException();

		CryptoLibException e = new CryptoLibException(expectedMessage, expectedCause);

		Assertions.assertEquals(expectedMessage, e.getMessage(), INVALID_MESSAGE);
		Assertions.assertEquals(expectedCause, e.getCause(), INVALID_CAUSE);
	}
}
