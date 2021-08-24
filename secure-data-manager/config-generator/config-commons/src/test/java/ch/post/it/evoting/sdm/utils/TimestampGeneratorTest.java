/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimestampGeneratorTest {

	private TimestampGenerator timestampGenerator;

	@BeforeEach
	void init() {
		timestampGenerator = new TimestampGenerator();
	}

	@Test
	void returnDateAndTimeInStringFormatThatCanBeReconstructedCorrectly() {

		final String formattedNow = assertDoesNotThrow(() -> timestampGenerator.getNewTimeStamp());
		final DateTimeFormatter formatter = assertDoesNotThrow(() -> timestampGenerator.getDateFormat());

		final LocalDateTime reformatted = LocalDateTime.parse(formattedNow, formatter);

		assertEquals(reformatted.format(formatter), formattedNow);
	}
}
