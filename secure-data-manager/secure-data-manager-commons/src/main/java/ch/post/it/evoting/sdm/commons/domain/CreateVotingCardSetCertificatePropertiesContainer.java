/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.commons.domain;

import java.util.Properties;

/**
 * Contains the properties needed to create the certificates that are created when creating voting card sets and verification card sets.
 */
public class CreateVotingCardSetCertificatePropertiesContainer {

	/**
	 * Properties needed for the creation of the Verification Card Set certificate.
	 */
	private Properties verificationCardSetCertificateProperties;

	/**
	 * Properties needed for the creation of the Credential Sign certificate.
	 */
	private Properties credentialSignCertificateProperties;

	/**
	 * Properties needed for the creation of the Credential Authentication certificate.
	 */
	private Properties credentialAuthCertificateProperties;

	public Properties getVerificationCardSetCertificateProperties() {
		return verificationCardSetCertificateProperties;
	}

	public void setVerificationCardSetCertificateProperties(Properties verificationCardSetCertificateProperties) {
		this.verificationCardSetCertificateProperties = verificationCardSetCertificateProperties;
	}

	public Properties getCredentialSignCertificateProperties() {
		return credentialSignCertificateProperties;
	}

	public void setCredentialSignCertificateProperties(Properties credentialSignCertificateProperties) {
		this.credentialSignCertificateProperties = credentialSignCertificateProperties;
	}

	public Properties getCredentialAuthCertificateProperties() {
		return credentialAuthCertificateProperties;
	}

	public void setCredentialAuthCertificateProperties(Properties credentialAuthCertificateProperties) {
		this.credentialAuthCertificateProperties = credentialAuthCertificateProperties;
	}
}
