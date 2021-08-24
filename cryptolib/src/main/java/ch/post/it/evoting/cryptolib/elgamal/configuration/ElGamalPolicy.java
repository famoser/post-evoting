/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.configuration;

import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.primitives.securerandom.configuration.SecureRandomPolicy;

/**
 * Defines the ElGamal policy. Extends the secure random policy.
 */
public interface ElGamalPolicy extends SecureRandomPolicy {

	/**
	 * Returns the {@link ConfigGroupType} that should be used when generating instances of {@link ElGamalEncryptionParameters}.
	 *
	 * @return the configuration for creating parameters of Zp subgroup.
	 */
	ConfigGroupType getGroupType();
}
