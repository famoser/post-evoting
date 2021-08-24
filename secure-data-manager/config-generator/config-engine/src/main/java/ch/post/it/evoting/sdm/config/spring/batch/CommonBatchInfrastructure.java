/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch;

import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_DIR_NAME_ONLINE;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_DIR_NAME_PRINTING;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_DIR_NAME_VOTERMATERIAL;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_DIR_NAME_VOTERVERIFICATION;
import static ch.post.it.evoting.sdm.commons.Constants.CONFIG_FILE_EXTENDED_AUTHENTICATION_DATA;
import static ch.post.it.evoting.sdm.commons.Constants.NUMBER_VOTING_CARDS;
import static ch.post.it.evoting.sdm.commons.Constants.TENANT_ID;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.commands.voters.VotersSerializationDestProvider;

@Configuration
@EnableBatchProcessing(modular = true)
@ComponentScan({ "ch.post.it.evoting.sdm.config.spring.config" })
public class CommonBatchInfrastructure extends DefaultBatchConfigurer {

	private static final Logger LOGGER = LoggerFactory.getLogger(CommonBatchInfrastructure.class);

	@Value("${spring.batch.jobs.concurrency:1}")
	private String jobConcurrency;
	@Value("${spring.batch.steps.concurrency:4}")
	private String stepConcurrency;

	@Bean
	public JobBuilderFactory jobBuilderFactory() {
		return new JobBuilderFactory(getJobRepository());
	}

	@Bean
	public StepBuilderFactory stepBuilderFactory() {
		return new StepBuilderFactory(getJobRepository(), getTransactionManager());
	}

	public JobBuilder getJobBuilder(final String jobName, final JobParametersIncrementer incrementer,
			final JobExecutionListener jobExecutionListener) {
		JobBuilder jobBuilder = jobBuilderFactory().get(jobName).preventRestart();
		if (incrementer != null) {
			jobBuilder = jobBuilder.incrementer(incrementer);
		}
		if (jobExecutionListener != null) {
			jobBuilder = jobBuilder.listener(jobExecutionListener);
		}
		return jobBuilder;
	}

	public StepBuilder getStepBuilder(final String stepName) {
		return stepBuilderFactory().get(stepName);
	}

	@Override
	protected JobLauncher createJobLauncher() {
		SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
		jobLauncher.setTaskExecutor(jobExecutor());
		jobLauncher.setJobRepository(getJobRepository());
		return jobLauncher;
	}

	@Bean
	@JobScope
	public TaskExecutor stepExecutor(
			@Value("#{jobExecutionContext['" + TENANT_ID + "']}")
			final String tenantId,
			@Value("#{jobExecutionContext['" + NUMBER_VOTING_CARDS + "']}")
			final Integer numberOfVotingCards) {

		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		// avoid creating more threads than necessary. If the vcset has a low number of voting cards, no
		// need to create many
		// thread that will not be doing anything (start, return null, die)
		// we MUST have at least 3 threads (2 already used, one for doing work)
		int workThreadCount = Math.min(Math.max(3, numberOfVotingCards), Math.abs(Integer.parseInt(stepConcurrency)));

		// for each job we are using at least 2 threads (one for the generation step and one for the
		// writing step).
		// so we have to add at least those 2 to the number of threads we will be using for processing
		// to avoid blocking the job.
		int coreThreadCount = workThreadCount + 2;
		int maxThreadCount = coreThreadCount;
		executor.setCorePoolSize(coreThreadCount);
		executor.setMaxPoolSize(maxThreadCount);
		LOGGER.info("Configured StepExecutor with [core={}, max={}] threads", workThreadCount, workThreadCount);
		final String groupName = Thread.currentThread().getName().split("-")[1];
		executor.setThreadNamePrefix("StepExecutor-" + groupName + "-");
		executor.setTaskDecorator(new StepTaskDecorator(tenantId));
		return executor;
	}

	@Bean
	public TaskExecutor jobExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		// limit the number of concurrent jobs executing to a maximum of the number of processors.
		// we may need to limit to a lower number than available processors due to memory usage
		int threadCount = Math.min(Integer.parseInt(jobConcurrency), Runtime.getRuntime().availableProcessors());
		executor.setAllowCoreThreadTimeOut(true);
		executor.setKeepAliveSeconds(60);
		executor.setCorePoolSize(threadCount);
		executor.setMaxPoolSize(threadCount);
		LOGGER.info("Configured JobExecutor with [core={}, max={}] threads", threadCount, threadCount);
		// max number of queued jobs for execution. this should be more than enough
		executor.setQueueCapacity(1000);
		executor.setThreadNamePrefix("JobExecutor-");
		executor.setTaskDecorator(new JobTaskDecorator(Constants.HUNDRED));
		return executor;
	}

	public VotersSerializationDestProvider getDataSerializationProvider(final String basePath, final String votingCardSetId,
			final String verificationCardSetId) {
		final Path voterMaterialOnlinePath = Paths.get(basePath, CONFIG_DIR_NAME_ONLINE, CONFIG_DIR_NAME_VOTERMATERIAL, votingCardSetId);

		final Path verificationCardOnlinePath = Paths.get(basePath, CONFIG_DIR_NAME_ONLINE, CONFIG_DIR_NAME_VOTERVERIFICATION, verificationCardSetId);

		final Path printingOnlinePath = Paths.get(basePath, CONFIG_DIR_NAME_ONLINE, CONFIG_DIR_NAME_PRINTING, votingCardSetId);

		final Path extendedAuthenticationPath = Paths
				.get(basePath, CONFIG_DIR_NAME_ONLINE, CONFIG_FILE_EXTENDED_AUTHENTICATION_DATA, votingCardSetId);

		return new VotersSerializationDestProvider(voterMaterialOnlinePath, verificationCardOnlinePath, printingOnlinePath,
				extendedAuthenticationPath);
	}
}
