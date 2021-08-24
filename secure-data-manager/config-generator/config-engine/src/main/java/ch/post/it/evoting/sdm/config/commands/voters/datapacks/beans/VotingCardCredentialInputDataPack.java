/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans;

import ch.post.it.evoting.cryptolib.certificates.bean.CredentialProperties;
import ch.post.it.evoting.sdm.datapacks.beans.InputDataPack;

public class VotingCardCredentialInputDataPack extends InputDataPack {

	private final CredentialProperties credentialSignProperties;

	private final CredentialProperties credentialAuthProperties;

	public VotingCardCredentialInputDataPack(final CredentialProperties credentialSignProperties,
			final CredentialProperties credentialAuthProperties) {
		super();
		this.credentialSignProperties = credentialSignProperties;
		this.credentialAuthProperties = credentialAuthProperties;
	}

	public CredentialProperties getCredentialSignProperties() {
		return credentialSignProperties;
	}

	public CredentialProperties getCredentialAuthProperties() {
		return credentialAuthProperties;
	}
}
