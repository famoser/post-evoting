/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;

/**
 * Tests of {@link CcrjReturnCodesKeys}.
 */
class CcrjReturnCodesKeysTest {

	@Test
	void testBuilder() throws GeneralCryptoLibException {
		ElGamalServiceAPI service = new ElGamalService();
		BigInteger P = new BigInteger(
				"25515082852221325227734875679796454760326467690112538918409444238866830264288928368643860210692030230970372642053699673880830938513755311613746769767735066124931265104230246714327140720231537205767076779634365989939295710998787785801877310580401262530818848712843191597770750843711630250668056368624192328749556025449493888902777252341817892959006585132698115406972938429732386814317498812002229915393331703423250137659204137625584559844531972832055617091033311878843608854983169553055109029654797488332746885443611918764277292979134833642098989040604523427961162591459163821790507259475762650859921432844527734894939");
		BigInteger Q = new BigInteger(
				"12757541426110662613867437839898227380163233845056269459204722119433415132144464184321930105346015115485186321026849836940415469256877655806873384883867533062465632552115123357163570360115768602883538389817182994969647855499393892900938655290200631265409424356421595798885375421855815125334028184312096164374778012724746944451388626170908946479503292566349057703486469214866193407158749406001114957696665851711625068829602068812792279922265986416027808545516655939421804427491584776527554514827398744166373442721805959382138646489567416821049494520302261713980581295729581910895253629737881325429960716422263867447469");
		BigInteger G = new BigInteger("3");

		ElGamalEncryptionParameters parameters = new ElGamalEncryptionParameters(P, Q, G);

		ElGamalKeyPair generationKeys = service.generateKeyPair(parameters, 1);
		byte[] generationPublicKeySignature = { 1 };
		ElGamalKeyPair decryptionKeys = service.generateKeyPair(parameters, 1);
		byte[] decryptionPublicKeySignature = { 2 };

		CcrjReturnCodesKeys.Builder builder = new CcrjReturnCodesKeys.Builder();
		builder.setCcrjReturnCodesGenerationKeys(generationKeys.getPrivateKeys(), generationKeys.getPublicKeys(), generationPublicKeySignature);
		builder.setCcrjChoiceReturnCodesEncryptionKeys(decryptionKeys.getPrivateKeys(), decryptionKeys.getPublicKeys(), decryptionPublicKeySignature);

		CcrjReturnCodesKeys ccrjReturnCodesKeys = builder.build();

		assertAll(() -> assertEquals(ccrjReturnCodesKeys.getCcrjReturnCodesGenerationSecretKey(), generationKeys.getPrivateKeys()),
				() -> assertEquals(ccrjReturnCodesKeys.getCcrjReturnCodesGenerationPublicKey(), generationKeys.getPublicKeys()),
				() -> assertArrayEquals(ccrjReturnCodesKeys.getCcrjReturnCodesGenerationPublicKeySignature(), generationPublicKeySignature),
				() -> assertEquals(ccrjReturnCodesKeys.getCcrjChoiceReturnCodesEncryptionSecretKey(), decryptionKeys.getPrivateKeys()),
				() -> assertEquals(ccrjReturnCodesKeys.getCcrjChoiceReturnCodesEncryptionPublicKey(), decryptionKeys.getPublicKeys()),
				() -> assertArrayEquals(ccrjReturnCodesKeys.getCcrjChoiceReturnCodesEncryptionPublicKeySignature(), decryptionPublicKeySignature));
	}
}
