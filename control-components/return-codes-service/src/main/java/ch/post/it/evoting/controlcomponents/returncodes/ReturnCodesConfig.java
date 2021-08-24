/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponents.commons.keymanagement.KeysManager;
import ch.post.it.evoting.controlcomponents.returncodes.securelogger.SecureLogAppender;
import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.proofs.ProofsServiceAPI;
import ch.post.it.evoting.cryptolib.certificates.utils.CryptolibPayloadSigningCertificateValidator;
import ch.post.it.evoting.cryptolib.certificates.utils.PayloadSigningCertificateValidator;
import ch.post.it.evoting.cryptolib.elgamal.codec.ElGamalCiphertextCodec;
import ch.post.it.evoting.cryptolib.elgamal.codec.ElGamalCiphertextCodecImpl;
import ch.post.it.evoting.cryptolib.elgamal.exponentiation.ExponentiationService;
import ch.post.it.evoting.cryptolib.elgamal.exponentiation.ExponentiationServiceImpl;
import ch.post.it.evoting.cryptolib.mathematical.groups.activity.GroupElementsCompressor;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.domain.election.payload.sign.CryptolibPayloadSigner;
import ch.post.it.evoting.domain.election.payload.sign.PayloadSigner;
import ch.post.it.evoting.domain.election.payload.verify.CryptolibPayloadVerifier;
import ch.post.it.evoting.domain.election.payload.verify.PayloadVerifier;
import ch.post.it.evoting.domain.mixnet.ObjectMapperMixnetConfig;
import ch.post.it.evoting.logging.api.formatter.MessageFormatter;
import ch.post.it.evoting.logging.core.formatter.PipeSeparatedFormatter;

@Configuration
public class ReturnCodesConfig {

	@Bean
	ExponentiationService exponentiationService(ProofsServiceAPI proofsService) {
		return new ExponentiationServiceImpl(proofsService);
	}

	@Bean
	public SecureLogAppender secureLogAppender(KeysManager manager) {
		final SecureLogAppender secureLogAppender = SecureLogAppender.getAppender();
		secureLogAppender.setKeysManager(manager);
		return secureLogAppender;
	}

	@Bean
	public MessageFormatter messageFormatter() {
		return new PipeSeparatedFormatter("OV", "CCGEN");
	}

	@Bean
	public ElGamalCiphertextCodec elGamalCiphertextCodec() {
		return ElGamalCiphertextCodecImpl.getInstance();
	}

	@Bean
	public PayloadSigner payloadSigner(AsymmetricServiceAPI asymmetricService) {
		return new CryptolibPayloadSigner(asymmetricService);
	}

	@Bean
	public PayloadSigningCertificateValidator certificateChainValidator() {
		return new CryptolibPayloadSigningCertificateValidator();
	}

	@Bean
	public PayloadVerifier payloadVerifier(AsymmetricServiceAPI asymmetricService, PayloadSigningCertificateValidator certificateChainValidator) {
		return new CryptolibPayloadVerifier(asymmetricService, certificateChainValidator);
	}

	@Bean
	public GroupElementsCompressor<ZpGroupElement> groupElementsCompressor() {
		return new GroupElementsCompressor<>();
	}

	@Bean
	ObjectMapper objectMapper() {
		return ObjectMapperMixnetConfig.getNewInstance();
	}

}
