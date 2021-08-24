/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.securelogger.events;

import java.util.Base64;
import java.util.SortedMap;

import org.apache.logging.log4j.core.LogEvent;

public class AbstractCheckpointSecureLogEvent extends AbstractSecureLogEvent {
	protected static final Base64.Encoder ENCODER = Base64.getEncoder();

	protected AbstractCheckpointSecureLogEvent(LogEvent delegate, SortedMap<SecureLogProperty, String> properties) {
		super(delegate, properties);
	}

	public abstract static class Builder<B extends Builder<B>> {
		protected LogEvent delegate;
		protected String encryptedHmacKey;
		protected String lines;
		protected String hmac;
		protected String signature;

		B withDelegate(LogEvent delegate) {
			this.delegate = delegate;
			return asBuilder();
		}

		B withEncryptedHmacKey(byte[] encryptedHmacKey) {
			this.encryptedHmacKey = ENCODER.encodeToString(encryptedHmacKey);
			return asBuilder();
		}

		B withLines(int lines) {
			this.lines = Integer.toString(lines);
			return asBuilder();
		}

		B withHmac(byte[] hmac) {
			this.hmac = ENCODER.encodeToString(hmac);
			return asBuilder();
		}

		B withSignature(byte[] signature) {
			this.signature = ENCODER.encodeToString(signature);
			return asBuilder();
		}

		public B asBuilder() {
			return (B) this;
		}

	}
}
