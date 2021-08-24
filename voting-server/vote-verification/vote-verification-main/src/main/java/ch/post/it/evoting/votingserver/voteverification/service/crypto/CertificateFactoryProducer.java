/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.service.crypto;

import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.enterprise.inject.Produces;

/**
 *
 */
public class CertificateFactoryProducer {

	@Produces
	public CertificateFactory getInstance() throws CertificateException {

		return CertificateFactory.getInstance("X.509");
	}

}
