/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.shares.keys.elgamal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.sdm.config.constants.Constants;

class ElGamalUtilsTest {

	private static final ElGamalUtils target = new ElGamalUtils();

	private static ElGamalPublicKey longElGamalPublicKey;
	private static ElGamalPrivateKey longElGamalPrivateKey;

	@BeforeAll
	public static void setUp() throws GeneralCryptoLibException {
		loadLargeConfiguration();
	}

	private static void loadLargeConfiguration() throws GeneralCryptoLibException {

		BigInteger p = new BigInteger(
				"27839315951550607954908947302805005270748306495150332019506497627681949106676211603621655834342899312853752944034571664077675893773681536386846966667523575670806834175256557051826034202665533443825675341545392523192426729654012224826614092343340720174718101364493840811991904431138360523983907332811743305613997816019580431245599135029769534308919329503387684189735998288344293295738991424137467298627689337334152522616643588240444981657026169355687112876037117176846382595626492699708769169931274950646847052618070904638628820367215645566336974910524181637499593007713578628942182502228080597082761613515206700523109");
		BigInteger q = new BigInteger("72672713147406890228989244889596588007200985967243596643124233889542687254499");
		BigInteger g = new BigInteger(
				"4267702297229469673790018703734877296807681845981859915554469538283570502856417708468191328245393782101264664502289657179067509176893351929373100181771085681303723016005109429862508282653479146065243990330272150374977895870396872387540216688889850476152205361728439710042818398798929578066878456091896533097188888338823697398469244803574755568190747136605054794708632236694927928994592574083595309695961503955530493578418229984920184090088732231205782707337157009325887375886691582508754748112217552487874508954018581071754592385039406544226588417197430102130453807486352982973717703392982010821660259571513106072633");

		ZpSubgroup zpsubgroup = new ZpSubgroup(g, p, q);

		BigInteger element1AsBigInt = new BigInteger(
				"5008939184699864548607896225874028920107047771989161953101261926559201320952984261775704392906959200137839399428076081539430300826679626719072412606725700589444631257809848782050767717647169248021545487564030383986410955747062025425171610933788466017679864114385991951853644177866263156959639945620111519033778589572988311335137371925882167492697074310400017857786377017543029286066030984102220518927056975015416726900615833872530871172461912572032189018733507444137466145651277336030430096745736375288491987991503680387548628987156236293179130879514047500787990880625812611975758774694756127351626810081636446404777");
		ZpGroupElement element1 = new ZpGroupElement(element1AsBigInt, zpsubgroup);

		BigInteger element2AsBigInt = new BigInteger(
				"4248419382516873356196914503010907576408089329091227785906678292559559013494329064050084190970017690927198459675357790359665519131874428717023800384971209955791568211256275806572804673825164112459733926950660275792491113726706368954587752375119745484024513477338599670110150661890711291999912232748780253498225306253629264745337400154965576411838140659596300214603479107813937689451847979762153100255777981577983999683551742115878141536836515986039955069422872559289326305297387501804800433122960228984674242103919388638202452652400616523408338859443501780872285374057355877622999870695478953927384347559571251513923");
		ZpGroupElement element2 = new ZpGroupElement(element2AsBigInt, zpsubgroup);

		BigInteger element3sBigInt = new BigInteger(
				"23344446876897401533521827162622517402979206098637515460751901995343832219884513019863329061776015563853785350518587986509876131697304941445649534238065094617908644492599138385544864516953793439170531989484035452326357560395845134765939552557301066814622805214391192075689058594545712394362538922607958368843127924525346325473966430266501043625553332242894046509879129358505075133143969202046094279073044061868468454834419448528120090562518455265978705206652585766963615867681290765378910999245840848397115375224428139332040377317810025010216631152810280504578242513232089560938206255771991385440882930034838093749799");
		ZpGroupElement element3 = new ZpGroupElement(element3sBigInt, zpsubgroup);

		List<ZpGroupElement> listOfElements = new ArrayList<>();
		listOfElements.add(element1);
		listOfElements.add(element2);
		listOfElements.add(element3);

		BigInteger exponent1AsBigInt = new BigInteger("42184442855678687308396843350203126607197504880774028232590214773117384453069");
		BigInteger exponent2AsBigInt = new BigInteger("66425874163214327845574137159949011887433419310286261382849711727085208194072");
		BigInteger exponent3AsBigInt = new BigInteger("45814418626181669583290588215193599088696761148366741373962628544823271967819");

		Exponent exponent1 = new Exponent(q, exponent1AsBigInt);
		Exponent exponent2 = new Exponent(q, exponent2AsBigInt);
		Exponent exponent3 = new Exponent(q, exponent3AsBigInt);

		List<Exponent> exponents = new ArrayList<>();
		exponents.add(exponent1);
		exponents.add(exponent2);
		exponents.add(exponent3);

		longElGamalPublicKey = new ElGamalPublicKey(listOfElements, zpsubgroup);

		longElGamalPrivateKey = new ElGamalPrivateKey(exponents, zpsubgroup);
	}

	@Test
	void whenSerializeGivenLongElGamalKeys() {

		byte[] serializedKey = target.serialize(longElGamalPrivateKey);

		int numExponentsPrivateKey = longElGamalPrivateKey.getKeys().size();

		assertEquals(0, ((serializedKey.length / Constants.MAX_EXPONENT_SIZE) % numExponentsPrivateKey));
	}

	@Test
	void whenSerializeGivenNullElGamalKeys() {
		assertThrows(IllegalArgumentException.class, () -> target.serialize(null));
	}

	@Test
	void whenReconstructGivenNullElGamalKeys() {
		byte[] serializedKey = target.serialize(longElGamalPrivateKey);
		assertThrows(IllegalArgumentException.class, () -> target.reconstruct(null, serializedKey));
	}

	@Test
	void whenReconstructGivenNullByteArray() {
		assertThrows(IllegalArgumentException.class, () -> target.reconstruct(longElGamalPublicKey, null));
	}

	@Test
	void whenSerializeAndReconstructGivenLongElGamalKeysAndAnUnchangedByteArray() throws GeneralCryptoLibException {

		byte[] serializedKey = target.serialize(longElGamalPrivateKey);

		ElGamalPrivateKey reconstructed = target.reconstruct(longElGamalPublicKey, serializedKey);

		assertEquals(reconstructed, longElGamalPrivateKey);
	}

	@Test
	void whenSerializeAndReconstructGivenLongElGamalKeysAndAShorterByteArray() throws GeneralCryptoLibException {

		byte[] serializedKey = target.serialize(longElGamalPrivateKey);

		BigInteger tmp = new BigInteger(serializedKey);

		byte[] shorterSerializedKey = tmp.toByteArray();

		ElGamalPrivateKey reconstructed = target.reconstruct(longElGamalPublicKey, shorterSerializedKey);

		assertEquals(reconstructed, longElGamalPrivateKey);
	}

	@Test
	void whenSerializeAndReconstructGivenLongElGamalKeysAndAWrongShorterByteArray() throws GeneralCryptoLibException {

		byte[] serializedKey = target.serialize(longElGamalPrivateKey);

		BigInteger tmp = new BigInteger(serializedKey).add(BigInteger.ONE);

		byte[] shorterSerializedKey = tmp.toByteArray();

		ElGamalPrivateKey reconstructed = target.reconstruct(longElGamalPublicKey, shorterSerializedKey);

		assertNotEquals(reconstructed, longElGamalPrivateKey);
	}
}
