/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.logging.api.formatter;

import ch.post.it.evoting.logging.api.domain.LogContent;
import ch.post.it.evoting.logging.api.writer.LoggingWriter;

/**
 * Specifies the log message format, it will be used by {@link LoggingWriter}.
 */
public interface MessageFormatter {

	/**
	 * Given a {@link LogContent} that contains all log information, this method builds a specific message.
	 *
	 * @param logContent
	 * @return String log message
	 */
	String buildMessage(LogContent logContent);

}
