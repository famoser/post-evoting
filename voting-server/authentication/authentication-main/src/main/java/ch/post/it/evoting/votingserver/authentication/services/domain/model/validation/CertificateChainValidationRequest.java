/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.authentication.services.domain.model.validation;

/**
 * Class to represent a request of Certificate Chain validation.
 */
public class CertificateChainValidationRequest {

	/**
	 * The certificate content to be validated.
	 */
	private String certificateContent;

	public String getCertificateContent() {
		return certificateContent;
	}

	public void setCertificateContent(String certificateContent) {
		this.certificateContent = certificateContent;
	}
}
