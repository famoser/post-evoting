/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.deleteIfExists;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.certificates.CertificatesServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.certificates.bean.RootCertificateData;
import ch.post.it.evoting.cryptolib.certificates.bean.ValidityDates;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.service.CertificatesService;
import ch.post.it.evoting.sdm.commons.PathResolver;

/**
 * Tests of {@link PlatformRootCAService}.
 */
class RootCAServiceTest {

	private Path file;
	private RootCAService service;

	@BeforeEach
	void setUp() throws IOException {
		file = createTempFile("platformRootCA", ".pem");
		PathResolver resolver = args -> file;
		service = new FileRootCAService(resolver, file.getFileName().toString());
	}

	@AfterEach
	void tearDown() throws IOException {
		deleteIfExists(file);
	}

	@Test
	void testSaveLoad() throws GeneralCryptoLibException, CertificateManagementException {
		AsymmetricServiceAPI asymmetricService = new AsymmetricService();
		KeyPair pair = asymmetricService.getKeyPairForSigning();
		CertificatesServiceAPI certificateService = new CertificatesService();
		RootCertificateData data = new RootCertificateData();
		X509DistinguishedName name = new X509DistinguishedName.Builder("TEST", "ES").build();
		data.setSubjectPublicKey(pair.getPublic());
		data.setSubjectDn(name);
		Date from = new Date();
		Date to = new Date(from.getTime() + 1000);
		ValidityDates dates = new ValidityDates(from, to);
		data.setValidityDates(dates);
		CryptoAPIX509Certificate certificate = certificateService.createRootAuthorityX509Certificate(data, pair.getPrivate());

		X509Certificate expected = certificate.getCertificate();
		service.save(expected);
		X509Certificate actual = service.load();
		assertEquals(expected, actual);
	}
}
