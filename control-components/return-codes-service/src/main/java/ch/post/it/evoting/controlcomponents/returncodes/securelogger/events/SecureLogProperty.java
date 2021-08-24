/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.securelogger.events;

public enum SecureLogProperty {
	PREVIOUS_KEY("LSK"),
	ENCRYPTED_KEY("ESK"),
	PREVIOUS_HMAC("PHMAC"),
	LINES("LS"),
	TIMESTAMP("TS"),
	HMAC("HMAC"),
	COUNTER("LC"),
	SIGNATURE("SG");

	public final String label;

	SecureLogProperty(String label) {
		this.label = label;
	}
}
