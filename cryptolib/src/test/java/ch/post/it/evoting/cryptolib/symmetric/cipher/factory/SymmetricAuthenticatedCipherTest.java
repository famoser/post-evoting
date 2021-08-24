/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.symmetric.cipher.factory;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.primitives.service.PrimitivesService;
import ch.post.it.evoting.cryptolib.symmetric.cipher.configuration.SymmetricCipherPolicy;
import ch.post.it.evoting.cryptolib.symmetric.cipher.configuration.SymmetricCipherPolicyFromProperties;
import ch.post.it.evoting.cryptolib.symmetric.service.SymmetricService;

class SymmetricAuthenticatedCipherTest {

	private static final int DATA_BYTE_LENGTH = 100;

	private static SecretKey key;

	private static byte[] data;

	private static SymmetricAuthenticatedCipher symmetricAuthenticatedCipher;

	private static SymmetricCipherPolicy symmetricCipherPolicy;

	@BeforeAll
	public static void setUp() {

		SymmetricService symmetricServiceFromDefaultConstructor = new SymmetricService();

		key = symmetricServiceFromDefaultConstructor.getSecretKeyForEncryption();

		PrimitivesService primitivesService = new PrimitivesService();

		data = primitivesService.genRandomBytes(DATA_BYTE_LENGTH);

		symmetricCipherPolicy = new SymmetricCipherPolicyFromProperties();
		symmetricAuthenticatedCipher = new SymmetricAuthenticatedCipherFactory(symmetricCipherPolicy).create();
	}

	@Test
	void testEncryptAndDecryptDataByteArray() throws GeneralCryptoLibException {

		byte[] encryptedData = symmetricAuthenticatedCipher.genAuthenticatedEncryption(key, data);

		byte[] initVector = symmetricAuthenticatedCipher.getInitVector(data);

		byte[] decryptedData = symmetricAuthenticatedCipher.getAuthenticatedDecryption(key, encryptedData);

		Assertions.assertArrayEquals(decryptedData, data);

		Assertions.assertEquals(initVector.length, symmetricCipherPolicy.getSymmetricCipherAlgorithmAndSpec().getInitVectorBitLength() / Byte.SIZE);
	}
}
