/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.certificates.CertificatesServiceAPI;
import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateData;
import ch.post.it.evoting.cryptolib.certificates.bean.RootCertificateData;
import ch.post.it.evoting.cryptolib.certificates.bean.ValidityDates;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.service.CertificatesService;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;

/**
 * Tests of {@link CCPublicKeySignatureValidatorImpl}.
 */
class CCPublicKeySignatureValidatorImplTest {

	private static final String VERIFICATION_CARD_SET_ID = "verificationCardSetId";
	private static final String ELECTORL_AUTHORITY_ID = "electoralAuthorityId";
	private static final String ELECTION_EVENT_ID = "electionEventId";
	private static final String ENCRYPTION_PARAMETERS_JSON = "{\"encryptionParams\":{\"p\":\"AMoeTsDNAOCZ1I+tv3/xjI/HVT8Ab3h8Ifm/d1623VoBQaL8mzFgtUerEiw/FcjyUnjwvu0cc2kPR6uM4eMgK6c7YXE/LSJ2BDgNMVLUgIIC+qVZLiEG/yPZAT+8akHS0Audz7XIxBnd1EzqsnkIFjsgtER3iVWpC7BTxJ1sT19pB+OaA2b+L6nVW0sZNwiHFCvvQ5rLTEc4uDEBcT6aNIXCK4Qkztc1hTfccn3k6158NNGMddvBB5GzqBoQMzjW35mKUBXOufRhQTYVQjNSoapfVfVKDsTkD1BDvdffJC/gP82m+/kud0x0p6RWfmfHgFoNmG6EjJkoMGfGLVJAMVs=\",\"q\":\"ZQ8nYGaAcEzqR9bfv/jGR+Oqn4A3vD4Q/N+7r1turQCg0X5NmLBao9WJFh+K5HkpPHhfdo45tIej1cZw8ZAV052wuJ+WkTsCHAaYqWpAQQF9UqyXEIN/keyAn941IOloBc7n2uRiDO7qJnVZPIQLHZBaIjvEqtSF2CniTrYnr7SD8c0Bs38X1OqtpYybhEOKFfehzWWmI5xcGIC4n00aQuEVwhJna5rCm+45PvJ1rz4aaMY67eCDyNnUDQgZnGtvzMUoCudc+jCgmwqhGalQ1S+q+qUHYnIHqCHe6++SF/Af5tN9/Jc7pjpT0is/M+PALQbMN0JGTJQYM+MWqSAYrQ==\",\"g\":\"Aw==\"}}";

	private static AsymmetricService asymmetricService;
	private static X509Certificate rootCACertificate;
	private static X509Certificate nodeCACertificate;
	private static PrivateKey signerPrivateKey;
	private static X509Certificate signerCertificate;
	private static ElGamalPublicKey key;
	private CCPublicKeySignatureValidatorImpl validator;

	@BeforeAll
	static void beforeClass() throws GeneralCryptoLibException {

		asymmetricService = new AsymmetricService();
		CertificatesServiceAPI certificatesService = new CertificatesService();
		ElGamalServiceAPI elGamalService = new ElGamalService();

		Date from = new Date();
		Date to = new Date(from.getTime() + 1000);
		ValidityDates dates = new ValidityDates(from, to);

		KeyPair rootPair = asymmetricService.getKeyPairForSigning();
		RootCertificateData rootData = new RootCertificateData();
		rootData.setSubjectDn(new X509DistinguishedName.Builder("root", "ES").build());
		rootData.setSubjectPublicKey(rootPair.getPublic());
		rootData.setValidityDates(dates);
		rootCACertificate = certificatesService.createRootAuthorityX509Certificate(rootData, rootPair.getPrivate()).getCertificate();

		KeyPair nodePair = asymmetricService.getKeyPairForSigning();
		CertificateData nodeData = new CertificateData();
		nodeData.setSubjectDn(new X509DistinguishedName.Builder("node", "ES").build());
		nodeData.setSubjectPublicKey(nodePair.getPublic());
		nodeData.setValidityDates(dates);
		nodeData.setIssuerDn(rootData.getSubjectDn());
		nodeCACertificate = certificatesService.createIntermediateAuthorityX509Certificate(nodeData, rootPair.getPrivate()).getCertificate();

		KeyPair signerPair = asymmetricService.getKeyPairForSigning();
		signerPrivateKey = signerPair.getPrivate();
		CertificateData signerData = new CertificateData();
		signerData.setSubjectDn(new X509DistinguishedName.Builder("signer", "ES").build());
		signerData.setSubjectPublicKey(signerPair.getPublic());
		signerData.setValidityDates(dates);
		signerData.setIssuerDn(nodeData.getSubjectDn());
		signerCertificate = certificatesService.createSignX509Certificate(signerData, nodePair.getPrivate()).getCertificate();

		ElGamalEncryptionParameters parameters = ElGamalEncryptionParameters.fromJson(ENCRYPTION_PARAMETERS_JSON);
		key = elGamalService.generateKeyPair(parameters, 2).getPublicKeys();
	}

	@BeforeEach
	void setUp() {
		validator = new CCPublicKeySignatureValidatorImpl(asymmetricService);
	}

	@Test
	void testCheckChoiceCodesEncryptionKeySignature() throws GeneralCryptoLibException, SignatureException {
		byte[][] data = { key.toJson().getBytes(UTF_8), ELECTION_EVENT_ID.getBytes(UTF_8), VERIFICATION_CARD_SET_ID.getBytes(UTF_8) };
		byte[] signature = asymmetricService.sign(signerPrivateKey, data);
		X509Certificate[] chain = { signerCertificate, nodeCACertificate, rootCACertificate };
		validator.checkChoiceCodesEncryptionKeySignature(signature, chain, key, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID);
	}

	@Test
	void testCheckChoiceCodesEncryptionKeySignatureBadChain() throws GeneralCryptoLibException {
		byte[][] data = { key.toJson().getBytes(UTF_8), ELECTION_EVENT_ID.getBytes(UTF_8), VERIFICATION_CARD_SET_ID.getBytes(UTF_8) };
		byte[] signature = asymmetricService.sign(signerPrivateKey, data);
		X509Certificate[] chain = { signerCertificate, rootCACertificate };

		assertThrows(SignatureException.class,
				() -> validator.checkChoiceCodesEncryptionKeySignature(signature, chain, key, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID));
	}

	@Test
	void testCheckChoiceCodesEncryptionKeySignatureBadSignature() throws GeneralCryptoLibException {
		byte[][] data = { key.toJson().getBytes(UTF_8), ELECTION_EVENT_ID.getBytes(UTF_8) };
		byte[] signature = asymmetricService.sign(signerPrivateKey, data);
		X509Certificate[] chain = { signerCertificate, nodeCACertificate, rootCACertificate };

		assertThrows(SignatureException.class,
				() -> validator.checkChoiceCodesEncryptionKeySignature(signature, chain, key, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID));
	}

	@Test
	void testCheckMixingKeySignature() throws GeneralCryptoLibException, SignatureException {
		byte[][] data = { key.toJson().getBytes(UTF_8), ELECTION_EVENT_ID.getBytes(UTF_8), ELECTORL_AUTHORITY_ID.getBytes(UTF_8) };
		byte[] signature = asymmetricService.sign(signerPrivateKey, data);
		X509Certificate[] chain = { signerCertificate, nodeCACertificate, rootCACertificate };
		validator.checkMixingKeySignature(signature, chain, key, ELECTION_EVENT_ID, ELECTORL_AUTHORITY_ID);
	}

	@Test
	void testCheckMixingKeySignatureBadChain() throws GeneralCryptoLibException {
		byte[][] data = { key.toJson().getBytes(UTF_8), ELECTION_EVENT_ID.getBytes(UTF_8), ELECTORL_AUTHORITY_ID.getBytes(UTF_8) };
		byte[] signature = asymmetricService.sign(signerPrivateKey, data);
		X509Certificate[] chain = { signerCertificate, rootCACertificate };

		assertThrows(SignatureException.class,
				() -> validator.checkMixingKeySignature(signature, chain, key, ELECTION_EVENT_ID, ELECTORL_AUTHORITY_ID));
	}

	@Test
	void testCheckMixingKeySignatureBadSignature() throws GeneralCryptoLibException {
		byte[][] data = { key.toJson().getBytes(UTF_8), ELECTION_EVENT_ID.getBytes(UTF_8) };
		byte[] signature = asymmetricService.sign(signerPrivateKey, data);
		X509Certificate[] chain = { signerCertificate, nodeCACertificate, rootCACertificate };

		assertThrows(SignatureException.class,
				() -> validator.checkMixingKeySignature(signature, chain, key, ELECTION_EVENT_ID, ELECTORL_AUTHORITY_ID));
	}
}
