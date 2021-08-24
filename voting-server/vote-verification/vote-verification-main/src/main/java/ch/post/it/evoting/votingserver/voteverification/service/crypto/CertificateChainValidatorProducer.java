/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.service.crypto;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import ch.post.it.evoting.cryptolib.certificates.utils.CryptolibPayloadSigningCertificateValidator;
import ch.post.it.evoting.cryptolib.certificates.utils.PayloadSigningCertificateValidator;

public class CertificateChainValidatorProducer {

	@Produces
	@ApplicationScoped
	public PayloadSigningCertificateValidator certificateChainValidator() {
		return new CryptolibPayloadSigningCertificateValidator();
	}

}


