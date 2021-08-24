/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.payloadsignature;

import static ch.post.it.evoting.cryptolib.certificates.utils.CertificateChainValidator.isCertificateChainValid;
import static com.google.common.base.Preconditions.checkNotNull;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;
import ch.post.it.evoting.domain.election.model.messaging.PayloadSignatureException;
import ch.post.it.evoting.domain.election.model.messaging.PayloadVerificationException;

@Service
public class CryptolibPayloadSignatureService {

	private final AsymmetricServiceAPI asymmetricService;

	@Autowired
	public CryptolibPayloadSignatureService(final AsymmetricServiceAPI asymmetricService) {
		this.asymmetricService = checkNotNull(asymmetricService);
	}

	public CryptolibPayloadSignature sign(final byte[] payloadHash, final PrivateKey signingKey, final X509Certificate[] certificateChain)
			throws PayloadSignatureException {

		checkNotNull(payloadHash);
		checkNotNull(signingKey);
		checkNotNull(certificateChain);

		final byte[] signature;
		try {
			signature = asymmetricService.sign(signingKey, payloadHash);
		} catch (GeneralCryptoLibException e) {
			throw new PayloadSignatureException("Failed to sign payload hash: " + e.getMessage());
		}

		return new CryptolibPayloadSignature(signature, certificateChain);
	}

	public boolean verify(final CryptolibPayloadSignature signature, final X509Certificate platformRootCA, final byte[] payloadHash)
			throws PayloadVerificationException {

		checkNotNull(signature);
		checkNotNull(platformRootCA);
		checkNotNull(payloadHash);

		final X509Certificate[] certificateChain = signature.getCertificateChain();

		try {
			// Verify certificate chain
			if (!isCertificateChainValid(certificateChain, platformRootCA)) {
				return false;
			}
			// Verify signature
			final PublicKey publicKey = certificateChain[0].getPublicKey();
			return asymmetricService.verifySignature(signature.getSignatureContents(), publicKey, payloadHash);
		} catch (GeneralCryptoLibException e) {
			throw new PayloadVerificationException(e);
		}
	}
}
