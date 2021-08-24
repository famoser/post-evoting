/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.logging.core.factory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.post.it.evoting.logging.api.factory.LoggingFactory;
import ch.post.it.evoting.logging.api.formatter.MessageFormatter;
import ch.post.it.evoting.logging.api.writer.LoggingWriter;
import ch.post.it.evoting.logging.core.writer.LoggingWriterLog4j;

public class LoggingFactoryLog4j implements LoggingFactory {
	private final MessageFormatter messageFormatter;

	public LoggingFactoryLog4j(final MessageFormatter messageFormatter) {
		this.messageFormatter = messageFormatter;
	}

	/**
	 * @see LoggingFactory#getLogger(java.lang.Class)
	 */
	@Override
	public LoggingWriter getLogger(final Class<?> clazz) {
		final Logger logger = LogManager.getLogger(clazz);

		return new LoggingWriterLog4j(logger, messageFormatter);
	}

	@Override
	public LoggingWriter getLogger(final String loggerName) {
		final Logger logger = LogManager.getLogger(loggerName);

		return new LoggingWriterLog4j(logger, messageFormatter);
	}

}
