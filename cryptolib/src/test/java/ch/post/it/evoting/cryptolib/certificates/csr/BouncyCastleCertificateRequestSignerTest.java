/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.certificates.csr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.certificates.bean.CSRSigningInputProperties;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateData;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateParameters;
import ch.post.it.evoting.cryptolib.certificates.bean.ValidityDates;
import ch.post.it.evoting.cryptolib.certificates.bean.X509CertificateType;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.utils.X509CertificateTestDataGenerator;

class BouncyCastleCertificateRequestSignerTest {

	private static PrivateKey privateKey;
	private static PublicKey publicKey;
	private static BouncyCastleCertificateRequestSigner certificateRequestSigner;
	private static ZonedDateTime notBefore;
	private static ZonedDateTime notAfter;
	private static X509Certificate issuerCA;
	private static JcaPKCS10CertificationRequest jcaPKCS10CertificationRequest;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException, OperatorCreationException {
		certificateRequestSigner = new BouncyCastleCertificateRequestSigner();
		AsymmetricService asymmetricService = new AsymmetricService();
		KeyPair keyPairForSigning = asymmetricService.getKeyPairForSigning();
		publicKey = keyPairForSigning.getPublic();
		privateKey = keyPairForSigning.getPrivate();

		String commonName = "Test certificate";
		String country = "XX";
		String organizationalUnit = "Test";
		String organization = "Organization";
		String locality = "Locality";

		X509DistinguishedName.Builder builder = new X509DistinguishedName.Builder(commonName, country);
		builder.addOrganization(organization);
		builder.addOrganizationalUnit(organizationalUnit);
		builder.addLocality(locality);

		X509DistinguishedName subjectDn = builder.build();
		X509DistinguishedName issuerDn = builder.build();

		notBefore = ZonedDateTime.now();
		notAfter = notBefore.plusYears(1);

		Date notAfterCertificate = Date.from(notBefore.plusYears(2).toInstant());

		ValidityDates validityDates = new ValidityDates(Date.from(notBefore.toInstant()), notAfterCertificate);

		CertificateData certificateData = new CertificateData() {
			{
				setIssuerDn(issuerDn);
				setSubjectDn(subjectDn);
				setSubjectPublicKey(publicKey);
				setValidityDates(validityDates);
			}
		};

		CryptoAPIX509Certificate certificate = X509CertificateTestDataGenerator
				.getCertificateAuthorityX509CertificateWithNoKeyUsage(certificateData, keyPairForSigning);

		issuerCA = certificate.getCertificate();
		X500Principal subject = issuerCA.getSubjectX500Principal();
		CSRGenerator csrGenerator = new CSRGenerator();
		PKCS10CertificationRequest csr = csrGenerator.generate(publicKey, privateKey, subject);

		jcaPKCS10CertificationRequest = new JcaPKCS10CertificationRequest(csr);
	}

	@Test
	void signCSRCertificateAuthorityType() throws GeneralCryptoLibException {
		CSRSigningInputProperties csrSigningInputProperties = new CSRSigningInputProperties(notBefore, notAfter,
				CertificateParameters.Type.INTERMEDIATE);

		CryptoAPIX509Certificate certificate = certificateRequestSigner
				.signCSR(issuerCA, privateKey, jcaPKCS10CertificationRequest, csrSigningInputProperties);

		assertTrue(certificate.isCertificateType(X509CertificateType.CERTIFICATE_AUTHORITY));
	}

	@Test
	void signCSRSignType() throws GeneralCryptoLibException {
		CSRSigningInputProperties csrSigningInputProperties = new CSRSigningInputProperties(notBefore, notAfter, CertificateParameters.Type.SIGN);

		CryptoAPIX509Certificate certificate = certificateRequestSigner
				.signCSR(issuerCA, privateKey, jcaPKCS10CertificationRequest, csrSigningInputProperties);

		assertTrue(certificate.isCertificateType(X509CertificateType.SIGN));
	}

	@Test
	void signCSREncryptType() throws GeneralCryptoLibException {
		CSRSigningInputProperties csrSigningInputProperties = new CSRSigningInputProperties(notBefore, notAfter,
				CertificateParameters.Type.ENCRYPTION);

		CryptoAPIX509Certificate certificate = certificateRequestSigner
				.signCSR(issuerCA, privateKey, jcaPKCS10CertificationRequest, csrSigningInputProperties);

		assertTrue(certificate.isCertificateType(X509CertificateType.ENCRYPT));
	}

	@Test
	void signCSRFailBeforeDateTest() {
		CSRSigningInputProperties csrSigningInputProperties = new CSRSigningInputProperties(ZonedDateTime.now().minusMonths(1), notAfter,
				CertificateParameters.Type.INTERMEDIATE);

		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> certificateRequestSigner.signCSR(issuerCA, privateKey, jcaPKCS10CertificationRequest, csrSigningInputProperties));
		assertEquals("The tenant \"start\" time should be strictly after the root \"start\" time", exception.getMessage());
	}

	@Test
	void signCSRFailAfterDateTest() {
		CSRSigningInputProperties csrSigningInputProperties = new CSRSigningInputProperties(notBefore, ZonedDateTime.now().plusYears(3),
				CertificateParameters.Type.INTERMEDIATE);

		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> certificateRequestSigner.signCSR(issuerCA, privateKey, jcaPKCS10CertificationRequest, csrSigningInputProperties));
		assertEquals("The tenant \"end\" time should be strictly before the root \"end\" time", exception.getMessage());
	}
}
