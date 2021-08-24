/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring;

import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.Base64;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.certificates.CertificatesServiceAPI;
import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.extendedkeystore.KeyStoreService;
import ch.post.it.evoting.cryptolib.api.primitives.PrimitivesServiceAPI;
import ch.post.it.evoting.cryptolib.api.proofs.ProofsServiceAPI;
import ch.post.it.evoting.cryptolib.api.securerandom.CryptoAPIRandomString;
import ch.post.it.evoting.cryptolib.api.services.ServiceFactory;
import ch.post.it.evoting.cryptolib.api.symmetric.SymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricServiceFactoryHelper;
import ch.post.it.evoting.cryptolib.certificates.factory.X509CertificateGenerator;
import ch.post.it.evoting.cryptolib.certificates.factory.builders.CertificateDataBuilder;
import ch.post.it.evoting.cryptolib.certificates.service.CertificatesServiceFactoryHelper;
import ch.post.it.evoting.cryptolib.elgamal.codec.ElGamalCiphertextCodec;
import ch.post.it.evoting.cryptolib.elgamal.codec.ElGamalCiphertextCodecImpl;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalServiceFactoryHelper;
import ch.post.it.evoting.cryptolib.extendedkeystore.service.ExtendedKeyStoreService;
import ch.post.it.evoting.cryptolib.primitives.service.PrimitivesServiceFactoryHelper;
import ch.post.it.evoting.cryptolib.proofs.service.ProofsServiceFactoryHelper;
import ch.post.it.evoting.cryptolib.returncode.VoterCodesService;
import ch.post.it.evoting.cryptolib.returncode.VoterCodesServiceImpl;
import ch.post.it.evoting.cryptolib.symmetric.service.SymmetricServiceFactoryHelper;
import ch.post.it.evoting.logging.api.factory.LoggingFactory;
import ch.post.it.evoting.logging.api.formatter.MessageFormatter;
import ch.post.it.evoting.logging.api.writer.LoggingWriter;
import ch.post.it.evoting.logging.core.factory.LoggingFactoryLog4j;
import ch.post.it.evoting.logging.core.formatter.PipeSeparatedFormatter;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.commands.api.ConfigurationService;
import ch.post.it.evoting.sdm.config.commands.ballotbox.BallotBoxGenerator;
import ch.post.it.evoting.sdm.config.commands.ballotbox.BallotBoxHolderInitializer;
import ch.post.it.evoting.sdm.config.commands.electionevent.CreateElectionEventGenerator;
import ch.post.it.evoting.sdm.config.commands.electionevent.CreateElectionEventHolderInitializer;
import ch.post.it.evoting.sdm.config.commands.electionevent.CreateElectionEventSerializer;
import ch.post.it.evoting.sdm.config.commands.electionevent.datapacks.beans.ElectionInputDataPackUtils;
import ch.post.it.evoting.sdm.config.commands.electionevent.datapacks.generators.ElectionCredentialDataPackGenerator;
import ch.post.it.evoting.sdm.config.commands.progress.ProgressManager;
import ch.post.it.evoting.sdm.config.commands.progress.ProgressManagerImpl;
import ch.post.it.evoting.sdm.config.commands.voters.JobExecutionObjectContext;
import ch.post.it.evoting.sdm.config.commands.voters.JobSelectionStrategy;
import ch.post.it.evoting.sdm.config.commands.voters.PropertiesBasedJobSelectionStrategy;
import ch.post.it.evoting.sdm.config.commands.voters.VotersHolderInitializer;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.generators.VerificationCardCredentialDataPackGenerator;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.generators.VerificationCardSetCredentialDataPackGenerator;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.generators.VotingCardCredentialDataPackGenerator;
import ch.post.it.evoting.sdm.readers.ConfigurationInputReader;
import ch.post.it.evoting.sdm.utils.ConfigObjectMapper;
import ch.post.it.evoting.sdm.utils.EncryptionParametersLoader;
import ch.post.it.evoting.sdm.utils.IDsParser;
import ch.post.it.evoting.sdm.utils.KeyStoreReader;
import ch.post.it.evoting.sdm.utils.X509CertificateLoader;

@Configuration
@PropertySource("classpath:properties/springConfig.properties")
public class SpringConfigServices {

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public GenericObjectPoolConfig genericObjectPoolConfig(Environment env) {

		GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
		genericObjectPoolConfig.setMaxTotal(Integer.parseInt(env.getProperty("services.cryptolib.pool.size")));
		genericObjectPoolConfig.setMaxIdle(Integer.parseInt(env.getProperty("services.cryptolib.timeout")));

		return genericObjectPoolConfig;
	}

	@Bean
	public ServiceFactory<SymmetricServiceAPI> symmetricServiceFactoryHelper(final GenericObjectPoolConfig genericObjectPoolConfig) {
		return SymmetricServiceFactoryHelper.getFactoryOfThreadSafeServices(genericObjectPoolConfig);
	}

	@Bean
	public SymmetricServiceAPI symmetricServiceAPI(final ServiceFactory<SymmetricServiceAPI> symmetricServiceFactoryHelper)
			throws GeneralCryptoLibException {
		return symmetricServiceFactoryHelper.create();
	}

	@Bean
	public ServiceFactory<AsymmetricServiceAPI> asymmetricServiceAPIServiceFactory(final GenericObjectPoolConfig genericObjectPoolConfig) {
		return AsymmetricServiceFactoryHelper.getFactoryOfThreadSafeServices(genericObjectPoolConfig);
	}

	@Bean("asymmetricServiceAPI")
	public AsymmetricServiceAPI asymmetricServiceAPI(final ServiceFactory<AsymmetricServiceAPI> asymmetricServiceAPIServiceFactory)
			throws GeneralCryptoLibException {
		return asymmetricServiceAPIServiceFactory.create();
	}

	@Bean
	public ServiceFactory<CertificatesServiceAPI> certificatesServiceAPIServiceFactory(final GenericObjectPoolConfig genericObjectPoolConfig) {
		return CertificatesServiceFactoryHelper.getFactoryOfThreadSafeServices(genericObjectPoolConfig);
	}

	@Bean
	public CertificatesServiceAPI certificatesServiceAPI(final ServiceFactory<CertificatesServiceAPI> certificatesServiceAPIServiceFactory)
			throws GeneralCryptoLibException {
		return certificatesServiceAPIServiceFactory.create();
	}

	@Bean
	public KeyStoreService storesServiceAPI() {
		return new ExtendedKeyStoreService();
	}

	@Bean
	public ServiceFactory<PrimitivesServiceAPI> primitivesServiceAPIServiceFactory(final GenericObjectPoolConfig genericObjectPoolConfig) {
		return PrimitivesServiceFactoryHelper.getFactoryOfThreadSafeServices(genericObjectPoolConfig);
	}

	@Bean
	public PrimitivesServiceAPI primitivesServiceAPI(final ServiceFactory<PrimitivesServiceAPI> primitivesServiceAPIServiceFactory)
			throws GeneralCryptoLibException {
		return primitivesServiceAPIServiceFactory.create();
	}

	@Bean
	public ServiceFactory<ProofsServiceAPI> proofsServiceAPIServiceFactory(final GenericObjectPoolConfig genericObjectPoolConfig) {
		return ProofsServiceFactoryHelper.getFactoryOfThreadSafeServices(genericObjectPoolConfig);
	}

	@Bean
	public ProofsServiceAPI proofsServiceAPI(final ServiceFactory<ProofsServiceAPI> proofsServiceAPIServiceFactory) throws GeneralCryptoLibException {
		return proofsServiceAPIServiceFactory.create();
	}

	@Bean
	public VoterCodesService voterCodesService(final PrimitivesServiceAPI primitivesService, final SymmetricServiceAPI symmetricService) {
		return new VoterCodesServiceImpl(primitivesService, symmetricService);
	}

	@Bean
	public CryptoAPIRandomString cryptoAPIRandomString(final PrimitivesServiceAPI primitivesServiceAPI) {
		return primitivesServiceAPI.get32CharAlphabetCryptoRandomString();
	}

	@Bean
	public CertificateDataBuilder certificateDataBuilder() {
		return new CertificateDataBuilder();
	}

	@Bean
	public X509CertificateLoader x509CertificateLoader() {
		return new X509CertificateLoader();
	}

	@Bean
	public X509CertificateGenerator certificatesGenerator(final CertificatesServiceAPI certificatesService,
			final CertificateDataBuilder certificateDataBuilder) {
		return new X509CertificateGenerator(certificatesService, certificateDataBuilder);
	}

	@Bean
	public ElectionCredentialDataPackGenerator electionCredentialDataPackGenerator(
			@Qualifier("asymmetricServiceAPI")
			final AsymmetricServiceAPI asymmetricService, final X509CertificateGenerator certificatesGenerator, final KeyStoreService storesService,
			final CryptoAPIRandomString cryptoRandomString) {
		return new ElectionCredentialDataPackGenerator(asymmetricService, certificatesGenerator, storesService, cryptoRandomString);
	}

	@Bean
	public VotingCardCredentialDataPackGenerator votingCardCredentialDataPackGenerator(
			@Qualifier("asymmetricServiceAPI")
			final AsymmetricServiceAPI asymmetricService, final X509CertificateGenerator certificatesGenerator, final KeyStoreService storesService,
			final CryptoAPIRandomString cryptoRandomString) {
		return new VotingCardCredentialDataPackGenerator(asymmetricService, certificatesGenerator, storesService, cryptoRandomString);

	}

	@Bean
	public VerificationCardSetCredentialDataPackGenerator verificationCardSetCredentialDataPackGenerator(
			@Qualifier("asymmetricServiceAPI")
			final AsymmetricServiceAPI asymmetricService, final X509CertificateGenerator certificatesGenerator, final KeyStoreService storesService,
			final CryptoAPIRandomString cryptoRandomString, final ElGamalServiceAPI elGamalService) {
		return new VerificationCardSetCredentialDataPackGenerator(asymmetricService, certificatesGenerator, storesService, cryptoRandomString,
				elGamalService);
	}

	@Bean
	public CreateElectionEventGenerator createElectionEventGenerator(final ElectionCredentialDataPackGenerator electionCredentialDataPackGenerator,
			final ProgressManager votersProgressManager) {
		return new CreateElectionEventGenerator(electionCredentialDataPackGenerator, votersProgressManager);
	}

	@Bean
	public BallotBoxHolderInitializer createBallotBoxHolderInitializer(final ConfigurationInputReader configurationInputReader,
			final X509CertificateLoader x509CertificateLoader, final EncryptionParametersLoader encryptionParametersLoader,
			final KeyStoreReader keyStoreReader) {

		return new BallotBoxHolderInitializer(configurationInputReader, x509CertificateLoader, encryptionParametersLoader, keyStoreReader);
	}

	@Bean
	public CreateElectionEventHolderInitializer createElectionEventHolderInitializer(final ConfigurationInputReader configurationInputReader) {

		return new CreateElectionEventHolderInitializer(configurationInputReader);
	}

	@Bean
	public VotersHolderInitializer createVotingCardSetInitializer(final ConfigurationInputReader configurationInputReader,
			final X509CertificateLoader x509CertificateLoader, final KeyStoreService storesService,
			final EncryptionParametersLoader encryptionParametersLoader) {

		return new VotersHolderInitializer(configurationInputReader, x509CertificateLoader, storesService, encryptionParametersLoader);
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	public BallotBoxGenerator createBallotBoxGenerator(final ElectionCredentialDataPackGenerator electionCredentialDataPackGenerator) {
		return new BallotBoxGenerator(electionCredentialDataPackGenerator);
	}

	@Bean
	public ConfigurationInputReader configurationInputReader() {
		return new ConfigurationInputReader();
	}

	@Bean
	public CreateElectionEventSerializer createElectionEventSerializer() {
		return new CreateElectionEventSerializer();
	}

	@Bean
	public KeyStoreReader keyStoreReader() {
		return new KeyStoreReader();
	}

	@Bean
	public ElectionInputDataPackUtils inputDataPackUtils() {
		return new ElectionInputDataPackUtils();
	}

	@Bean
	public IDsParser idsParser() {
		return new IDsParser();
	}

	@Bean
	public VerificationCardCredentialDataPackGenerator verificationCardCredentialDataPackGenerator(final PrimitivesServiceAPI primitivesService,
			@Qualifier("asymmetricServiceAPI")
			final AsymmetricServiceAPI asymmetricService, final X509CertificateGenerator certificatesGenerator,
			final ElGamalServiceAPI elgamalService) {

		final ExtendedKeyStoreService keyStoreService = new ExtendedKeyStoreService();
		final CryptoAPIRandomString cryptoRandomString = primitivesService.get32CharAlphabetCryptoRandomString();

		return new VerificationCardCredentialDataPackGenerator(asymmetricService, cryptoRandomString, certificatesGenerator, keyStoreService,
				elgamalService);

	}

	@Bean
	public ProgressManager votersProgressManager() {
		return new ProgressManagerImpl();
	}

	@Bean
	public ConfigObjectMapper configObjectMapper() {
		return new ConfigObjectMapper();
	}

	@Bean
	public EncryptionParametersLoader encryptionParametersLoader() throws CertificateException, NoSuchProviderException {
		return new EncryptionParametersLoader();
	}

	@Bean
	public ServiceFactory<ElGamalServiceAPI> elGamalServiceAPIServiceFactory(final GenericObjectPoolConfig genericObjectPoolConfig) {
		return ElGamalServiceFactoryHelper.getFactoryOfThreadSafeServices(genericObjectPoolConfig);
	}

	@Bean
	public ElGamalServiceAPI elGamalServiceAPI(final ServiceFactory<ElGamalServiceAPI> elGamalServiceAPIServiceFactory)
			throws GeneralCryptoLibException {
		return elGamalServiceAPIServiceFactory.create();
	}

	@Bean
	public ConfigurationService configurationService() {
		return new ConfigurationService();
	}

	@Bean
	public MessageFormatter messageFormatter() {
		return new PipeSeparatedFormatter("OV", "SDM");
	}

	@Bean
	public LoggingFactory loggingFactory() {
		return new LoggingFactoryLog4j(messageFormatter());
	}

	@Bean
	public LoggingWriter defaultLoggingWriter() {
		return loggingFactory().getLogger("splunkable");
	}

	@Bean
	public ProgressManager progressManager() {
		return new ProgressManagerImpl();
	}

	@Bean
	public Base64.Encoder encoder() {
		return Base64.getEncoder();
	}

	@Bean
	@Qualifier("urlSafeEncoder")
	public Base64.Encoder urlSafeEncoder() {
		return Base64.getUrlEncoder();
	}

	@Bean
	public JobSelectionStrategy jobSelectionStrategy(
			@Value("${spring.batch.jobs.qualifier:product}")
					String jobQualifier) {

		return new PropertiesBasedJobSelectionStrategy(Constants.VOTING_CARD_SET_GENERATION, jobQualifier);
	}

	@Bean
	public JobExecutionObjectContext stepExecutionObjectContext() {
		return new JobExecutionObjectContext();
	}

	@Bean
	public ElGamalCiphertextCodec elGamalCiphertextCodec() {
		return ElGamalCiphertextCodecImpl.getInstance();
	}

	@Configuration
	@Profile("standard")
	@PropertySource("classpath:properties/springConfig.properties")
	@PropertySource(value = "classpath:properties/springConfig-standard.properties")
	static class StandardConfiguration {
	}

	@Configuration
	@Profile("challenge")
	@PropertySource("classpath:properties/springConfig.properties")
	@PropertySource(value = "classpath:properties/springConfig-challenge.properties")
	static class ChallengeConfiguration {
	}
}
