/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.config;

/**
 * Enum representing the different status allowed for handling the smart cards
 */
public enum SmartCardConfig {

	FILE(false),
	SMART_CARD(true);

	private final boolean smartCardEnabled;

	SmartCardConfig(boolean smartCardEnabled) {
		this.smartCardEnabled = smartCardEnabled;
	}

	public boolean isSmartCardEnabled() {
		return smartCardEnabled;
	}
}
