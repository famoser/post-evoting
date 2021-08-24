/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election;

/**
 * Bean inside the BallotBox class, representing information about the election public key.
 */
public class ElectionPublicKey {

	private String id;

	private String publicKey;

	/**
	 * @return Returns the id.
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id The id to set.
	 */
	public void setId(final String id) {
		this.id = id;
	}

	/**
	 * @return Returns the publicKey.
	 */
	public String getPublicKey() {
		return publicKey;
	}

	/**
	 * @param publicKey The publicKey to set.
	 */
	public void setPublicKey(final String publicKey) {
		this.publicKey = publicKey;
	}

}
