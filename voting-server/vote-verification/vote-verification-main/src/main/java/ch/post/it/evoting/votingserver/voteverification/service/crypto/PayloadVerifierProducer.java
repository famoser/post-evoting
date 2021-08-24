/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.service.crypto;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.certificates.utils.CryptolibPayloadSigningCertificateValidator;
import ch.post.it.evoting.cryptolib.certificates.utils.PayloadSigningCertificateValidator;
import ch.post.it.evoting.domain.election.payload.verify.CryptolibPayloadVerifier;
import ch.post.it.evoting.domain.election.payload.verify.PayloadVerifier;

/**
 * Producer of Payload signature verifier
 */
public class PayloadVerifierProducer {

	@Inject
	private AsymmetricServiceAPI asymmetricServiceAPI;

	@Produces
	@ApplicationScoped
	public PayloadVerifier payloadVerifier() {
		PayloadSigningCertificateValidator certificateChainValidator = new CryptolibPayloadSigningCertificateValidator();
		return new CryptolibPayloadVerifier(asymmetricServiceAPI, certificateChainValidator);
	}

}


