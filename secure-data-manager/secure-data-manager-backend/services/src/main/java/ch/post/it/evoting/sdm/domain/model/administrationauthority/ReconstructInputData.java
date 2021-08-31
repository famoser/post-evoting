/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.administrationauthority;

import java.util.List;

/**
 * Bean holding the list of serialized shares required to reconstruct a private key.
 */
public class ReconstructInputData {

	private List<String> serializedShares;

	private String serializedPublicKey;

	public List<String> getSerializedShares() {
		return serializedShares;
	}

	public void setSerializedShares(List<String> serializedShares) {
		this.serializedShares = serializedShares;
	}

	public String getSerializedPublicKey() {
		return serializedPublicKey;
	}

	public void setSerializedPublicKey(String serializedPublicKey) {
		this.serializedPublicKey = serializedPublicKey;
	}
}
