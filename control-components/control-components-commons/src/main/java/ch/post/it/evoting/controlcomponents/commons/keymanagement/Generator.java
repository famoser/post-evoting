/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import static java.lang.System.arraycopy;

import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore.PasswordProtection;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.certificates.CertificatesServiceAPI;
import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.primitives.PrimitivesServiceAPI;
import ch.post.it.evoting.cryptolib.api.securerandom.CryptoAPIRandomString;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateData;
import ch.post.it.evoting.cryptolib.certificates.bean.ValidityDates;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.factory.CryptoX509Certificate;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;

@Service
public class Generator {
	private static final int PASSWORD_LENGTH = 26;

	private static final String FAILED_TO_GENERATE_NODE_ENCRYPTION_CERTIFICATE = "Failed to generate node encryption certificate.";

	private final AsymmetricServiceAPI asymmetricService;

	private final CertificatesServiceAPI certificatesService;

	private final ElGamalServiceAPI elGamalService;

	private final PrimitivesServiceAPI primitivesService;

	private final Codec codec;

	private final String nodeId;

	public Generator(final AsymmetricServiceAPI asymmetricService, final CertificatesServiceAPI certificatesService,
			final ElGamalServiceAPI elGamalService, final PrimitivesServiceAPI primitivesService, final Codec codec,
			@Value("${key.node.id}")
			final String nodeId) {
		this.asymmetricService = asymmetricService;
		this.certificatesService = certificatesService;
		this.elGamalService = elGamalService;
		this.primitivesService = primitivesService;
		this.codec = codec;
		this.nodeId = nodeId;
	}

	private static X509DistinguishedName generateSubjectDn(String commonName, X509DistinguishedName issuerDn) throws GeneralCryptoLibException {
		return new X509DistinguishedName.Builder(commonName, issuerDn.getCountry()).addLocality(issuerDn.getLocality())
				.addOrganization(issuerDn.getOrganization()).addOrganizationalUnit(issuerDn.getOrganizationalUnit()).build();
	}

	private static X509Certificate[] newCertificateChain(X509Certificate certificate, X509Certificate[] issuerChain) {
		X509Certificate[] chain = new X509Certificate[issuerChain.length + 1];
		chain[0] = certificate;
		arraycopy(issuerChain, 0, chain, 1, issuerChain.length);
		return chain;
	}

	public CcrjReturnCodesKeys generateCcrjReturnCodesKeys(final CcrjReturnCodesKeysSpec ccrjReturnCodesKeysSpec,
			final ElectionSigningKeys electionSigningKeys) throws KeyManagementException {

		final CcrjReturnCodesKeys.Builder builder = new CcrjReturnCodesKeys.Builder();

		final ElGamalKeyPair ccrjReturnCodesGenerationKeyPair = generateElGamalKeyPair(ccrjReturnCodesKeysSpec.getParameters(),
				ccrjReturnCodesKeysSpec.getCcrjReturnCodesGenerationKeyLength());
		final byte[] generationPublicKeySignature = signElGamalPublicKey(electionSigningKeys.privateKey(),
				ccrjReturnCodesGenerationKeyPair.getPublicKeys(), ccrjReturnCodesKeysSpec.getElectionEventId(),
				ccrjReturnCodesKeysSpec.getVerificationCardSetId());
		builder.setCcrjReturnCodesGenerationKeys(ccrjReturnCodesGenerationKeyPair.getPrivateKeys(), ccrjReturnCodesGenerationKeyPair.getPublicKeys(),
				generationPublicKeySignature);

		final ElGamalKeyPair ccrjChoiceReturnCodesEncryptionKeyPair = generateElGamalKeyPair(ccrjReturnCodesKeysSpec.getParameters(),
				ccrjReturnCodesKeysSpec.getCcrjChoiceReturnCodesEncryptionKeyLength());
		final byte[] decryptionPublicKeySignature = signElGamalPublicKey(electionSigningKeys.privateKey(),
				ccrjChoiceReturnCodesEncryptionKeyPair.getPublicKeys(), ccrjReturnCodesKeysSpec.getElectionEventId(),
				ccrjReturnCodesKeysSpec.getVerificationCardSetId());
		builder.setCcrjChoiceReturnCodesEncryptionKeys(ccrjChoiceReturnCodesEncryptionKeyPair.getPrivateKeys(),
				ccrjChoiceReturnCodesEncryptionKeyPair.getPublicKeys(), decryptionPublicKeySignature);

		return builder.build();
	}

	public ElectionSigningKeys generateElectionSigningKeys(String electionEventId, Date validFrom, Date validTo, NodeKeys nodeKeys)
			throws KeyManagementException {
		KeyPair pair = asymmetricService.getKeyPairForSigning();
		X509Certificate certificate = generateElectionSigningCertificate(electionEventId, validFrom, validTo, pair.getPublic(),
				nodeKeys.caPrivateKey(), nodeKeys.caCertificate());
		X509Certificate[] certificateChain = newCertificateChain(certificate, nodeKeys.caCertificateChain());
		return new ElectionSigningKeys(pair.getPrivate(), certificateChain);
	}

	public CcmjElectionKeys generateCcmjElectionKeys(final CcmjElectionKeysSpec ccmjElectionKeysSpec, final ElectionSigningKeys electionSigningKeys)
			throws KeyManagementException {

		final ElGamalKeyPair ccmjElectionKeyPair = generateElGamalKeyPair(ccmjElectionKeysSpec.getParameters(), ccmjElectionKeysSpec.getLength());

		final byte[] publicKeySignature = signElGamalPublicKey(electionSigningKeys.privateKey(), ccmjElectionKeyPair.getPublicKeys(),
				ccmjElectionKeysSpec.getElectionEventId(), ccmjElectionKeysSpec.getElectoralAuthorityId());

		return new CcmjElectionKeys(ccmjElectionKeyPair.getPrivateKeys(), ccmjElectionKeyPair.getPublicKeys(), publicKeySignature);
	}

	public NodeKeys generateNodeKeys(PrivateKey caPrivateKey, X509Certificate[] caCertificateChain) throws KeyManagementException {
		NodeKeys.Builder builder = new NodeKeys.Builder();

		builder.setCAKeys(caPrivateKey, caCertificateChain);

		KeyPair encryptionPair = asymmetricService.getKeyPairForEncryption();
		X509Certificate encryptionCertificate = generateNodeEncryptionCertificate(encryptionPair.getPublic(), caPrivateKey, caCertificateChain[0]);

		X509Certificate[] encryptionCertificateChain = newCertificateChain(encryptionCertificate, caCertificateChain);
		builder.setEncryptionKeys(encryptionPair.getPrivate(), encryptionCertificateChain);

		KeyPair logSignPair = asymmetricService.getKeyPairForSigning();
		X509Certificate logSignCertificate = generateNodeLogSignCertificate(logSignPair.getPublic(), caPrivateKey, caCertificateChain[0]);
		X509Certificate[] logSignCertificateChain = newCertificateChain(logSignCertificate, caCertificateChain);
		builder.setLogSigningKeys(logSignPair.getPrivate(), logSignCertificateChain);

		KeyPair logEncryptionPair = asymmetricService.getKeyPairForEncryption();
		X509Certificate logEncryptionCertificate = generateNodeLogEncryptionCertificate(logEncryptionPair.getPublic(), caPrivateKey,
				caCertificateChain[0]);
		X509Certificate[] logEncryptionCertificateChain = newCertificateChain(logEncryptionCertificate, caCertificateChain);
		builder.setLogEncryptionKeys(logEncryptionPair.getPrivate(), logEncryptionCertificateChain);

		return builder.build();
	}

	public PasswordProtection generatePassword() throws KeyManagementException {
		CryptoAPIRandomString generator = primitivesService.get32CharAlphabetCryptoRandomString();
		String password;
		try {
			password = generator.nextRandom(PASSWORD_LENGTH);
		} catch (GeneralCryptoLibException e) {
			throw new KeyManagementException("Failed to generate password.", e);
		}
		return new PasswordProtection(password.toCharArray());
	}

	private X509Certificate generateElectionSigningCertificate(String electionEventId, Date validFrom, Date validTo, PublicKey publicKey,
			PrivateKey caPrivateKey, X509Certificate caCertificate) throws KeyManagementException {
		try {
			CryptoX509Certificate issuerCertificate = new CryptoX509Certificate(caCertificate);
			X509DistinguishedName issuerDn = issuerCertificate.getSubjectDn();

			CertificateData data = new CertificateData();
			X509DistinguishedName subjectDn = generateSubjectDn(electionEventId, issuerDn);
			data.setSubjectDn(subjectDn);
			data.setSubjectPublicKey(publicKey);
			ValidityDates dates = new ValidityDates(validFrom, validTo);
			data.setValidityDates(dates);
			data.setIssuerDn(issuerDn);

			return certificatesService.createSignX509Certificate(data, caPrivateKey).getCertificate();
		} catch (GeneralCryptoLibException e) {
			throw new KeyManagementException("Failed to generate election signing certificate.", e);
		}
	}

	private ElGamalKeyPair generateElGamalKeyPair(final ElGamalEncryptionParameters parameters, final int length) throws KeyManagementException {
		try {
			return elGamalService.generateKeyPair(parameters, length);
		} catch (GeneralCryptoLibException e) {
			throw new KeyManagementException("Failed to generate ElGamal key pair.", e);
		}
	}

	private X509Certificate generateNodeEncryptionCertificate(PublicKey publicKey, PrivateKey caPrivateKey, X509Certificate caCertificate)
			throws KeyManagementException {
		try {
			CryptoX509Certificate issuerCertificate = new CryptoX509Certificate(caCertificate);
			X509DistinguishedName issuerDn = issuerCertificate.getSubjectDn();

			CertificateData data = new CertificateData();
			String commonName = nodeId + " Encryption";
			data.setSubjectDn(generateSubjectDn(commonName, issuerDn));
			data.setSubjectPublicKey(publicKey);
			data.setValidityDates(new ValidityDates(issuerCertificate.getNotBefore(), issuerCertificate.getNotAfter()));
			data.setIssuerDn(issuerDn);

			return certificatesService.createEncryptionX509Certificate(data, caPrivateKey).getCertificate();
		} catch (GeneralCryptoLibException e) {
			throw new KeyManagementException(FAILED_TO_GENERATE_NODE_ENCRYPTION_CERTIFICATE, e);
		}
	}

	private X509Certificate generateNodeLogEncryptionCertificate(PublicKey publicKey, PrivateKey caPrivateKey, X509Certificate caCertificate)
			throws KeyManagementException {
		try {
			CryptoX509Certificate issuerCertificate = new CryptoX509Certificate(caCertificate);
			X509DistinguishedName issuerDn = issuerCertificate.getSubjectDn();

			CertificateData data = new CertificateData();
			String commonName = nodeId + " Log Encryption";
			data.setSubjectDn(generateSubjectDn(commonName, issuerDn));
			data.setSubjectPublicKey(publicKey);
			data.setValidityDates(new ValidityDates(issuerCertificate.getNotBefore(), issuerCertificate.getNotAfter()));
			data.setIssuerDn(issuerDn);

			return certificatesService.createEncryptionX509Certificate(data, caPrivateKey).getCertificate();
		} catch (GeneralCryptoLibException e) {
			throw new KeyManagementException(FAILED_TO_GENERATE_NODE_ENCRYPTION_CERTIFICATE, e);
		}
	}

	private X509Certificate generateNodeLogSignCertificate(PublicKey publicKey, PrivateKey caPrivateKey, X509Certificate caCertificate)
			throws KeyManagementException {
		try {
			CryptoX509Certificate issuerCertificate = new CryptoX509Certificate(caCertificate);
			X509DistinguishedName issuerDn = issuerCertificate.getSubjectDn();

			CertificateData data = new CertificateData();
			String commonName = nodeId + " Log Sign";
			data.setSubjectDn(generateSubjectDn(commonName, issuerDn));
			data.setSubjectPublicKey(publicKey);
			data.setValidityDates(new ValidityDates(issuerCertificate.getNotBefore(), issuerCertificate.getNotAfter()));
			data.setIssuerDn(issuerDn);

			return certificatesService.createSignX509Certificate(data, caPrivateKey).getCertificate();
		} catch (GeneralCryptoLibException e) {
			throw new KeyManagementException(FAILED_TO_GENERATE_NODE_ENCRYPTION_CERTIFICATE, e);
		}
	}

	private byte[] signElGamalPublicKey(PrivateKey signingKey, ElGamalPublicKey key, String... attributes) throws KeyManagementException {
		byte[][] data = new byte[attributes.length + 1][];
		data[0] = codec.encodeElGamalPublicKey(key);
		for (int i = 0; i < attributes.length; i++) {
			data[i + 1] = attributes[i].getBytes(StandardCharsets.UTF_8);
		}
		try {
			return asymmetricService.sign(signingKey, data);
		} catch (GeneralCryptoLibException e) {
			throw new KeyManagementException("Failed to sign ElGamal public key.", e);
		}
	}
}
