/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.api;

import static ch.post.it.evoting.sdm.commons.Constants.BALLOT_BOX_ID;
import static ch.post.it.evoting.sdm.commons.Constants.BALLOT_ID;
import static ch.post.it.evoting.sdm.commons.Constants.ELECTION_EVENT_ID;
import static ch.post.it.evoting.sdm.commons.Constants.ELECTORAL_AUTHORITY_ID;
import static ch.post.it.evoting.sdm.commons.Constants.GENERATED_VC_COUNT;
import static ch.post.it.evoting.sdm.commons.Constants.JOB_INSTANCE_ID;
import static ch.post.it.evoting.sdm.commons.Constants.NUMBER_VOTING_CARDS;
import static ch.post.it.evoting.sdm.commons.Constants.SEMICOLON;
import static ch.post.it.evoting.sdm.commons.Constants.TENANT_ID;
import static ch.post.it.evoting.sdm.commons.Constants.VALIDITY_PERIOD_END;
import static ch.post.it.evoting.sdm.commons.Constants.VALIDITY_PERIOD_START;
import static ch.post.it.evoting.sdm.commons.Constants.VERIFICATION_CARD_SET_ID;
import static ch.post.it.evoting.sdm.commons.Constants.VOTING_CARD_SET_ID;
import static ch.post.it.evoting.sdm.commons.Constants.VOTING_CARD_SET_NAME;
import static ch.post.it.evoting.sdm.config.logevents.ConfigGeneratorLogEvents.GENVCD_ERROR_GENERATING_VERIFICATION_CARD_IDS;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ch.post.it.evoting.cryptoprimitives.CryptoPrimitives;
import ch.post.it.evoting.cryptoprimitives.CryptoPrimitivesService;
import ch.post.it.evoting.logging.api.domain.Level;
import ch.post.it.evoting.logging.api.domain.LogContent;
import ch.post.it.evoting.logging.api.factory.LoggingFactory;
import ch.post.it.evoting.logging.api.writer.LoggingWriter;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.domain.StartVotingCardGenerationJobResponse;
import ch.post.it.evoting.sdm.commons.domain.spring.batch.SensitiveAwareJobParametersBuilder;
import ch.post.it.evoting.sdm.config.commands.api.output.BallotBoxesServiceOutput;
import ch.post.it.evoting.sdm.config.commands.api.output.ElectionEventServiceOutput;
import ch.post.it.evoting.sdm.config.commands.ballotbox.BallotBoxGenerator;
import ch.post.it.evoting.sdm.config.commands.ballotbox.BallotBoxHolderInitializer;
import ch.post.it.evoting.sdm.config.commands.ballotbox.BallotBoxParametersHolder;
import ch.post.it.evoting.sdm.config.commands.electionevent.CreateElectionEventGenerator;
import ch.post.it.evoting.sdm.config.commands.electionevent.CreateElectionEventHolderInitializer;
import ch.post.it.evoting.sdm.config.commands.electionevent.CreateElectionEventOutput;
import ch.post.it.evoting.sdm.config.commands.electionevent.CreateElectionEventParametersHolder;
import ch.post.it.evoting.sdm.config.commands.electionevent.CreateElectionEventSerializer;
import ch.post.it.evoting.sdm.config.commands.progress.ProgressManager;
import ch.post.it.evoting.sdm.config.commands.voters.JobExecutionObjectContext;
import ch.post.it.evoting.sdm.config.commands.voters.JobSelectionStrategy;
import ch.post.it.evoting.sdm.config.commands.voters.VotersParametersHolder;
import ch.post.it.evoting.sdm.config.commons.beans.VotingCardGenerationJobStatus;
import ch.post.it.evoting.sdm.config.commons.progress.JobProgressDetails;
import ch.post.it.evoting.sdm.config.exceptions.ConfigurationEngineException;
import ch.post.it.evoting.sdm.config.exceptions.CreateBallotBoxesException;
import ch.post.it.evoting.sdm.config.exceptions.CreateElectionEventException;
import ch.post.it.evoting.sdm.config.exceptions.specific.GenerateVerificationCardDataException;
import ch.post.it.evoting.sdm.config.logevents.ConfigGeneratorLogEvents;

public class ConfigurationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationService.class);

	private static final String VOTINGCARDSET_GENERATION = "votingcardset-generation-*";
	private static final String TAB_LOG = "\t {}";

	private final CryptoPrimitives cryptoPrimitives = CryptoPrimitivesService.get();
	@Autowired
	LoggingFactory loggerFactory;
	@Autowired
	private CreateElectionEventGenerator createElectionEventGenerator;
	@Autowired
	private CreateElectionEventSerializer createElectionEventSerializer;
	@Autowired
	private BallotBoxGenerator ballotBoxGenerator;
	@Autowired
	private BallotBoxHolderInitializer ballotBoxHolderInitializer;
	@Autowired
	private CreateElectionEventHolderInitializer electionEventHolderInitializer;
	@Autowired
	private JobExecutionObjectContext jobExecutionObjectContext;
	@Autowired
	private ProgressManager progressManager;
	@Autowired
	private JobLauncher jobLauncher;
	@Autowired
	private JobExplorer jobExplorer;
	@Autowired
	private JobSelectionStrategy jobSelectionStrategy;
	@Autowired
	private JobRegistry jobRegistry;

	private LoggingWriter logWriter;

	@PostConstruct
	private void initializeLogger() {
		logWriter = loggerFactory.getLogger(ConfigurationService.class);
	}

	public ElectionEventServiceOutput createElectionEvent(CreateElectionEventParametersHolder holder) {

		try {
			LOGGER.info("Loading internal configuration...");

			try (InputStream is = getKeysConfiguration()) {
				electionEventHolderInitializer.init(holder, is);
			}

			LOGGER.info("Generating Election Event...");

			final CreateElectionEventOutput createElectionEventOutput = createElectionEventGenerator.generate(holder);
			try {
				LOGGER.info("Processing the output...");

				createElectionEventSerializer.serialize(holder, createElectionEventOutput);
			} finally {
				createElectionEventOutput.clearPasswords();
			}

			LOGGER.info("The generation of Election Event finished correctly. It can be found in:");
			LOGGER.info(TAB_LOG, holder.getOfflineFolder().toAbsolutePath());
			LOGGER.info(TAB_LOG, holder.getOnlineAuthenticationFolder().toAbsolutePath());
			LOGGER.info(TAB_LOG, holder.getOnlineElectionInformationFolder().toAbsolutePath());

			ElectionEventServiceOutput electionEventOutput = new ElectionEventServiceOutput();
			electionEventOutput.setOfflineFolder(holder.getOfflineFolder().toAbsolutePath().toString());
			electionEventOutput.setOnlineAuthenticationFolder(holder.getOnlineAuthenticationFolder().toAbsolutePath().toString());
			electionEventOutput.setOnlineElectionInformationFolder(holder.getOnlineElectionInformationFolder().toAbsolutePath().toString());

			return electionEventOutput;

		} catch (Exception e) {
			throw new CreateElectionEventException(e);
		}
	}

	public BallotBoxesServiceOutput createBallotBoxes(BallotBoxParametersHolder ballotBoxHolder) {

		try {
			LOGGER.info("Loading required certificates and private keys...");

			try (InputStream is = getKeysConfiguration()) {
				ballotBoxHolderInitializer.init(ballotBoxHolder, is);
			}

			LOGGER.info("Creating ballot boxes...");

			final BallotBoxesServiceOutput boxesServiceOutput = ballotBoxGenerator.generate(ballotBoxHolder);

			LOGGER.info("The ballot boxes were successfully created. They can be found in:");
			LOGGER.info(TAB_LOG, boxesServiceOutput.getOutputPath());

			return boxesServiceOutput;

		} catch (Exception e) {
			throw new CreateBallotBoxesException(e);
		}
	}

	/**
	 * Get job status for the specified job
	 *
	 * @param tenantId        the tenant
	 * @param electionEventId the election event
	 * @param jobId           the job instance id
	 * @return job status for the specified job
	 */
	public Optional<VotingCardGenerationJobStatus> getVotingCardGenerationJobStatus(final String tenantId, final String electionEventId,
			final String jobId) {

		final Function<JobExecution, VotingCardGenerationJobStatus> mapToJobStatus = mapJobExecutionToJobStatus();
		// JobParameters by id because it might get a request before it runs the
		// "job preparation task" where it populates the job execution context.
		// Meanwhile, the execution context is empty and will return null or empty.
		final Predicate<JobExecution> filterByJobInstance = jobExecution ->
				jobExecution.getJobParameters().getString(TENANT_ID).equalsIgnoreCase(tenantId) && jobExecution.getJobParameters()
						.getString(ELECTION_EVENT_ID).equalsIgnoreCase(electionEventId) && jobExecution.getJobParameters().getString(JOB_INSTANCE_ID)
						.equalsIgnoreCase(jobId);

		final List<JobInstance> jobInstances = jobExplorer.findJobInstancesByJobName(VOTINGCARDSET_GENERATION, 0, Integer.MAX_VALUE);
		return jobInstances.stream().map(jobExplorer::getJobExecutions).flatMap(Collection::stream).filter(filterByJobInstance).map(mapToJobStatus)
				.findAny();
	}

	private Function<JobExecution, VotingCardGenerationJobStatus> mapJobExecutionToJobStatus() {
		return je -> {
			if (BatchStatus.UNKNOWN.equals(je.getStatus())) {
				return VotingCardGenerationJobStatus.UNKNOWN;
			} else {
				ExecutionContext executionContext = je.getExecutionContext();
				// JobParameters by id because it might get a request before it runs the
				// "job preparation task" where it populates the job execution context.
				// Meanwhile, the execution context is empty and will return null or empty.
				final String id = je.getJobParameters().getString(JOB_INSTANCE_ID);
				final String status = je.getStatus().toString();
				final String statusDetails = je.getStatus().isUnsuccessful() ? je.getExitStatus().getExitDescription() : null;
				// start time may be null if the job has not started yet
				final Instant startTime = Optional.ofNullable(je.getStartTime()).orElse(Date.from(Instant.EPOCH)).toInstant();
				final Optional<JobProgressDetails> details = progressManager.getJobProgress(UUID.fromString(id));
				final String verificationCardSetId = executionContext.getString(VERIFICATION_CARD_SET_ID, null);
				final int votingCardCount = executionContext.getInt(GENERATED_VC_COUNT, 0);
				final int errorCount = executionContext.getInt(Constants.ERROR_COUNT, 0);
				return new VotingCardGenerationJobStatus(UUID.fromString(id), status, startTime, statusDetails, details.orElse(null),
						verificationCardSetId, votingCardCount, errorCount);
			}
		};
	}

	/**
	 * Get list of all Voting Card Generation jobs for the specified tenant and electionEvent with a specific status
	 *
	 * @param tenantId        the tenant
	 * @param electionEventId the election event
	 * @param jobStatus       the job status
	 * @return list of all Voting Card Generation jobs for the specified tenant and electionEvent
	 */
	public List<VotingCardGenerationJobStatus> getJobsWithStatus(final String tenantId, final String electionEventId, final String jobStatus) {
		final Function<JobExecution, VotingCardGenerationJobStatus> mapToJobStatus = mapJobExecutionToJobStatus();
		final Predicate<JobExecution> filterWithStatus = jobExecution ->
				jobExecution.getJobParameters().getString(TENANT_ID).equalsIgnoreCase(tenantId) && jobExecution.getJobParameters()
						.getString(ELECTION_EVENT_ID).equalsIgnoreCase(electionEventId) && BatchStatus.valueOf(jobStatus.toUpperCase())
						.equals(jobExecution.getStatus());

		final List<JobInstance> jobInstances = jobExplorer.findJobInstancesByJobName(VOTINGCARDSET_GENERATION, 0, Integer.MAX_VALUE);

		return jobInstances.stream().map(jobExplorer::getJobExecutions).flatMap(Collection::stream).filter(filterWithStatus).map(mapToJobStatus)
				.collect(Collectors.toList());
	}

	/**
	 * Get list of all Voting Card Generation jobs for the specified tenant and election event
	 *
	 * @return list of all Voting Card Generation jobs for the specified tenant and election event
	 */
	public List<VotingCardGenerationJobStatus> getJobs() {
		final Function<JobExecution, VotingCardGenerationJobStatus> mapToJobStatus = mapJobExecutionToJobStatus();

		final List<JobInstance> jobInstances = jobExplorer.findJobInstancesByJobName(VOTINGCARDSET_GENERATION, 0, Integer.MAX_VALUE);
		// one way of having a count at the end of the stream

		return jobInstances.stream().map(jobExplorer::getJobExecutions).flatMap(Collection::stream).map(mapToJobStatus).collect(Collectors.toList());
	}

	/**
	 * Start a new voting card generation Spring Batch job with the specified input parameters.
	 *
	 * @param holder holder class for all the needed job parameters
	 * @return object with the initial status of the job
	 */
	public StartVotingCardGenerationJobResponse startVotingCardGenerationJob(final String tenantId, final String electionEventId,
			final VotersParametersHolder holder) {

		JobParameters jobParams = prepareJobParameters(tenantId, electionEventId, holder);
		final String jobInstanceId = jobParams.getString(JOB_INSTANCE_ID);
		jobExecutionObjectContext.put(jobInstanceId, holder, VotersParametersHolder.class);

		final UUID jobId = UUID.fromString(jobInstanceId);
		try {
			final String jobQualifier = jobSelectionStrategy.select();
			final Job job = jobRegistry.getJob(jobQualifier);
			final JobExecution jobExecution = jobLauncher.run(job, jobParams);

			final BatchStatus jobStatus = jobExecution.getStatus();
			final Instant created = jobExecution.getCreateTime().toInstant();
			final String createdStr = created.atZone(ZoneId.systemDefault()).toString();

			final long numberOfVotingCards = Long.parseLong(jobParams.getString(NUMBER_VOTING_CARDS));
			progressManager.registerJob(jobId, new JobProgressDetails(jobId, numberOfVotingCards));

			return new StartVotingCardGenerationJobResponse(jobInstanceId, jobStatus, createdStr);
		} catch (JobExecutionException e) {
			// in case we registered the job, remove it.
			progressManager.unregisterJob(jobId);
			throw new ConfigurationEngineException(e);
		}
	}

	/**
	 * Generate the number of verificationCardIds specified by parameter.
	 *
	 * @return a stream of the requested number of verification cards IDs.
	 */
	public Stream<String> createVerificationCardIdStream(final String electionEventId, final String verificationCardSetId,
			final int numberOfVerificationCardIdsToGenerate) {
		return IntStream.range(0, numberOfVerificationCardIdsToGenerate)
				.mapToObj(i -> generateVerificationCardId(verificationCardSetId, electionEventId, numberOfVerificationCardIdsToGenerate));
	}

	/**
	 * Generate a verification card id
	 */
	private String generateVerificationCardId(final String verificationCardSetId, final String electionEventId, final int numberOfVerificationCards) {
		String verificationCardId;
		try {
			verificationCardId = cryptoPrimitives.genRandomBase16String(Constants.BASE16_ID_LENGTH).toLowerCase();
			logWriter.log(Level.DEBUG,
					new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENVCD_SUCCESS_GENERATING_VERIFICATION_CARD_IDS)
							.user("adminID").electionEvent(electionEventId).additionalInfo("verifcs_id", verificationCardSetId).createLogInfo());
		} catch (RuntimeException e) {
			logWriter.log(Level.ERROR,
					new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENVCD_ERROR_GENERATING_VERIFICATION_CARD_IDS)
							.user("adminID").electionEvent(electionEventId).objectId(verificationCardSetId)
							.additionalInfo("verifcs_id", verificationCardSetId)
							.additionalInfo("vcs_size", Integer.toString(numberOfVerificationCards)).additionalInfo("err_desc", e.getMessage())
							.createLogInfo());
			throw new GenerateVerificationCardDataException(GENVCD_ERROR_GENERATING_VERIFICATION_CARD_IDS.getInfo(), e);
		}
		return verificationCardId;
	}

	private JobParameters prepareJobParameters(final String tenantId, final String electionEventId, final VotersParametersHolder input) {

		final SensitiveAwareJobParametersBuilder jobParametersBuilder = new SensitiveAwareJobParametersBuilder();
		jobParametersBuilder.addString(JOB_INSTANCE_ID, UUID.randomUUID().toString(), true);
		jobParametersBuilder.addString(TENANT_ID, tenantId, true);
		jobParametersBuilder.addString(ELECTION_EVENT_ID, electionEventId, true);
		jobParametersBuilder.addString(BALLOT_BOX_ID, input.getBallotBoxID(), true);
		jobParametersBuilder.addString(BALLOT_ID, input.getBallotID());
		jobParametersBuilder.addString(ELECTORAL_AUTHORITY_ID, input.getElectoralAuthorityID());
		jobParametersBuilder.addString(VOTING_CARD_SET_ID, input.getVotingCardSetID());
		jobParametersBuilder.addString(VERIFICATION_CARD_SET_ID, input.getVerificationCardSetID());
		jobParametersBuilder.addString(NUMBER_VOTING_CARDS, Integer.toString(input.getNumberVotingCards()));
		jobParametersBuilder.addString(VOTING_CARD_SET_NAME, input.getVotingCardSetAlias());
		jobParametersBuilder.addString(VALIDITY_PERIOD_START, input.getCertificatesStartValidityPeriod().toString());
		jobParametersBuilder.addString(VALIDITY_PERIOD_END, input.getCertificatesEndValidityPeriod().toString());
		jobParametersBuilder.addString(Constants.BASE_PATH, input.getAbsoluteBasePath().toString());

		String choiceCodeEncryptionKeyAsString = String.join(SEMICOLON, input.getChoiceCodesEncryptionKey());
		jobParametersBuilder.addString(Constants.CHOICE_CODES_ENCRYPTION_KEY, choiceCodeEncryptionKeyAsString);
		jobParametersBuilder.addString(Constants.PLATFORM_ROOT_CA_CERTIFICATE, input.getPlatformRootCACertificate());

		return jobParametersBuilder.toJobParameters();
	}

	private InputStream getKeysConfiguration() throws FileNotFoundException {
		// get inputstream to keys_config.json from classpath
		String resourceName = "/" + Constants.KEYS_CONFIG_FILENAME;
		final InputStream resourceAsStream = this.getClass().getResourceAsStream(resourceName);
		if (resourceAsStream == null) {
			throw new FileNotFoundException(String.format("Resource file '%s' was not found on the classpath", resourceName));
		}
		return resourceAsStream;
	}
}
