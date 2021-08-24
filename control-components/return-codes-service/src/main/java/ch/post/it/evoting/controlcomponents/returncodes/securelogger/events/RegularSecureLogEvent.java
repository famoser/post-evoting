/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.securelogger.events;

import java.util.AbstractMap;
import java.util.Base64;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.util.Strings;

public class RegularSecureLogEvent extends AbstractSecureLogEvent {
	private static final Base64.Encoder encoder = Base64.getEncoder();

	private RegularSecureLogEvent(LogEvent delegate, SortedMap<SecureLogProperty, String> properties) {
		super(delegate, properties);
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder implements org.apache.logging.log4j.core.util.Builder<RegularSecureLogEvent> {
		private LogEvent delegate;
		private String hmac;
		private String lineCounter;

		Builder withDelegate(LogEvent delegate) {
			this.delegate = delegate;
			return this;
		}

		Builder withHMAC(byte[] hmac) {
			this.hmac = encoder.encodeToString(hmac);
			return this;
		}

		Builder withLineCounter(int lineCounter) {
			this.lineCounter = Integer.toString(lineCounter);
			return this;
		}

		@Override
		public RegularSecureLogEvent build() {
			if (delegate == null || Strings.isBlank(hmac) || Strings.isBlank(lineCounter)) {
				throw new IllegalStateException("missing properties");
			}

			final Map<SecureLogProperty, String> properties = Stream.of(new AbstractMap.SimpleEntry<>(SecureLogProperty.HMAC, hmac),
					new AbstractMap.SimpleEntry<>(SecureLogProperty.TIMESTAMP, Long.toString(delegate.getInstant().getEpochMillisecond())),
					new AbstractMap.SimpleEntry<>(SecureLogProperty.COUNTER, lineCounter))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

			return new RegularSecureLogEvent(delegate, new TreeMap<>(properties));
		}
	}

}
