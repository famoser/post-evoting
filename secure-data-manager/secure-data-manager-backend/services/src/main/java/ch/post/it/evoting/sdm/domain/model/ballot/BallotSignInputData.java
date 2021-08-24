/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.ballot;

public class BallotSignInputData {

	private String privateKeyPEM;

	public String getPrivateKeyPEM() {
		return privateKeyPEM;
	}

	public void setPrivateKeyPEM(String privateKeyPEM) {
		this.privateKeyPEM = privateKeyPEM;
	}
}
