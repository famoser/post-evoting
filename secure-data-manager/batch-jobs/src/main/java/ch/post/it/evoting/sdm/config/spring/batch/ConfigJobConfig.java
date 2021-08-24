/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch;

import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.derivation.CryptoAPIPBKDFDeriver;
import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.primitives.PrimitivesServiceAPI;
import ch.post.it.evoting.cryptolib.api.securerandom.CryptoAPIRandomString;
import ch.post.it.evoting.cryptolib.api.services.ServiceFactory;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricServiceFactoryHelper;
import ch.post.it.evoting.cryptolib.certificates.factory.X509CertificateGenerator;
import ch.post.it.evoting.cryptolib.certificates.utils.CryptolibPayloadSigningCertificateValidator;
import ch.post.it.evoting.cryptolib.certificates.utils.PayloadSigningCertificateValidator;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.elgamal.codec.ElGamalCiphertextCodec;
import ch.post.it.evoting.cryptolib.extendedkeystore.service.ExtendedKeyStoreService;
import ch.post.it.evoting.cryptoprimitives.hashing.HashService;
import ch.post.it.evoting.domain.election.payload.verify.CryptolibPayloadVerifier;
import ch.post.it.evoting.domain.election.payload.verify.PayloadVerifier;
import ch.post.it.evoting.domain.mixnet.ObjectMapperMixnetConfig;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.CryptolibPayloadSignatureService;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.commons.PrefixPathResolver;
import ch.post.it.evoting.sdm.commons.domain.VcIdCombinedReturnCodesGenerationValues;
import ch.post.it.evoting.sdm.config.actions.ExtendedAuthenticationService;
import ch.post.it.evoting.sdm.config.commands.progress.ProgressManager;
import ch.post.it.evoting.sdm.config.commands.voters.JobExecutionObjectContext;
import ch.post.it.evoting.sdm.config.commands.voters.NodeContributionsPath;
import ch.post.it.evoting.sdm.config.commands.voters.VotersGenerationTaskStaticContentProvider;
import ch.post.it.evoting.sdm.config.commands.voters.VotersParametersHolder;
import ch.post.it.evoting.sdm.config.commands.voters.VotersSerializationDestProvider;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.generators.VerificationCardCredentialDataPackGenerator;
import ch.post.it.evoting.sdm.config.model.authentication.AuthenticationKeyGenerator;
import ch.post.it.evoting.sdm.config.model.authentication.AuthenticationKeyGeneratorStrategyType;
import ch.post.it.evoting.sdm.config.model.authentication.ChallengeGenerator;
import ch.post.it.evoting.sdm.config.model.authentication.ChallengeGeneratorStrategyType;
import ch.post.it.evoting.sdm.config.model.authentication.service.AuthenticationGeneratorFactory;
import ch.post.it.evoting.sdm.config.model.authentication.service.AuthenticationKeyCryptoService;
import ch.post.it.evoting.sdm.config.model.authentication.service.ChallengeGeneratorFactory;
import ch.post.it.evoting.sdm.config.model.authentication.service.ChallengeService;
import ch.post.it.evoting.sdm.config.model.authentication.service.ChallengeServiceAPI;
import ch.post.it.evoting.sdm.config.model.authentication.service.ProvidedChallengeSource;
import ch.post.it.evoting.sdm.config.model.authentication.service.SequentialProvidedChallengeSource;
import ch.post.it.evoting.sdm.config.model.authentication.service.StartVotingKeyService;
import ch.post.it.evoting.sdm.config.spring.SpringConfigServices;
import ch.post.it.evoting.sdm.config.spring.batch.listeners.NodeContributionsStepListener;
import ch.post.it.evoting.sdm.config.spring.batch.listeners.VotingCardGeneratedOutputWriterListener;
import ch.post.it.evoting.sdm.config.spring.batch.listeners.VotingCardGenerationJobListener;
import ch.post.it.evoting.sdm.config.spring.batch.listeners.VotingCardGenerationStepListener;
import ch.post.it.evoting.sdm.config.spring.batch.readers.ComputedValuesReader;
import ch.post.it.evoting.sdm.config.spring.batch.readers.NodeContributionsReader;
import ch.post.it.evoting.sdm.config.spring.batch.readers.OutputQueueReader;
import ch.post.it.evoting.sdm.config.spring.batch.writers.CodesMappingTableWriter;
import ch.post.it.evoting.sdm.config.spring.batch.writers.CompositeOutputWriter;
import ch.post.it.evoting.sdm.config.spring.batch.writers.ComputedValuesOutputWriter;
import ch.post.it.evoting.sdm.config.spring.batch.writers.CredentialDataWriter;
import ch.post.it.evoting.sdm.config.spring.batch.writers.DerivedKeyCommitmentsWriter;
import ch.post.it.evoting.sdm.config.spring.batch.writers.ExtendedAuthenticationWriter;
import ch.post.it.evoting.sdm.config.spring.batch.writers.GeneratedVotingCardOutputWriter;
import ch.post.it.evoting.sdm.config.spring.batch.writers.VerificationCardDataWriter;
import ch.post.it.evoting.sdm.config.spring.batch.writers.VoterInformationWriter;

/**
 * Configuration class for voting card generation jobs
 */
@Configuration("configuration-job-config")
public class ConfigJobConfig {

	private static final String STR_REPLACED_AT_RUNTIME = null;
	private static final int INT_REPLACED_AT_RUNTIME = -1;

	@Autowired
	Environment env;

	@Autowired
	ElGamalCiphertextCodec codec;

	@Autowired
	private SpringConfigServices springConfigServices;

	@Value("${spring.batch.steps.concurrency:4}")
	private String stepConcurrency;

	@Value("${spring.batch.steps.queue.capacity:1000}")
	private int queueCapacity;

	@Value("${maximum.number.credentials.per.file:1000}")
	private int maxNumCredentialsPerFile;

	@Value("${user.home}")
	private String prefix;

	@Autowired
	private JobExecutionObjectContext objectContext;

	@Autowired
	private CommonBatchInfrastructure commonBatchInfrastructure;

	@Bean
	Job job(PrimitivesServiceAPI primitivesService, ProgressManager progressManager) {

		/*
		 * Defines a template for job configuration.
		 * pre-processing flow -> processing flow -> post-processing flow
		 * Each implementation add the steps it needs in the different flows.
		 */
		return commonBatchInfrastructure
				.getJobBuilder(Constants.VOTING_CARD_SET_GENERATION + "-product", new RunIdIncrementer(), jobExecutionListener())
				.start(preProcessingFlow().build()) //
				.next(verificationCombinationAndGenerationFlow(primitivesService, progressManager).build()) //
				.next(postProcessingFlow().build()) //
				.end().build();
	}

	protected FlowBuilder<Flow> verificationCombinationAndGenerationFlow(PrimitivesServiceAPI primitivesService, ProgressManager progressManager) {
		Flow verificationAndCombinationFlow = new FlowBuilder<Flow>("verificationAndCombinationFlow").start(verificationAndCombinationStep()).end();

		Flow generationFlow = new FlowBuilder<Flow>("generationFlow").start(generateVotingCardStep(primitivesService)).end();

		Flow writingFlow = new FlowBuilder<Flow>("writingFlow").start(writeOutputStep(progressManager)).end();

		return new FlowBuilder<Flow>("processingFlow").split(commonBatchInfrastructure.stepExecutor(STR_REPLACED_AT_RUNTIME, INT_REPLACED_AT_RUNTIME))
				.add(verificationAndCombinationFlow, generationFlow, writingFlow);
	}

	@Bean
	@JobScope
	ItemWriter<List<VcIdCombinedReturnCodesGenerationValues>> computedValuesWriter() {
		return new ComputedValuesOutputWriter(computedValuesQueue());
	}

	@Bean("asymmetricServiceWithJobScope")
	@JobScope
	public AsymmetricServiceAPI asymmetricServiceWithJobScope(final GenericObjectPoolConfig genericObjectPoolConfig)
			throws GeneralCryptoLibException {
		return asymmetricServiceAPIServiceFactory(genericObjectPoolConfig).create();

	}

	@Bean
	public ServiceFactory<AsymmetricServiceAPI> asymmetricServiceAPIServiceFactory(final GenericObjectPoolConfig genericObjectPoolConfig) {
		return AsymmetricServiceFactoryHelper.getFactoryOfThreadSafeServices(genericObjectPoolConfig);
	}

	@Bean("verificationCardCredentialDataPackGeneratorWithJobScope")
	@JobScope
	public VerificationCardCredentialDataPackGenerator verificationCardCredentialDataPackGeneratorWithJobScope(
			final PrimitivesServiceAPI primitivesService,
			@Qualifier("asymmetricServiceWithJobScope")
			final AsymmetricServiceAPI asymmetricService, final X509CertificateGenerator certificatesGenerator,
			final ElGamalServiceAPI elgamalService) {

		final ExtendedKeyStoreService keyStoreService = new ExtendedKeyStoreService();
		final CryptoAPIRandomString cryptoRandomString = primitivesService.get32CharAlphabetCryptoRandomString();

		return new VerificationCardCredentialDataPackGenerator(asymmetricService, cryptoRandomString, certificatesGenerator, keyStoreService,
				elgamalService);

	}

	@Bean("certificateValidatorWithJobScope")
	@JobScope
	public PayloadSigningCertificateValidator certificateValidatorWithJobScope() {
		return new CryptolibPayloadSigningCertificateValidator();
	}

	@Bean("payloadVerifierWithJobScope")
	@JobScope
	public PayloadVerifier payloadVerifierWithJobScope(
			@Qualifier("asymmetricServiceWithJobScope")
					AsymmetricServiceAPI asymmetricServiceAPI,
			@Qualifier("certificateValidatorWithJobScope")
					PayloadSigningCertificateValidator certificateValidator) {
		return new CryptolibPayloadVerifier(asymmetricServiceAPI, certificateValidator);
	}

	@Bean
	@JobScope
	ItemWriter<List<VcIdCombinedReturnCodesGenerationValues>> derivedKeyCommitmentsWriter(
			@Value("#{jobExecutionContext['" + Constants.BASE_PATH + "']}")
			final String basePath,
			@Value("#{jobExecution}")
			final JobExecution jobExecution) {
		final VotingCardGenerationJobExecutionContext jobExecutionContext = new VotingCardGenerationJobExecutionContext(
				jobExecution.getExecutionContext());
		Path derivedKeysPath = commonBatchInfrastructure
				.getDataSerializationProvider(basePath, jobExecutionContext.getVotingCardSetId(), jobExecutionContext.getVerificationCardSetId())
				.getDerivedKeys();

		return new DerivedKeyCommitmentsWriter(derivedKeysPath.getParent(), maxNumCredentialsPerFile);
	}

	@Bean
	@JobScope
	ItemProcessor<NodeContributions, List<VcIdCombinedReturnCodesGenerationValues>> encryptedLongReturnCodesCombiner(
			@Value("#{jobExecutionContext['" + Constants.PLATFORM_ROOT_CA_CERTIFICATE + "']}")
			final String platformRootCACertificate) {

		X509Certificate certificate;
		try {
			certificate = (X509Certificate) PemUtils.certificateFromPem(platformRootCACertificate);
		} catch (GeneralCryptoLibException e) {
			throw new IllegalStateException("Error obtaining platform root certificate", e);
		}

		return new EncryptedLongReturnCodesCombiner(certificate);
	}

	@Bean
	@JobScope
	ItemReader<NodeContributions> nodeContributionsReader(
			@Value("#{jobExecutionContext['" + Constants.BASE_PATH + "']}")
			final String basePath,
			@Value("#{jobExecution}")
			final JobExecution jobExecution) {

		final VotingCardGenerationJobExecutionContext jobExecutionContext = new VotingCardGenerationJobExecutionContext(
				jobExecution.getExecutionContext());

		List<NodeContributionsPath> allComputeInputAndOutputFiles = commonBatchInfrastructure
				.getDataSerializationProvider(basePath, jobExecutionContext.getVotingCardSetId(), jobExecutionContext.getVerificationCardSetId())
				.getNodeContributions();

		return new NodeContributionsReader(allComputeInputAndOutputFiles);
	}

	protected FlowBuilder<Flow> postProcessingFlow() {
		return new FlowBuilder<Flow>("endFlow").start(writeVerificationDataStep());
	}

	protected FlowBuilder<Flow> preProcessingFlow() {
		return new FlowBuilder<Flow>("preparation").from(prepareJobExecutionContextStep()).next(prepareVotingCardGenerationDataStep());
	}

	@Bean
	public JobExecutionListener jobExecutionListener() {
		return new VotingCardGenerationJobListener();
	}

	/**
	 * This step loads all input data received from the request, necessary for voting card generation, in to the spring batch job execution context
	 *
	 * @return the step
	 */
	@Bean
	public Step prepareJobExecutionContextStep() {
		return commonBatchInfrastructure.getStepBuilder("prepareExecutionContext").tasklet(prepareJobExecutionContextTasklet()).build();
	}

	@Bean
	public Tasklet prepareJobExecutionContextTasklet() {
		return new PrepareJobExecutionContextTasklet();
	}

	/**
	 * This step creates and stores in a 'cache' bean some extra complex classes needed for the voting card generation that are not possible (or easy)
	 * to store in the standard spring batch job execution context.
	 *
	 * @return the step
	 */
	@Bean
	public Step prepareVotingCardGenerationDataStep() {
		return commonBatchInfrastructure.getStepBuilder("prepareVotingCardGenerationDataStep").tasklet(prepareVotingCardGenerationDataTasklet())
				.build();
	}

	@Bean
	public Tasklet prepareVotingCardGenerationDataTasklet() {
		return new PrepareVotingCardGenerationDataTasklet();
	}

	@Bean
	public Step verificationAndCombinationStep() {
		CompositeItemWriter<List<VcIdCombinedReturnCodesGenerationValues>> compositeComptuedValuesWriter = new CompositeItemWriter<>();
		compositeComptuedValuesWriter.setDelegates(Arrays.asList(computedValuesWriter(), derivedKeyCommitmentsWriter(STR_REPLACED_AT_RUNTIME, null)));

		return commonBatchInfrastructure.getStepBuilder("verificationAndCombinationStep")
				.<NodeContributions, List<VcIdCombinedReturnCodesGenerationValues>>chunk(1)
				.reader(nodeContributionsReader(STR_REPLACED_AT_RUNTIME, null)).processor(encryptedLongReturnCodesCombiner(STR_REPLACED_AT_RUNTIME))
				.writer(compositeComptuedValuesWriter).listener(nodeContributionsStepListener()).build();
	}

	/**
	 * This step generates the voting cards and other associated data one by one and puts them in a queue used as a buffer for writing to the various
	 * output files.
	 *
	 * @return the step
	 */
	@Bean
	public Step generateVotingCardStep(PrimitivesServiceAPI primitivesService) {
		return commonBatchInfrastructure.getStepBuilder("generateVotingCardStep")
				.<VcIdCombinedReturnCodesGenerationValues, GeneratedVotingCardOutput>chunk(1).reader(computedValuesReader())
				.processor(votingCardGenerator(primitivesService, null)).writer(generatedVotingCardOutputWriter())
				.taskExecutor(commonBatchInfrastructure.stepExecutor(STR_REPLACED_AT_RUNTIME, INT_REPLACED_AT_RUNTIME))
				.throttleLimit(Integer.parseInt(stepConcurrency)).listener(generationStepExecutionListener()).build();
	}

	@Bean
	@JobScope
	ItemReader<VcIdCombinedReturnCodesGenerationValues> computedValuesReader() {
		return new ComputedValuesReader(computedValuesQueue());
	}

	@Bean
	@JobScope
	StepExecutionListener generationStepExecutionListener() {
		return new VotingCardGenerationStepListener(generationOutputQueue());
	}

	@Bean
	@JobScope
	StepExecutionListener nodeContributionsStepListener() {
		return new NodeContributionsStepListener(computedValuesQueue());
	}

	@Bean
	public Step writeOutputStep(ProgressManager progressManager) {
		return commonBatchInfrastructure.getStepBuilder("writeOutputStep").<GeneratedVotingCardOutput, GeneratedVotingCardOutput>chunk(1)
				.reader(generationOutputQueueReader())
				.writer(compositeOutputWriter(STR_REPLACED_AT_RUNTIME, STR_REPLACED_AT_RUNTIME, STR_REPLACED_AT_RUNTIME))
				.listener(votingCardGeneratedOutputWriterListener(progressManager, STR_REPLACED_AT_RUNTIME, null)).build();
	}

	@Bean
	public Step writeVerificationDataStep() {
		return commonBatchInfrastructure.getStepBuilder("writeVerificationData")
				.tasklet(writeVerificationDataTasklet(STR_REPLACED_AT_RUNTIME, STR_REPLACED_AT_RUNTIME, STR_REPLACED_AT_RUNTIME)).build();
	}

	@Bean
	@JobScope
	Tasklet writeVerificationDataTasklet(
			@Value("#{jobExecutionContext['" + Constants.BASE_PATH + "']}")
			final String outputPath,
			@Value("#{jobExecutionContext['" + Constants.VOTING_CARD_SET_ID + "']}")
			final String votingCardSetId,
			@Value("#{jobExecutionContext['" + Constants.VERIFICATION_CARD_SET_ID + "']}")
			final String verificationCardSetId) {

		final VotersSerializationDestProvider destProvider = commonBatchInfrastructure
				.getDataSerializationProvider(outputPath, votingCardSetId, verificationCardSetId);
		return new WriteVerificationDataTasklet(destProvider, objectContext);
	}

	@Bean
	@JobScope
	GeneratedVotingCardOutputWriter generatedVotingCardOutputWriter() {
		return new GeneratedVotingCardOutputWriter(generationOutputQueue());
	}

	@Bean
	@JobScope
	VotingCardGenerator votingCardGenerator(PrimitivesServiceAPI primitivesService,
			@Value("#{jobExecution}")
			final JobExecution jobExecution) {

		final CryptoAPIPBKDFDeriver pbkdfDeriver = primitivesService.getPBKDFDeriver();
		final VotingCardGenerationJobExecutionContext jobExecutionContext = new VotingCardGenerationJobExecutionContext(
				jobExecution.getExecutionContext());
		final VotersParametersHolder holder = objectContext.get(jobExecutionContext.getJobInstanceId(), VotersParametersHolder.class);
		final VotersGenerationTaskStaticContentProvider staticContentProvider = new VotersGenerationTaskStaticContentProvider(
				holder.getEncryptionParameters(), holder);

		return new VotingCardGenerator(holder, jobExecutionContext, pbkdfDeriver, staticContentProvider);
	}

	@Bean
	@JobScope
	VotingCardGeneratedOutputWriterListener votingCardGeneratedOutputWriterListener(ProgressManager progressManager,
			@Value("#{jobExecutionContext['" + Constants.JOB_INSTANCE_ID + "']}")
			final String jobInstanceId,
			@Value("#{jobExecution}")
			final JobExecution jobExecution) {

		return new VotingCardGeneratedOutputWriterListener(UUID.fromString(jobInstanceId), jobExecution.getExecutionContext(), progressManager);
	}

	@Bean
	@JobScope
	CompositeOutputWriter compositeOutputWriter(
			@Value("#{jobExecutionContext['" + Constants.BASE_PATH + "']}")
			final String basePath,
			@Value("#{jobExecutionContext['" + Constants.VOTING_CARD_SET_ID + "']}")
			final String votingCardSetId,
			@Value("#{jobExecutionContext['" + Constants.VERIFICATION_CARD_SET_ID + "']}")
			final String verificationCardSetId) {

		final CompositeOutputWriter writer = new CompositeOutputWriter();
		writer.setDelegates(Arrays.asList(voterInformationWriter(basePath, votingCardSetId, verificationCardSetId),
				credentialDataWriter(basePath, votingCardSetId, verificationCardSetId),
				codesMappingTableWriter(basePath, votingCardSetId, verificationCardSetId),
				verificationCardDataWriter(basePath, votingCardSetId, verificationCardSetId),
				extendedAuthenticationWriter(basePath, votingCardSetId, verificationCardSetId)));
		return writer;
	}

	VerificationCardDataWriter verificationCardDataWriter(final String basePath, final String votingCardSetId, final String verificationCardSetId) {
		final Path path = commonBatchInfrastructure.getDataSerializationProvider(basePath, votingCardSetId, verificationCardSetId)
				.getVerificationCardData();
		return new VerificationCardDataWriter(path, maxNumCredentialsPerFile);
	}

	ExtendedAuthenticationWriter extendedAuthenticationWriter(final String basePath, final String votingCardSetId,
			final String verificationCardSetId) {
		final Path path = commonBatchInfrastructure.getDataSerializationProvider(basePath, votingCardSetId, verificationCardSetId)
				.getTempExtendedAuth("");
		return new ExtendedAuthenticationWriter(path, maxNumCredentialsPerFile);
	}

	CodesMappingTableWriter codesMappingTableWriter(final String basePath, final String votingCardSetId, final String verificationCardSetId) {
		final Path path = commonBatchInfrastructure.getDataSerializationProvider(basePath, votingCardSetId, verificationCardSetId)
				.getCodesMappingTablesContextData();
		return new CodesMappingTableWriter(path, maxNumCredentialsPerFile);
	}

	CredentialDataWriter credentialDataWriter(final String basePath, final String votingCardSetId, final String verificationCardSetId) {
		final Path path = commonBatchInfrastructure.getDataSerializationProvider(basePath, votingCardSetId, verificationCardSetId)
				.getCredentialsData();
		return new CredentialDataWriter(path, maxNumCredentialsPerFile);
	}

	VoterInformationWriter voterInformationWriter(final String basePath, final String votingCardSetId, final String verificationCardSetId) {
		final Path path = commonBatchInfrastructure.getDataSerializationProvider(basePath, votingCardSetId, verificationCardSetId)
				.getVoterInformation();
		return new VoterInformationWriter(path, maxNumCredentialsPerFile);
	}

	@Bean
	@JobScope
	BlockingQueue<GeneratedVotingCardOutput> generationOutputQueue() {
		return new LinkedBlockingQueue<>();
	}

	@Bean
	@JobScope
	OutputQueueReader generationOutputQueueReader() {
		return new OutputQueueReader(generationOutputQueue());
	}

	@Bean
	@JobScope
	BlockingQueue<VcIdCombinedReturnCodesGenerationValues> computedValuesQueue() {
		return new LinkedBlockingQueue<>(queueCapacity);
	}

	@Bean
	@JobScope
	public ProvidedChallengeSource providedChallengeSource(
			@Value("#{jobExecutionContext['" + Constants.BASE_PATH + "']}")
			final String outputPath,
			@Value("#{jobExecutionContext['" + Constants.VOTING_CARD_SET_ID + "']}")
			final String votingCardSetId,
			@Value("#{jobExecutionContext['" + Constants.VERIFICATION_CARD_SET_ID + "']}")
			final String verificationCardSetId) {

		final Path providedChallengePath = commonBatchInfrastructure.getDataSerializationProvider(outputPath, votingCardSetId, verificationCardSetId)
				.getProvidedChallenge();
		return new SequentialProvidedChallengeSource(providedChallengePath);
	}

	@Bean
	@JobScope
	AuthenticationKeyCryptoService authKeyService() {
		return new AuthenticationKeyCryptoService();
	}

	@Bean
	@JobScope
	ChallengeGeneratorFactory challengeGeneratorFactory() {
		return new ChallengeGeneratorFactory();
	}

	@Bean
	@JobScope
	ChallengeGenerator challengeGenerator(final ChallengeGeneratorFactory challengeGeneratorFactory) {
		String property = env.getProperty("challenge.generator.type");
		ChallengeGeneratorStrategyType challengeGeneratorStrategyType = ChallengeGeneratorStrategyType.valueOf(property);
		return challengeGeneratorFactory.createStrategy(challengeGeneratorStrategyType);
	}

	@Bean
	@JobScope
	ChallengeServiceAPI challengeService(final PrimitivesServiceAPI primitivesService, final ChallengeGenerator challengeGenerator) {
		return new ChallengeService(primitivesService, challengeGenerator);
	}

	@Bean
	@JobScope
	AuthenticationGeneratorFactory authenticationGeneratorFactory() {
		return new AuthenticationGeneratorFactory();
	}

	@Bean
	@JobScope
	AuthenticationKeyGenerator authenticationKeyGenerator(final AuthenticationGeneratorFactory authenticationGeneratorFactory) {
		String property = env.getProperty("auth.generator.type");
		AuthenticationKeyGeneratorStrategyType authenticationKeyGeneratorStrategyType = AuthenticationKeyGeneratorStrategyType.valueOf(property);
		return authenticationGeneratorFactory.createStrategy(authenticationKeyGeneratorStrategyType);
	}

	@Bean
	@JobScope
	ExtendedAuthenticationService createAndHandleAuthKey(final AuthenticationKeyCryptoService authKeyService,
			final AuthenticationKeyGenerator authenticationKeyGenerator, final ChallengeServiceAPI challengeService) {
		return new ExtendedAuthenticationService(authKeyService, authenticationKeyGenerator, challengeService);
	}

	@Bean
	@JobScope
	StartVotingKeyService startVotingKeyService(final AuthenticationKeyGenerator authenticationKeyGenerator) {
		return new StartVotingKeyService(authenticationKeyGenerator);
	}

	@Bean
	HashService hashService() {
		return new HashService();
	}

	@Bean
	CryptolibPayloadSignatureService payloadSignatureService(final AsymmetricServiceAPI asymmetricServiceAPI) {
		return new CryptolibPayloadSignatureService(asymmetricServiceAPI);
	}

	@Bean
	public ObjectMapper objectMapper() {
		return ObjectMapperMixnetConfig.getNewInstance();
	}

	@Bean
	PathResolver pathResolver() {
		return new PrefixPathResolver(prefix);
	}

}
