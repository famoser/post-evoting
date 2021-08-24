/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.mixing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The contents of the request body of the SDM back-end's mixing endpoint.
 */
public class MixingKeys {

	private final String administrationBoardPrivateKeyPEM;
	private final String electoralBoardPrivateKey;
	private final String adminBoardId;

	@JsonCreator
	public MixingKeys(
			@JsonProperty("privateKeyPEM")
					String administrationBoardPrivateKeyPEM,
			@JsonProperty("serializedPrivateKey")
					String electoralBoardPrivateKey,
			@JsonProperty("adminBoardId")
					String adminBoardId) {
		this.administrationBoardPrivateKeyPEM = administrationBoardPrivateKeyPEM;
		this.electoralBoardPrivateKey = electoralBoardPrivateKey;
		this.adminBoardId = adminBoardId;
	}

	public String getElectoralBoardPrivateKey() {
		return electoralBoardPrivateKey;
	}

	/**
	 * Returns the current value of the field privateKeyPEM.
	 *
	 * @return Returns the privateKeyPEM.
	 */
	public String getAdministrationBoardPrivateKeyPEM() {
		return administrationBoardPrivateKeyPEM;
	}

	public String getAdminBoardId() {
		return adminBoardId;
	}
}
