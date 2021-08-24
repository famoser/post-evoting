/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.logging.core.itest;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.logging.api.domain.Level;
import ch.post.it.evoting.logging.api.domain.LogContent;
import ch.post.it.evoting.logging.api.domain.LogContent.LogContentBuilder;
import ch.post.it.evoting.logging.api.factory.LoggingFactory;
import ch.post.it.evoting.logging.api.formatter.MessageFormatter;
import ch.post.it.evoting.logging.api.writer.LoggingWriter;
import ch.post.it.evoting.logging.core.factory.LoggingFactoryLog4j;
import ch.post.it.evoting.logging.core.formatter.PipeSeparatedFormatter;

/**
 * Integration test to verify the logging system case of use
 */
class LoggingIntegrationTest {

	private Path folder;

	@BeforeEach
	public void setUp() {
		folder = Paths.get("target/logs").toAbsolutePath();
	}

	@Test
	void itShouldLogWithLog4jWithDifferentLoggers() throws Exception {

		// 0. configuration is loaded from test classpath: log4j2.xml

		// 1. create an log by using LogContent class
		LogContentBuilder builder = new LogContentBuilder();
		builder.logEvent(LogEventTest.TEST_EVENT);
		builder.objectType("objectType");
		builder.objectId("objectId");
		builder.user("userID");
		builder.electionEvent("123456789");
		builder.additionalInfo("message", "test compatibility log4j");
		builder.additionalInfo("errorcode", "0");
		builder.additionalInfo("exception", "no error");

		LogContent content = builder.createLogInfo();

		// 2. create instance of MessageFormatter
		MessageFormatter formatter = new PipeSeparatedFormatter("APPLICATION", "MODULE");

		// 3. create single instance of factory, ideally is use ejb
		LoggingFactory loggerFactory = new LoggingFactoryLog4j(formatter);

		// 4. obtain LoggingWriter instance
		final LoggingWriter writer = loggerFactory.getLogger(LoggingIntegrationTest.class);

		Path infoFile = folder.resolve("info-log4j.log").toAbsolutePath();
		Path errorFile = folder.resolve("error-log4j.log");
		Path warnFile = folder.resolve("warn-log4j.log");
		Path debugFile = folder.resolve("debug-log4j.log");

		long infoSize = Files.exists(infoFile) ? Files.size(infoFile) : 0;
		long errorSize = Files.exists(errorFile) ? Files.size(errorFile) : 0;
		long warnSize = Files.exists(warnFile) ? Files.size(warnFile) : 0;
		long debugrSize = Files.exists(debugFile) ? Files.size(debugFile) : 0;

		writer.log(Level.DEBUG, content);
		writer.log(Level.INFO, content);
		writer.log(Level.WARN, content);
		writer.log(Level.ERROR, content);

		assertAll(() -> assertTrue(Files.size(infoFile) > infoSize), () -> assertTrue(Files.size(errorFile) > errorSize),
				() -> assertTrue(Files.size(warnFile) > warnSize), () -> assertTrue(Files.size(debugFile) > debugrSize));
	}

	@Test
	void itShouldLogWithLog4jWithDifferentLoggersOnlyWithMandatoryFields() throws Exception {

		// 0. configuration is loaded from test classpath: log4j2.xml

		// 1. create an log by using LogContent class
		final LogContentBuilder builder = new LogContentBuilder();
		builder.logEvent(LogEventTest.TEST_EVENT);
		builder.user("userID");
		builder.additionalInfo("message", "test compatibility log4j");
		builder.additionalInfo("errorcode", "0");
		builder.additionalInfo("exception", "no error");

		final LogContent content = builder.createLogInfo();

		// 2. create instance of MessageFormatter
		final MessageFormatter formatter = new PipeSeparatedFormatter("APPLICATION", "MODULE");

		// 3. create single instance of factory, ideally is use ejb
		final LoggingFactory loggerFactory = new LoggingFactoryLog4j(formatter);

		Path infoFile = folder.resolve("info-log4j.log").toAbsolutePath();
		Path errorFile = folder.resolve("error-log4j.log");
		Path warnFile = folder.resolve("warn-log4j.log");
		Path debugFile = folder.resolve("debug-log4j.log");

		long infoSize = Files.exists(infoFile) ? Files.size(infoFile) : 0;
		long errorSize = Files.exists(errorFile) ? Files.size(errorFile) : 0;
		long warnSize = Files.exists(warnFile) ? Files.size(warnFile) : 0;
		long debugrSize = Files.exists(debugFile) ? Files.size(debugFile) : 0;

		// 4. obtain LoggingWriter instance
		final LoggingWriter writer = loggerFactory.getLogger(LoggingIntegrationTest.class);
		writer.log(Level.DEBUG, content);
		writer.log(Level.INFO, content);
		writer.log(Level.WARN, content);
		writer.log(Level.ERROR, content);

		assertAll(() -> assertTrue(Files.size(infoFile) > infoSize), () -> assertTrue(Files.size(errorFile) > errorSize),
				() -> assertTrue(Files.size(warnFile) > warnSize), () -> assertTrue(Files.size(debugFile) > debugrSize));
	}
}
