/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.cryptolib.certificates.csr;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Properties;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.bean.CSRSigningInputProperties;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateParameters;
import ch.post.it.evoting.cryptolib.certificates.bean.CredentialProperties;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.factory.CryptoX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.factory.X509CertificateGenerator;
import ch.post.it.evoting.cryptolib.certificates.factory.builders.CertificateDataBuilder;
import ch.post.it.evoting.cryptolib.certificates.service.CertificatesService;

public class BouncyCastleCertificateRequestSigner implements CertificateRequestSigner {

	public static final String START_DATE = "start";
	public static final String END_DATE = "end";

	private static X509CertificateGenerator createCertificateGenerator() {
		CertificatesService certificatesService = new CertificatesService();
		CertificateDataBuilder certificateDataBuilder = new CertificateDataBuilder();

		return new X509CertificateGenerator(certificatesService, certificateDataBuilder);
	}

	public CSRSigningInputProperties getCsrSigningInputProperties(final CredentialProperties credentialProperties) throws GeneralCryptoLibException {

		final Properties props = new Properties();
		try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(credentialProperties.getPropertiesFile())) {
			props.load(input);
		} catch (IOException e) {
			throw new GeneralCryptoLibException("An error occurred while loading the tenant certificate properties", e);
		}

		String start = props.getProperty(START_DATE);
		String end = props.getProperty(END_DATE);

		ZonedDateTime notBefore = ZonedDateTime.ofInstant(Instant.parse(start), ZoneOffset.UTC);
		ZonedDateTime notAfter = ZonedDateTime.ofInstant(Instant.parse(end), ZoneOffset.UTC);

		return new CSRSigningInputProperties(notBefore, notAfter, credentialProperties.getCredentialType());
	}

	public CryptoAPIX509Certificate signCSR(final X509Certificate issuerCA, final PrivateKey issuerPrivateKey,
			final JcaPKCS10CertificationRequest csr, final CSRSigningInputProperties csrSigningInputProperties) throws GeneralCryptoLibException {

		CryptoAPIX509Certificate issuerCert = getCryptoAPIX509CertificateFromX509Certificate(issuerCA);

		X500Name subject = csr.getSubject();

		RDN cn = subject.getRDNs(BCStyle.CN)[0];
		String subjectCN = IETFUtils.valueToString(cn.getFirst().getValue());

		RDN ou = subject.getRDNs(BCStyle.OU)[0];
		String subjectOU = IETFUtils.valueToString(ou.getFirst().getValue());

		RDN o = subject.getRDNs(BCStyle.O)[0];
		String subjectO = IETFUtils.valueToString(o.getFirst().getValue());

		RDN c = subject.getRDNs(BCStyle.C)[0];
		String subjectC = IETFUtils.valueToString(c.getFirst().getValue());

		final CertificateParameters certificateParameters = new CertificateParameters();

		certificateParameters.setType(csrSigningInputProperties.getType());

		certificateParameters.setUserSubjectCn(subjectCN);
		certificateParameters.setUserSubjectOrgUnit(subjectOU);
		certificateParameters.setUserSubjectOrg(subjectO);
		certificateParameters.setUserSubjectCountry(subjectC);

		X509DistinguishedName issuerDN = issuerCert.getSubjectDn();
		certificateParameters.setUserIssuerCn(issuerDN.getCommonName());
		certificateParameters.setUserIssuerOrgUnit(issuerDN.getOrganizationalUnit());
		certificateParameters.setUserIssuerOrg(issuerDN.getOrganization());
		certificateParameters.setUserIssuerCountry(issuerDN.getCountry());

		if (Date.from(csrSigningInputProperties.getNotBefore().toInstant()).before(issuerCert.getNotBefore())) {
			throw new GeneralCryptoLibException("The tenant \"start\" time should be strictly after the root \"start\" time");
		}
		if (Date.from(csrSigningInputProperties.getNotAfter().toInstant()).after(issuerCert.getNotAfter())) {
			throw new GeneralCryptoLibException("The tenant \"end\" time should be strictly before the root \"end\" time");
		}

		certificateParameters.setUserNotBefore(csrSigningInputProperties.getNotBefore());
		certificateParameters.setUserNotAfter(csrSigningInputProperties.getNotAfter());

		PublicKey subjectPublicKey = getPublickeyFromCsr(csr);
		X509CertificateGenerator certificateGenerator = createCertificateGenerator();
		CryptoAPIX509Certificate tenantCACert;

		try {
			tenantCACert = certificateGenerator.generate(certificateParameters, subjectPublicKey, issuerPrivateKey);
		} catch (GeneralCryptoLibException e) {
			throw new GeneralCryptoLibException("An error occurred while creating the tenant certificate", e);
		}

		return tenantCACert;
	}

	private PublicKey getPublickeyFromCsr(final JcaPKCS10CertificationRequest csr) throws GeneralCryptoLibException {
		PublicKey subjectPublicKey;
		try {
			subjectPublicKey = csr.getPublicKey();
		} catch (InvalidKeyException | NoSuchAlgorithmException e) {
			throw new GeneralCryptoLibException("Could not retrieve the public key from the CSR", e);
		}
		return subjectPublicKey;
	}

	private CryptoAPIX509Certificate getCryptoAPIX509CertificateFromX509Certificate(final X509Certificate issuerCA) throws GeneralCryptoLibException {
		CryptoAPIX509Certificate issuerCert;
		try {
			issuerCert = new CryptoX509Certificate(issuerCA);
		} catch (GeneralCryptoLibException e) {
			throw new GeneralCryptoLibException("An error occurred while reading the platform root certificate data", e);
		}
		return issuerCert;
	}
}
