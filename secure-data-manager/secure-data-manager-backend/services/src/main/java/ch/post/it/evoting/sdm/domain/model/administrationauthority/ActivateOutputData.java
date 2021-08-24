/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.administrationauthority;

public class ActivateOutputData {

	private String issuerPublicKeyPEM;

	private String serializedSubjectPublicKey;

	public String getIssuerPublicKeyPEM() {
		return issuerPublicKeyPEM;
	}

	public void setIssuerPublicKeyPEM(String issuerPublicKeyPEM) {
		this.issuerPublicKeyPEM = issuerPublicKeyPEM;
	}

	public String getSerializedSubjectPublicKey() {
		return serializedSubjectPublicKey;
	}

	public void setSerializedSubjectPublicKey(String serializedSubjectPublicKey) {
		this.serializedSubjectPublicKey = serializedSubjectPublicKey;
	}
}
