/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.certificates.utils.CryptolibPayloadSigningCertificateValidator;
import ch.post.it.evoting.cryptolib.certificates.utils.PayloadSigningCertificateValidator;
import ch.post.it.evoting.domain.election.payload.verify.CryptolibPayloadVerifier;
import ch.post.it.evoting.domain.election.payload.verify.PayloadVerifier;

@Configuration
public class PayloadVerifierConfig {

	@Bean
	public PayloadSigningCertificateValidator certificateValidator() {
		return new CryptolibPayloadSigningCertificateValidator();
	}

	@Bean
	public PayloadVerifier payloadVerifier(AsymmetricServiceAPI asymmetricServiceAPI, PayloadSigningCertificateValidator certificateValidator) {
		return new CryptolibPayloadVerifier(asymmetricServiceAPI, certificateValidator);
	}

}


