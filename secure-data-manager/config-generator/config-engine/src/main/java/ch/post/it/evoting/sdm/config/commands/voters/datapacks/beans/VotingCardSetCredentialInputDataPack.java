/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans;

import ch.post.it.evoting.cryptolib.certificates.bean.CredentialProperties;
import ch.post.it.evoting.sdm.datapacks.beans.InputDataPack;

public class VotingCardSetCredentialInputDataPack extends InputDataPack {

	private final CredentialProperties credentialProperties;

	public VotingCardSetCredentialInputDataPack(final CredentialProperties credentialProperties) {
		super();
		this.credentialProperties = credentialProperties;
	}

	public CredentialProperties getCredentialProperties() {
		return credentialProperties;
	}
}
