/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.cryptolib.elgamal.exponentiation;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.proofs.ProofsServiceAPI;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofVerifierAPI;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;
import ch.post.it.evoting.cryptolib.proofs.service.ProofsService;

class ExponentiationServiceImplTest {
	private ProofsServiceAPI proofsService;

	private ZpSubgroup group;

	private Exponent exponent;

	private ZpGroupElement element1;

	private ZpGroupElement element2;

	private ZpGroupElement element3;

	private ExponentiationServiceImpl exponentiationService;

	@BeforeEach
	public void setUp() throws GeneralCryptoLibException {
		proofsService = new ProofsService();
		group = new ZpSubgroup(new BigInteger("3"), new BigInteger(
				"25515082852221325227734875679796454760326467690112538918409444238866830264288928368643860210692030230970372642053699673880830938513755311613746769767735066124931265104230246714327140720231537205767076779634365989939295710998787785801877310580401262530818848712843191597770750843711630250668056368624192328749556025449493888902777252341817892959006585132698115406972938429732386814317498812002229915393331703423250137659204137625584559844531972832055617091033311878843608854983169553055109029654797488332746885443611918764277292979134833642098989040604523427961162591459163821790507259475762650859921432844527734894939"),
				new BigInteger(
						"12757541426110662613867437839898227380163233845056269459204722119433415132144464184321930105346015115485186321026849836940415469256877655806873384883867533062465632552115123357163570360115768602883538389817182994969647855499393892900938655290200631265409424356421595798885375421855815125334028184312096164374778012724746944451388626170908946479503292566349057703486469214866193407158749406001114957696665851711625068829602068812792279922265986416027808545516655939421804427491584776527554514827398744166373442721805959382138646489567416821049494520302261713980581295729581910895253629737881325429960716422263867447469"));
		exponent = new Exponent(group.getQ(), BigInteger.TEN);
		element1 = new ZpGroupElement(BigInteger.valueOf(5), group);
		element2 = new ZpGroupElement(BigInteger.valueOf(7), group);
		element3 = new ZpGroupElement(BigInteger.valueOf(17), group);
		exponentiationService = new ExponentiationServiceImpl(proofsService);
	}

	@Test
	void testExponentiateCleartexts() throws GeneralCryptoLibException {
		List<BigInteger> cleartexts = asList(element1.getValue(), element2.getValue(), element3.getValue());

		ExponentiatedElementsAndProof<BigInteger> exponentiatedElementsAndProof = exponentiationService
				.exponentiateCleartexts(cleartexts, exponent, group);

		List<BigInteger> powers = exponentiatedElementsAndProof.exponentiatedElements();
		assertEquals(3, powers.size());
		assertEquals(new BigInteger("9765625"), powers.get(0));
		assertEquals(new BigInteger("282475249"), powers.get(1));
		assertEquals(new BigInteger("2015993900449"), powers.get(2));

		List<ZpGroupElement> exponentiatedElements = asList(group.getGenerator().exponentiate(exponent), new ZpGroupElement(powers.get(0), group),
				new ZpGroupElement(powers.get(1), group), new ZpGroupElement(powers.get(2), group));
		List<ZpGroupElement> baseElements = asList(group.getGenerator(), element1, element2, element3);
		Proof proof = exponentiatedElementsAndProof.exponentiationProof();
		ProofVerifierAPI verifier = proofsService.createProofVerifierAPI(group);
		assertTrue(verifier.verifyExponentiationProof(exponentiatedElements, baseElements, proof));
	}

	@Test
	void testExponentiateCiphertexts() throws GeneralCryptoLibException {

		ElGamalCiphertext ciphertext1 = new ElGamalCiphertext(asList(element1, element2));
		ElGamalCiphertext ciphertext2 = new ElGamalCiphertext(asList(element1, element3));
		List<ElGamalCiphertext> ciphertexts = asList(ciphertext1, ciphertext2);

		ExponentiatedElementsAndProof<ElGamalCiphertext> exponentiatedElementsAndProof = exponentiationService
				.exponentiateCiphertexts(ciphertexts, exponent, group);

		assertEquals(2, exponentiatedElementsAndProof.exponentiatedElements().size());

		ElGamalCiphertext power1 = exponentiatedElementsAndProof.exponentiatedElements().get(0);
		assertEquals(new BigInteger("9765625"), power1.getGamma().getValue());
		assertEquals(new BigInteger("282475249"), power1.getPhis().get(0).getValue());

		ElGamalCiphertext power2 = exponentiatedElementsAndProof.exponentiatedElements().get(1);
		assertEquals(new BigInteger("9765625"), power2.getGamma().getValue());
		assertEquals(new BigInteger("2015993900449"), power2.getPhis().get(0).getValue());

		List<ZpGroupElement> exponentiatedElements = asList(group.getGenerator().exponentiate(exponent), power1.getGamma(), power1.getPhis().get(0),
				power2.getGamma(), power2.getPhis().get(0));
		List<ZpGroupElement> baseElements = asList(group.getGenerator(), element1, element2, element1, element3);
		Proof proof = exponentiatedElementsAndProof.exponentiationProof();
		ProofVerifierAPI verifier = proofsService.createProofVerifierAPI(group);
		assertTrue(verifier.verifyExponentiationProof(exponentiatedElements, baseElements, proof));
	}

}
