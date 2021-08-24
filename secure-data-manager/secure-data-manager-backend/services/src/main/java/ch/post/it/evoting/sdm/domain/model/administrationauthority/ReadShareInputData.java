/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.administrationauthority;

/**
 * Bean holding the information about the smart card pin
 */
public class ReadShareInputData {

	private String pin;

	private String publicKeyPEM;

	public String getPin() {
		return pin;
	}

	public void setPin(String pin) {
		this.pin = pin;
	}

	public String getPublicKeyPEM() {
		return publicKeyPEM;
	}

	public void setPublicKeyPEM(String publicKeyPEM) {
		this.publicKeyPEM = publicKeyPEM;
	}
}
