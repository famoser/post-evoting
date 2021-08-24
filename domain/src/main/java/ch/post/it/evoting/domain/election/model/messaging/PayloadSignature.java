/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.model.messaging;

import java.io.Serializable;
import java.security.cert.X509Certificate;

/**
 * Represents a signature for a payload.
 */
public interface PayloadSignature extends Serializable {

	/**
	 * Gets a certificate chain whose last element is the public key used to validate the signature.
	 *
	 * @return the certificate chain
	 */
	X509Certificate[] getCertificateChain();

	/**
	 * @return the byte array representing the signature
	 */
	byte[] getSignatureContents();
}
