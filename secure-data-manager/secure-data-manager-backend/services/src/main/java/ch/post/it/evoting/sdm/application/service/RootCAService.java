/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import java.security.cert.X509Certificate;

public interface RootCAService {

	/**
	 * Stores a given certificate.
	 *
	 * @param certificate the certificate
	 */
	void save(X509Certificate certificate) throws CertificateManagementException;

	/**
	 * Retrieves a certificate.
	 *
	 * @return the certificate
	 */
	X509Certificate load() throws CertificateManagementException;

}
