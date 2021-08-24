/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.model.authentication.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.model.authentication.AuthenticationKey;
import ch.post.it.evoting.sdm.config.model.authentication.AuthenticationKeyGenerator;
import ch.post.it.evoting.sdm.config.model.authentication.StartVotingKey;

/**
 * Pretty Basic strategy in which only the SVK will be used as the only secret.
 */
public class SimpleAuthenticationKeyGenerator implements AuthenticationKeyGenerator {

	@Override
	public AuthenticationKey generateAuthKey(StartVotingKey startVotingKey) {

		final Optional<List<String>> secrets = Optional.of(Arrays.asList(startVotingKey.getValue()));

		return AuthenticationKey.ofSecrets(startVotingKey.getValue(), secrets);
	}

	@Override
	public int getSecretsLength() {
		return Constants.SVK_LENGTH;
	}

}
