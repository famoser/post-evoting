/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election;

/**
 * Bean representing the public part of the Electoral Authority key.
 */
public class ElectoralAuthorityPublicKey {

	private String electoralAuthorityId;

	/**
	 * ElGamalPublicKey in its JSON representation encoded in BASE64
	 */
	private String publicKey;

	public String getElectoralAuthorityId() {
		return electoralAuthorityId;
	}

	public void setElectoralAuthorityId(String electoralAuthorityId) {
		this.electoralAuthorityId = electoralAuthorityId;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}

}
