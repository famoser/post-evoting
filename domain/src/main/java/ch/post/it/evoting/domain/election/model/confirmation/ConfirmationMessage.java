/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.model.confirmation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a confirmation message (Confirmation Key + Signature)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfirmationMessage {

	// confirmation key calculated in the client side
	private String confirmationKey;

	// signature of the confirmation key
	private String signature;

	public String getConfirmationKey() {
		return confirmationKey;
	}

	public void setConfirmationKey(String confirmationKey) {
		this.confirmationKey = confirmationKey;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}
}
