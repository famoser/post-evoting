/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A utility class to share a DateTimeFormatter with yyyyMMddHHmmssnnnnnnnnn format.
 */
public class TimestampGenerator {

	private static final DateTimeFormatter FORMATTING_PATTERN = DateTimeFormatter.ofPattern("yyyyMMddHHmmssnnnnnnnnn");

	public String getNewTimeStamp() {
		final LocalDateTime dateTime = LocalDateTime.now();
		return dateTime.format(FORMATTING_PATTERN);
	}

	/**
	 * @return Returns the dateFormat.
	 */
	public DateTimeFormatter getDateFormat() {
		return FORMATTING_PATTERN;
	}

}
