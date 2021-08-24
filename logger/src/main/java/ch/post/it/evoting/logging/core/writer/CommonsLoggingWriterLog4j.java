/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.logging.core.writer;

import org.apache.logging.log4j.Logger;

import ch.post.it.evoting.logging.api.domain.Level;
import ch.post.it.evoting.logging.api.formatter.MessageFormatter;

public class CommonsLoggingWriterLog4j {

	protected final Logger logger;

	protected final MessageFormatter messageFormatter;

	public CommonsLoggingWriterLog4j(final Logger logger, final MessageFormatter messageFormatter) {
		this.logger = logger;
		this.messageFormatter = messageFormatter;
	}

	protected void addMessageWithLevel(final Level level, final String message) {

		if (level == Level.INFO) {
			logger.info(message);
		} else if (level == Level.ERROR) {
			logger.error(message);
		} else if (level == Level.WARN) {
			logger.warn(message);
		} else if (level == Level.DEBUG) {
			logger.debug(message);
		}
	}
}
