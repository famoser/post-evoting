/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.integration;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncrypterValues;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.configuration.ElGamalPolicy;
import ch.post.it.evoting.cryptolib.elgamal.configuration.ElGamalPolicyFromProperties;
import ch.post.it.evoting.cryptolib.elgamal.factory.CryptoElGamalDecrypter;
import ch.post.it.evoting.cryptolib.elgamal.factory.CryptoElGamalEncrypter;
import ch.post.it.evoting.cryptolib.elgamal.factory.ElGamalFactory;
import ch.post.it.evoting.cryptolib.mathematical.groups.GroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

class CryptoEncryptionAndDecryptionTest {

	private static BigInteger smallP;

	private static BigInteger smallQ;

	private static BigInteger smallG;

	private static List<ZpGroupElement> smallPublicKeysHardcoded;

	private static List<Exponent> smallPrivateKeysHardcoded;

	private static ElGamalPolicy smallCryptoElGamalPolicy;

	private static ElGamalFactory smallCryptoElGamalFactory;

	private static ZpSubgroup smallZpSubgroup;

	private static CryptoElGamalEncrypter smallEncrypterHardcodedKeys;

	private static CryptoElGamalDecrypter smallDecrypterHardcodedKeys;

	private static BigInteger realP;

	private static BigInteger realQ;

	private static BigInteger realG;

	private static List<ZpGroupElement> realPublicKeysHardcoded;

	private static List<Exponent> realPrivateKeysHardcoded;

	private static ElGamalPolicy realCryptoElGamalPolicy;

	private static ElGamalFactory realCryptoElGamalFactory;

	private static ZpSubgroup realZpSubgroup;

	private static CryptoElGamalEncrypter realEncrypterHardcodedKeys;

	private static CryptoElGamalDecrypter realDecrypterHardcodedKeys;

	@BeforeAll
	public static void setUp() throws GeneralCryptoLibException {

		loadSmallConfiguration();
		loadRealConfiguration();

		smallEncrypterHardcodedKeys = smallCryptoElGamalFactory.createEncrypter(new ElGamalPublicKey(smallPublicKeysHardcoded, smallZpSubgroup));

		smallDecrypterHardcodedKeys = smallCryptoElGamalFactory.createDecrypter(new ElGamalPrivateKey(smallPrivateKeysHardcoded, smallZpSubgroup));

		realEncrypterHardcodedKeys = realCryptoElGamalFactory.createEncrypter(new ElGamalPublicKey(realPublicKeysHardcoded, realZpSubgroup));

		realDecrypterHardcodedKeys = realCryptoElGamalFactory.createDecrypter(new ElGamalPrivateKey(realPrivateKeysHardcoded, realZpSubgroup));
	}

	private static ZpSubgroup getZpSubgroup(final BigInteger g, final BigInteger p, final BigInteger q) throws GeneralCryptoLibException {

		return new ZpSubgroup(g, p, q);
	}

	private static List<Exponent> getPrivateKeys(final ZpSubgroup g, final BigInteger... keys) throws GeneralCryptoLibException {
		Exponent exponent;
		List<Exponent> privateKeys = new ArrayList<>();

		for (BigInteger key : keys) {
			exponent = new Exponent(g.getQ(), key);
			privateKeys.add(exponent);
		}

		return privateKeys;
	}

	private static List<ZpGroupElement> getPublicKeys(final ZpSubgroup g, final BigInteger... keys) throws GeneralCryptoLibException {
		ZpGroupElement element;
		List<ZpGroupElement> publicKeys = new ArrayList<>();

		for (BigInteger key : keys) {
			element = new ZpGroupElement(key, g);
			publicKeys.add(element);
		}

		return publicKeys;
	}

	/**
	 * Loads test configuration
	 *
	 * @throws GeneralCryptoLibException
	 */
	private static void loadSmallConfiguration() throws GeneralCryptoLibException {

		smallP = new BigInteger("23");
		smallQ = new BigInteger("11");
		smallG = new BigInteger("2");

		smallZpSubgroup = getZpSubgroup(smallG, smallP, smallQ);

		smallCryptoElGamalPolicy = new ElGamalPolicyFromProperties();

		smallCryptoElGamalFactory = new ElGamalFactory(smallCryptoElGamalPolicy);

		smallPrivateKeysHardcoded = getPrivateKeys(smallZpSubgroup, BigInteger.ONE, new BigInteger("2"));
		smallPublicKeysHardcoded = getPublicKeys(smallZpSubgroup, new BigInteger("2"), new BigInteger("4"));
	}

	/**
	 * Loads a real scenario configuration
	 *
	 * @throws GeneralCryptoLibException
	 */
	private static void loadRealConfiguration() throws GeneralCryptoLibException {

		realP = new BigInteger(
				"25878792566670842099842137716422866466252991028815773139028451679515364679624923581358662655689289205766441980239548823737806954397019411202244121935752456749381769565031670387914863935577896116425654849306598185507995737892509839616944496073707445338806101425467388977937489020456783676102620561970644684015868766028080049372849872115052208214439472603355483095640041515460851475971118272125133224007949688443680429668091313474118875081620746919907567682398209044343652147328622866834600839878114285018818463110227111614032671442085465843940709084719667865761125514800243342061732684028802646193202210299179139410607");
		realQ = new BigInteger(
				"12939396283335421049921068858211433233126495514407886569514225839757682339812461790679331327844644602883220990119774411868903477198509705601122060967876228374690884782515835193957431967788948058212827424653299092753997868946254919808472248036853722669403050712733694488968744510228391838051310280985322342007934383014040024686424936057526104107219736301677741547820020757730425737985559136062566612003974844221840214834045656737059437540810373459953783841199104522171826073664311433417300419939057142509409231555113555807016335721042732921970354542359833932880562757400121671030866342014401323096601105149589569705303");
		realG = new BigInteger(
				"23337993065784550228812110720552652305178266477392633588884900695706615523553977368516877521940228584865573144621632575456086035440118913707895716109366641541746808409917179478292952139273396531060021729985473121368590574110220870149822495151519706210399569901298027813383104891697930149341258267962490850297875794622068418425473578455187344232698462829084010585324877420343904740081787639502967515631687068869545665294697583750184911025514712871193837246483893950501015755683415509019863976071649325968623617568219864744389709563087949389080252971419711636380986100047871404548371112472694814597772988558887480308242");

		realZpSubgroup = getZpSubgroup(realG, realP, realQ);

		realCryptoElGamalPolicy = new ElGamalPolicyFromProperties();

		realCryptoElGamalFactory = new ElGamalFactory(realCryptoElGamalPolicy);

		realPrivateKeysHardcoded = getPrivateKeys(realZpSubgroup, new BigInteger(
						"19932290298126983822867373246283694543834419444448689387027938121267278297690369083865205185937225553946698937771529278177448965152988087940288333275652867866197468584904193639808285279573404886376170146138923013466997865394411013033589133127405286904582891808686947313183243255910228279949181818238658864287585958169067297934354631387987451313123988211850808977971146508607133777397002899865332248785578309214179182602443053969288258864213973827536851717416916385105956235720163370004298189528382210074328598493302826727281275733318359279641378534728810069395703258431116367239913924616427295756294583567073396132741"),
				new BigInteger(
						"7577689178299053378025254165498777990855334617862256718327226463721764741583484454093269000328148335682856492924773910667544587336325922889256151180332100653754327285144917296147066439365449004239784202513534311409488352456747202671798659147817096528453115200056937800230539886947124207663419998972244638553002561054271980225111687656902641911235998639469734777832498679320977231339705464833025304454996110583985661736573069868478918748203761800351243404612284301662483435138804753635654540381762463438773405967794218429676239599853078295451426556494902828889387202479882230599369972987637455867152360420905963929754"),
				new BigInteger(
						"129547331161844130899924173735104728702220700682642138851022406761777994632728337153635583150724889632689084497665212926336088840842855597736181764744122977345480665146165524664735861913761423291109155186483836806324970977794125768575887671259015293979900588616487195411477129839925141145884519189238456836206862720534398725375349481201999965487100563522135988094619173050092410477941592825803862292425503577691441721033806466916501473758812704373810862790693842410609705251142958265348495650713103821889837809522418329172422392659115758749365074645804533890934339070499112165048929164809093509978635413193830265570"));

		realPublicKeysHardcoded = getPublicKeys(realZpSubgroup, new BigInteger(
						"10813600900774814195443606333960059840422668831651681762833741452209135730650864452130214650095752196571587702814444040062441118243327189166429884552159103387262050740332278254634882734924804644475224947872860032029859548750165768253226371255576644038692950180048665862716256869418687777905840129397057458233193867066348913299433809742746548665012143326171826247072111249488645241753486115635552758795831876906471529661551590917603438603247246774800973230633192834223627007745551660750221242611378094020803537796039757375005102923882805858377750794407562467092342593013192207441450132449006118950054688176587133961409"),
				new BigInteger(
						"9853567379170719777334566185703517962033907422082302290399539440945952704005195929747815492644386838518442489536168205168599971300023125537903954233589099450715318090993081445448000242320602780219383745672665060815409904594899636902384560406070750905212550608478584637229051043378424454988191292610492156611060987968379597746425215082609688334075995471885941390029962061830957304051378886661637882858376301662242976957205952138813983434210148145737465280463387503441953147312554462853944227422544694220254912841114943534241459615181877522308107316310818302531331583848802731174268791963178918947077447910490965216158"),
				new BigInteger(
						"15794379152029066838713804099734041126263932884995939213200727618402701551640677264676158281588744352993384547095094861783499389509798314014022069284171927996645400863301983568835178765405753107568243261485121885250020254379553972077632932375247364025813645736274557285578258334201060356153010774984159753076425595005818465482275931002143043064230447825744365372619735966376047086115776775312766029914867090765966581873109229928865155261673639010273082385667041760650533946369556580260795606883221254081319827267155147350340767060491743854037592935823565514546490962430394933911057965977363230092438984010824700325167"));
	}

	@Test
	void givenSmallGroupAndMessagesWhenGenerateKeysAndEncryptAndDecryptThenOriginalMessages() throws GeneralCryptoLibException {

		List<ZpGroupElement> messages = getMessagesToEncrypt(smallZpSubgroup, new BigInteger("4"), new BigInteger("4"), new BigInteger("8"),
				new BigInteger("4"));

		int numKeys = messages.size();

		ElGamalEncryptionParameters elGamalEncryptionParameters = new ElGamalEncryptionParameters(smallZpSubgroup.getP(), smallZpSubgroup.getQ(),
				smallZpSubgroup.getGenerator().getValue());

		ElGamalKeyPair keyPair = smallCryptoElGamalFactory.createCryptoElGamalKeyPairGenerator().generateKeys(elGamalEncryptionParameters, numKeys);

		CryptoElGamalEncrypter encrypter = smallCryptoElGamalFactory.createEncrypter(keyPair.getPublicKeys());

		ElGamalCiphertext encryptedMessage = encrypter.encryptGroupElements(messages).getElGamalCiphertext();

		CryptoElGamalDecrypter decrypter = realCryptoElGamalFactory.createDecrypter(keyPair.getPrivateKeys());

		List<ZpGroupElement> decryptedMessage = decrypter.decrypt(encryptedMessage, true);

		Assertions.assertEquals(messages, decryptedMessage, "The decrypted message is not the original message.");
	}

	@Test
	void givenSmallGroupAndMessagesSmallerThanKeysWhenEncryptAndDecryptThenOriginalMessages() throws GeneralCryptoLibException {

		List<ZpGroupElement> messages = getMessagesToEncrypt(smallZpSubgroup, new BigInteger("4"), new BigInteger("4"), new BigInteger("8"),
				new BigInteger("4"));

		int numKeys = 6;

		ElGamalEncryptionParameters elGamalEncryptionParameters = new ElGamalEncryptionParameters(smallZpSubgroup.getP(), smallZpSubgroup.getQ(),
				smallZpSubgroup.getGenerator().getValue());

		ElGamalKeyPair keyPair = smallCryptoElGamalFactory.createCryptoElGamalKeyPairGenerator().generateKeys(elGamalEncryptionParameters, numKeys);

		CryptoElGamalEncrypter encrypter = smallCryptoElGamalFactory.createEncrypter(keyPair.getPublicKeys());

		ElGamalCiphertext encryptedMessage = encrypter.encryptGroupElements(messages).getElGamalCiphertext();

		CryptoElGamalDecrypter decrypter = realCryptoElGamalFactory.createDecrypter(keyPair.getPrivateKeys());

		List<ZpGroupElement> decryptedMessage = decrypter.decrypt(encryptedMessage, true);

		Assertions.assertEquals(messages, decryptedMessage, "The decrypted message is not the original message.");
	}

	@Test
	void givenRealGroupAndMessagesWhenGenerateKeysAndEncryptAndDecryptThenOriginalMessages() throws GeneralCryptoLibException {

		List<ZpGroupElement> messages = getMessagesToEncrypt(realZpSubgroup, new BigInteger("197"), new BigInteger("199"), new BigInteger("211"),
				new BigInteger("211"));

		int numKeys = messages.size();

		ElGamalEncryptionParameters elGamalEncryptionParameters = new ElGamalEncryptionParameters(realZpSubgroup.getP(), realZpSubgroup.getQ(),
				realZpSubgroup.getGenerator().getValue());

		ElGamalKeyPair keyPair = realCryptoElGamalFactory.createCryptoElGamalKeyPairGenerator().generateKeys(elGamalEncryptionParameters, numKeys);

		CryptoElGamalEncrypter encrypter = realCryptoElGamalFactory.createEncrypter(keyPair.getPublicKeys());

		ElGamalCiphertext encryptedMessage = encrypter.encryptGroupElements(messages).getElGamalCiphertext();

		CryptoElGamalDecrypter decrypter = realCryptoElGamalFactory.createDecrypter(keyPair.getPrivateKeys());

		List<ZpGroupElement> decryptedMessage = decrypter.decrypt(encryptedMessage, true);

		Assertions.assertEquals(messages, decryptedMessage, "The decrypted message is not the original message.");
	}

	@Test
	void givenRealGroupAndMessagesWhenGenerateKeysAndEncryptAndDecryptTwiceThenOriginalMessages() throws GeneralCryptoLibException {

		List<ZpGroupElement> messages = getMessagesToEncrypt(realZpSubgroup, new BigInteger("197"));

		ElGamalEncryptionParameters elGamalEncryptionParameters = new ElGamalEncryptionParameters(realZpSubgroup.getP(), realZpSubgroup.getQ(),
				realZpSubgroup.getGenerator().getValue());

		ElGamalKeyPair keyPair = realCryptoElGamalFactory.createCryptoElGamalKeyPairGenerator().generateKeys(elGamalEncryptionParameters, 3);

		CryptoElGamalEncrypter encrypter = realCryptoElGamalFactory.createEncrypter(keyPair.getPublicKeys());

		ElGamalCiphertext encryptedMessage = encrypter.encryptGroupElements(messages).getElGamalCiphertext();

		CryptoElGamalDecrypter decrypter = realCryptoElGamalFactory.createDecrypter(keyPair.getPrivateKeys());

		List<ZpGroupElement> decryptedMessage1 = decrypter.decrypt(encryptedMessage, true);

		List<ZpGroupElement> decryptedMessage2 = decrypter.decrypt(encryptedMessage, true);

		Assertions.assertEquals(decryptedMessage1, decryptedMessage2, "The decrypted messages dont match.");

		Assertions.assertEquals(messages, decryptedMessage1, "The decrypted message is not the original message.");

		Assertions.assertEquals(messages, decryptedMessage2, "The decrypted message is not the original message.");
	}

	@Test
	void givenRealGroupAndMessagesSmallerThanKeysWhenEncryptAndDecryptThenOriginalMessages() throws GeneralCryptoLibException {

		List<ZpGroupElement> messages = getMessagesToEncrypt(realZpSubgroup, new BigInteger("197"), new BigInteger("199"), new BigInteger("211"),
				new BigInteger("211"));

		int numKeys = 10;

		ElGamalEncryptionParameters elGamalEncryptionParameters = new ElGamalEncryptionParameters(realZpSubgroup.getP(), realZpSubgroup.getQ(),
				realZpSubgroup.getGenerator().getValue());

		ElGamalKeyPair keyPair = realCryptoElGamalFactory.createCryptoElGamalKeyPairGenerator().generateKeys(elGamalEncryptionParameters, numKeys);

		CryptoElGamalEncrypter encrypter = realCryptoElGamalFactory.createEncrypter(keyPair.getPublicKeys());

		ElGamalCiphertext encryptedMessage = encrypter.encryptGroupElements(messages).getElGamalCiphertext();

		CryptoElGamalDecrypter decrypter = realCryptoElGamalFactory.createDecrypter(keyPair.getPrivateKeys());

		List<ZpGroupElement> decryptedMessage = decrypter.decrypt(encryptedMessage, true);

		Assertions.assertEquals(messages, decryptedMessage, "The decrypted message is not the original message.");
	}

	@Test
	void givenSmallGroupAndKeysAndMessageWhenEncryptAndDecryptThenOriginalMessages() throws GeneralCryptoLibException {

		List<ZpGroupElement> messages = getMessagesToEncrypt(smallZpSubgroup, new BigInteger("4"), new BigInteger("4"));

		ElGamalCiphertext encryptedMessage = smallEncrypterHardcodedKeys.encryptGroupElements(messages).getElGamalCiphertext();

		List<ZpGroupElement> decryptedMessage = smallDecrypterHardcodedKeys.decrypt(encryptedMessage, true);

		Assertions.assertEquals(messages, decryptedMessage, "The decrypted message is not the original message.");
	}

	@Test
	void givenRealGroupAndKeysAndMessaWhenEncryptAndDecryptThenOriginalMessages() throws GeneralCryptoLibException {

		List<ZpGroupElement> messages = getMessagesToEncrypt(realZpSubgroup, new BigInteger("199"), new BigInteger("197"), new BigInteger("211"));

		ElGamalCiphertext encryptedMessage = realEncrypterHardcodedKeys.encryptGroupElements(messages).getElGamalCiphertext();

		List<ZpGroupElement> decryptedMessage = realDecrypterHardcodedKeys.decrypt(encryptedMessage, true);

		Assertions.assertEquals(messages, decryptedMessage, "The decrypted message is not the original message.");
	}

	@Test
	void testGivenMessageOneWhenEncryptTwiceThenCiphertextIsDifferent() throws GeneralCryptoLibException {

		List<ZpGroupElement> messages = createMessageWithAllElementsOne();

		ElGamalEncrypterValues encryptedValues1 = realEncrypterHardcodedKeys.encryptGroupElements(messages);

		ElGamalEncrypterValues encryptedValues2;
		do {
			encryptedValues2 = realEncrypterHardcodedKeys.encryptGroupElements(messages);
		} while (encryptedValues2.getR().equals(encryptedValues1.getR()));

		Assertions.assertNotEquals(encryptedValues1.getElements(), encryptedValues2.getElements());
	}

	@Test
	void testGivenMessageOneWhenEncryptMultipleTimesThenCiphertextIsDifferent() throws GeneralCryptoLibException {

		List<ZpGroupElement> messages = createMessageWithAllElementsOne();

		ElGamalCiphertext ciphertext1 = realEncrypterHardcodedKeys.encryptGroupElements(messages).getElGamalCiphertext();

		ElGamalCiphertext ciphertext2 = realEncrypterHardcodedKeys.encryptGroupElements(messages).getElGamalCiphertext();

		ElGamalCiphertext ciphertext3 = realEncrypterHardcodedKeys.encryptGroupElements(messages).getElGamalCiphertext();

		ElGamalCiphertext ciphertext4 = realEncrypterHardcodedKeys.encryptGroupElements(messages).getElGamalCiphertext();

		assertCiphertextNotEqual(ciphertext1, ciphertext2, ciphertext3, ciphertext4);
	}

	@Test
	void testGivenGammaPrePhisAsCiphertextWhenDecryptThenMessageOne() throws GeneralCryptoLibException {

		ElGamalCiphertext preComputeValues = realEncrypterHardcodedKeys.preCompute().getElGamalCiphertext();

		// build a ciphertext with gamma equal to the gamma from the
		// precomputation operation and the list of phis equal to the list of
		// prephis resulted from the precomputation
		ElGamalCiphertext ciphertext = new ElGamalCiphertext(preComputeValues.getGamma(), preComputeValues.getPhis());

		List<ZpGroupElement> decryptedMessage = realDecrypterHardcodedKeys.decrypt(ciphertext, true);
		for (ZpGroupElement element : decryptedMessage) {
			Assertions.assertEquals(element, realZpSubgroup.getIdentity(), "Not all elements in the decrypted message are equal to 1");
		}
	}

	@Test
	void testGivenMessageOneWhenEncryptWithPrecomputePhisArePrePhis() throws GeneralCryptoLibException {

		ElGamalEncrypterValues elGamalEncrypterValues = realEncrypterHardcodedKeys.preCompute();

		ElGamalCiphertext preComputeValues = elGamalEncrypterValues.getElGamalCiphertext();

		List<ZpGroupElement> messages = createMessageWithAllElementsOne();

		// encrypt messages equal to 1 using encryption with precompute
		ElGamalCiphertext ciphertext = realEncrypterHardcodedKeys.encryptGroupElements(messages, elGamalEncrypterValues).getElGamalCiphertext();

		Assertions.assertEquals(ciphertext.getGamma(), preComputeValues.getGamma(),
				"Gamma of the ciphertext is  not equal to gamma of the precomputation");
		Assertions.assertEquals(ciphertext.getPhis(), preComputeValues.getPhis(),
				"Phis of the ciphertext are not equal to prePhis of the precomputation");
	}

	@Test
	void testGivenMessageOneWhenEncryptWithPrecomputeTwiceThenCiphertextIsDifferent() throws GeneralCryptoLibException {

		List<ZpGroupElement> messages = createMessageWithAllElementsOne();

		ElGamalEncrypterValues elGamalEncrytorValues1 = realEncrypterHardcodedKeys.preCompute();
		ElGamalCiphertext ciphertext1 = realEncrypterHardcodedKeys.encryptGroupElements(messages, elGamalEncrytorValues1).getElGamalCiphertext();

		ElGamalEncrypterValues elGamalEncrytorValues2 = realEncrypterHardcodedKeys.preCompute();
		ElGamalCiphertext ciphertext2 = realEncrypterHardcodedKeys.encryptGroupElements(messages, elGamalEncrytorValues2).getElGamalCiphertext();

		assertCiphertextNotEqual(ciphertext1, ciphertext2);
	}

	@Test
	void testGivenMessageOneWhenEncryptWithPrecomputeMultipleTimesThenCiphertextIsDifferent() throws GeneralCryptoLibException {

		List<ZpGroupElement> messages = createMessageWithAllElementsOne();

		ElGamalEncrypterValues elGamalEncrytorValues1 = realEncrypterHardcodedKeys.preCompute();
		ElGamalCiphertext ciphertext1 = realEncrypterHardcodedKeys.encryptGroupElements(messages, elGamalEncrytorValues1).getElGamalCiphertext();

		ElGamalEncrypterValues elGamalEncrytorValues2 = realEncrypterHardcodedKeys.preCompute();
		ElGamalCiphertext ciphertext2 = realEncrypterHardcodedKeys.encryptGroupElements(messages, elGamalEncrytorValues2).getElGamalCiphertext();

		ElGamalEncrypterValues elGamalEncrytorValues3 = realEncrypterHardcodedKeys.preCompute();
		ElGamalCiphertext ciphertext3 = realEncrypterHardcodedKeys.encryptGroupElements(messages, elGamalEncrytorValues3).getElGamalCiphertext();

		ElGamalEncrypterValues elGamalEncrytorValues4 = realEncrypterHardcodedKeys.preCompute();
		ElGamalCiphertext ciphertext4 = realEncrypterHardcodedKeys.encryptGroupElements(messages, elGamalEncrytorValues4).getElGamalCiphertext();

		assertCiphertextNotEqual(ciphertext1, ciphertext2, ciphertext3, ciphertext4);
	}

	/**
	 * Encrypt a message m[] with a public key pubKey[]. Change the value of phi [1] to be phi [1]*2 mod p. Decrypt the ciphertext with the
	 * corresponding secret key privKey[]. Expected result should be a decrypted message md[] such that md [1]=m [1]*2 mod p and md[i]=m[i] for i
	 * greater or equal than 2.
	 *
	 * @throws GeneralCryptoLibException
	 */

	@Test
	void givenAMessageWhenModifyFirstPhiAfterEncryptThenDecryptedMessageIsAlsoModified() throws GeneralCryptoLibException {
		List<ZpGroupElement> messages = getMessagesToEncrypt(smallZpSubgroup, BigInteger.ONE, new BigInteger("2"));

		ElGamalCiphertext encryptedMessage = smallEncrypterHardcodedKeys.encryptGroupElements(messages).getElGamalCiphertext();

		ElGamalCiphertext changedEncription = changeEncryptedMessage(encryptedMessage, smallZpSubgroup);

		List<ZpGroupElement> decryptedUpdatedMessage = smallDecrypterHardcodedKeys.decrypt(changedEncription, true);

		List<ZpGroupElement> expectedMessages = getMessagesToEncrypt(smallZpSubgroup, new BigInteger("2"), new BigInteger("2"));

		Assertions.assertEquals(expectedMessages, decryptedUpdatedMessage, "The resulting message is not the expected one");
	}

	/**
	 * Encrypt a message m[] with a public key pubKey[], obtaining a ciphertext (gamma, phi[]). Encrypt a message mprime[] such that all values are 1
	 * with the same public key, obtaining another ciphertext (gammaprime, phiprime[]). Construct a new ciphertext (newGamma, newPhi[]) defined by
	 * newGamma=gamma*gammaprime mod p and newPhi[i]=phi[i]*phiprime[i] mod p for all i. Decrypt the ciphertext (newGamma, newPhi[]) by using the
	 * corresponding secret key. Expected result should be a ciphertext md[] such that md[]=m[].
	 *
	 * @throws GeneralCryptoLibException
	 */
	@Test
	void givenTwoEncryptedMessageWhenMultipliedThenResultAsExpected() throws GeneralCryptoLibException {
		List<ZpGroupElement> messages = getMessagesToEncrypt(smallZpSubgroup, BigInteger.ONE, new BigInteger("2"));
		List<ZpGroupElement> messagePrime = getMessagesToEncrypt(smallZpSubgroup, BigInteger.ONE, BigInteger.ONE);

		ElGamalCiphertext encryptedMessage = smallEncrypterHardcodedKeys.encryptGroupElements(messages).getElGamalCiphertext();

		ElGamalCiphertext encryptedMessagePrime = smallEncrypterHardcodedKeys.encryptGroupElements(messagePrime).getElGamalCiphertext();

		ElGamalCiphertext multiplied = getNewEncryptedMessageFromMultiplying(encryptedMessage, encryptedMessagePrime);

		List<ZpGroupElement> decryptResult = smallDecrypterHardcodedKeys.decrypt(multiplied, true);

		Assertions.assertEquals(messages, decryptResult, "The decrypted message is not the expected one");
	}

	/**
	 * Encrypt a message m[] with a public key pubKey[]. Change phi[i] for 1 for all values of i and change gamma for g. Then decrypt the changed
	 * ciphertext. Expected result should be a decrypted message md[] such that md[i]=pubKey[i].modInverse(p).
	 *
	 * @throws GeneralCryptoLibException
	 */
	@Test
	void givenSmallMessageWhenEncryptedAndChangedAllPhisThenDecryptedMessageAsExpected() throws GeneralCryptoLibException {

		List<ZpGroupElement> messages = getMessagesToEncrypt(smallZpSubgroup, BigInteger.ONE, new BigInteger("2"));

		ElGamalCiphertext encryptedMessage = smallEncrypterHardcodedKeys.encryptGroupElements(messages).getElGamalCiphertext();

		BigInteger newGamma = smallG;
		BigInteger newPhis = BigInteger.ONE;
		ElGamalCiphertext updated = changeGammaAndPhis(newGamma, newPhis, encryptedMessage.getPhis().size(), smallZpSubgroup);

		List<ZpGroupElement> decryptedUpdatedMessage = smallDecrypterHardcodedKeys.decrypt(updated, true);

		for (int i = 0; i < messages.size(); i++) {
			Assertions.assertEquals(decryptedUpdatedMessage.get(i).getValue(), smallPublicKeysHardcoded.get(i).getValue().modInverse(smallP), "");
		}
	}

	/**
	 * Encrypt a message m[] with a public key pubKey[]. Change phi[i] for 1 for all values of i and change gamma for g. Then decrypt the changed
	 * ciphertext. Expected result should be a decrypted message md[] such that md[i]=pubKey[i].modInverse(p).
	 *
	 * @throws GeneralCryptoLibException
	 */
	@Test
	void givenRealMessageWhenEncryptedAndChangedAllPhisThenDecryptedMessageAsExpected() throws GeneralCryptoLibException {

		List<ZpGroupElement> messages = getMessagesToEncrypt(realZpSubgroup, new BigInteger("197"), new BigInteger("199"), new BigInteger("211"));

		ElGamalCiphertext encryptedMessage = realEncrypterHardcodedKeys.encryptGroupElements(messages).getElGamalCiphertext();

		BigInteger newGamma = realG;
		BigInteger newPhis = BigInteger.ONE;
		ElGamalCiphertext updated = changeGammaAndPhis(newGamma, newPhis, encryptedMessage.getPhis().size(), realZpSubgroup);

		List<ZpGroupElement> decryptedUpdatedMessage = realDecrypterHardcodedKeys.decrypt(updated, true);

		for (int i = 0; i < messages.size(); i++) {
			Assertions.assertEquals(decryptedUpdatedMessage.get(i).getValue(), realPublicKeysHardcoded.get(i).getValue().modInverse(realP), "");
		}
	}

	@Test
	void givenKeyPairFromJsonWhenEncryptAndDecryptThenOriginalMessages() throws GeneralCryptoLibException {

		List<ZpGroupElement> messages = getMessagesToEncrypt(smallZpSubgroup, new BigInteger("4"), new BigInteger("4"), new BigInteger("8"),
				new BigInteger("4"));

		int numKeys = messages.size();

		ElGamalEncryptionParameters elGamalEncryptionParameters = new ElGamalEncryptionParameters(smallZpSubgroup.getP(), smallZpSubgroup.getQ(),
				smallZpSubgroup.getGenerator().getValue());

		ElGamalKeyPair keyPair = smallCryptoElGamalFactory.createCryptoElGamalKeyPairGenerator().generateKeys(elGamalEncryptionParameters, numKeys);

		String publicKeyAsJson = keyPair.getPublicKeys().toJson();

		ElGamalPublicKey reconstructedPublicKey = ElGamalPublicKey.fromJson(publicKeyAsJson);

		CryptoElGamalEncrypter encrypter = smallCryptoElGamalFactory.createEncrypter(reconstructedPublicKey);

		ElGamalCiphertext encryptedMessage = encrypter.encryptGroupElements(messages).getElGamalCiphertext();

		String privateKeyAsJson = keyPair.getPrivateKeys().toJson();

		ElGamalPrivateKey reconstructedPrivateKey = ElGamalPrivateKey.fromJson(privateKeyAsJson);

		CryptoElGamalDecrypter decrypter = realCryptoElGamalFactory.createDecrypter(reconstructedPrivateKey);

		List<ZpGroupElement> decryptedMessage = decrypter.decrypt(encryptedMessage, true);

		Assertions.assertEquals(messages, decryptedMessage, "The decrypted message is not the original message.");
	}

	/**
	 * @param newGamma new value for the gamma
	 * @param newPhis  new value for all the phis
	 * @param phisSize the original encrypted message
	 * @return a new CompactElGamalEncryptionValues with new values for the gamma and all the phis
	 * @throws GeneralCryptoLibException
	 */
	private ElGamalCiphertext changeGammaAndPhis(final BigInteger newGamma, final BigInteger newPhis, final int phisSize, final ZpSubgroup group)
			throws GeneralCryptoLibException {

		List<ZpGroupElement> phis = new ArrayList<>(phisSize);
		ZpGroupElement gamma = new ZpGroupElement(newGamma, group);

		for (int i = 0; i < phisSize; i++) {
			phis.add(new ZpGroupElement(newPhis, group));
		}

		return new ElGamalCiphertext(gamma, phis);
	}

	/**
	 * Changes the first phi element of the list of phis for the encrypted message. The modification consists in changing the value of {@code phi[1]}
	 * to be {@code phi [1]*2 mod p}
	 *
	 * @param encryptedMessage the encrypted message to modify
	 * @throws GeneralCryptoLibException
	 */
	private ElGamalCiphertext changeEncryptedMessage(final ElGamalCiphertext encryptedMessage, final ZpSubgroup group)
			throws GeneralCryptoLibException {

		GroupElement two = new ZpGroupElement(new BigInteger("2"), group);

		List<ZpGroupElement> phis = new ArrayList<>(encryptedMessage.getPhis().size());

		ZpGroupElement newPhi = encryptedMessage.getPhis().get(0).multiply(two);

		ZpGroupElement element;

		for (int i = 0; i < encryptedMessage.getPhis().size(); i++) {
			if (i == 0) {
				phis.add(newPhi);
			} else {
				element = encryptedMessage.getPhis().get(i);
				phis.add(element);
			}
		}

		return new ElGamalCiphertext(encryptedMessage.getGamma(), phis);
	}

	private List<ZpGroupElement> getMessagesToEncrypt(final ZpSubgroup group, final BigInteger... messages) throws GeneralCryptoLibException {

		ZpGroupElement element;
		List<ZpGroupElement> result = new ArrayList<>(3);

		for (BigInteger message : messages) {
			element = new ZpGroupElement(message, group);
			result.add(element);
		}

		return result;
	}

	/**
	 * Construct a new ciphertext (newGamma, newPhi[]) defined by newGamma=gamma*gammaprime mod p and newPhi[i]=phi[i]*phiprime[i] mod p for all i.
	 *
	 * @throws GeneralCryptoLibException
	 */
	private ElGamalCiphertext getNewEncryptedMessageFromMultiplying(final ElGamalCiphertext encMessage1, final ElGamalCiphertext encMessage2)
			throws GeneralCryptoLibException {

		ZpGroupElement gamma;
		List<ZpGroupElement> phis = new ArrayList<>(encMessage1.getPhis().size());

		gamma = encMessage1.getGamma().multiply(encMessage2.getGamma());

		for (int i = 0; i < encMessage1.getPhis().size(); i++) {
			phis.add(encMessage1.getPhis().get(i).multiply(encMessage2.getPhis().get(i)));
		}

		return new ElGamalCiphertext(gamma, phis);
	}

	/*
	 * @return a list of messages where each element is equal to identity
	 * element of the group
	 */
	private List<ZpGroupElement> createMessageWithAllElementsOne() {

		List<ZpGroupElement> messages = new ArrayList<>(realPublicKeysHardcoded.size());
		for (int i = 0; i < realPublicKeysHardcoded.size(); i++) {
			messages.add(realZpSubgroup.getIdentity());
		}
		return messages;
	}

	/*
	 * Compares several ciphertext with each other regarding gamma and phis to
	 * test elements equality
	 */
	private void assertCiphertextNotEqual(final ElGamalCiphertext... ciphertext) {

		for (int i = 0; i < ciphertext.length; i++) {
			for (int j = i + 1; j < ciphertext.length; j++) {
				Assertions.assertNotEquals(ciphertext[i].getGamma(), ciphertext[j].getGamma(),
						String.format("Gamma from ciphertext %d has the same value as gamma from ciphertext %d", i, j));
				Assertions.assertNotEquals(ciphertext[i].getPhis(), ciphertext[j].getPhis(),
						String.format("Phis from ciphertext %d has the same value as phis from ciphertext %d", i, j));
			}
		}
	}
}
