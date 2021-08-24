/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.voters.datapacks.generators;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.extendedkeystore.KeyStoreService;
import ch.post.it.evoting.cryptolib.api.securerandom.CryptoAPIRandomString;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateParameters;
import ch.post.it.evoting.cryptolib.certificates.bean.CredentialProperties;
import ch.post.it.evoting.cryptolib.certificates.factory.X509CertificateGenerator;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;
import ch.post.it.evoting.cryptolib.extendedkeystore.service.ExtendedKeyStoreService;
import ch.post.it.evoting.domain.election.helpers.ReplacementsHolder;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VerificationCardCredentialDataPack;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VerificationCardCredentialInputDataPack;

@ExtendWith(MockitoExtension.class)
class VerificationCardCredentialDataPackGeneratorTest {
	private static final String ELECTION_EVENT_ID = "electionEventId";
	private static final String VERIFICATION_CARD_ID = "verificationCardId";
	private static final String VERIFICATION_CARD_SET_ID = "verificationCardSetId";
	private static final String KEYSTORE_SYMMETRIC_ENCRYPTION_KEY = "keystoreSymmetricEncryptionKey";

	private static AsymmetricService asymmetricService;

	private final KeyStoreService storesService = new ExtendedKeyStoreService();
	private final ElGamalServiceAPI elGamalService = new ElGamalService();

	private VerificationCardCredentialDataPackGenerator verificationCardCredentialDataPackGenerator;

	@Mock
	private X509CertificateGenerator certificateGenerator;

	@Mock
	private CryptoAPIRandomString cryptoRandomString;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException, IOException {
		asymmetricService = new AsymmetricService();

		generateVerificationKeyPairFile();
	}

	@AfterAll
	public static void cleanup() throws IOException {
		FileUtils.deleteDirectory(Paths.get("").resolve(Constants.CONFIG_DIR_NAME_OFFLINE).toFile());
	}

	private static void generateVerificationKeyPairFile() throws GeneralCryptoLibException, IOException {

		final BigInteger p = new BigInteger(
				"25515082852221325227734875679796454760326467690112538918409444238866830264288928368643860210692030230970372642053699673880830938513755311613746769767735066124931265104230246714327140720231537205767076779634365989939295710998787785801877310580401262530818848712843191597770750843711630250668056368624192328749556025449493888902777252341817892959006585132698115406972938429732386814317498812002229915393331703423250137659204137625584559844531972832055617091033311878843608854983169553055109029654797488332746885443611918764277292979134833642098989040604523427961162591459163821790507259475762650859921432844527734894939");
		final BigInteger q = new BigInteger(
				"12757541426110662613867437839898227380163233845056269459204722119433415132144464184321930105346015115485186321026849836940415469256877655806873384883867533062465632552115123357163570360115768602883538389817182994969647855499393892900938655290200631265409424356421595798885375421855815125334028184312096164374778012724746944451388626170908946479503292566349057703486469214866193407158749406001114957696665851711625068829602068812792279922265986416027808545516655939421804427491584776527554514827398744166373442721805959382138646489567416821049494520302261713980581295729581910895253629737881325429960716422263867447469");
		final BigInteger g = new BigInteger("3");

		final ElGamalEncryptionParameters elGamalEncryptionParameters = new ElGamalEncryptionParameters(p, q, g);
		final ElGamalKeyPair keyPair = new ElGamalService().generateKeyPair(elGamalEncryptionParameters, 1);

		Path verificationCardsKeyPairsFilePath = Paths.get("").resolve(Constants.CONFIG_DIR_NAME_OFFLINE)
				.resolve(Constants.CONFIG_VERIFICATION_CARDS_KEY_PAIR_DIRECTORY).resolve(VERIFICATION_CARD_SET_ID);

		verificationCardsKeyPairsFilePath.toFile().mkdirs();

		verificationCardsKeyPairsFilePath = verificationCardsKeyPairsFilePath.resolve(VERIFICATION_CARD_ID + Constants.KEY);

		Files.write(verificationCardsKeyPairsFilePath,
				(keyPair.getPrivateKeys().toJson() + System.lineSeparator() + keyPair.getPublicKeys().toJson()).getBytes(StandardCharsets.UTF_8));
	}

	@BeforeEach
	void init() {
		verificationCardCredentialDataPackGenerator = new VerificationCardCredentialDataPackGenerator(asymmetricService, cryptoRandomString,
				certificateGenerator, storesService, elGamalService);
	}

	@Test
	void generateCredentialHappyPath() {

		final VerificationCardCredentialInputDataPack inputDataPack = createInputDataPack();
		final PrivateKey verificationCardSetIssuerPrivateKey = asymmetricService.getKeyPairForSigning().getPrivate();

		assertDoesNotThrow(() -> verificationCardCredentialDataPackGenerator
				.generate(inputDataPack, ELECTION_EVENT_ID, VERIFICATION_CARD_ID, VERIFICATION_CARD_SET_ID, verificationCardSetIssuerPrivateKey,
						KEYSTORE_SYMMETRIC_ENCRYPTION_KEY.toCharArray(), Paths.get("")));
	}

	@Test
	void generateCorrectVerificationCardSignature() throws GeneralCryptoLibException {

		final KeyPair keyPairForSigning = asymmetricService.getKeyPairForSigning();
		final PrivateKey verificationCardSetIssuerPrivateKey = keyPairForSigning.getPrivate();

		final VerificationCardCredentialDataPack dataPack = verificationCardCredentialDataPackGenerator
				.generate(createInputDataPack(), ELECTION_EVENT_ID, VERIFICATION_CARD_ID, VERIFICATION_CARD_SET_ID,
						verificationCardSetIssuerPrivateKey, KEYSTORE_SYMMETRIC_ENCRYPTION_KEY.toCharArray(), Paths.get(""));

		final byte[] signature = dataPack.getSignatureVCardPubKeyEEIDVCID();
		final PublicKey publicKeyFromSigningKeyPair = keyPairForSigning.getPublic();

		final byte[] eeidAsBytes = ELECTION_EVENT_ID.getBytes(StandardCharsets.UTF_8);
		final byte[] verificationCardIDAsBytes = VERIFICATION_CARD_ID.getBytes(StandardCharsets.UTF_8);
		final byte[] publicKeyBytesFromVerificationCardKeyPair = dataPack.getVerificationCardKeyPair().getPublicKeys().toJson()
				.getBytes(StandardCharsets.UTF_8);

		assertTrue(asymmetricService.verifySignature(signature, publicKeyFromSigningKeyPair, publicKeyBytesFromVerificationCardKeyPair, eeidAsBytes,
				verificationCardIDAsBytes));
	}

	@Test
	void publicKeyAsJsonAlwaysReturnsSameValue() throws GeneralCryptoLibException {

		final BigInteger p = new BigInteger(
				"25515082852221325227734875679796454760326467690112538918409444238866830264288928368643860210692030230970372642053699673880830938513755311613746769767735066124931265104230246714327140720231537205767076779634365989939295710998787785801877310580401262530818848712843191597770750843711630250668056368624192328749556025449493888902777252341817892959006585132698115406972938429732386814317498812002229915393331703423250137659204137625584559844531972832055617091033311878843608854983169553055109029654797488332746885443611918764277292979134833642098989040604523427961162591459163821790507259475762650859921432844527734894939");
		final BigInteger q = new BigInteger(
				"12757541426110662613867437839898227380163233845056269459204722119433415132144464184321930105346015115485186321026849836940415469256877655806873384883867533062465632552115123357163570360115768602883538389817182994969647855499393892900938655290200631265409424356421595798885375421855815125334028184312096164374778012724746944451388626170908946479503292566349057703486469214866193407158749406001114957696665851711625068829602068812792279922265986416027808545516655939421804427491584776527554514827398744166373442721805959382138646489567416821049494520302261713980581295729581910895253629737881325429960716422263867447469");
		final BigInteger g = new BigInteger("3");

		final Set<String> jsons = new HashSet<>(1);
		final ElGamalKeyPair keyPair = new ElGamalService().generateKeyPair(new ElGamalEncryptionParameters(p, q, g), 1);

		for (int i = 0; i < 1000; i++) {
			final String json = keyPair.getPublicKeys().toJson();
			jsons.add(json);

			final ElGamalPublicKey pb = ElGamalPublicKey.fromJson(json);
			jsons.add(pb.toJson());
		}

		assertEquals(1, jsons.size());
	}

	private VerificationCardCredentialInputDataPack createInputDataPack() {

		final CredentialProperties credentialProperties = new CredentialProperties();
		credentialProperties.setAlias(new LinkedHashMap<>(1));
		credentialProperties.getAlias().put("privateKey", "elgamalprivatekey");
		credentialProperties.setCredentialType(CertificateParameters.Type.SIGN);
		credentialProperties.setName("verificationCardSet");
		credentialProperties.setParentName("servicesca");
		credentialProperties.setPropertiesFile("properties/verificationCardSetX509Certificate.properties");

		final VerificationCardCredentialInputDataPack inputDataPack = new VerificationCardCredentialInputDataPack(credentialProperties);
		inputDataPack.setParentKeyPair(asymmetricService.getKeyPairForSigning());
		inputDataPack.setEeid(ELECTION_EVENT_ID);

		final ReplacementsHolder replacementsHolder = new ReplacementsHolder(ELECTION_EVENT_ID);
		inputDataPack.setReplacementsHolder(replacementsHolder);

		final ZonedDateTime startValidityPeriod = ZonedDateTime.now(ZoneOffset.UTC);
		final ZonedDateTime endValidityPeriod = startValidityPeriod.plusYears(2);
		inputDataPack.setStartDate(startValidityPeriod);
		inputDataPack.setEndDate(endValidityPeriod);

		return inputDataPack;
	}
}
