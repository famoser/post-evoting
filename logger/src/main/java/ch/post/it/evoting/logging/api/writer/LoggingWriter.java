/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.logging.api.writer;

import ch.post.it.evoting.logging.api.domain.Level;
import ch.post.it.evoting.logging.api.domain.LogContent;

/**
 * Controls the levels logs and the process of log message construction
 */
public interface LoggingWriter {

	/**
	 * Given an log level and a POJO that contains all the log information, creates and writes a log in a file, database or anything else depending on
	 * the chosen implementation.
	 *
	 * @param level
	 * @param logContent
	 */
	void log(Level level, LogContent logContent);

}
