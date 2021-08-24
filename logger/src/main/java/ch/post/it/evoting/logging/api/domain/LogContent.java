/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.logging.api.domain;

import java.util.HashMap;
import java.util.Map;

public class LogContent {

	private final LogEvent logEvent;

	private final String objectType;

	private final String objectId;

	private final String user;

	private final String electionEvent;

	private final Map<String, String> additionalInfo;

	private LogContent(final LogContentBuilder builder) {
		logEvent = builder.logEvent;
		objectType = builder.objectType;
		objectId = builder.objectId;
		additionalInfo = builder.additionalInfo;
		user = builder.user;
		electionEvent = builder.electionEvent;
	}

	public LogEvent getLogEvent() {
		return logEvent;
	}

	public String getObjectType() {
		return objectType;
	}

	public String getObjectId() {
		return objectId;
	}

	public Map<String, String> getAdditionalInfo() {
		return additionalInfo;
	}

	public String getUser() {
		return user;
	}

	public String getElectionEvent() {
		return electionEvent;
	}

	public static class LogContentBuilder {

		private final Map<String, String> additionalInfo;

		private LogEvent logEvent;

		private String objectType;

		private String objectId;

		private String user;

		private String electionEvent;

		public LogContentBuilder() {
			additionalInfo = new HashMap<String, String>();
		}

		public LogContentBuilder logEvent(final LogEvent logEvent) {
			this.logEvent = logEvent;
			return this;
		}

		public LogContentBuilder objectType(final String objectType) {
			this.objectType = objectType;
			return this;
		}

		public LogContentBuilder objectId(final String objectId) {
			this.objectId = objectId;
			return this;
		}

		public LogContentBuilder additionalInfo(final String key, final String value) {
			additionalInfo.put(key, value);
			return this;
		}

		public LogContentBuilder user(final String user) {
			this.user = user;
			return this;
		}

		public LogContentBuilder electionEvent(final String electionEvent) {
			this.electionEvent = electionEvent;
			return this;
		}

		public LogContent createLogInfo() {
			return new LogContent(this);
		}

	}
}
