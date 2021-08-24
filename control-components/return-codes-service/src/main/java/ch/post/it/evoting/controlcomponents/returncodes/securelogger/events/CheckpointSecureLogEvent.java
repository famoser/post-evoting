/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.securelogger.events;

import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.util.Strings;

public class CheckpointSecureLogEvent extends AbstractCheckpointSecureLogEvent {

	private CheckpointSecureLogEvent(LogEvent delegate, SortedMap<SecureLogProperty, String> properties) {
		super(delegate, properties);
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractCheckpointSecureLogEvent.Builder<Builder>
			implements org.apache.logging.log4j.core.util.Builder<CheckpointSecureLogEvent> {
		private String previousHmac;
		private String previousHmacKey;

		Builder withPreviousHmac(byte[] previousHmac) {
			this.previousHmac = ENCODER.encodeToString(previousHmac);
			return this;
		}

		Builder withPreviousHmacKey(byte[] previousHmacKey) {
			this.previousHmacKey = ENCODER.encodeToString(previousHmacKey);
			return this;
		}

		@Override
		public CheckpointSecureLogEvent build() {
			if (delegate == null || Strings.isBlank(previousHmac) || Strings.isBlank(hmac) || Strings.isBlank(signature) || Strings.isBlank(lines)
					|| Strings.isBlank(previousHmacKey) || Strings.isBlank(encryptedHmacKey)) {
				throw new IllegalStateException("missing properties");
			}

			TreeMap<SecureLogProperty, String> props = new TreeMap<>();
			props.put(SecureLogProperty.PREVIOUS_KEY, previousHmacKey);
			props.put(SecureLogProperty.ENCRYPTED_KEY, encryptedHmacKey);
			props.put(SecureLogProperty.PREVIOUS_HMAC, previousHmac);
			props.put(SecureLogProperty.LINES, lines);
			props.put(SecureLogProperty.TIMESTAMP, Long.toString(delegate.getInstant().getEpochMillisecond()));
			props.put(SecureLogProperty.HMAC, hmac);
			props.put(SecureLogProperty.SIGNATURE, signature);

			return new CheckpointSecureLogEvent(delegate, props);
		}

	}
}
