/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain;

import static ch.post.it.evoting.domain.Validations.validateUUID;

public class EncryptedExponentiationLog implements ReturnCodes {

	protected final String eventType;
	protected final ControlComponentContext context;
	protected final String verificationCardId;

	protected EncryptedExponentiationLog(String eventType, ControlComponentContext context, String verificationCardId) {
		this.eventType = eventType;
		this.context = context;
		validateUUID(verificationCardId);
		this.verificationCardId = verificationCardId;
	}

	public String getEventType() {
		return eventType;
	}

	public ControlComponentContext getContext() {
		return context;
	}

	public String getVerificationCardId() {
		return verificationCardId;
	}
}
