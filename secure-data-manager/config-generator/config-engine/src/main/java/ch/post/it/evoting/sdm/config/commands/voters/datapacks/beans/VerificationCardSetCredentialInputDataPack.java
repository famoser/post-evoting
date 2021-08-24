/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans;

import ch.post.it.evoting.cryptolib.certificates.bean.CredentialProperties;
import ch.post.it.evoting.sdm.datapacks.beans.InputDataPack;

public class VerificationCardSetCredentialInputDataPack extends InputDataPack {

	private final CredentialProperties verificationCardSetProperties;

	public VerificationCardSetCredentialInputDataPack(final CredentialProperties verificationCardSetProperties) {
		super();

		this.verificationCardSetProperties = verificationCardSetProperties;
	}

	public CredentialProperties getVerificationCardSetProperties() {
		return verificationCardSetProperties;
	}
}
