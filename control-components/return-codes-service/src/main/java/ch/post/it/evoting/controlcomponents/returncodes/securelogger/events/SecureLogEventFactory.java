/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.securelogger.events;

import org.apache.logging.log4j.core.LogEvent;

public class SecureLogEventFactory {
	private SecureLogEventFactory() {
		// Explicitly empty
	}

	public static RegularSecureLogEvent regularEvent(LogEvent originalEvent, byte[] hmac, int lineCounter) {
		return RegularSecureLogEvent.newBuilder().withDelegate(originalEvent).withHMAC(hmac).withLineCounter(lineCounter).build();
	}

	public static FirstLineSecureLogEvent firstLineEvent(LogEvent originalEvent, byte[] encryptedHmacKey, int lines, byte[] hmac, byte[] signature) {
		return FirstLineSecureLogEvent.newBuilder().withDelegate(originalEvent).withEncryptedHmacKey(encryptedHmacKey).withLines(lines).withHmac(hmac)
				.withSignature(signature).build();
	}

	public static CheckpointSecureLogEvent checkpointEvent(LogEvent originalEvent, byte[] previousHmac, byte[] previousHmacKey,
			byte[] encryptedHmacKey, int lines, byte[] hmac, byte[] signature) {
		return CheckpointSecureLogEvent.newBuilder().withPreviousHmac(previousHmac).withPreviousHmacKey(previousHmacKey).withDelegate(originalEvent)
				.withEncryptedHmacKey(encryptedHmacKey).withLines(lines).withHmac(hmac).withSignature(signature).build();
	}
}
