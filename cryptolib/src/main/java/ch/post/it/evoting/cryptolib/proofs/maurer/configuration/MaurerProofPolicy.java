/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.configuration;

import ch.post.it.evoting.cryptolib.primitives.messagedigest.configuration.MessageDigestPolicy;
import ch.post.it.evoting.cryptolib.primitives.securerandom.configuration.SecureRandomPolicy;

/**
 * Defines Maurer Unified Proof policy.
 */
public interface MaurerProofPolicy extends SecureRandomPolicy, MessageDigestPolicy {

	/**
	 * Gets the character set to be used during the generation and verification of proofs.
	 *
	 * @return charset.
	 */
	ConfigProofHashCharset getCharset();
}
