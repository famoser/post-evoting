/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.logging.core.itest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import ch.post.it.evoting.logging.api.domain.LogContent;
import ch.post.it.evoting.logging.api.domain.LogField;
import ch.post.it.evoting.logging.api.exceptions.LoggingException;
import ch.post.it.evoting.logging.api.formatter.MessageFormatter;
import ch.post.it.evoting.logging.core.formatter.PipeSeparatedFormatter;

class LogRequiredFieldsTest {

	@Test
	void shouldNotThrowIfRequiredFieldsIsEmpty() {

		LogContent.LogContentBuilder builder = new LogContent.LogContentBuilder();

		builder.logEvent(LogEventTest.TEST_EVENT);
		LogContent logContent = builder.createLogInfo();

		MessageFormatter formatter = new PipeSeparatedFormatter("", "");

		assertDoesNotThrow(() -> formatter.buildMessage(logContent));
	}

	@Test
	void shouldNotThrowIfRequiredFieldsIsNull() {

		LogContent.LogContentBuilder builder = new LogContent.LogContentBuilder();

		builder.logEvent(LogEventTest.TEST_EVENT);
		LogContent logContent = builder.createLogInfo();

		MessageFormatter formatter = new PipeSeparatedFormatter("", "");

		assertDoesNotThrow(() -> formatter.buildMessage(logContent));
	}

	@Test
	void shouldThrowIfRequiredFieldIsNotPresent() {

		LogContent.LogContentBuilder builder = new LogContent.LogContentBuilder();

		builder.logEvent(LogEventTest.TEST_EVENT);
		LogContent logContent = builder.createLogInfo();

		MessageFormatter formatter = new PipeSeparatedFormatter("", "", LogField.USER);

		assertThrows(LoggingException.class, () -> formatter.buildMessage(logContent));
	}

	@Test
	void shouldNotThrowIfRequiredFieldIsPresent() {

		LogContent.LogContentBuilder builder = new LogContent.LogContentBuilder();

		builder.logEvent(LogEventTest.TEST_EVENT);
		builder.user("test");
		LogContent logContent = builder.createLogInfo();

		MessageFormatter formatter = new PipeSeparatedFormatter("", "", LogField.USER);

		assertDoesNotThrow(() -> formatter.buildMessage(logContent));
	}

	@Test
	void shouldNotThrowIfManyRequiredFieldArePresent() {

		LogContent.LogContentBuilder builder = new LogContent.LogContentBuilder();

		builder.logEvent(LogEventTest.TEST_EVENT);
		builder.objectId("object id test");
		builder.user("test");
		LogContent logContent = builder.createLogInfo();

		MessageFormatter formatter = new PipeSeparatedFormatter("", "", LogField.USER, LogField.OBJECTID);

		assertDoesNotThrow(() -> formatter.buildMessage(logContent));
	}

	@Test
	void shouldThrowIfOneOfManyRequiredFieldsIsNotPresent() {

		LogContent.LogContentBuilder builder = new LogContent.LogContentBuilder();

		builder.logEvent(LogEventTest.TEST_EVENT);
		builder.objectId("object id test");
		LogContent logContent = builder.createLogInfo();

		MessageFormatter formatter = new PipeSeparatedFormatter("", "", LogField.USER, LogField.OBJECTID);

		assertThrows(LoggingException.class, () -> formatter.buildMessage(logContent));
	}
}
