/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.logging.core.formatter;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import ch.post.it.evoting.logging.api.domain.LogContent;
import ch.post.it.evoting.logging.api.domain.LogEvent;
import ch.post.it.evoting.logging.api.domain.LogField;
import ch.post.it.evoting.logging.api.exceptions.LoggingException;
import ch.post.it.evoting.logging.api.formatter.MessageFormatter;

/**
 * Implementation of {@link MessageFormatter} that builds the log message respecting the splunk format and complying with the Common Criteria.
 */
public class PipeSeparatedFormatter implements MessageFormatter {

	private static final char SEPARATOR = '|';

	private static final Pattern pipeEscapedPattern = Pattern.compile("\\|");

	private static final String PIPE_REPLACEMENT = "(p)";

	private static final char ADDITIONAL_VALUES = '"';

	private static final char BLANK = ' ';

	private static final char EQUAL = '=';

	private static final char DEFAULT_VALUE = '-';

	private final String application;

	private final String component;

	private final Set<LogField> requiredFields = new HashSet<>();

	public PipeSeparatedFormatter(final String application, final String component) {
		this.application = application;
		this.component = component;

	}

	public PipeSeparatedFormatter(final String application, final String component,	final LogField... requiredFields) {
		this.application = application;
		this.component = component;
		if (requiredFields != null) {
			for (LogField field : requiredFields) {
				if (null != field) {
					this.requiredFields.add(field);
				}
			}
		}
	}

	/**
	 * @see MessageFormatter#buildMessage(LogContent)
	 */
	@Override
	public String buildMessage(final LogContent logContent) {

		final StringBuilder message = new StringBuilder();

		final LogEvent logEvent = logContent.getLogEvent();

		message.append(escapePipe(application));
		message.append(SEPARATOR);
		message.append(escapePipe(component));
		message.append(SEPARATOR);
		message.append(escapePipe(logEvent.getLayer()));
		message.append(SEPARATOR);
		message.append(escapePipe(logEvent.getAction()));
		message.append(SEPARATOR);

		appendOptionalField(LogField.OBJECTTYPE, escapePipe(logContent.getObjectType()), message);
		message.append(SEPARATOR);

		appendOptionalField(LogField.OBJECTID, escapePipe(logContent.getObjectId()), message);
		message.append(SEPARATOR);

		message.append(escapePipe(logEvent.getOutcome()));
		message.append(SEPARATOR);

		appendOptionalField(LogField.USER, escapePipe(logContent.getUser()), message);
		message.append(SEPARATOR);

		appendOptionalField(LogField.ELECTIONEVENT, escapePipe(logContent.getElectionEvent()), message);
		message.append(SEPARATOR);

		message.append(escapePipe(logEvent.getInfo()));
		message.append(SEPARATOR);

		final Map<String, String> additionalInfoMap = logContent.getAdditionalInfo();

		if (additionalInfoMap != null) {

			for (Entry<String, String> pair : additionalInfoMap.entrySet()) {

				message.append(escapePipe(pair.getKey())).append(EQUAL);
				message.append(ADDITIONAL_VALUES).append(escapePipe(pair.getValue())).append(ADDITIONAL_VALUES);
				message.append(BLANK);
			}
		}

		return message.toString();
	}

	private String escapePipe(String info) {
		if (info != null) {
			return pipeEscapedPattern.matcher(info).replaceAll(PIPE_REPLACEMENT);
		}
		return null;
	}

	private void appendOptionalField(LogField field, String fieldValue, StringBuilder sb) {
		if (requiredFields.contains(field) && fieldValue == null) {
			throw new LoggingException(String.format("Field '%s' is defined as required and is missing a value", field.getFieldName()));
		} else if (fieldValue == null) {
			sb.append(DEFAULT_VALUE);
		} else {
			sb.append(fieldValue);
		}
	}
}
