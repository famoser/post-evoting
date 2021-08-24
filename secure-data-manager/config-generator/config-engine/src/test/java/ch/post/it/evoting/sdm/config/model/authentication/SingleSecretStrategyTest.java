/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.model.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.model.authentication.service.SingleSecretAuthenticationKeyGenerator;

class SingleSecretStrategyTest {

	private final AuthenticationKeyGenerator singleSecretStrategy = new SingleSecretAuthenticationKeyGenerator();
	StartVotingKey ignored = null;
	Set<String> randomStrings = new HashSet<>();

	@Test
	void generateNotNull() {

		AuthenticationKey authenticationKey = singleSecretStrategy.generateAuthKey(ignored);
		assertNotNull(authenticationKey);

	}

	@Test
	void generatesRandomString() {
		for (int i = 0; i < 1000; i++) {
			AuthenticationKey authenticationKey = singleSecretStrategy.generateAuthKey(ignored);
			String authKeyValue = authenticationKey.getValue();
			boolean added = randomStrings.add(authKeyValue);
			// make sure each time is a different string
			assertTrue(added);
		}
	}

	@Test
	void havePresentSecret() {

		AuthenticationKey authenticationKey = singleSecretStrategy.generateAuthKey(ignored);
		Optional<List<String>> optionalSecrets = authenticationKey.getSecrets();
		assertTrue(optionalSecrets.isPresent());
	}

	@Test
	void haveSingleSecret() {

		AuthenticationKey authenticationKey = singleSecretStrategy.generateAuthKey(ignored);
		Optional<List<String>> optionalSecrets = authenticationKey.getSecrets();
		List<String> secrets = optionalSecrets.get();
		assertEquals(1, secrets.size());
	}

	@Test
	void generatesRandomStringWithCorrectSize() {
		AuthenticationKey authenticationKey = singleSecretStrategy.generateAuthKey(ignored);
		String authKeyValue = authenticationKey.getValue();
		assertEquals(Constants.SVK_LENGTH, authKeyValue.length());

	}
}
