/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.administrationauthority;

/**
 * Bean holding the serialized share.
 */
public class ReadShareOutputData {

	private String serializedShare;

	public String getSerializedShare() {
		return serializedShare;
	}

	public void setSerializedShare(String serializedShare) {
		this.serializedShare = serializedShare;
	}
}
