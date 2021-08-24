/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;

class ElGamalPublicKeyCombinerWithCompressionTest {

	private static ElGamalEncryptionParameters params;
	private static ElGamalService service;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException {

		BigInteger p = new BigInteger(
				"25515082852221325227734875679796454760326467690112538918409444238866830264288928368643860210692030230970372642053699673880830938513755311613746769767735066124931265104230246714327140720231537205767076779634365989939295710998787785801877310580401262530818848712843191597770750843711630250668056368624192328749556025449493888902777252341817892959006585132698115406972938429732386814317498812002229915393331703423250137659204137625584559844531972832055617091033311878843608854983169553055109029654797488332746885443611918764277292979134833642098989040604523427961162591459163821790507259475762650859921432844527734894939");
		BigInteger q = new BigInteger(
				"12757541426110662613867437839898227380163233845056269459204722119433415132144464184321930105346015115485186321026849836940415469256877655806873384883867533062465632552115123357163570360115768602883538389817182994969647855499393892900938655290200631265409424356421595798885375421855815125334028184312096164374778012724746944451388626170908946479503292566349057703486469214866193407158749406001114957696665851711625068829602068812792279922265986416027808545516655939421804427491584776527554514827398744166373442721805959382138646489567416821049494520302261713980581295729581910895253629737881325429960716422263867447469");
		BigInteger g = new BigInteger("3");

		params = new ElGamalEncryptionParameters(p, q, g);
		service = new ElGamalService();
	}

	@Test
	void whenNullPrimaryKeyThenException() throws GeneralCryptoLibException {
		ElGamalKeyPair keyPairWithTwoSubKeys = service.generateKeyPair(params, 2);

		List<ElGamalPublicKey> listOfKeys = new ArrayList<>();
		listOfKeys.add(keyPairWithTwoSubKeys.getPublicKeys());

		ElGamalPublicKeyCombinerWithCompression elGamalPublicKeyCombinerWithCompression = new ElGamalPublicKeyCombinerWithCompression();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> elGamalPublicKeyCombinerWithCompression.combine(null, listOfKeys));
		assertEquals("The primary public key was null", exception.getMessage());
	}

	@Test
	void givenTwoAdditionalKeysLargerThanPrimaryKey_whenCombineThenExpectedSize() throws GeneralCryptoLibException {
		ElGamalKeyPair keyPairWithThreeSubKeys = service.generateKeyPair(params, 3);

		ElGamalKeyPair keyPairWithSevenSubKeys = service.generateKeyPair(params, 7);
		ElGamalKeyPair keyPairWithFiveSubKeys = service.generateKeyPair(params, 5);

		List<ElGamalPublicKey> listOfKeys = new ArrayList<>();
		listOfKeys.add(keyPairWithSevenSubKeys.getPublicKeys());
		listOfKeys.add(keyPairWithFiveSubKeys.getPublicKeys());

		ElGamalPublicKeyCombinerWithCompression elGamalPublicKeyMultiplerWithCompression = new ElGamalPublicKeyCombinerWithCompression();

		ElGamalPublicKey combined = elGamalPublicKeyMultiplerWithCompression.combine(keyPairWithThreeSubKeys.getPublicKeys(), listOfKeys);

		assertEquals(3, combined.getKeys().size());
	}

	@Test
	void givenTwoAdditionalKeysOfSameSizeAsPrimaryKey_whenCombineThenExpectedSize() throws GeneralCryptoLibException {
		ElGamalKeyPair keyPairWithThreeSubKeys1 = service.generateKeyPair(params, 3);

		ElGamalKeyPair keyPairWithThreeSubKeys2 = service.generateKeyPair(params, 3);
		ElGamalKeyPair keyPairWithThreeSubKeys3 = service.generateKeyPair(params, 3);

		List<ElGamalPublicKey> listOfKeys = new ArrayList<>();
		listOfKeys.add(keyPairWithThreeSubKeys2.getPublicKeys());
		listOfKeys.add(keyPairWithThreeSubKeys3.getPublicKeys());

		ElGamalPublicKeyCombinerWithCompression elGamalPublicKeyMultiplerWithCompression = new ElGamalPublicKeyCombinerWithCompression();

		ElGamalPublicKey combined = elGamalPublicKeyMultiplerWithCompression.combine(keyPairWithThreeSubKeys1.getPublicKeys(), listOfKeys);

		assertEquals(3, combined.getKeys().size());
	}

	@Test
	void whenListIsEmptyThenOutputSizeEqualToPrimaryElGamalKeySize() throws GeneralCryptoLibException {
		ElGamalKeyPair keyPairWithThreeSubKeys = service.generateKeyPair(params, 3);

		List<ElGamalPublicKey> listOfKeys = new ArrayList<>();

		ElGamalPublicKeyCombinerWithCompression elGamalPublicKeyCombinerWithCompression = new ElGamalPublicKeyCombinerWithCompression();

		ElGamalPublicKey combined = elGamalPublicKeyCombinerWithCompression.combine(keyPairWithThreeSubKeys.getPublicKeys(), listOfKeys);

		assertEquals(3, combined.getKeys().size());
	}

	@Test
	void whenPrimaryKeyLargerThenOtherKeys() throws GeneralCryptoLibException {
		ElGamalKeyPair keyPairWithSevenSubKeys = service.generateKeyPair(params, 6);

		ElGamalKeyPair keyPairWithFourSubKeys = service.generateKeyPair(params, 3);
		ElGamalKeyPair keyPairWithFiveSubKeys = service.generateKeyPair(params, 2);

		List<ElGamalPublicKey> listOfKeys = new ArrayList<>();
		listOfKeys.add(keyPairWithFourSubKeys.getPublicKeys());
		listOfKeys.add(keyPairWithFiveSubKeys.getPublicKeys());

		ElGamalPublicKeyCombinerWithCompression elGamalPublicKeyCombinerWithCompression = new ElGamalPublicKeyCombinerWithCompression();

		final ElGamalPublicKey publicKeys = keyPairWithSevenSubKeys.getPublicKeys();
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> elGamalPublicKeyCombinerWithCompression.combine(publicKeys, listOfKeys));
		assertEquals("The primary key has more elements than one of the additional keys. Primary: 6, other: 3", exception.getMessage());
	}
}
