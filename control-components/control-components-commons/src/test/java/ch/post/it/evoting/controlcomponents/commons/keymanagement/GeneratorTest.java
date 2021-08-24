/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore.PasswordProtection;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.certificates.CertificatesServiceAPI;
import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.primitives.PrimitivesServiceAPI;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.certificates.bean.RootCertificateData;
import ch.post.it.evoting.cryptolib.certificates.bean.ValidityDates;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.service.CertificatesService;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.primitives.service.PrimitivesService;
import ch.post.it.evoting.cryptolib.stores.service.StoresService;

/**
 * Tests of {@link Generator}.
 */
class GeneratorTest {
	private static final String ENCRYPTION_PARAMETERS_JSON = "{\"encryptionParams\":{\"p\":\"AMoeTsDNAOCZ1I+tv3/xjI/HVT8Ab3h8Ifm/d1623VoBQaL8mzFgtUerEiw/FcjyUnjwvu0cc2kPR6uM4eMgK6c7YXE/LSJ2BDgNMVLUgIIC+qVZLiEG/yPZAT+8akHS0Audz7XIxBnd1EzqsnkIFjsgtER3iVWpC7BTxJ1sT19pB+OaA2b+L6nVW0sZNwiHFCvvQ5rLTEc4uDEBcT6aNIXCK4Qkztc1hTfccn3k6158NNGMddvBB5GzqBoQMzjW35mKUBXOufRhQTYVQjNSoapfVfVKDsTkD1BDvdffJC/gP82m+/kud0x0p6RWfmfHgFoNmG6EjJkoMGfGLVJAMVs=\",\"q\":\"ZQ8nYGaAcEzqR9bfv/jGR+Oqn4A3vD4Q/N+7r1turQCg0X5NmLBao9WJFh+K5HkpPHhfdo45tIej1cZw8ZAV052wuJ+WkTsCHAaYqWpAQQF9UqyXEIN/keyAn941IOloBc7n2uRiDO7qJnVZPIQLHZBaIjvEqtSF2CniTrYnr7SD8c0Bs38X1OqtpYybhEOKFfehzWWmI5xcGIC4n00aQuEVwhJna5rCm+45PvJ1rz4aaMY67eCDyNnUDQgZnGtvzMUoCudc+jCgmwqhGalQ1S+q+qUHYnIHqCHe6++SF/Af5tN9/Jc7pjpT0is/M+PALQbMN0JGTJQYM+MWqSAYrQ==\",\"g\":\"Aw==\"}}";
	private static final String ELECTORAL_AUTHORITY_ID = "electoralAuthorityId";
	private static final String ELECTION_EVENT_ID = "electionEventId";
	private static final String VERIFICATION_CARD_SET_ID = "verificationCardSetId";
	private static final String NODE_ID = "nodeId";
	private static final Date VALID_FROM;
	private static final Date VALID_TO;
	private static ElGamalServiceAPI elGamalService;
	private static ElGamalEncryptionParameters parameters;
	private static AsymmetricServiceAPI asymmetricService;
	private static CertificatesServiceAPI certificatesService;
	private static PrivateKey nodeCAPrivateKey;
	private static X509Certificate[] nodeCACertificateChain;

	static {
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.set(Calendar.MILLISECOND, 0);
		VALID_FROM = calendar.getTime();
		VALID_TO = new Date(VALID_FROM.getTime() + 1000);
	}

	private Codec codec;
	private Generator generator;

	@BeforeAll
	public static void beforeClass() throws GeneralCryptoLibException {

		elGamalService = new ElGamalService();

		parameters = ElGamalEncryptionParameters.fromJson(ENCRYPTION_PARAMETERS_JSON);

		asymmetricService = new AsymmetricService();
		certificatesService = new CertificatesService();

		KeyPair pair = asymmetricService.getKeyPairForSigning();
		nodeCAPrivateKey = pair.getPrivate();

		RootCertificateData data = new RootCertificateData();
		data.setSubjectDn(
				new X509DistinguishedName.Builder("AdministrationBoard electionEventId", "CH").addLocality("Bern").addOrganization("Swiss Post")
						.addOrganizationalUnit("Online Voting").build());
		data.setSubjectPublicKey(pair.getPublic());
		data.setValidityDates(new ValidityDates(VALID_FROM, VALID_TO));
		X509Certificate nodeCACertificate = certificatesService.createRootAuthorityX509Certificate(data, nodeCAPrivateKey).getCertificate();
		nodeCACertificateChain = new X509Certificate[] { nodeCACertificate };

	}

	@BeforeEach
	public void setUp() {
		PrimitivesServiceAPI primitivesService = new PrimitivesService();
		codec = new Codec(new StoresService(), asymmetricService);
		generator = new Generator(asymmetricService, certificatesService, elGamalService, primitivesService, codec, NODE_ID);
	}

	@Test
	void testGenerateCcrjReturnCodesKeys() throws KeyManagementException, GeneralCryptoLibException {
		NodeKeys nodeKeys = generator.generateNodeKeys(nodeCAPrivateKey, nodeCACertificateChain);
		ElectionSigningKeys electionSigningKeys = generator.generateElectionSigningKeys(ELECTION_EVENT_ID, VALID_FROM, VALID_TO, nodeKeys);
		CcrjReturnCodesKeysSpec spec = new CcrjReturnCodesKeysSpec.Builder().setElectionEventId(ELECTION_EVENT_ID)
				.setVerificationCardSetId(VERIFICATION_CARD_SET_ID).setParameters(parameters).setCcrjReturnCodesGenerationKeyLength(1)
				.setCcrjChoiceReturnCodesEncryptionKeyLength(2).build();

		CcrjReturnCodesKeys ccrjReturnCodesKeys = generator.generateCcrjReturnCodesKeys(spec, electionSigningKeys);

		ZpSubgroup group = ccrjReturnCodesKeys.getCcrjReturnCodesGenerationSecretKey().getGroup();
		assertEquals(parameters.getGroup(), group);

		assertEquals(1, ccrjReturnCodesKeys.getCcrjReturnCodesGenerationSecretKey().getKeys().size());
		assertEquals(1, ccrjReturnCodesKeys.getCcrjReturnCodesGenerationPublicKey().getKeys().size());
		assertTrue(asymmetricService
				.verifySignature(ccrjReturnCodesKeys.getCcrjReturnCodesGenerationPublicKeySignature(), electionSigningKeys.publicKey(),
						codec.encodeElGamalPublicKey(ccrjReturnCodesKeys.getCcrjReturnCodesGenerationPublicKey()),
						ELECTION_EVENT_ID.getBytes(StandardCharsets.UTF_8), VERIFICATION_CARD_SET_ID.getBytes(StandardCharsets.UTF_8)));

		assertEquals(2, ccrjReturnCodesKeys.getCcrjChoiceReturnCodesEncryptionSecretKey().getKeys().size());
		assertEquals(2, ccrjReturnCodesKeys.getCcrjChoiceReturnCodesEncryptionPublicKey().getKeys().size());
		assertTrue(asymmetricService
				.verifySignature(ccrjReturnCodesKeys.getCcrjChoiceReturnCodesEncryptionPublicKeySignature(), electionSigningKeys.publicKey(),
						codec.encodeElGamalPublicKey(ccrjReturnCodesKeys.getCcrjChoiceReturnCodesEncryptionPublicKey()),
						ELECTION_EVENT_ID.getBytes(StandardCharsets.UTF_8), VERIFICATION_CARD_SET_ID.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void testGenerateElectionSigningKeys()
			throws KeyManagementException, GeneralCryptoLibException, InvalidKeyException, CertificateException, NoSuchAlgorithmException,
			NoSuchProviderException, SignatureException {
		NodeKeys nodeKeys = generator.generateNodeKeys(nodeCAPrivateKey, nodeCACertificateChain);

		ElectionSigningKeys electionSigningKeys = generator.generateElectionSigningKeys(ELECTION_EVENT_ID, VALID_FROM, VALID_TO, nodeKeys);

		PrivateKey privateKey = electionSigningKeys.privateKey();
		PublicKey publicKey = electionSigningKeys.publicKey();
		byte[] bytes = publicKey.getEncoded();
		byte[] signature = asymmetricService.sign(privateKey, bytes);
		assertTrue(asymmetricService.verifySignature(signature, publicKey, bytes));

		X509Certificate[] certificateChain = electionSigningKeys.certificateChain();
		assertEquals(2, certificateChain.length);
		assertEquals(nodeKeys.caCertificate(), certificateChain[1]);
		X509Certificate certificate = certificateChain[0];
		assertEquals(VALID_FROM, certificate.getNotBefore());
		assertEquals(VALID_TO, certificate.getNotAfter());
		certificate.verify(nodeKeys.caPublicKey());
	}

	@Test
	void testGenerateCcmjElectionKeys() throws KeyManagementException, GeneralCryptoLibException {
		NodeKeys nodeKeys = generator.generateNodeKeys(nodeCAPrivateKey, nodeCACertificateChain);
		ElectionSigningKeys electionSigningKeys = generator.generateElectionSigningKeys(ELECTION_EVENT_ID, VALID_FROM, VALID_TO, nodeKeys);
		CcmjElectionKeysSpec spec = new CcmjElectionKeysSpec.Builder().setElectionEventId(ELECTION_EVENT_ID)
				.setElectoralAuthorityId(ELECTORAL_AUTHORITY_ID).setParameters(parameters).setLength(1).build();

		CcmjElectionKeys ccmjElectionKeys = generator.generateCcmjElectionKeys(spec, electionSigningKeys);

		ZpSubgroup group = ccmjElectionKeys.getCcmjElectionSecretKey().getGroup();
		assertEquals(parameters.getGroup(), group);

		assertEquals(1, ccmjElectionKeys.getCcmjElectionSecretKey().getKeys().size());
		assertEquals(1, ccmjElectionKeys.getCcmjElectionPublicKey().getKeys().size());
		assertTrue(asymmetricService.verifySignature(ccmjElectionKeys.getCcmjElectionPublicKeySignature(), electionSigningKeys.publicKey(),
				codec.encodeElGamalPublicKey(ccmjElectionKeys.getCcmjElectionPublicKey()), ELECTION_EVENT_ID.getBytes(StandardCharsets.UTF_8),
				ELECTORAL_AUTHORITY_ID.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void testGenerateNodeKeys()
			throws KeyManagementException, GeneralCryptoLibException, InvalidKeyException, CertificateException, NoSuchAlgorithmException,
			NoSuchProviderException, SignatureException {
		NodeKeys nodeKeys = generator.generateNodeKeys(nodeCAPrivateKey, nodeCACertificateChain);

		assertEquals(nodeCAPrivateKey, nodeKeys.caPrivateKey());
		assertArrayEquals(nodeCACertificateChain, nodeKeys.caCertificateChain());

		X509Certificate caCertificate = nodeKeys.caCertificate();

		PrivateKey encryptionPrivateKey = nodeKeys.encryptionPrivateKey();
		PublicKey encryptionPublicKey = nodeKeys.encryptionPublicKey();
		byte[] bytes = encryptionPublicKey.getEncoded();
		byte[] encrypted = asymmetricService.encrypt(encryptionPublicKey, bytes);
		assertArrayEquals(bytes, asymmetricService.decrypt(encryptionPrivateKey, encrypted));

		X509Certificate[] encryptionCertificateChain = nodeKeys.encryptionCertificateChain();
		assertEquals(2, encryptionCertificateChain.length);
		assertEquals(caCertificate, encryptionCertificateChain[1]);
		X509Certificate encryptionCertificate = encryptionCertificateChain[0];
		assertEquals(VALID_FROM, caCertificate.getNotBefore());
		assertEquals(VALID_TO, caCertificate.getNotAfter());
		encryptionCertificate.verify(caCertificate.getPublicKey());

		PrivateKey logSigningPrivateKey = nodeKeys.logSigningPrivateKey();
		PublicKey logSigningPublicKey = nodeKeys.logSigningPublicKey();
		bytes = logSigningPublicKey.getEncoded();
		encrypted = asymmetricService.encrypt(logSigningPublicKey, bytes);
		assertArrayEquals(bytes, asymmetricService.decrypt(logSigningPrivateKey, encrypted));

		X509Certificate[] logSigningCertificateChain1 = nodeKeys.logSigningCertificateChain();
		assertEquals(2, logSigningCertificateChain1.length);
		assertEquals(caCertificate, logSigningCertificateChain1[1]);
		X509Certificate logSigningCertificate = logSigningCertificateChain1[0];
		assertEquals(VALID_FROM, caCertificate.getNotBefore());
		assertEquals(VALID_TO, caCertificate.getNotAfter());
		logSigningCertificate.verify(caCertificate.getPublicKey());

		PrivateKey logEncryptionPrivateKey = nodeKeys.logEncryptionPrivateKey();
		PublicKey logEncryptionPublicKey = nodeKeys.logEncryptionPublicKey();
		bytes = logEncryptionPublicKey.getEncoded();
		encrypted = asymmetricService.encrypt(logEncryptionPublicKey, bytes);
		assertArrayEquals(bytes, asymmetricService.decrypt(logEncryptionPrivateKey, encrypted));

		X509Certificate[] logEncryptionCertificateChain = nodeKeys.logEncryptionCertificateChain();
		assertEquals(2, logEncryptionCertificateChain.length);
		assertEquals(caCertificate, logEncryptionCertificateChain[1]);
		X509Certificate logEncryptionCertificate = logEncryptionCertificateChain[0];
		assertEquals(VALID_FROM, caCertificate.getNotBefore());
		assertEquals(VALID_TO, caCertificate.getNotAfter());
		logEncryptionCertificate.verify(caCertificate.getPublicKey());
	}

	@Test
	void testGeneratePassword() throws KeyManagementException {
		PasswordProtection protection = generator.generatePassword();
		assertTrue(protection.getPassword().length >= 16);
	}
}
