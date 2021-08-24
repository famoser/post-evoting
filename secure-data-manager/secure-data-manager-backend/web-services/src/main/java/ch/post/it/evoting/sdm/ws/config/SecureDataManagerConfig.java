/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.ws.config;

import javax.annotation.PostConstruct;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.extendedkeystore.KeyStoreService;
import ch.post.it.evoting.cryptolib.api.services.ServiceFactory;
import ch.post.it.evoting.cryptolib.api.stores.StoresServiceAPI;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricServiceFactoryHelper;
import ch.post.it.evoting.cryptolib.certificates.utils.CryptolibPayloadSigningCertificateValidator;
import ch.post.it.evoting.cryptolib.certificates.utils.PayloadSigningCertificateValidator;
import ch.post.it.evoting.cryptolib.elgamal.codec.ElGamalCiphertextCodec;
import ch.post.it.evoting.cryptolib.elgamal.codec.ElGamalCiphertextCodecImpl;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalServiceFactoryHelper;
import ch.post.it.evoting.cryptolib.extendedkeystore.service.ExtendedKeyStoreServiceFactoryHelper;
import ch.post.it.evoting.cryptolib.stores.service.PollingStoresServiceFactory;
import ch.post.it.evoting.cryptoprimitives.hashing.HashService;
import ch.post.it.evoting.domain.election.payload.sign.CryptolibPayloadSigner;
import ch.post.it.evoting.domain.election.payload.sign.PayloadSigner;
import ch.post.it.evoting.domain.election.payload.verify.CryptolibPayloadVerifier;
import ch.post.it.evoting.domain.election.payload.verify.PayloadVerifier;
import ch.post.it.evoting.domain.mixnet.ObjectMapperMixnetConfig;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectWriterImpl;
import ch.post.it.evoting.logging.api.factory.LoggingFactory;
import ch.post.it.evoting.logging.api.formatter.MessageFormatter;
import ch.post.it.evoting.logging.api.writer.LoggingWriter;
import ch.post.it.evoting.logging.core.factory.LoggingFactoryLog4j;
import ch.post.it.evoting.logging.core.formatter.PipeSeparatedFormatter;
import ch.post.it.evoting.sdm.application.sign.FileSignerService;
import ch.post.it.evoting.sdm.commons.AbsolutePathResolver;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.commons.PrefixPathResolver;
import ch.post.it.evoting.sdm.infrastructure.DatabaseManager;
import ch.post.it.evoting.sdm.infrastructure.DatabaseManagerFactory;
import ch.post.it.evoting.sdm.readers.ConfigurationInputReader;

@Configuration
public class SecureDataManagerConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(SecureDataManagerConfig.class);

	@Value("${spring.profiles.active:}")
	private String activeProfiles;
	@Value("${user.home}")
	private String prefix;
	@Value("${database.type}")
	private String databaseType;
	@Value("${database.path}")
	private String databasePath;
	@Value("${database.name}")
	private String databaseName;
	@Value("${database.password.location}")
	private String passwordFile;

	@PostConstruct
	private void postConstruct() {
		LOGGER.info("Spring active profiles : {}", activeProfiles);
	}

	@Bean
	@Primary
	public PathResolver getPrefixPathResolver() {
		return new PrefixPathResolver(prefix);
	}

	@Bean
	@Qualifier("absolutePath")
	public PathResolver getAbsolutePathResolver() {
		return new AbsolutePathResolver();
	}

	@Bean
	public FileSignerService getFileSignerService(final AsymmetricServiceAPI asymmetricServiceAPI,
			@Qualifier("cryptoPrimitivesHashService")
			final HashService hashService) {
		return new FileSignerService(asymmetricServiceAPI, hashService);
	}

	@Bean
	public ServiceFactory<AsymmetricServiceAPI> asymmetricServiceAPIServiceFactory(final GenericObjectPoolConfig genericObjectPoolConfig) {
		return AsymmetricServiceFactoryHelper.getFactoryOfThreadSafeServices(genericObjectPoolConfig);
	}

	@Bean
	public AsymmetricServiceAPI asymmetricServiceAPI(final ServiceFactory<AsymmetricServiceAPI> asymmetricServiceAPIServiceFactory)
			throws GeneralCryptoLibException {
		return asymmetricServiceAPIServiceFactory.create();
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
	public ServiceFactory<KeyStoreService> extendedKeyStoreServiceAPIServiceFactory(final GenericObjectPoolConfig genericObjectPoolConfig) {
		return ExtendedKeyStoreServiceFactoryHelper.getFactoryOfThreadSafeServices(genericObjectPoolConfig);
	}

	@Bean
	public KeyStoreService extendedKeyStoreServiceAPI(final ServiceFactory<KeyStoreService> extendedKeyStoreServiceAPIServiceFactory)
			throws GeneralCryptoLibException {
		return extendedKeyStoreServiceAPIServiceFactory.create();
	}

	@Bean
	public ElGamalCiphertextCodec elGamalCiphertextCodec() {
		return ElGamalCiphertextCodecImpl.getInstance();
	}

	@Bean
	public GenericObjectPoolConfig genericObjectPoolConfig(Environment env) {

		GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
		genericObjectPoolConfig.setMaxTotal(Integer.parseInt(env.getProperty("services.cryptolib.pool.size")));
		genericObjectPoolConfig.setMaxIdle(Integer.parseInt(env.getProperty("services.cryptolib.timeout")));

		return genericObjectPoolConfig;
	}

	@Bean
	@Scope("singleton")
	public DatabaseManagerFactory databaseManagerFactory() {
		return new DatabaseManagerFactory(databaseType, databasePath, passwordFile);
	}

	@Bean(initMethod = "createDatabase")
	@Scope("singleton")
	public DatabaseManager databaseManager(DatabaseManagerFactory databaseManagerFactory) {
		return databaseManagerFactory.newDatabaseManager(databaseName);
	}

	@Bean
	public ConfigurationInputReader configurationInputReader() {
		return new ConfigurationInputReader();
	}

	@Bean
	PayloadSigner payloadSigner(AsymmetricServiceAPI asymmetricService) {
		return new CryptolibPayloadSigner(asymmetricService);
	}

	@Bean
	PayloadSigningCertificateValidator certificateValidator() {
		return new CryptolibPayloadSigningCertificateValidator();
	}

	@Bean
	PayloadVerifier payloadVerifier(AsymmetricServiceAPI asymmetricService, PayloadSigningCertificateValidator certificateValidator) {
		return new CryptolibPayloadVerifier(asymmetricService, certificateValidator);
	}

	@Bean
	public StoresServiceAPI storesService() {
		return new PollingStoresServiceFactory().create();
	}

	@Bean
	public StreamSerializableObjectWriterImpl serializableObjectWriter() {
		return new StreamSerializableObjectWriterImpl();
	}

	@Bean
	public ObjectMapper objectMapper() {
		return ObjectMapperMixnetConfig.getNewInstance();
	}

	@Bean
	public MessageFormatter messageFormatter() {
		return new PipeSeparatedFormatter("OV", "SDM");
	}

	@Bean
	public LoggingFactory loggingFactory(final MessageFormatter messageFormatter) {
		return new LoggingFactoryLog4j(messageFormatter);
	}

	@Bean
	public LoggingWriter defaultLoggingWriter(final LoggingFactory loggingFactory) {
		return loggingFactory.getLogger("splunkable");
	}
}
