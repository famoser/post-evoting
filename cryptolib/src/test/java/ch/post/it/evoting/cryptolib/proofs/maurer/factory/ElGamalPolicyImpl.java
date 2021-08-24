/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.factory;

import ch.post.it.evoting.cryptolib.elgamal.configuration.ConfigGroupType;
import ch.post.it.evoting.cryptolib.elgamal.configuration.ElGamalPolicy;
import ch.post.it.evoting.cryptolib.primitives.securerandom.configuration.ConfigSecureRandomAlgorithmAndProvider;

/**
 * Implementation of {@link ElGamalPolicy}, used for testing.
 */
public class ElGamalPolicyImpl implements ElGamalPolicy {

	private final ConfigSecureRandomAlgorithmAndProvider _secureRandomAlgorithmProviderPair;
	private final ConfigGroupType _groupType;

	/**
	 * Creates an instance of policy with provided {@code secureRandomAlgorithmProviderPair}.
	 *
	 * @param secureRandomAlgorithmProviderPair secure random number generation cryptographic policy.
	 */
	public ElGamalPolicyImpl(final ConfigSecureRandomAlgorithmAndProvider secureRandomAlgorithmProviderPair, final ConfigGroupType groupType) {

		_secureRandomAlgorithmProviderPair = secureRandomAlgorithmProviderPair;
		_groupType = groupType;
	}

	@Override
	public ConfigSecureRandomAlgorithmAndProvider getSecureRandomAlgorithmAndProvider() {
		return _secureRandomAlgorithmProviderPair;
	}

	@Override
	public ConfigGroupType getGroupType() {
		return _groupType;
	}
}
