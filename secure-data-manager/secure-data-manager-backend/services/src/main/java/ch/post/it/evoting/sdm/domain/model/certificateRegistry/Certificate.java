/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.certificateRegistry;

public class Certificate {

	private String certificateName;

	private String certificateContent;

	public String getCertificateName() {
		return certificateName;
	}

	public void setCertificateName(String certificateName) {
		this.certificateName = certificateName;
	}

	public String getCertificateContent() {
		return certificateContent;
	}

	public void setCertificateContent(String certificateContent) {
		this.certificateContent = certificateContent;
	}
}
