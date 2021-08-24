/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.beans.confirmation;

import ch.post.it.evoting.domain.election.model.confirmation.ConfirmationMessage;

/**
 * Represents the confirmation information sent by the client.
 */
public class ConfirmationInformation {

	private String credentialId;

	private ConfirmationMessage confirmationMessage;

	// certificate sent to verify signatures
	private String certificate;

	public String getCertificate() {
		return certificate;
	}

	public void setCertificate(final String certificate) {
		this.certificate = certificate;
	}

	public String getCredentialId() {
		return credentialId;
	}

	public void setCredentialId(final String credentialId) {
		this.credentialId = credentialId;
	}

	public ConfirmationMessage getConfirmationMessage() {
		return confirmationMessage;
	}

	public void setConfirmationMessage(final ConfirmationMessage confirmationMessage) {
		this.confirmationMessage = confirmationMessage;
	}
}
