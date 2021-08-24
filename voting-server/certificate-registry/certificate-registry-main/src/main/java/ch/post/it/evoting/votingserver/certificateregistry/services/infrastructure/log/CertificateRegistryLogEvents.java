/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.certificateregistry.services.infrastructure.log;

import ch.post.it.evoting.logging.api.domain.LogEvent;

/**
 * Enum with the log events for this context.
 */
public enum CertificateRegistryLogEvents implements LogEvent {

	CERTIFICATE_SAVED("CRSTOR", "001", "Certificate validated and saved"),
	CERTIFICATE_VALIDATION_FAILED("CRVAL", "002", "Certificate failed validation"),
	ERROR_SAVING_CERTIFICATE("CRSTOR", "003", "Error saving Certificate");

	private final String layer;

	private final String action;

	private final String outcome;

	private final String info;

	CertificateRegistryLogEvents(final String action, final String outcome, final String info) {
		layer = "";
		this.action = action;
		this.outcome = outcome;
		this.info = info;
	}

	/**
	 * @see CertificateRegistryLogEvents#getAction()
	 */
	@Override
	public String getAction() {
		return action;
	}

	/**
	 * @see CertificateRegistryLogEvents#getOutcome()
	 */
	@Override
	public String getOutcome() {
		return outcome;
	}

	/**
	 * @see CertificateRegistryLogEvents#getInfo()
	 */
	@Override
	public String getInfo() {
		return info;
	}

	/**
	 * @see CertificateRegistryLogEvents#getLayer()
	 */
	@Override
	public String getLayer() {
		return layer;
	}
}
