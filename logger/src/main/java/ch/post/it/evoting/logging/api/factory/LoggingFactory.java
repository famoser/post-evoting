/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.logging.api.factory;

import ch.post.it.evoting.logging.api.writer.LoggingWriter;

/**
 * Based on factory method pattern, it makes the logging easily extensible. The idea allow children decide which log technology use, techologies such
 * as log4j, logback, etc.
 */
public interface LoggingFactory {

	/**
	 * Given a class type the factory will return and log instance for this class.
	 *
	 * @param clazz
	 * @return LoggingWriter
	 */
	LoggingWriter getLogger(final Class<?> clazz);

	/**
	 * Given a logger name the factory will return and log instance for the name.
	 *
	 * @param loggerName Logger name
	 * @return LoggingWriter
	 */
	LoggingWriter getLogger(final String loggerName);

}
