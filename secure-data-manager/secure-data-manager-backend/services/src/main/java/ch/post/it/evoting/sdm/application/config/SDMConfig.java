/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.config;

import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.primitives.PrimitivesServiceAPI;
import ch.post.it.evoting.cryptolib.api.services.ServiceFactory;
import ch.post.it.evoting.cryptolib.api.symmetric.SymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.primitives.service.PrimitivesServiceFactoryHelper;
import ch.post.it.evoting.cryptolib.returncode.VoterCodesService;
import ch.post.it.evoting.cryptolib.returncode.VoterCodesServiceImpl;
import ch.post.it.evoting.cryptolib.symmetric.service.SymmetricServiceFactoryHelper;
import ch.post.it.evoting.cryptoprimitives.CryptoPrimitives;
import ch.post.it.evoting.cryptoprimitives.CryptoPrimitivesService;
import ch.post.it.evoting.cryptoprimitives.hashing.HashService;
import ch.post.it.evoting.cryptoprimitives.mixnet.Mixnet;
import ch.post.it.evoting.cryptoprimitives.mixnet.MixnetService;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofService;
import ch.post.it.evoting.sdm.application.service.KeyStoreService;
import ch.post.it.evoting.sdm.application.service.KeyStoreServiceImpl;
import ch.post.it.evoting.sdm.utils.EncryptionParametersLoader;
import ch.post.it.evoting.sdm.utils.SignatureVerifier;

/**
 * This class allows to configure environment of the SDM and its corresponding configuration properties.
 */
@Configuration
@EnableScheduling
@PropertySource("${upload.config.source}")
@PropertySource("${download.config.source}")
@PropertySource("classpath:config/services/smartcard.properties")
public class SDMConfig {

	private static final String SMARTCARDS_PROFILE = "smartcards.profile";
	private static final String PROFILE_E2E = "e2e";

	@Value("${connection.time.out}")
	private String connectionTimeOut;

	@Value("${read.time.out}")
	private String readTimeOut;

	@Value("${write.time.out}")
	private String writeTimeOut;

	@Bean
	public ObjectReader readerForDeserialization() {
		ObjectMapper mapper = mapperForDeserialization();
		return mapper.reader();
	}

	private ObjectMapper mapperForDeserialization() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		mapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
		mapper.findAndRegisterModules();
		return mapper;
	}

	/**
	 * Class for accessing rest-like services. If we have problems de-serializing json data we may have to create a custom jackson http message
	 * converter and added it to the RestTemplate
	 *
	 * @return spring rest template
	 */
	@Bean
	public RestTemplate restClient() {
		return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
	}

	@Bean
	SmartCardConfig getSmartCardConfigDefault(Environment env) {
		return PROFILE_E2E.equals(env.getProperty(SMARTCARDS_PROFILE)) ? SmartCardConfig.FILE : SmartCardConfig.SMART_CARD;
	}

	@Bean
	KeyStoreService getKeyStoreService() {
		return new KeyStoreServiceImpl();
	}

	@Bean
	RestConnectionConfig restConnectionConfig() {
		return new RestConnectionConfig(readTimeOut, writeTimeOut, connectionTimeOut);
	}

	@Bean
	public ServiceFactory<PrimitivesServiceAPI> primitivesServiceAPIServiceFactory(final GenericObjectPoolConfig genericObjectPoolConfig) {
		return PrimitivesServiceFactoryHelper.getFactoryOfThreadSafeServices(genericObjectPoolConfig);
	}

	@Bean
	public ServiceFactory<SymmetricServiceAPI> symmetricServiceAPIServiceFactory(final GenericObjectPoolConfig genericObjectPoolConfig) {
		return SymmetricServiceFactoryHelper.getFactoryOfThreadSafeServices(genericObjectPoolConfig);
	}

	@Bean
	public PrimitivesServiceAPI primitivesServiceAPI(final ServiceFactory<PrimitivesServiceAPI> primitivesServiceAPIServiceFactory)
			throws GeneralCryptoLibException {
		return primitivesServiceAPIServiceFactory.create();
	}

	@Bean
	public SymmetricServiceAPI symmetricServiceAPI(final ServiceFactory<SymmetricServiceAPI> symmetricServiceAPIServiceFactory)
			throws GeneralCryptoLibException {
		return symmetricServiceAPIServiceFactory.create();
	}

	@Bean
	public VoterCodesService voterCodesService(PrimitivesServiceAPI primitivesService, SymmetricServiceAPI symmetricService) {
		return new VoterCodesServiceImpl(primitivesService, symmetricService);
	}

	@Bean
	public SignatureVerifier signatureVerifier() throws CertificateException, NoSuchProviderException {
		return new SignatureVerifier();
	}

	@Bean
	public CryptoPrimitives cryptoPrimitives() {
		return CryptoPrimitivesService.get();
	}

	@Bean
	public Mixnet mixnet() {
		return new MixnetService();
	}

	@Bean
	public ZeroKnowledgeProof zeroKnowledgeProof() {
		return new ZeroKnowledgeProofService();
	}

	@Bean
	@Qualifier("cryptoPrimitivesHashService")
	public HashService cryptoPrimitivesHashService() {
		return new HashService();
	}

	@Bean
	public EncryptionParametersLoader encryptionParametersLoader() throws CertificateException, NoSuchProviderException {
		return new EncryptionParametersLoader();
	}
}

