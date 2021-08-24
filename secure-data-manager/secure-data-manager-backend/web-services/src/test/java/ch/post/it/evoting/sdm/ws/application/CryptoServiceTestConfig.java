/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.ws.application;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.primitives.PrimitivesServiceAPI;
import ch.post.it.evoting.cryptolib.api.proofs.ProofsServiceAPI;
import ch.post.it.evoting.cryptolib.certificates.utils.CryptolibPayloadSigningCertificateValidator;
import ch.post.it.evoting.cryptolib.certificates.utils.PayloadSigningCertificateValidator;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;
import ch.post.it.evoting.cryptolib.primitives.service.PrimitivesService;
import ch.post.it.evoting.cryptolib.proofs.service.ProofsService;
import ch.post.it.evoting.domain.election.payload.verify.CryptolibPayloadVerifier;
import ch.post.it.evoting.domain.election.payload.verify.PayloadVerifier;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectWriterImpl;

/**
 * MixDecValController bean for tests.
 */
@Configuration
class CryptoServiceTestConfig {

	@Bean
	PrimitivesServiceAPI primitivesService() {
		return new PrimitivesService();
	}

	@Bean
	ElGamalServiceAPI elGamalService() {
		return new ElGamalService();
	}

	@Bean
	ProofsServiceAPI proofsService() {
		return new ProofsService();
	}

	@Bean
	PayloadVerifier payloadVerifier(AsymmetricServiceAPI asymmetricService, PayloadSigningCertificateValidator certificateValidator) {
		return new CryptolibPayloadVerifier(asymmetricService, certificateValidator);
	}

	@Bean
	PayloadSigningCertificateValidator certificateValidator() {
		return new CryptolibPayloadSigningCertificateValidator();
	}

	@Bean
	public StreamSerializableObjectWriterImpl serializableObjectWriter() {
		return new StreamSerializableObjectWriterImpl();
	}
}
