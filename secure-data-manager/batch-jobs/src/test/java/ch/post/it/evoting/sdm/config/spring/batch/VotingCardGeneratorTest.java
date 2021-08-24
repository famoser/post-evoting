/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.derivation.CryptoAPIPBKDFDeriver;
import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.codec.ElGamalCiphertextCodec;
import ch.post.it.evoting.cryptolib.elgamal.codec.ElGamalCiphertextCodecImpl;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.CryptoAPIElGamalEncrypter;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.returncode.CodesMappingTableEntry;
import ch.post.it.evoting.cryptolib.returncode.VoterCodesService;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.domain.cryptoadapters.CryptoAdapters;
import ch.post.it.evoting.domain.election.EncryptionParameters;
import ch.post.it.evoting.logging.api.factory.LoggingFactory;
import ch.post.it.evoting.logging.core.factory.LoggingFactoryLog4j;
import ch.post.it.evoting.logging.core.formatter.PipeSeparatedFormatter;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.commons.domain.VcIdCombinedReturnCodesGenerationValues;
import ch.post.it.evoting.sdm.config.actions.ExtendedAuthenticationService;
import ch.post.it.evoting.sdm.config.commands.voters.JobExecutionObjectContext;
import ch.post.it.evoting.sdm.config.commands.voters.VotersGenerationTaskStaticContentProvider;
import ch.post.it.evoting.sdm.config.commands.voters.VotersParametersHolder;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VerificationCardCredentialDataPack;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VerificationCardSetCredentialDataPack;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VotingCardCredentialDataPack;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VotingCardCredentialInputDataPack;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.generators.VerificationCardCredentialDataPackGenerator;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.generators.VotingCardCredentialDataPackGenerator;
import ch.post.it.evoting.sdm.config.model.authentication.ExtendedAuthInformation;
import ch.post.it.evoting.sdm.config.model.authentication.service.StartVotingKeyService;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
class VotingCardGeneratorTest {
	private static final String P = "25515082852221325227734875679796454760326467690112538918409444238866830264288928368643860210692030230970372642053699673880830938513755311613746769767735066124931265104230246714327140720231537205767076779634365989939295710998787785801877310580401262530818848712843191597770750843711630250668056368624192328749556025449493888902777252341817892959006585132698115406972938429732386814317498812002229915393331703423250137659204137625584559844531972832055617091033311878843608854983169553055109029654797488332746885443611918764277292979134833642098989040604523427961162591459163821790507259475762650859921432844527734894939";

	private static final String Q = "12757541426110662613867437839898227380163233845056269459204722119433415132144464184321930105346015115485186321026849836940415469256877655806873384883867533062465632552115123357163570360115768602883538389817182994969647855499393892900938655290200631265409424356421595798885375421855815125334028184312096164374778012724746944451388626170908946479503292566349057703486469214866193407158749406001114957696665851711625068829602068812792279922265986416027808545516655939421804427491584776527554514827398744166373442721805959382138646489567416821049494520302261713980581295729581910895253629737881325429960716422263867447469";

	private static final String G = "3";

	@Autowired
	VotingCardGenerator votingCardGenerator;

	@Autowired
	JobExecutionObjectContext jobExecutionContextMock;

	@Autowired
	VotingCardGenerationJobExecutionContext jobContext;

	@Autowired
	VoterCodesService voterCodesService;

	@Autowired
	ExtendedAuthenticationService extendedAuthenticationServiceMock;

	@Autowired
	StartVotingKeyService startVotingKeyServiceMock;

	@Autowired
	VotingCardCredentialDataPackGenerator votingCardCredentialDataPackGeneratorMock;

	@Autowired
	VerificationCardCredentialDataPackGenerator verificationCardCredentialDataPackGeneratorMock;

	@Autowired
	VotersParametersHolder parametersHolderMock;

	@Autowired
	VcIdCombinedReturnCodesGenerationValues vcIdCombinedReturnCodesGenerationValuesMock;

	@Autowired
	VotingCardGenerationJobExecutionContext cardGenerationJobExecutionContextMock;

	@Autowired
	CryptoAPIPBKDFDeriver cryptoAPIPBKDFDeriverMock;

	@Autowired
	VotersGenerationTaskStaticContentProvider staticContentProviderMock;

	@Autowired
	@Qualifier("asymmetricServiceWithJobScope")
	AsymmetricServiceAPI asymmetricServiceWithJobScope;

	@Autowired
	ElGamalServiceAPI elGamalService;

	@Autowired
	ElGamalCiphertextCodec codec;

	@Autowired
	ElGamalKeyPair secureDataMangerKeyPair;

	@Autowired
	PathResolver pathResolver;

	@Test
	void generatesVotingCardOutput() throws Exception {

		// given
		setupMocks();

		// when
		final GeneratedVotingCardOutput output = votingCardGenerator.process(vcIdCombinedReturnCodesGenerationValuesMock);

		// then
		assertNotNull(output);
		assertNotNull(output.getBallotBoxId());
		assertNotNull(output.getBallotId());
		assertNotNull(output.getCredentialId());
		assertNotNull(output.getElectionEventId());
		assertNull(output.getError());
		assertNotNull(output.getExtendedAuthInformation());
		assertNotNull(output.getStartVotingKey());
		assertNotNull(output.getVerificationCardCodesDataPack());
		assertNotNull(output.getVerificationCardCredentialDataPack());
		assertNotNull(output.getVerificationCardId());
		assertNotNull(output.getVoterCredentialDataPack());
		assertNotNull(output.getVotingCardId());
		assertNotNull(output.getVotingCardSetId());
		assertEquals(Constants.NUM_DIGITS_BALLOT_CASTING_KEY, output.getVerificationCardCodesDataPack().getBallotCastingKey().length());

	}

	@Test
	void retrieveBallotCastingKeyTest() throws URISyntaxException {

		final Path basePath = Paths.get(VotingCardGeneratorTest.class.getClassLoader().getResource("votingCardGenerator/").toURI())
				.resolve("electionEventId");

		assertAll(() -> assertEquals("ballotCastingKey1",
				VotingCardGenerator.retrieveBallotCastingKey("verificationCardSetId", "verificationCardId1", basePath)),
				() -> assertEquals("ballotCastingKey2",
						VotingCardGenerator.retrieveBallotCastingKey("verificationCardSetId", "verificationCardId2", basePath)),
				() -> assertEquals("ballotCastingKey3",
						VotingCardGenerator.retrieveBallotCastingKey("verificationCardSetId", "verificationCardId3", basePath)));
	}

	private void setupMocks() throws Exception {
		VerificationCardSetCredentialDataPack verificationCardSetCredentialDataPack = mock(VerificationCardSetCredentialDataPack.class);

		KeyPair keyPair = asymmetricServiceWithJobScope.getKeyPairForSigning();
		when(verificationCardSetCredentialDataPack.getVerificationCardSetIssuerKeyPair()).thenReturn(keyPair);
		when(jobExecutionContextMock.get(anyString(), eq(VerificationCardSetCredentialDataPack.class)))
				.thenReturn(verificationCardSetCredentialDataPack);

		// ---
		when(voterCodesService.generateShortVoteCastReturnCode()).thenReturn("1");
		when(voterCodesService.generateShortChoiceReturnCode()).thenReturn("1");
		when(voterCodesService.generateLongReturnCode(any(), any(), any(), any())).thenReturn(new byte[] {});
		when(voterCodesService.generateCodesMappingTableEntry(any(), any()))
				.thenReturn(new CodesMappingTableEntry(new byte[] { 0 }, new byte[] { 1 }))
				.thenReturn(new CodesMappingTableEntry(new byte[] { 1 }, new byte[] { 1 }));

		// --
		when(extendedAuthenticationServiceMock.create(any(), any())).thenReturn(mock(ExtendedAuthInformation.class));

		// --
		when(startVotingKeyServiceMock.generateStartVotingKey()).thenReturn("1");

		// --
		when(votingCardCredentialDataPackGeneratorMock
				.generate(any(), any(), any(), anyString(), any(), any(), any(), any(CryptoAPIX509Certificate.class)))
				.thenReturn(mock(VotingCardCredentialDataPack.class));

		// --
		ElGamalEncryptionParameters params = new ElGamalEncryptionParameters(new BigInteger(P), new BigInteger(Q), new BigInteger(G));
		ElGamalKeyPair elGamalKeyPair = new ElGamalService().generateKeyPair(params, 1);
		VerificationCardCredentialDataPack verificationCardCredentialDataPack = mock(VerificationCardCredentialDataPack.class);
		EncryptionParameters encryptionParameters = new EncryptionParameters(P, Q, G);
		when(verificationCardCredentialDataPack.getVerificationCardKeyPair()).thenReturn(elGamalKeyPair);
		when(verificationCardCredentialDataPackGeneratorMock.generate(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(verificationCardCredentialDataPack);

		// --
		when(parametersHolderMock.getVotingCardCredentialInputDataPack()).thenReturn(mock(VotingCardCredentialInputDataPack.class));
		when(parametersHolderMock.getVotingCardCredentialInputDataPack().getEeid()).thenReturn("1");
		when(parametersHolderMock.getBallot().getElectionEvent().getId()).thenReturn("1");
		when(parametersHolderMock.getBallot().getEncodedVotingOptions()).thenReturn(Collections.singletonList(BigInteger.ONE));
		when(parametersHolderMock.getEncryptionParameters()).thenReturn(encryptionParameters);
		when(parametersHolderMock.getAbsoluteBasePath())
				.thenReturn(Paths.get(VotingCardGeneratorTest.class.getClassLoader().getResource("votingCardGenerator/").toURI()).resolve("1"));

		when(vcIdCombinedReturnCodesGenerationValuesMock.getVerificationCardId()).thenReturn("1");

		ZpSubgroup group = secureDataMangerKeyPair.getPrivateKeys().getGroup();
		ZpGroupElement powerOfPrime = new ZpGroupElement(BigInteger.valueOf(4), group);
		CryptoAPIElGamalEncrypter encrypter = elGamalService.createEncrypter(secureDataMangerKeyPair.getPublicKeys());
		ElGamalCiphertext value = encrypter.encryptGroupElements(singletonList(powerOfPrime)).getElGamalCiphertext();

		// setupSecretKey has been constructed from secureDataMangerKeyPair.getPrivateKeys().
		final Path setupKeyPath = Paths.get(VotingCardGeneratorTest.class.getResource("/setupSecretKey.json").toURI());
		when(pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, "1", Constants.CONFIG_DIR_NAME_OFFLINE, Constants.SETUP_SECRET_KEY_FILE_NAME))
				.thenReturn(setupKeyPath);

		final ElGamalMultiRecipientCiphertext preChoiceReturnCodes = CryptoAdapters.convert(value, group);

		when(vcIdCombinedReturnCodesGenerationValuesMock.getEncryptedPreChoiceReturnCodes()).thenReturn(preChoiceReturnCodes);

		ZpGroupElement computedBck = new ZpGroupElement(BigInteger.valueOf(4), group);
		ElGamalCiphertext encryptedComputedBck = encrypter.encryptGroupElements(singletonList(computedBck)).getElGamalCiphertext();
		final ElGamalMultiRecipientCiphertext castReturnCode = CryptoAdapters.convert(encryptedComputedBck, group);
		when(vcIdCombinedReturnCodesGenerationValuesMock.getEncryptedPreVoteCastReturnCode()).thenReturn(castReturnCode);

		// --
		when(cardGenerationJobExecutionContextMock.getVerificationCardSetId()).thenReturn("1");
		when(cardGenerationJobExecutionContextMock.getVotingCardSetId()).thenReturn("1");
		when(cardGenerationJobExecutionContextMock.getNumberOfVotingCards()).thenReturn(1);
		when(cardGenerationJobExecutionContextMock.getSaltKeystoreSymmetricEncryptionKey()).thenReturn("11");
		when(cardGenerationJobExecutionContextMock.getSaltCredentialId()).thenReturn("11");
		when(cardGenerationJobExecutionContextMock.getBallotId()).thenReturn("1");
		when(cardGenerationJobExecutionContextMock.getBallotBoxId()).thenReturn("1");
		when(cardGenerationJobExecutionContextMock.getVotingCardSetId()).thenReturn("1");
		when(cardGenerationJobExecutionContextMock.getJobInstanceId()).thenReturn("1");
		when(cardGenerationJobExecutionContextMock.getElectionEventId()).thenReturn("1");

		// --
		when(cryptoAPIPBKDFDeriverMock.deriveKey(any(), any())).thenReturn("1"::getBytes);

		// --
		when(staticContentProviderMock.getZpsubgroup()).thenReturn(new ZpSubgroup(new BigInteger(G), new BigInteger(P), new BigInteger(Q)));
		when(staticContentProviderMock.getRepresentationsWithCorrectness())
				.thenReturn(singletonMap(BigInteger.valueOf(2), singletonList("correctness")));

	}

	@Configuration
	@Import(TestConfigServices.class)
	static class PrivateConfiguration {

		@Bean
		JobExecutionObjectContext executionObjectContext(AsymmetricServiceAPI asymmetricService) {
			return mock(JobExecutionObjectContext.class);
		}

		@Bean
		VoterCodesService codesGenerator() {
			return mock(VoterCodesService.class);
		}

		@Bean
		ExtendedAuthenticationService extendedAuthenticationService() {
			return mock(ExtendedAuthenticationService.class);
		}

		@Bean
		StartVotingKeyService startVotingKeyService() {
			return mock(StartVotingKeyService.class);
		}

		@Bean
		VotingCardCredentialDataPackGenerator votingCardCredentialDataPackGenerator() {
			return mock(VotingCardCredentialDataPackGenerator.class);
		}

		@Bean("verificationCardCredentialDataPackGeneratorWithJobScope")
		VerificationCardCredentialDataPackGenerator verificationCardCredentialDataPackGenerator() {
			return mock(VerificationCardCredentialDataPackGenerator.class);
		}

		@Bean("asymmetricServiceWithJobScope")
		AsymmetricServiceAPI asymmetricServiceAPI() {
			return new AsymmetricService();
		}

		@Bean
		VotersParametersHolder holder() {
			return mock(VotersParametersHolder.class, RETURNS_DEEP_STUBS);
		}

		@Bean
		VcIdCombinedReturnCodesGenerationValues verificationCardIdComputedValues() {
			return mock(VcIdCombinedReturnCodesGenerationValues.class);
		}

		@Bean
		VotingCardGenerationJobExecutionContext jobContext(ElGamalKeyPair keys) {
			final VotingCardGenerationJobExecutionContext context = mock(VotingCardGenerationJobExecutionContext.class);
			when(context.getVerificationCardSetId()).thenReturn("1");
			when(context.getVotingCardSetId()).thenReturn("1");
			return context;
		}

		@Bean
		ElGamalKeyPair secureDataManagerKeyPair(ElGamalServiceAPI elGamalService) throws GeneralCryptoLibException {
			ElGamalEncryptionParameters parameters = new ElGamalEncryptionParameters(new BigInteger(VotingCardGeneratorTest.P),
					new BigInteger(VotingCardGeneratorTest.Q), new BigInteger(VotingCardGeneratorTest.G));
			return elGamalService.generateKeyPair(parameters, 1);
		}

		@Bean
		CryptoAPIPBKDFDeriver deriver() {
			return mock(CryptoAPIPBKDFDeriver.class);
		}

		@Bean
		VotersGenerationTaskStaticContentProvider contentProvider() {
			return mock(VotersGenerationTaskStaticContentProvider.class);
		}

		@Bean
		ElGamalServiceAPI elGamalService() {
			return new ElGamalService();
		}

		@Bean
		ElGamalCiphertextCodec elGamalCiphertextCodec() {
			return ElGamalCiphertextCodecImpl.getInstance();
		}

		@Bean
		VotingCardGenerator votingCardGenerator(VotersParametersHolder holder, VotingCardGenerationJobExecutionContext jobContext,
				CryptoAPIPBKDFDeriver deriver, VotersGenerationTaskStaticContentProvider contentProvider) {
			return new VotingCardGenerator(holder, jobContext, deriver, contentProvider);
		}

		@Bean
		public LoggingFactory loggingFactory() {
			PipeSeparatedFormatter pipeSeparatedFormatterMock = mock(PipeSeparatedFormatter.class);

			when(pipeSeparatedFormatterMock.buildMessage(any())).thenReturn("splunkFormatterMock buildMessage call");

			return new LoggingFactoryLog4j(pipeSeparatedFormatterMock);
		}

		@Bean
		public PathResolver pathResolver() {
			return mock(PathResolver.class);
		}
	}
}
