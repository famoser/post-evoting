/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service.impl;

import static java.util.Arrays.copyOfRange;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.StringJoiner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.bean.X509CertificateType;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.factory.CryptoX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.utils.X509CertificateChainValidator;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;

/**
 * Implementation of {@link CCPublicKeySignatureValidator}.
 */
@Service
public final class CCPublicKeySignatureValidatorImpl implements CCPublicKeySignatureValidator {

	private final AsymmetricServiceAPI asymmetricService;

	/**
	 * Constructor.
	 *
	 * @param asymmetricService
	 */
	public CCPublicKeySignatureValidatorImpl(
			@Autowired
					AsymmetricServiceAPI asymmetricService) {
		this.asymmetricService = asymmetricService;
	}

	@Override
	public void checkChoiceCodesEncryptionKeySignature(byte[] signature, X509Certificate[] certificateChain, ElGamalPublicKey key,
			String electionEventId, String verificationCardSetId) throws SignatureException {
		checkElGamalKeySignature(signature, certificateChain, key, electionEventId, verificationCardSetId);
	}

	@Override
	public void checkMixingKeySignature(byte[] signature, X509Certificate[] certificateChain, ElGamalPublicKey key, String electionEventId,
			String electoralAuthorityId) throws SignatureException {
		checkElGamalKeySignature(signature, certificateChain, key, electionEventId, electoralAuthorityId);
	}

	private void checkCertificateChain(X509Certificate[] chain) throws SignatureException {
		if (chain.length < 2) {
			throw new SignatureException("Invalid certificate chain.");
		}

		X509Certificate leafCertificate = chain[0];
		X509DistinguishedName leafCertificateSubjectDn = getSubjectDn(leafCertificate);

		X509Certificate[] intermediateChain = copyOfRange(chain, 1, chain.length - 1);
		X509DistinguishedName[] intermediateSubjectDns = new X509DistinguishedName[intermediateChain.length];
		for (int i = 0; i < intermediateChain.length; i++) {
			intermediateSubjectDns[i] = getSubjectDn(intermediateChain[i]);
		}

		X509Certificate trustedCertificate = chain[chain.length - 1];

		try {
			X509CertificateChainValidator validator = new X509CertificateChainValidator(leafCertificate, X509CertificateType.SIGN,
					leafCertificateSubjectDn, intermediateChain, intermediateSubjectDns, trustedCertificate);
			List<String> errors = validator.validate();
			if (!errors.isEmpty()) {
				StringJoiner message = new StringJoiner(" ", "Invalid certificate chain: ", "");
				errors.forEach(message::add);
				throw new SignatureException(message.toString());
			}
		} catch (GeneralCryptoLibException e) {
			throw new SignatureException("Failed to validate the certificate chain.", e);
		}
	}

	private void checkElGamalKeySignature(byte[] signature, X509Certificate[] certificateChain, ElGamalPublicKey key, String... attributes)
			throws SignatureException {
		checkCertificateChain(certificateChain);
		PublicKey publicKey = certificateChain[0].getPublicKey();
		byte[][] data = encode(key, attributes);
		checkSignature(signature, publicKey, data);
	}

	private void checkSignature(byte[] signature, PublicKey key, byte[][] data) throws SignatureException {
		try {
			if (!asymmetricService.verifySignature(signature, key, data)) {
				throw new SignatureException("Invalid signature.");
			}
		} catch (GeneralCryptoLibException e) {
			throw new SignatureException("Failed to validate signature.", e);
		}
	}

	private byte[][] encode(ElGamalPublicKey key, String[] attributes) throws SignatureException {
		byte[][] data = new byte[attributes.length + 1][];
		try {
			data[0] = key.toJson().getBytes(StandardCharsets.UTF_8);
		} catch (GeneralCryptoLibException e) {
			throw new SignatureException("Failed to validate signature.", e);
		}
		for (int i = 0; i < attributes.length; i++) {
			data[i + 1] = attributes[i].getBytes(StandardCharsets.UTF_8);
		}
		return data;
	}

	private X509DistinguishedName getSubjectDn(X509Certificate certificate) throws SignatureException {
		try {
			return new CryptoX509Certificate(certificate).getSubjectDn();
		} catch (GeneralCryptoLibException e) {
			throw new SignatureException("Failed to extract Subject DN.", e);
		}
	}

}
