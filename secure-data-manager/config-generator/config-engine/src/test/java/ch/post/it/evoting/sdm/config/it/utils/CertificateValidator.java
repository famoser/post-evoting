/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.it.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateParameters;
import ch.post.it.evoting.cryptolib.certificates.bean.CredentialProperties;
import ch.post.it.evoting.cryptolib.certificates.bean.X509CertificateType;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.factory.CryptoX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.utils.X509CertificateChainValidator;
import ch.post.it.evoting.domain.election.helpers.ReplacementsHolder;

public class CertificateValidator {

	private final ReplacementsHolder replacementsHolder;

	public CertificateValidator(final ReplacementsHolder replacements) {
		replacementsHolder = replacements;
	}

	public CryptoX509Certificate validateCert(final CryptoX509Certificate cert, final CredentialProperties credentialProperties) throws IOException {

		final Properties properties = readPropertiesFile(credentialProperties);

		assertTrue(cert.checkValidity().isOk());

		// Check Issuer Attributes if isn't a CA
		if (!credentialProperties.getCredentialType().equals(CertificateParameters.Type.ROOT)) {
			assertEquals(getProperty(properties, "issuer.common.name"), cert.getIssuerDn().getCommonName());
			assertEquals(getProperty(properties, "issuer.country"), cert.getIssuerDn().getCountry());
			assertEquals(getProperty(properties, "issuer.organization"), cert.getIssuerDn().getOrganization());
			assertEquals(getProperty(properties, "issuer.organizational.unit"), cert.getIssuerDn().getOrganizationalUnit());
		}

		// Check Subject Attributes
		assertEquals(getProperty(properties, "subject.common.name"), cert.getSubjectDn().getCommonName());
		assertEquals(getProperty(properties, "subject.country"), cert.getSubjectDn().getCountry());
		assertEquals(getProperty(properties, "subject.organization"), cert.getSubjectDn().getOrganization());
		assertEquals(getProperty(properties, "subject.organizational.unit"), cert.getSubjectDn().getOrganizationalUnit());

		validateKeyType(credentialProperties.getCredentialType(), cert);

		return cert;
	}

	public List<String> checkChain(final CryptoX509Certificate root, final CryptoX509Certificate intermediate, final CryptoX509Certificate leaf)
			throws GeneralCryptoLibException {

		final ArrayList<X509Certificate> arrayListCert = new ArrayList<>();
		final ArrayList<X509DistinguishedName> arrayListSubj = new ArrayList<>();
		X509Certificate[] chain = new X509Certificate[1];

		arrayListCert.add(intermediate.getCertificate());
		arrayListSubj.add(intermediate.getSubjectDn());

		X509DistinguishedName[] subjects = new X509DistinguishedName[arrayListSubj.size()];
		subjects = arrayListSubj.toArray(subjects);
		chain = arrayListCert.toArray(chain);

		final X509DistinguishedName leafX509DistinguishedName_imported = new X509DistinguishedName.Builder(leaf.getSubjectDn().getCommonName(),
				leaf.getSubjectDn().getCountry()).addOrganization(leaf.getSubjectDn().getOrganization())
				.addOrganizationalUnit(leaf.getSubjectDn().getOrganizationalUnit()).addLocality(leaf.getSubjectDn().getLocality()).build();

		final X509CertificateChainValidator x509CertificateChainValidator = new X509CertificateChainValidator(leaf.getCertificate(),
				X509CertificateType.SIGN, leafX509DistinguishedName_imported, null, chain, subjects, root.getCertificate());

		return x509CertificateChainValidator.validate();
	}

	private Properties readPropertiesFile(final CredentialProperties credentialProperties) throws IOException {
		final Properties props = new Properties();

		try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(credentialProperties.getPropertiesFile())) {
			props.load(input);
		}

		return props;
	}

	private String getProperty(final Properties properties, final String name) {
		final String value = properties.getProperty(name);
		return replacementsHolder.applyReplacements(value);
	}

	private boolean validateKeyType(final CertificateParameters.Type credentialType, final CryptoX509Certificate cert) {
		boolean hasExpectedKeyUsage;
		switch (credentialType) {
		case ROOT:
			hasExpectedKeyUsage = cert.isCertificateType(X509CertificateType.CERTIFICATE_AUTHORITY);
			break;
		case INTERMEDIATE:
			hasExpectedKeyUsage = cert.isCertificateType(X509CertificateType.SIGN);
			break;
		// Default is for sign or encryption purposes
		default:
			hasExpectedKeyUsage = cert.isCertificateType(X509CertificateType.ENCRYPT);
			break;
		}

		return hasExpectedKeyUsage;
	}
}
