/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election;

public class VerificationCardPublicKeyAndSignature {

	private String publicKey;

	private String signature;

	public String getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(final String publicKey) {
		this.publicKey = publicKey;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

}
