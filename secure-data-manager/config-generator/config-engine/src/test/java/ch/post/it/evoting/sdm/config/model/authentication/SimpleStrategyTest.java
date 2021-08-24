/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.model.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ch.post.it.evoting.sdm.config.model.authentication.service.SimpleAuthenticationKeyGenerator;

class SimpleStrategyTest {

	private final SimpleAuthenticationKeyGenerator simpleStrategy = new SimpleAuthenticationKeyGenerator();
	private final StartVotingKey startVotingKey = StartVotingKey.ofValue("68n7vr7znmrdmq2hkpj7");

	@Test
	void generateNotNull() {

		AuthenticationKey authenticationKey = simpleStrategy.generateAuthKey(startVotingKey);
		assertNotNull(authenticationKey);

	}

	@Test
	void haveValueEqualToSVK() {

		AuthenticationKey authenticationKey = simpleStrategy.generateAuthKey(startVotingKey);
		String authKeyValue = authenticationKey.getValue();
		assertEquals(startVotingKey.getValue(), authKeyValue);

	}

	@Test
	void havePresentSecret() {

		AuthenticationKey authenticationKey = simpleStrategy.generateAuthKey(startVotingKey);
		Optional<List<String>> optionalSecrets = authenticationKey.getSecrets();
		assertTrue(optionalSecrets.isPresent());
	}

	@Test
	void haveSingleSecret() {

		AuthenticationKey authenticationKey = simpleStrategy.generateAuthKey(startVotingKey);
		Optional<List<String>> optionalSecrets = authenticationKey.getSecrets();
		List<String> secrets = optionalSecrets.get();
		assertEquals(1, secrets.size());

	}

	@Test
	void haveSecretEqualToSVK() {

		AuthenticationKey authenticationKey = simpleStrategy.generateAuthKey(startVotingKey);
		Optional<List<String>> optionalSecrets = authenticationKey.getSecrets();
		List<String> secrets = optionalSecrets.get();
		assertEquals(startVotingKey.getValue(), secrets.get(0));

	}

}
