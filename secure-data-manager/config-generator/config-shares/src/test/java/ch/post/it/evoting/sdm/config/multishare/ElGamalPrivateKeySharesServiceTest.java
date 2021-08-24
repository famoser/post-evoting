/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.multishare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigInteger;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;
import ch.post.it.evoting.sdm.config.shares.exception.SharesException;
import ch.post.it.evoting.sdm.config.shares.keys.elgamal.ElGamalKeyPairGenerator;
import ch.post.it.evoting.sdm.config.shares.keys.elgamal.ElGamalPrivateKeyAdapter;
import ch.post.it.evoting.sdm.config.shares.keys.elgamal.ElGamalPublicKeyAdapter;

class ElGamalPrivateKeySharesServiceTest {

	private static ElGamalPrivateKeySharesService target;
	private static ElGamalServiceAPI elgamalServiceAPI;
	private static BigInteger p;
	private static BigInteger q;
	private static BigInteger g;

	@BeforeAll
	public static void init() {

		target = new ElGamalPrivateKeySharesService();

		elgamalServiceAPI = new ElGamalService();

		p = new BigInteger(
				"16370518994319586760319791526293535327576438646782139419846004180837103527129035954742043590609421369665944746587885814920851694546456891767644945459124422553763416586515339978014154452159687109161090635367600349264934924141746082060353483306855352192358732451955232000593777554431798981574529854314651092086488426390776811367125009551346089319315111509277347117467107914073639456805159094562593954195960531136052208019343392906816001017488051366518122404819967204601427304267380238263913892658950281593755894747339126531018026798982785331079065126375455293409065540731646939808640273393855256230820509217411510058759");
		q = new BigInteger(
				"8185259497159793380159895763146767663788219323391069709923002090418551763564517977371021795304710684832972373293942907460425847273228445883822472729562211276881708293257669989007077226079843554580545317683800174632467462070873041030176741653427676096179366225977616000296888777215899490787264927157325546043244213195388405683562504775673044659657555754638673558733553957036819728402579547281296977097980265568026104009671696453408000508744025683259061202409983602300713652133690119131956946329475140796877947373669563265509013399491392665539532563187727646704532770365823469904320136696927628115410254608705755029379");
		g = new BigInteger("2");
	}

	static Stream<Arguments> splitAndRecoverArgumentProvider() {
		return Stream.of(arguments(2, 2, 2), arguments(5, 2, 1), arguments(3, 5, 5));
	}

	@ParameterizedTest
	@MethodSource("splitAndRecoverArgumentProvider")
	void whenSplitAndRecoverKeyThenOk(final int subkeyCount, final int numberShares, final int threshold) throws Exception {
		ElGamalEncryptionParameters elGamalEncryptionParameters = new ElGamalEncryptionParameters(p, q, g);

		ElGamalKeyPairGenerator elGamalKeyPairGenerator = new ElGamalKeyPairGenerator(elGamalEncryptionParameters, subkeyCount, elgamalServiceAPI);

		KeyPair keyPair = elGamalKeyPairGenerator.generate();
		ElGamalPrivateKey elGamalPrivateKey = getElGamalPrivateKey(keyPair);
		ElGamalPublicKey elGamalPublicKey = getElGamalPublicKey(keyPair);

		List<MultipleSharesContainer> containers = target.split(elGamalPrivateKey, numberShares, threshold);
		assertEquals(numberShares, containers.size());

		ElGamalPrivateKey recovered = target.recover(containers, elGamalPublicKey);
		assertEquals(recovered, elGamalPrivateKey);
	}

	@Test
	void whenSplitAndRecoverKeyThenOk_numSubKeys10_numShares6_threshold4() throws GeneralCryptoLibException, KeyException, SharesException {

		int subkeyCount = 10;
		int numberShares = 6;
		int threshold = 4;

		ElGamalEncryptionParameters elGamalEncryptionParameters = new ElGamalEncryptionParameters(p, q, g);

		ElGamalKeyPairGenerator elGamalKeyPairGenerator = new ElGamalKeyPairGenerator(elGamalEncryptionParameters, subkeyCount, elgamalServiceAPI);

		KeyPair keyPair = elGamalKeyPairGenerator.generate();
		ElGamalPrivateKey elGamalPrivateKey = getElGamalPrivateKey(keyPair);
		ElGamalPublicKey elGamalPublicKey = getElGamalPublicKey(keyPair);

		List<MultipleSharesContainer> containers = target.split(elGamalPrivateKey, numberShares, threshold);
		assertEquals(numberShares, containers.size());

		containers.remove(0);

		ElGamalPrivateKey recovered = target.recover(containers, elGamalPublicKey);
		assertEquals(recovered, elGamalPrivateKey);
	}

	@Test
	void whenChangeOrderAndRecoverKeyThenOk_numSubKeys10_numShares6_threshold4() throws GeneralCryptoLibException, KeyException, SharesException {

		int subkeyCount = 10;
		int numberShares = 6;
		int threshold = 4;

		ElGamalEncryptionParameters elGamalEncryptionParameters = new ElGamalEncryptionParameters(p, q, g);

		ElGamalKeyPairGenerator elGamalKeyPairGenerator = new ElGamalKeyPairGenerator(elGamalEncryptionParameters, subkeyCount, elgamalServiceAPI);

		KeyPair keyPair = elGamalKeyPairGenerator.generate();
		ElGamalPrivateKey elGamalPrivateKey = getElGamalPrivateKey(keyPair);
		ElGamalPublicKey elGamalPublicKey = getElGamalPublicKey(keyPair);

		List<MultipleSharesContainer> containers = target.split(elGamalPrivateKey, numberShares, threshold);
		assertEquals(numberShares, containers.size());

		Collections.reverse(containers);

		ElGamalPrivateKey recovered = target.recover(containers, elGamalPublicKey);
		assertEquals(recovered, elGamalPrivateKey);
	}

	@Test
	void whenThresholdNotMetThenException_numSubKeys3_numShares5_threshold5() throws GeneralCryptoLibException, KeyException, SharesException {

		int subkeyCount = 3;
		int numberShares = 5;
		int threshold = 5;

		ElGamalEncryptionParameters elGamalEncryptionParameters = new ElGamalEncryptionParameters(p, q, g);

		ElGamalKeyPairGenerator elGamalKeyPairGenerator = new ElGamalKeyPairGenerator(elGamalEncryptionParameters, subkeyCount, elgamalServiceAPI);

		KeyPair keyPair = elGamalKeyPairGenerator.generate();
		ElGamalPrivateKey elGamalPrivateKey = getElGamalPrivateKey(keyPair);
		ElGamalPublicKey elGamalPublicKey = getElGamalPublicKey(keyPair);

		List<MultipleSharesContainer> containers = target.split(elGamalPrivateKey, numberShares, threshold);
		assertEquals(numberShares, containers.size());

		containers.remove(4);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> target.recover(containers, elGamalPublicKey));
		assertEquals("Underflow: given number of shares is lower than threshold", exception.getMessage());
	}

	@Test
	void whenSplitAndRecoverKeyWithExactlyThresholdThenOk_numSubKeys3_numShares5_threshold3()
			throws GeneralCryptoLibException, KeyException, SharesException {

		int subkeyCount = 3;
		int numberShares = 5;
		int threshold = 3;

		ElGamalEncryptionParameters elGamalEncryptionParameters = new ElGamalEncryptionParameters(p, q, g);

		ElGamalKeyPairGenerator elGamalKeyPairGenerator = new ElGamalKeyPairGenerator(elGamalEncryptionParameters, subkeyCount, elgamalServiceAPI);

		KeyPair keyPair = elGamalKeyPairGenerator.generate();
		ElGamalPrivateKey elGamalPrivateKey = getElGamalPrivateKey(keyPair);
		ElGamalPublicKey elGamalPublicKey = getElGamalPublicKey(keyPair);

		List<MultipleSharesContainer> containers = target.split(elGamalPrivateKey, numberShares, threshold);
		assertEquals(numberShares, containers.size());

		// remove two containers to represents some cards not being present at the
		// moment of recovering the key (but the threshold number of keys being available)
		containers.remove(4);
		containers.remove(3);
		assertEquals(threshold, containers.size());

		ElGamalPrivateKey recovered = target.recover(containers, elGamalPublicKey);
		assertEquals(recovered, elGamalPrivateKey);
	}

	private ElGamalPublicKey getElGamalPublicKey(KeyPair keyPair) {

		PublicKey publicKey = keyPair.getPublic();

		ElGamalPublicKeyAdapter elGamalPublicKeyAdapter = null;
		if (publicKey instanceof ElGamalPublicKeyAdapter) {
			elGamalPublicKeyAdapter = (ElGamalPublicKeyAdapter) publicKey;
		}

		return elGamalPublicKeyAdapter.getPublicKey();
	}

	private ElGamalPrivateKey getElGamalPrivateKey(KeyPair keyPair) {

		PrivateKey privateKey = keyPair.getPrivate();

		ElGamalPrivateKeyAdapter elGamalPrivateKeyAdapter = null;
		if (privateKey instanceof ElGamalPrivateKeyAdapter) {
			elGamalPrivateKeyAdapter = (ElGamalPrivateKeyAdapter) privateKey;
		}

		return elGamalPrivateKeyAdapter.getPrivateKey();
	}
}
