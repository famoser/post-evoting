/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.logging.core.writer;

import org.apache.logging.log4j.Logger;

import ch.post.it.evoting.logging.api.domain.Level;
import ch.post.it.evoting.logging.api.domain.LogContent;
import ch.post.it.evoting.logging.api.formatter.MessageFormatter;
import ch.post.it.evoting.logging.api.writer.LoggingWriter;

public class LoggingWriterLog4j extends CommonsLoggingWriterLog4j implements LoggingWriter {

	/**
	 * @param logger
	 * @param messageFormatter
	 */
	public LoggingWriterLog4j(final Logger logger, final MessageFormatter messageFormatter) {
		super(logger, messageFormatter);
	}

	/**
	 * @see LoggingWriter#log(Level, LogContent)
	 */
	@Override
	public void log(final Level level, final LogContent logContent) {
		String message = messageFormatter.buildMessage(logContent);

		addMessageWithLevel(level, message);

	}
}
