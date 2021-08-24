/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.factory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.configuration.ElGamalPolicy;
import ch.post.it.evoting.cryptolib.elgamal.configuration.ElGamalPolicyFromProperties;

class ElGamalFactoryTest {

	private static ElGamalFactory cryptoElGamalFactory;
	private static ElGamalKeyPair cryptoElGamalKeyPair;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException {

		BigInteger g = new BigInteger("2");
		BigInteger q = new BigInteger("11");
		BigInteger p = new BigInteger("23");

		int numKeys = 4;

		ElGamalPolicy cryptoElGamalPolicyImpl = new ElGamalPolicyFromProperties();

		cryptoElGamalFactory = new ElGamalFactory(cryptoElGamalPolicyImpl);

		ElGamalEncryptionParameters elGamalEncryptionParameters = new ElGamalEncryptionParameters(p, q, g);

		cryptoElGamalKeyPair = cryptoElGamalFactory.createCryptoElGamalKeyPairGenerator().generateKeys(elGamalEncryptionParameters, numKeys);
	}

	@Test
	void givenFactoryWhenCreateKeyPairGeneratorThenExpectedNumKeys() {
		CryptoElGamalKeyPairGenerator cryptoElGamalKeyPairGenerator = cryptoElGamalFactory.createCryptoElGamalKeyPairGenerator();

		assertNotNull(cryptoElGamalKeyPairGenerator);
	}

	@Test
	void givenFactoryWhenCreateEncrypterThenOk() {
		assertDoesNotThrow(() -> cryptoElGamalFactory.createEncrypter(cryptoElGamalKeyPair.getPublicKeys()));
	}

	@Test
	void givenFactoryWhenCreateDecrypterThenOk() {
		assertDoesNotThrow(() -> cryptoElGamalFactory.createDecrypter(cryptoElGamalKeyPair.getPrivateKeys()));
	}

}
