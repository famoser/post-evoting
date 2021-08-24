/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.securelogger.events;

import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.util.Strings;

public class FirstLineSecureLogEvent extends AbstractCheckpointSecureLogEvent {
	private FirstLineSecureLogEvent(LogEvent delegate, SortedMap<SecureLogProperty, String> properties) {
		super(delegate, properties);
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractCheckpointSecureLogEvent.Builder<Builder>
			implements org.apache.logging.log4j.core.util.Builder<FirstLineSecureLogEvent> {
		@Override
		public FirstLineSecureLogEvent build() {
			if (delegate == null || Strings.isBlank(hmac) || Strings.isBlank(signature) || Strings.isBlank(lines) || Strings
					.isBlank(encryptedHmacKey)) {
				throw new IllegalStateException("missing properties");
			}

			TreeMap<SecureLogProperty, String> props = new TreeMap<>();
			props.put(SecureLogProperty.ENCRYPTED_KEY, encryptedHmacKey);
			props.put(SecureLogProperty.LINES, lines);
			props.put(SecureLogProperty.TIMESTAMP, Long.toString(delegate.getInstant().getEpochMillisecond()));
			props.put(SecureLogProperty.HMAC, hmac);
			props.put(SecureLogProperty.SIGNATURE, signature);

			return new FirstLineSecureLogEvent(delegate, props);
		}
	}
}
