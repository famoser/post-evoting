/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.sign;

import java.math.BigInteger;

import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

/**
 * Provide methods to generate cryptographic data for use in SDM tests.
 */
public class CryptoTestData {

	public static final BigInteger P = new BigInteger(
			"16370518994319586760319791526293535327576438646782139419846004180837103527129035954742043590609421369665944746587885814920851694546456891767644945459124422553763416586515339978014154452159687109161090635367600349264934924141746082060353483306855352192358732451955232000593777554431798981574529854314651092086488426390776811367125009551346089319315111509277347117467107914073639456805159094562593954195960531136052208019343392906816001017488051366518122404819967204601427304267380238263913892658950281593755894747339126531018026798982785331079065126375455293409065540731646939808640273393855256230820509217411510058759");

	public static final BigInteger Q = new BigInteger(
			"8185259497159793380159895763146767663788219323391069709923002090418551763564517977371021795304710684832972373293942907460425847273228445883822472729562211276881708293257669989007077226079843554580545317683800174632467462070873041030176741653427676096179366225977616000296888777215899490787264927157325546043244213195388405683562504775673044659657555754638673558733553957036819728402579547281296977097980265568026104009671696453408000508744025683259061202409983602300713652133690119131956946329475140796877947373669563265509013399491392665539532563187727646704532770365823469904320136696927628115410254608705755029379");

	public static final BigInteger G = new BigInteger("2");

	private static ZpSubgroup zpSubgroup;

	private static ZpSubgroup encryptionParameters;

	private static ElGamalServiceAPI elGamalService;

	/**
	 * @return a Zp subgroup with pre-determined values.
	 */
	public static ZpSubgroup getZpSubgroup() throws GeneralCryptoLibException {
		if (null == zpSubgroup) {
			zpSubgroup = new ZpSubgroup(G, P, Q);
		}

		return zpSubgroup;
	}

	/**
	 * @return the ElGamal encryption parameters
	 */
	public static ZpSubgroup getEncryptionParameters() throws GeneralCryptoLibException {
		if (encryptionParameters == null) {
			encryptionParameters = new ZpSubgroup(G, P, Q);
		}

		return encryptionParameters;
	}

	/**
	 * @param numElements How many elements should the new key pair have.
	 * @return an ElGamal key pair for encryption and decryption.
	 */
	public static ElGamalKeyPair generateElGamalKeyPair(int numElements) throws GeneralCryptoLibException {
		ElGamalEncryptionParameters elGamalEncryptionParameters = new ElGamalEncryptionParameters(P, Q, G);

		return getElGamalService().generateKeyPair(elGamalEncryptionParameters, numElements);

	}

	public static ElGamalServiceAPI getElGamalService() throws GeneralCryptoLibException {
		if (null == elGamalService) {
			elGamalService = new ElGamalService();
		}

		return elGamalService;
	}
}
