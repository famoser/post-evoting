/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch;

import static ch.post.it.evoting.sdm.commons.Constants.BALLOT_BOX_ID;
import static ch.post.it.evoting.sdm.commons.Constants.BALLOT_ID;
import static ch.post.it.evoting.sdm.commons.Constants.BASE_PATH;
import static ch.post.it.evoting.sdm.commons.Constants.ELECTION_EVENT_ID;
import static ch.post.it.evoting.sdm.commons.Constants.ELECTORAL_AUTHORITY_ID;
import static ch.post.it.evoting.sdm.commons.Constants.JOB_INSTANCE_ID;
import static ch.post.it.evoting.sdm.commons.Constants.NUMBER_VOTING_CARDS;
import static ch.post.it.evoting.sdm.commons.Constants.TENANT_ID;
import static ch.post.it.evoting.sdm.commons.Constants.VALIDITY_PERIOD_END;
import static ch.post.it.evoting.sdm.commons.Constants.VALIDITY_PERIOD_START;
import static ch.post.it.evoting.sdm.commons.Constants.VERIFICATION_CARD_SET_ID;
import static ch.post.it.evoting.sdm.commons.Constants.VOTING_CARD_SET_ID;
import static ch.post.it.evoting.sdm.commons.Constants.VOTING_CARD_SET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateData;
import ch.post.it.evoting.cryptolib.certificates.bean.ValidityDates;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.service.CertificatesService;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;
import ch.post.it.evoting.cryptolib.extendedkeystore.cryptoapi.CryptoAPIExtendedKeyStore;
import ch.post.it.evoting.cryptolib.extendedkeystore.service.ExtendedKeyStoreService;
import ch.post.it.evoting.domain.election.EncryptionParameters;
import ch.post.it.evoting.domain.election.VerificationCardSetData;
import ch.post.it.evoting.domain.election.VoteVerificationContextData;
import ch.post.it.evoting.sdm.config.commands.voters.JobExecutionObjectContext;
import ch.post.it.evoting.sdm.config.commands.voters.VotersParametersHolder;
import ch.post.it.evoting.sdm.config.commands.voters.VotersSerializationDestProvider;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VerificationCardSetCredentialDataPack;

/**
 * This tests a specific batch job step. By default, JobLauncherTestUtils expects to find _only one_ Job bean in the context, that's why i made the
 * batch configuration class an inner class and not a "shared" class for all tests. We could extract the class into a different file but i don't see
 * any advantage
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
class WriteVerificationDataTaskletTest {

	private static final String STEP_IN_TEST = "writeVerificationData";
	private final ObjectMapper jsonMapper = new ObjectMapper();
	@TempDir
	Path tempFolder;
	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private JobExecutionObjectContext stepExecutionObjectContext;

	@Autowired
	private VotersSerializationDestProvider destProviderMock;

	private File verificationCardSetDataExpectedFile;
	private File voteVerificationContextDataExpectedFile;

	@Test
	void generateVerificationDataFiles() {

		// given
		JobParameters jobParameters = getJobInputParameters();

		// when
		JobExecution jobExecution = jobLauncherTestUtils.launchStep(STEP_IN_TEST, jobParameters);

		// then (we want to know that the "new" job parameters are generated and
		// stored in the context)
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

		// we have to assert that the output files of this tasklet are generated
		// and have correct data
		// the files are json files created from the following classes:
		// VerificationCardSetData, VoteVerificationContextData
		// we could go further and verify the content matches all the "test"
		// data, but it's maybe overkill
		assertTrue(verificationCardSetDataExpectedFile.exists());
		try {
			jsonMapper.readValue(verificationCardSetDataExpectedFile, VerificationCardSetData.class);
		} catch (IOException e) {
			fail("unexpected format for VerificationCardSetData");
		}

		assertTrue(voteVerificationContextDataExpectedFile.exists());
		try {
			jsonMapper.readValue(voteVerificationContextDataExpectedFile, VoteVerificationContextData.class);
		} catch (IOException e) {
			fail("unexpected format for VoteVerificationContextData");
		}
	}

	private JobParameters getJobInputParameters() {
		JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
		jobParametersBuilder.addString(JOB_INSTANCE_ID, UUID.randomUUID().toString(), true);
		jobParametersBuilder.addString(TENANT_ID, "tenantId", true);
		jobParametersBuilder.addString(ELECTION_EVENT_ID, "electionEventId", true);
		jobParametersBuilder.addString(BALLOT_BOX_ID, "ballotBoxId", true);
		jobParametersBuilder.addString(BALLOT_ID, "ballotId");
		jobParametersBuilder.addString(ELECTORAL_AUTHORITY_ID, "electoralAuthorityId");
		jobParametersBuilder.addString(VOTING_CARD_SET_ID, "votingCardSetId");
		jobParametersBuilder.addString(NUMBER_VOTING_CARDS, "10");
		jobParametersBuilder.addString(VOTING_CARD_SET_NAME, "votingCardSetAlias");
		jobParametersBuilder.addString(VALIDITY_PERIOD_START, "2017-02-15");
		jobParametersBuilder.addString(VALIDITY_PERIOD_END, "2018-02-15");
		jobParametersBuilder.addString(BASE_PATH, "absoluteOutputPath");
		jobParametersBuilder.addString(VERIFICATION_CARD_SET_ID, "verificationCardSetId");
		return jobParametersBuilder.toJobParameters();
	}

	@BeforeEach
	public void setup() throws Exception {

		verificationCardSetDataExpectedFile = tempFolder.resolve("vcsData").toFile();
		voteVerificationContextDataExpectedFile = tempFolder.resolve("vvcData").toFile();

		VotersParametersHolder parametersHolder = mock(VotersParametersHolder.class);
		when(parametersHolder.getEncryptionParameters()).thenReturn(new EncryptionParameters("23", "11", "2"));
		when(stepExecutionObjectContext.get(any(), eq(VotersParametersHolder.class))).thenReturn(parametersHolder);

		BigInteger P = new BigInteger(
				"25515082852221325227734875679796454760326467690112538918409444238866830264288928368643860210692030230970372642053699673880830938513755311613746769767735066124931265104230246714327140720231537205767076779634365989939295710998787785801877310580401262530818848712843191597770750843711630250668056368624192328749556025449493888902777252341817892959006585132698115406972938429732386814317498812002229915393331703423250137659204137625584559844531972832055617091033311878843608854983169553055109029654797488332746885443611918764277292979134833642098989040604523427961162591459163821790507259475762650859921432844527734894939");
		BigInteger Q = new BigInteger(
				"12757541426110662613867437839898227380163233845056269459204722119433415132144464184321930105346015115485186321026849836940415469256877655806873384883867533062465632552115123357163570360115768602883538389817182994969647855499393892900938655290200631265409424356421595798885375421855815125334028184312096164374778012724746944451388626170908946479503292566349057703486469214866193407158749406001114957696665851711625068829602068812792279922265986416027808545516655939421804427491584776527554514827398744166373442721805959382138646489567416821049494520302261713980581295729581910895253629737881325429960716422263867447469");
		BigInteger G = new BigInteger("3");

		ElGamalEncryptionParameters params = new ElGamalEncryptionParameters(P, Q, G);
		ElGamalKeyPair elGamalKeyPair = new ElGamalService().generateKeyPair(params, 1);
		KeyPair certificateKeyPair = new AsymmetricService().getKeyPairForSigning();
		CryptoAPIExtendedKeyStore keyStore = new ExtendedKeyStoreService().createKeyStore();

		VerificationCardSetCredentialDataPack verificationCardSetCredentialDataPack = mock(VerificationCardSetCredentialDataPack.class);
		when(verificationCardSetCredentialDataPack.getChoiceCodesEncryptionPublicKey()).thenReturn(elGamalKeyPair.getPublicKeys());
		when(verificationCardSetCredentialDataPack.getNonCombinedChoiceCodesEncryptionPublicKeys()).thenReturn(
				new ElGamalPublicKey[] { elGamalKeyPair.getPublicKeys(), elGamalKeyPair.getPublicKeys(), elGamalKeyPair.getPublicKeys(),
						elGamalKeyPair.getPublicKeys() });
		when(verificationCardSetCredentialDataPack.getVerificationCardSetIssuerCert()).thenReturn(generateCertificate(certificateKeyPair));
		when(verificationCardSetCredentialDataPack.getSerializedKeyStore()).thenReturn("{}");
		when(stepExecutionObjectContext.get(any(), eq(VerificationCardSetCredentialDataPack.class)))
				.thenReturn(verificationCardSetCredentialDataPack);

		when(destProviderMock.getVerificationCardSetData()).thenReturn(verificationCardSetDataExpectedFile.toPath());
		when(destProviderMock.getVoteVerificationContextData()).thenReturn(voteVerificationContextDataExpectedFile.toPath());
	}

	private CryptoAPIX509Certificate generateCertificate(KeyPair keyPair) throws Exception {

		CertificateData certificateData = new CertificateData();
		certificateData.setSubjectPublicKey(keyPair.getPublic());
		ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
		ZonedDateTime end = now.plusYears(1);
		ValidityDates validityDates = new ValidityDates(Date.from(now.toInstant()), Date.from(end.toInstant()));
		certificateData.setValidityDates(validityDates);
		X509DistinguishedName distinguishedName = new X509DistinguishedName.Builder("commonName", "ES").build();
		certificateData.setSubjectDn(distinguishedName);
		certificateData.setIssuerDn(distinguishedName);

		return new CertificatesService().createSignX509Certificate(certificateData, keyPair.getPrivate());
	}

	@Configuration
	@EnableBatchProcessing
	@Import(TestConfigServices.class)
	static class JobConfiguration {

		@Autowired
		JobBuilderFactory jobBuilder;

		@Autowired
		StepBuilderFactory stepBuilder;

		@Autowired
		VotersSerializationDestProvider destProvider;

		@Autowired
		private JobExecutionObjectContext objectContext;

		@Bean
		JobLauncherTestUtils testUtils() {
			return new JobLauncherTestUtils();
		}

		@Bean
		public Step step(Tasklet tasklet) {
			return stepBuilder.get(STEP_IN_TEST).tasklet(tasklet).build();
		}

		@Bean
		public Tasklet tasklet() {
			return new WriteVerificationDataTasklet(destProvider, objectContext);
		}

		@Bean
		Job job(Step step) {
			return jobBuilder.get("job").start(step).build();
		}

		@Bean
		VotersParametersHolder holder() {
			return mock(VotersParametersHolder.class);
		}
	}
}
