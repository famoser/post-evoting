/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;

class PasswordEncrypterTest {

	private static final char[] PLAINTEXT_PASSWORD = "Q5H4B5WUIQWCR6J3UQS2KEVVZU".toCharArray();
	private static AsymmetricService asymmetricService;
	private static final PasswordEncrypter TARGET = new PasswordEncrypter(asymmetricService);

	@BeforeAll
	static void init() {
		asymmetricService = new AsymmetricService();
	}

	@Test
	void givenEmptyPrivateKeyWhenEncryptThenReturnOriginalPassword() throws GeneralCryptoLibException {

		String NO_KEY = "";

		String encryptedPassword = TARGET.encryptPasswordIfEncryptionKeyAvailable(PLAINTEXT_PASSWORD, NO_KEY);

		String errorMsg = "Encrypted password did not match original password";
		assertEquals(new String(PLAINTEXT_PASSWORD), encryptedPassword, errorMsg);
	}

}
