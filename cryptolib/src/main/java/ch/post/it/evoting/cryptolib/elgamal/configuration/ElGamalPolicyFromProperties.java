/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.configuration;

import java.util.Properties;

import ch.post.it.evoting.cryptolib.api.exceptions.CryptoLibException;
import ch.post.it.evoting.cryptolib.commons.configuration.PolicyFromPropertiesHelper;
import ch.post.it.evoting.cryptolib.commons.system.OperatingSystem;
import ch.post.it.evoting.cryptolib.primitives.securerandom.configuration.ConfigSecureRandomAlgorithmAndProvider;

/**
 * An implementation of {@link ElGamalPolicy} which reads the policy parameters from a properties source. For a key that is not set in the properties
 * input, a default value will be used. The default values are:
 * <ul>
 *     <li>elgamal.securerandom.unix=NATIVE_PRNG_SUN</li>
 *     <li>elgamal.securerandom.windows=PRNG_SUN_MSCAPI</li>
 *     <li>elgamal.grouptype=QR_2048</li>
 * </ul>
 */
public final class ElGamalPolicyFromProperties implements ElGamalPolicy {

	static final ConfigSecureRandomAlgorithmAndProvider DEFAULT_UNIX_SECURE_RANDOM = ConfigSecureRandomAlgorithmAndProvider.NATIVE_PRNG_SUN;
	static final ConfigSecureRandomAlgorithmAndProvider DEFAULT_WINDOWS_SECURE_RANDOM = ConfigSecureRandomAlgorithmAndProvider.PRNG_SUN_MSCAPI;
	static final ConfigGroupType DEFAULT_GROUP_TYPE = ConfigGroupType.QR_2048;
	static final String ELGAMAL_SECURE_RANDOM_KEY = "elgamal.securerandom";
	static final String ELGAMAL_GROUP_TYPE_KEY = "elgamal.grouptype";

	private final ConfigSecureRandomAlgorithmAndProvider secureRandomAlgorithmAndProvider;
	private final ConfigGroupType groupType;

	/**
	 * Initializes a class instance with the standard properties.
	 */
	public ElGamalPolicyFromProperties() {
		this(PolicyFromPropertiesHelper.loadCryptolibProperties());
	}

	/**
	 * Initializes a class instance with the properties specified.
	 *
	 * @param properties the properties that define the policy.
	 */
	public ElGamalPolicyFromProperties(Properties properties) {

		PolicyFromPropertiesHelper helper = new PolicyFromPropertiesHelper(properties);
		try {
			secureRandomAlgorithmAndProvider = ConfigSecureRandomAlgorithmAndProvider.valueOf(
					helper.getNotBlankOrDefaultOSDependentPropertyValue(ELGAMAL_SECURE_RANDOM_KEY, DEFAULT_UNIX_SECURE_RANDOM.name(),
							DEFAULT_WINDOWS_SECURE_RANDOM.name()));

			if (!secureRandomAlgorithmAndProvider.isOSCompliant(OperatingSystem.current())) {
				throw new CryptoLibException("Illegal property value");
			}

			groupType = ConfigGroupType.valueOf(helper.getNotBlankOrDefaultPropertyValue(ELGAMAL_GROUP_TYPE_KEY, DEFAULT_GROUP_TYPE.name()));

		} catch (IllegalArgumentException e) {
			throw new CryptoLibException("Illegal property value", e);
		}
	}

	@Override
	public ConfigSecureRandomAlgorithmAndProvider getSecureRandomAlgorithmAndProvider() {
		return secureRandomAlgorithmAndProvider;
	}

	@Override
	public ConfigGroupType getGroupType() {
		return groupType;
	}
}
