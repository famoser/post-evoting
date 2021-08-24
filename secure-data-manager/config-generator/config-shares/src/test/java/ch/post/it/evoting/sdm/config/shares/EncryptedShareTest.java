/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

package ch.post.it.evoting.sdm.config.shares;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.secretsharing.Share;
import ch.post.it.evoting.sdm.config.shares.exception.SharesException;

class EncryptedShareTest extends AbstractWithSharesTest {

	private EncryptedShare encryptedShare;

	@BeforeEach
	void init() {
		encryptedShare = new EncryptedShare(shares.iterator().next(), keyPair.getPrivate());
	}

	@Test
	void testEncryptDecrypt() throws SharesException {
		final Set<WrittenShare> writtenShares = new HashSet<>();

		for (Share share : shares) {
			EncryptedShare encrypting = new EncryptedShare(share, keyPair.getPrivate());
			writtenShares
					.add(new WrittenShare(encrypting.getSecretKeyBytes(), encrypting.getEncryptedShare(), encrypting.getEncryptedShareSignature()));

			encrypting.destroy();
			assertEmpty(encrypting.getSecretKeyBytes());
			assertEmpty(encrypting.getEncryptedShare());
			assertEmpty(encrypting.getEncryptedShareSignature());
		}

		for (WrittenShare ws : writtenShares) {
			EncryptedShare decrypting = new EncryptedShare(ws.getEncryptedShare(), ws.getEncryptedShareSignature(), keyPair.getPublic());
			Share decrypted = decrypting.decrypt(ws.getSecretKeyBytes());

			assertTrue(shares.contains(decrypted));
		}
	}

	@Test
	void testShareOfDifferentBoard() {

		final SharesException sharesException = assertThrows(SharesException.class,
				() -> new EncryptedShare(encryptedShare.getEncryptedShare(), encryptedShare.getEncryptedShareSignature(),
						asymmetricService.getKeyPairForEncryption().getPublic()));

		assertEquals("This share does not belong to this board", sharesException.getMessage());
	}

	@Test
	void testShareWithIllegalKey() throws NoSuchAlgorithmException {
		// Generate another keypair with different key size. 3072 as defined in cryptolibPolicy.properties in this resources.
		final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(3072);

		final SharesException sharesException = assertThrows(SharesException.class,
				() -> new EncryptedShare(encryptedShare.getEncryptedShare(), encryptedShare.getEncryptedShareSignature(),
						keyPairGenerator.generateKeyPair().getPublic()));

		assertAll(() -> assertTrue(sharesException.getMessage().contains(
				"Byte length of signature verification public key must be equal to byte length of corresponding key in cryptographic policy for asymmetric service")),
				() -> assertTrue(sharesException.getCause() instanceof GeneralCryptoLibException));
	}

	@Test
	void testDecryptWithNullKey() throws SharesException {
		assertNull(encryptedShare.decrypt(null));
	}

}
