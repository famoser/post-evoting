/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.model.authentication;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.SpringConfigTest;
import ch.post.it.evoting.sdm.config.model.authentication.service.AuthenticationKeyCryptoService;
import ch.post.it.evoting.sdm.config.model.authentication.service.StartVotingKeyService;

/**
 * Test class for the StartVotingKeyService
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SpringConfigTest.class, loader = AnnotationConfigContextLoader.class)
class StartVotingKeyServiceLongSecretsTest {

	private static final String ELECTION_EVENT_ID = "cfdee3c861e944a4bf505bd86e9c4b73";

	@Autowired
	@Qualifier("LONGSECRETS")
	private StartVotingKeyService startVotingKeyService;

	@Autowired
	@Qualifier("BEYONDLIMIT")
	private StartVotingKeyService startVotingKeyServiceBeyondLimit;

	@Autowired
	private AuthenticationKeyCryptoService authenticationKeyCryptoService;

	@Test
	void tryToDeriveBeyondLimit() {

		assertThrows(GeneralCryptoLibException.class, () -> startVotingKeyServiceBeyondLimit.generateStartVotingKey());
	}

	@Test
	void tryToDeriveUpToLimit() {

		final String svk = assertDoesNotThrow(() -> startVotingKeyService.generateStartVotingKey());

		final AuthenticationDerivedElement credentialID = authenticationKeyCryptoService
				.deriveElement(Constants.CREDENTIAL_ID, ELECTION_EVENT_ID, svk);
		final AuthenticationDerivedElement pin = authenticationKeyCryptoService.deriveElement(Constants.KEYSTORE_PIN, ELECTION_EVENT_ID, svk);

		assertAll(() -> assertNotNull(credentialID.getDerivedKeyInEx()), () -> assertNotNull(pin.getDerivedKeyInEx()));
	}

}
