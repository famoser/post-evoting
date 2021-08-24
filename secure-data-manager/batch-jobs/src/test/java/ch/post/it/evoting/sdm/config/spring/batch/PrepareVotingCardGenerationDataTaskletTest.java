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
import static ch.post.it.evoting.sdm.commons.Constants.SALT_CREDENTIAL_ID;
import static ch.post.it.evoting.sdm.commons.Constants.SALT_KEYSTORE_SYM_ENC_KEY;
import static ch.post.it.evoting.sdm.commons.Constants.TENANT_ID;
import static ch.post.it.evoting.sdm.commons.Constants.VALIDITY_PERIOD_END;
import static ch.post.it.evoting.sdm.commons.Constants.VALIDITY_PERIOD_START;
import static ch.post.it.evoting.sdm.commons.Constants.VERIFICATION_CARD_SET_ID;
import static ch.post.it.evoting.sdm.commons.Constants.VOTING_CARD_SET_ID;
import static ch.post.it.evoting.sdm.commons.Constants.VOTING_CARD_SET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.batch.test.ExecutionContextTestUtils;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import ch.post.it.evoting.sdm.commons.domain.CreateVotingCardSetCertificatePropertiesContainer;
import ch.post.it.evoting.sdm.config.commands.voters.VotersParametersHolder;

/**
 * This tests a specific batch job step. By default, JobLauncherTestUtils expects to find _only one_ Job bean in the context, that's why i made the
 * batch configuration class an inner class and not a "shared" class for all tests. We could extract the class into a different file but i don't see
 * any advantage
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
class PrepareVotingCardGenerationDataTaskletTest {

	private static final String STEP_IN_TEST = "prepareVotingCardGenerationDataStep";

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	private static CreateVotingCardSetCertificatePropertiesContainer getCreateVotingCardSetCertificateProperties() throws IOException {

		CreateVotingCardSetCertificatePropertiesContainer createVotingCardSetCertificateProperties = new CreateVotingCardSetCertificatePropertiesContainer();

		String verificationCardSetCertificatePropertiesPath = "properties/verificationCardSetX509Certificate.properties";
		Properties loadedVerificationCardSetCertificateProperties = getCertificateParameters(verificationCardSetCertificatePropertiesPath);

		String credentailSignCertificatePropertiesPath = "properties/credentialSignX509Certificate.properties";
		Properties loadedcredentailSignCertificateProperties = getCertificateParameters(credentailSignCertificatePropertiesPath);

		String credentailAuthCertificatePropertiesPath = "properties/credentialSignX509Certificate.properties";
		Properties loadedCredentailAuthCertificateProperties = getCertificateParameters(credentailAuthCertificatePropertiesPath);

		createVotingCardSetCertificateProperties.setVerificationCardSetCertificateProperties(loadedVerificationCardSetCertificateProperties);
		createVotingCardSetCertificateProperties.setCredentialSignCertificateProperties(loadedcredentailSignCertificateProperties);
		createVotingCardSetCertificateProperties.setCredentialAuthCertificateProperties(loadedCredentailAuthCertificateProperties);

		return createVotingCardSetCertificateProperties;
	}

	private static Properties getCertificateParameters(String path) throws IOException {

		final Properties props = new Properties();

		try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
			props.load(input);
		}

		return props;
	}

	@Test
	void generateAndAddSaltParametersToJobExecutionContext() {

		// given
		JobParameters jobParameters = getJobInputParameters();

		// when
		JobExecution jobExecution = jobLauncherTestUtils.launchStep(STEP_IN_TEST, jobParameters);

		// then (we want to know that the "new" job parameters are generated and stored in the context)
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

		final String saltCredentialId = ExecutionContextTestUtils.getValueFromJob(jobExecution, SALT_CREDENTIAL_ID);
		assertNotNull(saltCredentialId);

		final String saltKeystoreSymmetricEncryptionKey = ExecutionContextTestUtils.getValueFromJob(jobExecution, SALT_KEYSTORE_SYM_ENC_KEY);
		assertNotNull(saltKeystoreSymmetricEncryptionKey);
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

	@Configuration
	@EnableBatchProcessing
	@Import(TestConfigServices.class)
	static class JobConfiguration {

		@Autowired
		JobBuilderFactory jobBuilder;

		@Autowired
		StepBuilderFactory stepBuilder;

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
			return new PrepareVotingCardGenerationDataTasklet();
		}

		@Bean
		Job job(Step step) {
			return jobBuilder.get("job").start(step).build();
		}

		@Bean
		VotersParametersHolder holder() throws IOException {
			VotersParametersHolder holder = mock(VotersParametersHolder.class);
			when(holder.getCreateVotingCardSetCertificateProperties()).thenReturn(getCreateVotingCardSetCertificateProperties());
			return holder;
		}
	}
}
