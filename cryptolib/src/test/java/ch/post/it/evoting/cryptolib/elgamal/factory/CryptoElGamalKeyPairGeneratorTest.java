/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.VerifiableElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.configuration.ElGamalPolicy;
import ch.post.it.evoting.cryptolib.elgamal.configuration.ElGamalPolicyFromProperties;
import ch.post.it.evoting.cryptolib.elgamal.utils.ElGamalTestDataGenerator;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

class CryptoElGamalKeyPairGeneratorTest {

	private static BigInteger p;
	private static BigInteger q;
	private static BigInteger g;
	private static ZpSubgroup group;
	private static int numKeys;
	private static CryptoElGamalKeyPairGenerator cryptoElGamalKeyPairGenerator;
	private static ElGamalKeyPair generatedKeys;
	private static ElGamalKeyPair generatedKeyWithVerifiableParams;
	private static VerifiableElGamalEncryptionParameters verifiableEncParams;
	private static ZpSubgroup verifiableParamsGroup;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException, IOException {

		loadConfiguration();

		ElGamalPolicy cryptoElGamalPolicy = new ElGamalPolicyFromProperties();

		group = new ZpSubgroup(g, p, q);

		ElGamalFactory elGamalFactory = new ElGamalFactory(cryptoElGamalPolicy);

		cryptoElGamalKeyPairGenerator = elGamalFactory.createCryptoElGamalKeyPairGenerator();

		ElGamalEncryptionParameters elGamalEncryptionParameters = new ElGamalEncryptionParameters(p, q, g);
		generatedKeys = cryptoElGamalKeyPairGenerator.generateKeys(elGamalEncryptionParameters, numKeys);

		verifiableEncParams = ElGamalTestDataGenerator.getElGamalVerifiableEncryptionParameters();

		verifiableParamsGroup = (ZpSubgroup) verifiableEncParams.getGroup();

		generatedKeyWithVerifiableParams = cryptoElGamalKeyPairGenerator.generateKeys(verifiableEncParams, numKeys);
	}

	private static void loadConfiguration() throws GeneralCryptoLibException {

		p = new BigInteger("23");
		q = new BigInteger("11");
		g = new BigInteger("2");

		group = new ZpSubgroup(g, p, q);

		numKeys = 15;
	}

	@Test
	void givenZeroKeysRequestedWhenGenerateKeysThenException() throws GeneralCryptoLibException {
		ElGamalEncryptionParameters encParams = new ElGamalEncryptionParameters(p, q, g);

		assertThrows(GeneralCryptoLibException.class, () -> cryptoElGamalKeyPairGenerator.generateKeys(encParams, 0));
	}

	@Test
	void givenZeroKeysRequestedWhenGenerateKeysThenExceptionWithVerifiableParams() {
		assertThrows(GeneralCryptoLibException.class, () -> cryptoElGamalKeyPairGenerator.generateKeys(verifiableEncParams, 0));
	}

	@Test
	void testThatGeneratedKeysSizesAreTheExpectedValues() {
		int numGeneratedPrivateKeys = generatedKeys.getPrivateKeys().getKeys().size();
		int numGeneratedPublicKeys = generatedKeys.getPublicKeys().getKeys().size();
		String errorMsgWrongSize = "The generated keys are not the expected size";

		// Test that the sizes of the generated keys are the expected size
		assertEquals(numKeys, numGeneratedPrivateKeys, errorMsgWrongSize);
		assertEquals(numKeys, numGeneratedPublicKeys, errorMsgWrongSize);
	}

	@Test
	void testThatGeneratedKeysSizesAreTheExpectedValuesWithVerifiableParams() {
		int numGeneratedPrivateKeys = generatedKeyWithVerifiableParams.getPrivateKeys().getKeys().size();
		int numGeneratedPublicKeys = generatedKeyWithVerifiableParams.getPublicKeys().getKeys().size();
		String errorMsgWrongSize = "The generated keys are not the expected size";

		// Test that the sizes of the generated keys are the expected size
		assertEquals(numKeys, numGeneratedPrivateKeys, errorMsgWrongSize);
		assertEquals(numKeys, numGeneratedPublicKeys, errorMsgWrongSize);
	}

	@Test
	void testThatAllGenerateKeysAreMembersOfTheGroup() {
		String errorMsgNotMembersOfGroup = "The generated keys are not all members of the group";

		// Test that all of the public keys are members of the group
		for (int i = 0; i < generatedKeys.getPublicKeys().getKeys().size(); i++) {
			assertTrue(group.isGroupMember(generatedKeys.getPublicKeys().getKeys().get(i)), errorMsgNotMembersOfGroup);
		}
	}

	@Test
	void testThatAllGenerateKeysAreMembersOfTheGroupWithVerifiableParams() {
		String errorMsgNotMembersOfGroup = "The generated keys are not all members of the group";

		// Test that all of the public keys are members of the group
		for (int i = 0; i < generatedKeyWithVerifiableParams.getPublicKeys().getKeys().size(); i++) {
			assertTrue(group.isGroupMember(generatedKeys.getPublicKeys().getKeys().get(i)), errorMsgNotMembersOfGroup);
		}
	}

	@Test
	void testVerifyThatKeysAreMathematicallyRelated() throws GeneralCryptoLibException {
		ZpGroupElement gRaisedToPrivateKey;
		Exponent privateKey;
		ZpGroupElement publicKey;

		String errorMsgKeysNotMathematicallyRelated = "The generated keys are not mathematically related ";

		for (int i = 0; i < numKeys; i++) {

			publicKey = generatedKeys.getPublicKeys().getKeys().get(i);
			privateKey = generatedKeys.getPrivateKeys().getKeys().get(i);
			gRaisedToPrivateKey = group.getGenerator().exponentiate(privateKey);

			assertEquals(publicKey, gRaisedToPrivateKey, errorMsgKeysNotMathematicallyRelated);
		}
	}

	@Test
	void testVerifyThatKeysAreMathematicallyRelatedWithVerifiableParams() throws GeneralCryptoLibException {
		ZpGroupElement gRaisedToPrivateKey;
		Exponent privateKey;
		ZpGroupElement publicKey;

		String errorMsgKeysNotMathematicallyRelated = "The generated keys are not mathematically related ";

		for (int i = 0; i < numKeys; i++) {

			publicKey = generatedKeyWithVerifiableParams.getPublicKeys().getKeys().get(i);
			privateKey = generatedKeyWithVerifiableParams.getPrivateKeys().getKeys().get(i);
			gRaisedToPrivateKey = verifiableParamsGroup.getGenerator().exponentiate(privateKey);

			assertEquals(publicKey, gRaisedToPrivateKey, errorMsgKeysNotMathematicallyRelated);
		}
	}

}
