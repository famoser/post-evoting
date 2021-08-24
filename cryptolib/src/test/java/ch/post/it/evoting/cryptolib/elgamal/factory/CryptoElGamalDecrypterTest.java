/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.configuration.ElGamalPolicyFromProperties;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

class CryptoElGamalDecrypterTest {

	private static ZpSubgroup group_g2_q11;
	private static ElGamalFactory cryptoElGamalFactory;
	private static CryptoElGamalDecrypter decrypter;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException {

		BigInteger p = new BigInteger("23");
		BigInteger q = new BigInteger("11");
		BigInteger g = new BigInteger("2");

		group_g2_q11 = new ZpSubgroup(g, p, q);

		int numKeys = 2;

		cryptoElGamalFactory = new ElGamalFactory(new ElGamalPolicyFromProperties());

		ElGamalEncryptionParameters elGamalEncryptionParameters = new ElGamalEncryptionParameters(p, q, g);
		ElGamalKeyPair cryptoElGamalKeyPair = cryptoElGamalFactory.createCryptoElGamalKeyPairGenerator()
				.generateKeys(elGamalEncryptionParameters, numKeys);

		ElGamalPrivateKey privKey = cryptoElGamalKeyPair.getPrivateKeys();

		decrypter = cryptoElGamalFactory.createDecrypter(privKey);
	}

	@Test
	void testGammaNotGroupMember() throws GeneralCryptoLibException {
		ZpGroupElement gamma = new ZpGroupElement(new BigInteger("7"), group_g2_q11);

		List<ZpGroupElement> phis = new ArrayList<>();

		phis.add(new ZpGroupElement(BigInteger.ONE, group_g2_q11));
		phis.add(new ZpGroupElement(new BigInteger("2"), group_g2_q11));

		ElGamalCiphertext ciphertext = new ElGamalCiphertext(gamma, phis);

		assertThrows(GeneralCryptoLibException.class, () -> decrypter.decrypt(ciphertext, true));
	}

	@Test
	void testPhiNotGroupMember() throws GeneralCryptoLibException {
		ZpGroupElement gamma = new ZpGroupElement(new BigInteger("7"), group_g2_q11);

		List<ZpGroupElement> phis = new ArrayList<>();

		phis.add(gamma);
		phis.add(new ZpGroupElement(new BigInteger("6"), group_g2_q11));

		ElGamalCiphertext ciphertext = new ElGamalCiphertext(gamma, phis);

		assertThrows(GeneralCryptoLibException.class, () -> decrypter.decrypt(ciphertext, true));
	}

	@Test
	void givenAValidInputWhenDecryptThenSucceed() throws GeneralCryptoLibException {
		ElGamalCiphertext constructedEncryptionValues = buildEncryptionValues();

		List<Exponent> constructedPrivateKeys = buildKeys();

		BigInteger plaintextPart1 = new BigInteger("6");
		BigInteger plaintestPart2 = new BigInteger("12");

		CryptoElGamalDecrypter decrypterWithConstructedKeys = cryptoElGamalFactory
				.createDecrypter(new ElGamalPrivateKey(constructedPrivateKeys, group_g2_q11));

		List<ZpGroupElement> decryptedMessages = decrypterWithConstructedKeys.decrypt(constructedEncryptionValues, true);

		String errorMessage = "The decrypted message does not match with the original message";
		assertEquals(plaintextPart1, decryptedMessages.get(0).getValue(), errorMessage);
		assertEquals(plaintestPart2, decryptedMessages.get(1).getValue(), errorMessage);
	}

	private ElGamalCiphertext buildEncryptionValues() throws GeneralCryptoLibException {

		List<ZpGroupElement> phis = new ArrayList<>();

		ZpGroupElement gamma = new ZpGroupElement(new BigInteger("4"), group_g2_q11);
		ZpGroupElement phi1 = new ZpGroupElement(new BigInteger("9"), group_g2_q11);
		ZpGroupElement phi2 = new ZpGroupElement(new BigInteger("16"), group_g2_q11);
		phis.add(phi1);
		phis.add(phi2);

		return new ElGamalCiphertext(gamma, phis);
	}

	private List<Exponent> buildKeys() throws GeneralCryptoLibException {

		List<Exponent> privKeys = new ArrayList<>();
		privKeys.add(new Exponent(group_g2_q11.getQ(), new BigInteger("9")));
		privKeys.add(new Exponent(group_g2_q11.getQ(), new BigInteger("8")));

		return privKeys;
	}
}
