/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.certificates.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.certificates.CertificatesServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateData;
import ch.post.it.evoting.cryptolib.certificates.bean.RootCertificateData;
import ch.post.it.evoting.cryptolib.certificates.bean.ValidityDates;
import ch.post.it.evoting.cryptolib.certificates.bean.X509CertificateType;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.service.CertificatesService;

class X509CertificateChainValidatorValidationTest {

	private static final long gap = 1000L;
	private static final int OK_TIME_REFERENCE = 5;
	private static AsymmetricServiceAPI asymmetricService;
	private static CertificatesServiceAPI certificateService;
	private static KeyPair intermediate11Keys;
	private static CryptoAPIX509Certificate root1AuthorityX509Certificate;
	private static KeyPair root1Keys;
	private static CryptoAPIX509Certificate root2AuthorityX509Certificate;
	private static KeyPair root2Keys;
	private static X509CertificateChainValidator target;
	private static KeyPair oneKeys;

	@BeforeAll
	public static void setUp() throws GeneralCryptoLibException {
		certificateService = new CertificatesService();

		asymmetricService = new AsymmetricService();

		setup();
	}

	private static CertificateData createIntermediateCertificateData(final String identifier, final PublicKey key, final int notBefore,
			final int notAfter, final X509DistinguishedName issuer) throws GeneralCryptoLibException {
		CertificateData certificateData = new CertificateData();
		X509DistinguishedName subjectDn = new X509DistinguishedName.Builder("root" + identifier + "CN", "CA").addOrganization("O")
				.addOrganizationalUnit("OU").build();
		certificateData.setSubjectDn(subjectDn);
		certificateData.setIssuerDn(issuer);
		certificateData.setSubjectPublicKey(key);
		ValidityDates validityDates = new ValidityDates(new Date(notBefore * gap), new Date(notAfter * gap));
		certificateData.setValidityDates(validityDates);
		return certificateData;
	}

	private static RootCertificateData createRootCertificateData(final String identifier, final PublicKey key, final int notBefore,
			final int notAfter) throws GeneralCryptoLibException {
		RootCertificateData certificateData = new RootCertificateData();
		X509DistinguishedName subjectDn = new X509DistinguishedName.Builder("root" + identifier + "CN", "CA").addOrganization("O")
				.addOrganizationalUnit("OU").build();
		certificateData.setSubjectDn(subjectDn);
		certificateData.setSubjectPublicKey(key);
		ValidityDates validityDates = new ValidityDates(new Date(notBefore * gap), new Date(notAfter * gap));
		certificateData.setValidityDates(validityDates);
		return certificateData;
	}

	private static void setup() throws GeneralCryptoLibException {

		root1Keys = asymmetricService.getKeyPairForSigning();
		RootCertificateData root1CertificateData = createRootCertificateData("1", root1Keys.getPublic(), 0, 100);
		root1AuthorityX509Certificate = certificateService.createRootAuthorityX509Certificate(root1CertificateData, root1Keys.getPrivate());

		root2Keys = asymmetricService.getKeyPairForSigning();
		RootCertificateData root2CertificateData = createRootCertificateData("2", root2Keys.getPublic(), 50, 150);
		root2AuthorityX509Certificate = certificateService.createRootAuthorityX509Certificate(root2CertificateData, root2Keys.getPrivate());

		intermediate11Keys = asymmetricService.getKeyPairForSigning();

		oneKeys = asymmetricService.getKeyPairForSigning();
	}

	@Test
	void create2LayerCertificatesExpiredTest() throws GeneralCryptoLibException {

		X509Certificate[] chain = null;
		X509DistinguishedName[] subjects = null;
		int timeReference = 1000;
		create2LayerCertificates(chain, subjects, timeReference);

		List<String> lst = target.validate();
		assertFalse(lst.isEmpty());
		assertArrayEquals(new String[] { "Time" }, lst.toArray(new String[0]));
	}

	@Test
	void create2LayerCertificatesOKNullArraysTest() throws GeneralCryptoLibException {
		X509Certificate[] chain = null;
		X509DistinguishedName[] subjects = null;

		List<String> lst = create2LayerCertificates(chain, subjects, OK_TIME_REFERENCE);
		assertTrue(lst.isEmpty());
	}

	@Test
	void create2LayerCertificatesOKEmptyArraysTest() throws GeneralCryptoLibException {
		X509Certificate[] chain = new X509Certificate[0];
		X509DistinguishedName[] subjects = new X509DistinguishedName[0];
		List<String> lst = create2LayerCertificates(chain, subjects, OK_TIME_REFERENCE);
		assertTrue(lst.isEmpty());
	}

	private List<String> create2LayerCertificates(final X509Certificate[] chain, final X509DistinguishedName[] subjects, final int timeReference)
			throws GeneralCryptoLibException {

		KeyPair leafKeys = oneKeys;

		CertificateData leafCertificateData = createLeafCertificateData(leafKeys.getPublic(), 1, 10, root1AuthorityX509Certificate.getSubjectDn());

		CryptoAPIX509Certificate leafX509Certificate = certificateService.createSignX509Certificate(leafCertificateData, root1Keys.getPrivate());

		X509CertificateType leafKeyType = X509CertificateType.SIGN;

		target = new X509CertificateChainValidator(leafX509Certificate.getCertificate(), leafKeyType, leafX509Certificate.getSubjectDn(),
				new Date(timeReference * gap), chain, subjects, root1AuthorityX509Certificate.getCertificate());
		return target.validate();
	}

	@Test
	void twoLayerInvalidOtherIssuer() throws GeneralCryptoLibException {

		KeyPair leafKeys = oneKeys;

		CertificateData leafCertificateData = createLeafCertificateData(leafKeys.getPublic(), 1, 10, root1AuthorityX509Certificate.getSubjectDn());

		CryptoAPIX509Certificate leafX509Certificate = certificateService.createSignX509Certificate(leafCertificateData, root2Keys.getPrivate());

		X509CertificateType leafKeyType = X509CertificateType.SIGN;

		final X509Certificate[] chain = null;
		final X509DistinguishedName[] subjects = null;

		X509CertificateChainValidator validator = new X509CertificateChainValidator(leafX509Certificate.getCertificate(), leafKeyType,
				leafX509Certificate.getSubjectDn(), new Date(OK_TIME_REFERENCE * gap), chain, subjects,
				root1AuthorityX509Certificate.getCertificate());
		List<String> lst = validator.validate();
		assertArrayEquals(new String[] { "SIGNATURE_0" }, lst.toArray(new String[0]));
	}

	@Test
	void twoLayerInvalidOtherSubjectNameIssuer() throws GeneralCryptoLibException {

		KeyPair leafKeys = oneKeys;

		CertificateData leafCertificateData = createLeafCertificateData(leafKeys.getPublic(), 1, 10, root2AuthorityX509Certificate.getSubjectDn());

		CryptoAPIX509Certificate leafX509Certificate = certificateService.createSignX509Certificate(leafCertificateData, root1Keys.getPrivate());

		X509CertificateType leafKeyType = X509CertificateType.SIGN;

		final X509Certificate[] chain = null;
		final X509DistinguishedName[] subjects = null;

		X509CertificateChainValidator validator = new X509CertificateChainValidator(leafX509Certificate.getCertificate(), leafKeyType,
				leafX509Certificate.getSubjectDn(), new Date(OK_TIME_REFERENCE * gap), chain, subjects,
				root1AuthorityX509Certificate.getCertificate());
		List<String> lst = validator.validate();
		assertArrayEquals(new String[] { "ISSUER_0" }, lst.toArray(new String[0]));
	}

	@Test
	void twoLayerInvalidSubjectName() throws GeneralCryptoLibException {

		KeyPair leafKeys = oneKeys;

		CertificateData leafCertificateData = createLeafCertificateData(leafKeys.getPublic(), 1, 10, root1AuthorityX509Certificate.getSubjectDn());

		CryptoAPIX509Certificate leafX509Certificate = certificateService.createSignX509Certificate(leafCertificateData, root1Keys.getPrivate());

		X509CertificateType leafKeyType = X509CertificateType.SIGN;

		final X509Certificate[] chain = null;
		final X509DistinguishedName[] subjects = null;

		X509CertificateChainValidator validator = new X509CertificateChainValidator(leafX509Certificate.getCertificate(), leafKeyType,
				root1AuthorityX509Certificate.getSubjectDn(), new Date(OK_TIME_REFERENCE * gap), chain, subjects,
				root1AuthorityX509Certificate.getCertificate());
		List<String> lst = validator.validate();
		assertArrayEquals(new String[] { "SUBJECT_0" }, lst.toArray(new String[0]));
	}

	@Test
	void twoLayerInvalidKeyType() throws GeneralCryptoLibException {

		KeyPair leafKeys = oneKeys;

		CertificateData leafCertificateData = createLeafCertificateData(leafKeys.getPublic(), 1, 10, root1AuthorityX509Certificate.getSubjectDn());

		CryptoAPIX509Certificate leafX509Certificate = certificateService.createSignX509Certificate(leafCertificateData, root1Keys.getPrivate());

		X509CertificateType leafKeyType = X509CertificateType.ENCRYPT;

		final X509Certificate[] chain = null;
		final X509DistinguishedName[] subjects = null;

		X509CertificateChainValidator validator = new X509CertificateChainValidator(leafX509Certificate.getCertificate(), leafKeyType,
				leafX509Certificate.getSubjectDn(), new Date(OK_TIME_REFERENCE * gap), chain, subjects,
				root1AuthorityX509Certificate.getCertificate());
		List<String> lst = validator.validate();
		assertArrayEquals(new String[] { "KEY_TYPE_0" }, lst.toArray(new String[0]));
	}

	@Test
	void twoLayerNoKeyType() throws GeneralCryptoLibException {

		KeyPair leafKeys = oneKeys;

		CertificateData leafCertificateData = createLeafCertificateData(leafKeys.getPublic(), 1, 10, root1AuthorityX509Certificate.getSubjectDn());

		CryptoAPIX509Certificate leafX509Certificate = X509CertificateTestDataGenerator
				.getX509CertificateWithNoKeyUsage(leafCertificateData, root1Keys);

		X509CertificateType leafKeyType = X509CertificateType.ENCRYPT;

		final X509Certificate[] chain = null;
		final X509DistinguishedName[] subjects = null;

		X509CertificateChainValidator validator = new X509CertificateChainValidator(leafX509Certificate.getCertificate(), leafKeyType,
				leafX509Certificate.getSubjectDn(), new Date(OK_TIME_REFERENCE * gap), chain, subjects,
				root1AuthorityX509Certificate.getCertificate());
		List<String> lst = validator.validate();
		assertArrayEquals(new String[] { "KEY_TYPE_0" }, lst.toArray(new String[0]));
	}

	@Test
	void twoLayerCaNoKeyType() throws GeneralCryptoLibException {

		CertificateData intermediate1CertificateData = createIntermediateCertificateData("11", intermediate11Keys.getPublic(), 1, 10,
				root1AuthorityX509Certificate.getSubjectDn());

		CryptoAPIX509Certificate leafX509Certificate = X509CertificateTestDataGenerator
				.getCertificateAuthorityX509CertificateWithNoKeyUsage(intermediate1CertificateData, root1Keys);

		X509CertificateType leafKeyType = X509CertificateType.CERTIFICATE_AUTHORITY;

		final X509Certificate[] chain = null;
		final X509DistinguishedName[] subjects = null;

		X509CertificateChainValidator validator = new X509CertificateChainValidator(leafX509Certificate.getCertificate(), leafKeyType,
				leafX509Certificate.getSubjectDn(), new Date(OK_TIME_REFERENCE * gap), chain, subjects,
				root1AuthorityX509Certificate.getCertificate());
		List<String> lst = validator.validate();
		assertArrayEquals(new String[] { "KEY_TYPE_0" }, lst.toArray(new String[0]));
	}

	private CertificateData createLeafCertificateData(final PublicKey key, final int notBefore, final int notAfter,
			final X509DistinguishedName issuer) throws GeneralCryptoLibException {

		CertificateData certificateData = new CertificateData();
		X509DistinguishedName leafSubjectDn = new X509DistinguishedName.Builder("leafCN", "CA").addOrganization("O").addOrganizationalUnit("OU")
				.build();

		certificateData.setIssuerDn(issuer);
		certificateData.setSubjectDn(leafSubjectDn);
		certificateData.setSubjectPublicKey(key);
		ValidityDates leafValidityDates = new ValidityDates(new Date(notBefore * gap), new Date(notAfter * gap));
		certificateData.setValidityDates(leafValidityDates);
		return certificateData;
	}
}
