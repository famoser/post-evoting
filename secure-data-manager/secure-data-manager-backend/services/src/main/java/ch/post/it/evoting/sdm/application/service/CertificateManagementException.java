/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

/**
 * An exception while retrieving or storing a certificate.
 */
public class CertificateManagementException extends Exception {

	private static final long serialVersionUID = 1588022932761467427L;

	public CertificateManagementException(Throwable cause) {
		super(cause);
	}
}
