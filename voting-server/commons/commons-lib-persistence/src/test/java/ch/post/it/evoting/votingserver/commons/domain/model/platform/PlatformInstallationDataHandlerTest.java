/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.domain.model.platform;

import java.security.KeyPair;
import java.util.Calendar;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.domain.election.model.platform.PlatformInstallationData;
import ch.post.it.evoting.votingserver.commons.beans.validation.CertificateValidationService;
import ch.post.it.evoting.votingserver.commons.beans.validation.CertificateValidationServiceImpl;
import ch.post.it.evoting.votingserver.commons.domain.model.BaseRepository;

public class PlatformInstallationDataHandlerTest {

	// Common Name: Root Org CA
	// Organization: Root Org
	// Organization Unit: Online Voting
	// Locality:
	// Country: CH
	// Valid From: Calendar.getInstance()
	// Valid To: Calendar.getInstance() + 1y
	// Issuer: Root Org CA, Root Org
	private static String PLATFORM_CA;

	// Common Name: swisspost Root CA
	// Organization: SwissPost
	// Organization Unit: Online Voting
	// Locality:
	// Country: CH
	// Valid From: Calendar.getInstance()
	// Valid To: Calendar.getInstance() + 2y
	// Issuer: TEST INTERMEDIATE CA, TEST ORGANIZATION (intermediate CA)
	private static String PLATFORM_CA_RECERTIFIED;

	// Common Name: TEST INTERMEDIATE CA
	// Organization: TEST ORGANIZATION
	// Organization Unit: TEST ORGANIZATIONAL UNIT
	// Country: CH
	// Valid From: Calendar.getInstance()
	// Valid To: Calendar.getInstance() + 3y
	// Issuer: TEST ROOT CA, TEST ORGANIZATION (root CA)
	private static String INTERMEDIATE_CA;

	private final CertificateValidationService certificateValidationService = new CertificateValidationServiceImpl();

	@SuppressWarnings("unchecked")
	protected BaseRepository<PlatformCAEntity, Long> repository = Mockito.mock(BaseRepository.class);

	@BeforeClass
	public static void setUp() throws GeneralCryptoLibException {
		createCertificates();
	}

	private static void createCertificates() throws GeneralCryptoLibException {
		AsymmetricServiceAPI asymmetricService = new AsymmetricService();
		Calendar platformNow = Calendar.getInstance();
		Calendar platformFuture = Calendar.getInstance();
		platformFuture.add(Calendar.YEAR, 1);
		KeyPair platformKeyPair = asymmetricService.getKeyPairForSigning();
		CryptoAPIX509Certificate createRootSelfSignedCertificate = CertificateUtil
				.createRootSelfSignedCertificate("Root Org CA", "Root Org", "Online Voting", "CH", platformNow.getTime(), platformFuture.getTime(),
						platformKeyPair);
		PLATFORM_CA = PemUtils.certificateToPem(createRootSelfSignedCertificate.getCertificate());

		Calendar maintTrustedNow = Calendar.getInstance();
		Calendar maintTrustedFuture = Calendar.getInstance();
		maintTrustedFuture.add(Calendar.YEAR, 3);
		KeyPair maintTrustedAuthKeyPair = asymmetricService.getKeyPairForSigning();
		CryptoAPIX509Certificate maintTrustedAuthSelfSignedCertificate = CertificateUtil
				.createRootSelfSignedCertificate("TEST ROOT CA", "TEST ORGANIZATION", "TEST ORGANIZATIONAL UNIT", "CH", maintTrustedNow.getTime(),
						maintTrustedFuture.getTime(), maintTrustedAuthKeyPair);

		Calendar intermediateNow = Calendar.getInstance();
		Calendar intermediateFuture = Calendar.getInstance();
		intermediateFuture.add(Calendar.YEAR, 2);
		KeyPair intermediateKeyPair = asymmetricService.getKeyPairForSigning();
		CryptoAPIX509Certificate intermediateCert = CertificateUtil
				.createIntermediateCertificate("TEST INTERMEDIATE CA", "TEST ORGANIZATION", "TEST ORGANIZATIONAL UNIT", "CH",
						intermediateNow.getTime(), intermediateFuture.getTime(), intermediateKeyPair, maintTrustedAuthKeyPair,
						maintTrustedAuthSelfSignedCertificate.getSubjectDn());
		INTERMEDIATE_CA = PemUtils.certificateToPem(intermediateCert.getCertificate());

		Calendar platformRecertifiedNow = Calendar.getInstance();
		Calendar platformRecertifiedFuture = Calendar.getInstance();
		platformRecertifiedFuture.add(Calendar.YEAR, 1);
		KeyPair platformRecertifiedKeyPair = asymmetricService.getKeyPairForSigning();
		CryptoAPIX509Certificate platformRecertifiedCert = CertificateUtil
				.createIntermediateCertificate("swisspost Root CA", "SwissPost", "Online Voting", "CH", platformRecertifiedNow.getTime(),
						platformRecertifiedFuture.getTime(), platformRecertifiedKeyPair, intermediateKeyPair, intermediateCert.getSubjectDn());
		PLATFORM_CA_RECERTIFIED = PemUtils.certificateToPem(platformRecertifiedCert.getCertificate());
	}

	@Test
	public void testPlatformCertificateChain() {
		PlatformInstallationData data = new PlatformInstallationData();
		try {
			data.setPlatformRootCaPEM(PLATFORM_CA_RECERTIFIED);
			data.setPlatformRootIssuerCaPEM(INTERMEDIATE_CA);
			PlatformInstallationDataHandler
					.savePlatformCertificateChain(data, repository, certificateValidationService, new PlatformCAEntity(), new PlatformCAEntity());
		} catch (Exception e) {
			Assert.fail();
		}

	}

	@Test
	public void testInvalidPlatformCertificateChain() {
		PlatformInstallationData data = new PlatformInstallationData();
		try {
			data.setPlatformRootCaPEM(INTERMEDIATE_CA);
			data.setPlatformRootIssuerCaPEM(PLATFORM_CA);
			PlatformInstallationDataHandler
					.savePlatformCertificateChain(data, repository, certificateValidationService, new PlatformCAEntity(), new PlatformCAEntity());
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals("Certificate validation failed for the following validation types: [SIGNATURE]", e.getMessage());
		}

	}

	@Test
	public void testSelfSignedPlatformCertificate() {
		PlatformInstallationData data = new PlatformInstallationData();
		try {
			data.setPlatformRootCaPEM(PLATFORM_CA);
			PlatformInstallationDataHandler
					.savePlatformCertificateChain(data, repository, certificateValidationService, new PlatformCAEntity(), new PlatformCAEntity());
		} catch (Exception e) {
			Assert.fail();
		}

	}

	@Test
	public void testInvalidSelfSignedPlatformCertificate() {
		PlatformInstallationData data = new PlatformInstallationData();
		try {
			data.setPlatformRootCaPEM(PLATFORM_CA_RECERTIFIED);
			PlatformInstallationDataHandler
					.savePlatformCertificateChain(data, repository, certificateValidationService, new PlatformCAEntity(), new PlatformCAEntity());
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals("Certificate validation failed for the following validation types: [SIGNATURE]", e.getMessage());
		}

	}

}
