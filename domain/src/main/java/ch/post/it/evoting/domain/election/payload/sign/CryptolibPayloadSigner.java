/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.payload.sign;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;
import ch.post.it.evoting.domain.election.model.messaging.Payload;
import ch.post.it.evoting.domain.election.model.messaging.PayloadSignature;
import ch.post.it.evoting.domain.election.model.messaging.PayloadSignatureException;

/**
 * A service that signs and verifies payloads based on the cryptolib.
 */
public class CryptolibPayloadSigner implements PayloadSigner {

	private static final Logger LOGGER = LoggerFactory.getLogger(CryptolibPayloadSigner.class);

	private final AsymmetricServiceAPI asymmetricService;

	/**
	 * Initialises the service with the signing key.
	 *
	 * @param asymmetricService an instance of the service used for signing
	 */
	public CryptolibPayloadSigner(AsymmetricServiceAPI asymmetricService) {
		this.asymmetricService = asymmetricService;
	}

	@Override
	public PayloadSignature sign(Payload payload, PrivateKey signingKey, X509Certificate[] certificateChain) throws PayloadSignatureException {
		LOGGER.info("Signing payload {}...", payload);

		Objects.requireNonNull(payload, "A payload is required for signing");
		Objects.requireNonNull(signingKey, "A signing key is required for signing");
		Objects.requireNonNull(certificateChain, "The verification key's certificate chain is required for signing");

		try {
			byte[] signatureContents = asymmetricService.sign(signingKey, payload.getSignableContent());
			PayloadSignature payloadSignature = new CryptolibPayloadSignature(signatureContents, certificateChain);
			LOGGER.info("Payload {} signed successfully", payload);

			return payloadSignature;
		} catch (IOException | GeneralCryptoLibException e) {
			LOGGER.error("Failed signing payload {}!", payload, e);
			throw new PayloadSignatureException(e);
		}
	}
}
