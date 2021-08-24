/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.multishare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.secretsharing.Share;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.secretsharing.service.ThresholdSecretSharingService;
import ch.post.it.evoting.sdm.config.shares.exception.SharesException;
import ch.post.it.evoting.sdm.config.shares.keys.elgamal.ElGamalKeyPairGenerator;
import ch.post.it.evoting.sdm.config.shares.keys.elgamal.ElGamalPrivateKeyAdapter;

class MultipleSharesContainerTest {

	private static final BigInteger p = new BigInteger(
			"16370518994319586760319791526293535327576438646782139419846004180837103527129035954742043590609421369665944746587885814920851694546456891767644945459124422553763416586515339978014154452159687109161090635367600349264934924141746082060353483306855352192358732451955232000593777554431798981574529854314651092086488426390776811367125009551346089319315111509277347117467107914073639456805159094562593954195960531136052208019343392906816001017488051366518122404819967204601427304267380238263913892658950281593755894747339126531018026798982785331079065126375455293409065540731646939808640273393855256230820509217411510058759");
	private static final BigInteger q = new BigInteger(
			"8185259497159793380159895763146767663788219323391069709923002090418551763564517977371021795304710684832972373293942907460425847273228445883822472729562211276881708293257669989007077226079843554580545317683800174632467462070873041030176741653427676096179366225977616000296888777215899490787264927157325546043244213195388405683562504775673044659657555754638673558733553957036819728402579547281296977097980265568026104009671696453408000508744025683259061202409983602300713652133690119131956946329475140796877947373669563265509013399491392665539532563187727646704532770365823469904320136696927628115410254608705755029379");
	private static final BigInteger g = new BigInteger("2");

	private static ThresholdSecretSharingService thresholdSecretSharingService;
	private static List<MultipleSharesContainer> secrets2_shares2_threshold2;
	private static List<MultipleSharesContainer> secrets2_shares3_threshold3;
	private static BigInteger modulus;
	private static PrivateKey privateKey;
	private static ZpSubgroup group;

	@BeforeAll
	public static void setUpClass() throws KeyException, GeneralCryptoLibException, SharesException {

		// ENCRYPTION WHERE P AND Q HAVE 2048 BITS (EXPONENT IS 256 BYTES)
		group = new ZpSubgroup(g, p, q);

		ElGamalEncryptionParameters elGamalEncryptionParameters = new ElGamalEncryptionParameters(p, q, g);

		ElGamalServiceAPI elgamalServiceAPI = new ElGamalService();

		int numRequiredSubkeys = 3;
		ElGamalKeyPairGenerator elGamalKeyPairGenerator = new ElGamalKeyPairGenerator(elGamalEncryptionParameters, numRequiredSubkeys,
				elgamalServiceAPI);

		KeyPair keyPair = elGamalKeyPairGenerator.generate();
		privateKey = keyPair.getPrivate();
		ElGamalPrivateKeyAdapter rsaPrivateKey = (ElGamalPrivateKeyAdapter) privateKey;
		modulus = rsaPrivateKey.getPrivateKey().getGroup().getQ();

		thresholdSecretSharingService = new ThresholdSecretSharingService();

		secrets2_shares2_threshold2 = createListOfContainers_secrets2_shares2_threshold2();

		secrets2_shares3_threshold3 = createListOfContainers_secrets2_shares3_threshold3();

	}

	private static List<MultipleSharesContainer> createListOfContainers_secrets2_shares2_threshold2() throws SharesException {

		int no = 2;
		int threshold = 2;

		String originalData1 = getSmallString1();
		String originalData2 = getSmallString2();

		////////////////////////////////////////////////////
		//
		// split each piece of data into shares
		//
		////////////////////////////////////////////////////

		Set<Share> shares1 = thresholdSecretSharingService.split(originalData1.getBytes(StandardCharsets.UTF_8), no, threshold, modulus);
		Set<Share> shares2 = thresholdSecretSharingService.split(originalData2.getBytes(StandardCharsets.UTF_8), no, threshold, modulus);

		////////////////////////////////////////////////////
		//
		// arrange data into the structures to stored in the containers
		//
		////////////////////////////////////////////////////

		List<Share> shares1AsList = new ArrayList<>(shares1);
		List<Share> shares2AsList = new ArrayList<>(shares2);

		List<byte[]> sharesForCard1 = new ArrayList<>();
		sharesForCard1.add(thresholdSecretSharingService.serialize(shares1AsList.get(0)));
		sharesForCard1.add(thresholdSecretSharingService.serialize(shares2AsList.get(0)));

		List<byte[]> sharesForCard2 = new ArrayList<>();
		sharesForCard2.add(thresholdSecretSharingService.serialize(shares1AsList.get(1)));
		sharesForCard2.add(thresholdSecretSharingService.serialize(shares2AsList.get(1)));

		MultipleSharesContainer container1 = new MultipleSharesContainer(no, threshold, sharesForCard1, modulus);
		MultipleSharesContainer container2 = new MultipleSharesContainer(no, threshold, sharesForCard2, modulus);

		List<MultipleSharesContainer> listMultipleSharesContainers = new ArrayList<>();
		listMultipleSharesContainers.add(container1);
		listMultipleSharesContainers.add(container2);

		return listMultipleSharesContainers;
	}

	private static List<MultipleSharesContainer> createListOfContainers_secrets2_shares3_threshold3() throws SharesException {

		int no = 3;
		int threshold = 3;

		String originalData1 = getSmallString1();
		String originalData2 = getSmallString2();

		////////////////////////////////////////////////////
		//
		// split each piece of data into shares
		//
		////////////////////////////////////////////////////

		Set<Share> shares1 = thresholdSecretSharingService.split(originalData1.getBytes(StandardCharsets.UTF_8), no, threshold, modulus);
		Set<Share> shares2 = thresholdSecretSharingService.split(originalData2.getBytes(StandardCharsets.UTF_8), no, threshold, modulus);

		////////////////////////////////////////////////////
		//
		// arrange data into the structures to stored in the containers
		//
		////////////////////////////////////////////////////

		List<Share> shares1AsList = new ArrayList<>(shares1);
		List<Share> shares2AsList = new ArrayList<>(shares2);

		List<byte[]> sharesForCard1 = new ArrayList<>();
		sharesForCard1.add(thresholdSecretSharingService.serialize(shares1AsList.get(0)));
		sharesForCard1.add(thresholdSecretSharingService.serialize(shares2AsList.get(0)));

		List<byte[]> sharesForCard2 = new ArrayList<>();
		sharesForCard2.add(thresholdSecretSharingService.serialize(shares1AsList.get(1)));
		sharesForCard2.add(thresholdSecretSharingService.serialize(shares2AsList.get(1)));

		List<byte[]> sharesForCard3 = new ArrayList<>();
		sharesForCard3.add(thresholdSecretSharingService.serialize(shares1AsList.get(2)));
		sharesForCard3.add(thresholdSecretSharingService.serialize(shares2AsList.get(2)));

		MultipleSharesContainer container1 = new MultipleSharesContainer(no, threshold, sharesForCard1, modulus);
		MultipleSharesContainer container2 = new MultipleSharesContainer(no, threshold, sharesForCard2, modulus);
		MultipleSharesContainer container3 = new MultipleSharesContainer(no, threshold, sharesForCard3, modulus);

		List<MultipleSharesContainer> listMultipleSharesContainers = new ArrayList<>();
		listMultipleSharesContainers.add(container1);
		listMultipleSharesContainers.add(container2);
		listMultipleSharesContainers.add(container3);

		return listMultipleSharesContainers;
	}

	private static String getSmallString1() {
		return "aaaaaaaaaaaaaaaaaaaaaaaaaaaa111111111111111111111111111111111111";
	}

	private static String getSmallString2() {
		return "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb222222222222222222222222222222222222222222";
	}

	@Test
	void whenSerializeAndReconstructThenOk() throws SharesException {

		MultipleSharesContainer container1 = secrets2_shares2_threshold2.get(0);
		MultipleSharesContainer container2 = secrets2_shares2_threshold2.get(1);

		byte[] container1Serailized = container1.serialize();
		byte[] container2Serailized = container2.serialize();

		MultipleSharesContainer reconstructed1 = new MultipleSharesContainer(container1Serailized,
				MultipleSharesContainer.getModulusFromSerializedData(container1Serailized));
		MultipleSharesContainer reconstructed2 = new MultipleSharesContainer(container2Serailized,
				MultipleSharesContainer.getModulusFromSerializedData(container2Serailized));

		assertEquals(container1.getModulus(), reconstructed1.getModulus());
		assertEquals(container2.getModulus(), reconstructed2.getModulus());

		assertTrue(checkIfContainersAreEqual(container1, reconstructed1));
		assertTrue(checkIfContainersAreEqual(container2, reconstructed2));
	}

	@Test
	void whenCheckIfTwoCompatibleContainersAreCompatibleThenTrue() {

		MultipleSharesContainer container1 = secrets2_shares2_threshold2.get(0);
		MultipleSharesContainer container2 = secrets2_shares2_threshold2.get(1);

		assertTrue(container1.isCompatible(container2));
	}

	@Test
	void whenCheckIfTwoCompatibleContainersAreCompatibleThenTrue2() {

		MultipleSharesContainer container1 = secrets2_shares3_threshold3.get(0);
		MultipleSharesContainer container2 = secrets2_shares3_threshold3.get(1);

		assertTrue(container1.isCompatible(container2));
	}

	@Test
	void whenCheckIfTwoNotCompatibleContainersAreCompatibleThenFalse() {

		MultipleSharesContainer container1 = secrets2_shares2_threshold2.get(0);
		MultipleSharesContainer container2 = secrets2_shares3_threshold3.get(1);

		assertFalse(container1.isCompatible(container2));
	}

	@Test
	void whenCheckThenOk() throws SharesException {

		secrets2_shares2_threshold2.get(0).check();
		secrets2_shares2_threshold2.get(1).check();
		secrets2_shares3_threshold3.get(0).check();
		secrets2_shares3_threshold3.get(1).check();
	}

	@Test
	void whenGetSharesThenExpectedSize() {

		assertEquals(2, secrets2_shares2_threshold2.get(0).getShares().size());
		assertEquals(2, secrets2_shares2_threshold2.get(1).getShares().size());
		assertEquals(2, secrets2_shares3_threshold3.get(0).getShares().size());
		assertEquals(2, secrets2_shares3_threshold3.get(1).getShares().size());
	}

	@Test
	void whenGetNumberOfPartsThenExpectedSize() {

		assertEquals(2, secrets2_shares2_threshold2.get(0).getNumberOfParts());
		assertEquals(2, secrets2_shares2_threshold2.get(1).getNumberOfParts());
		assertEquals(3, secrets2_shares3_threshold3.get(0).getNumberOfParts());
		assertEquals(3, secrets2_shares3_threshold3.get(1).getNumberOfParts());
	}

	@Test
	void whenGetModulusThenExpectedSize() {

		int bitLenth = secrets2_shares2_threshold2.get(0).getModulus().bitLength();
		assertEquals(2047, bitLenth);

		bitLenth = secrets2_shares2_threshold2.get(1).getModulus().bitLength();
		assertEquals(2047, bitLenth);

		bitLenth = secrets2_shares3_threshold3.get(0).getModulus().bitLength();
		assertEquals(2047, bitLenth);

		bitLenth = secrets2_shares3_threshold3.get(1).getModulus().bitLength();
		assertEquals(2047, bitLenth);
	}

	@Test
	void whenGetPointThenException() {
		final MultipleSharesContainer multipleSharesContainer = secrets2_shares2_threshold2.get(0);
		assertThrows(UnsupportedOperationException.class, multipleSharesContainer::getPoints);
	}

	@Test
	void whenSplitAndRecoverStringsThenOk() throws SharesException, GeneralCryptoLibException {

		int no = 2;
		int threshold = 2;

		String originalData1 = getSmallString1();
		String originalData2 = getSmallString2();

		////////////////////////////////////////////////////
		//
		// split each piece of data into shares
		//
		////////////////////////////////////////////////////

		Set<Share> shares1 = thresholdSecretSharingService.split(originalData1.getBytes(StandardCharsets.UTF_8), no, threshold, modulus);
		Set<Share> shares2 = thresholdSecretSharingService.split(originalData2.getBytes(StandardCharsets.UTF_8), no, threshold, modulus);

		////////////////////////////////////////////////////
		//
		// arrange data into the structures to stored in the containers
		//
		////////////////////////////////////////////////////

		List<Share> shares1AsList = new ArrayList<>(shares1);
		List<Share> shares2AsList = new ArrayList<>(shares2);

		List<byte[]> sharesForCard1 = new ArrayList<>();
		sharesForCard1.add(thresholdSecretSharingService.serialize(shares1AsList.get(0)));
		sharesForCard1.add(thresholdSecretSharingService.serialize(shares2AsList.get(0)));

		List<byte[]> sharesForCard2 = new ArrayList<>();
		sharesForCard2.add(thresholdSecretSharingService.serialize(shares1AsList.get(1)));
		sharesForCard2.add(thresholdSecretSharingService.serialize(shares2AsList.get(1)));

		MultipleSharesContainer container1 = new MultipleSharesContainer(no, threshold, sharesForCard1, modulus);
		MultipleSharesContainer container2 = new MultipleSharesContainer(no, threshold, sharesForCard2, modulus);

		////////////////////////////////////////////////////
		//
		// extract data from the containers
		//
		////////////////////////////////////////////////////

		List<byte[]> fromcard1 = container1.getShares();
		List<byte[]> fromcard2 = container2.getShares();

		Set<Share> data1Shares = new HashSet<>();
		data1Shares.add(thresholdSecretSharingService.deserialize(fromcard1.get(0)));
		data1Shares.add(thresholdSecretSharingService.deserialize(fromcard2.get(0)));

		Set<Share> data2Shares = new HashSet<>();
		data2Shares.add(thresholdSecretSharingService.deserialize(fromcard1.get(1)));
		data2Shares.add(thresholdSecretSharingService.deserialize(fromcard2.get(1)));

		byte[] recoveredData1 = thresholdSecretSharingService.recover(data1Shares);
		byte[] recoveredData2 = thresholdSecretSharingService.recover(data2Shares);

		String string1 = new String(recoveredData1, StandardCharsets.UTF_8);
		String string2 = new String(recoveredData2, StandardCharsets.UTF_8);

		assertEquals(originalData1, string1);
		assertEquals(originalData2, string2);
	}

	@Test
	void whenSplitAndRecoverElGamalPrivateKeyThenOk() throws GeneralCryptoLibException, SharesException {

		int numberShares = 2;
		int threshold = 2;

		ElGamalPrivateKeyAdapter elGamalPrivateKeyAdapter = null;
		if (privateKey instanceof ElGamalPrivateKeyAdapter) {
			elGamalPrivateKeyAdapter = (ElGamalPrivateKeyAdapter) privateKey;
		}
		ElGamalPrivateKey elGamalPrivateKey = elGamalPrivateKeyAdapter.getPrivateKey();

		List<Exponent> subkeys = elGamalPrivateKey.getKeys();
		int numSubkeys = subkeys.size();

		////////////////////////////////////////////////////
		//
		// split each piece of data into shares
		//
		////////////////////////////////////////////////////

		List<List<Share>> allShares = new ArrayList<>();
		for (Exponent subkey : subkeys) {

			BigInteger value = subkey.getValue();
			byte[] secret = value.toByteArray();
			Set<Share> shareForSubkey = thresholdSecretSharingService.split(secret, numberShares, threshold, elGamalPrivateKey.getGroup().getQ());
			allShares.add(new ArrayList<>(shareForSubkey));
		}

		////////////////////////////////////////////////////
		//
		// arrange data into the structures to be stored in the containers
		//
		////////////////////////////////////////////////////

		List<MultipleSharesContainer> containers = new ArrayList<>();

		for (int i = 0; i < numberShares; i++) {

			List<byte[]> sharesForCard = new ArrayList<>();

			for (int j = 0; j < numSubkeys; j++) {
				sharesForCard.add(thresholdSecretSharingService.serialize(allShares.get(j).get(i)));
			}
			containers.add(new MultipleSharesContainer(numberShares, threshold, sharesForCard, modulus));
		}

		////////////////////////////////////////////////////
		//
		// extract data from the containers
		//
		////////////////////////////////////////////////////

		List<Exponent> exps = new ArrayList<>();

		for (int i = 0; i < numSubkeys; i++) {

			Set<Share> sharesForSingleSubkey = new HashSet<>();
			for (int j = 0; j < numberShares; j++) {
				List<byte[]> fromcard1 = containers.get(j).getShares();
				sharesForSingleSubkey.add(thresholdSecretSharingService.deserialize(fromcard1.get(i)));
			}

			byte[] recoveredData1 = thresholdSecretSharingService.recover(sharesForSingleSubkey);
			Exponent recovered1 = new Exponent(q, new BigInteger(recoveredData1));
			exps.add(recovered1);
		}

		ElGamalPrivateKey recoveredKey = new ElGamalPrivateKey(exps, group);

		assertEquals(elGamalPrivateKey, recoveredKey);
	}

	private boolean checkIfContainersAreEqual(MultipleSharesContainer c1, MultipleSharesContainer c2) {

		if (c1 == null || c2 == null) {
			return false;
		}

		List<byte[]> sharesC1 = c1.getShares();
		List<byte[]> sharesC2 = c2.getShares();

		if (sharesC1.size() != sharesC2.size()) {
			return false;
		}

		for (int i = 0; i < sharesC1.size(); i++) {
			if (!Arrays.equals(sharesC1.get(i), sharesC2.get(i))) {
				return false;
			}
		}

		return c1.getModulus().equals(c2.getModulus());
	}
}
