/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.configuration;

import java.util.Properties;

import ch.post.it.evoting.cryptolib.api.exceptions.CryptoLibException;
import ch.post.it.evoting.cryptolib.commons.configuration.PolicyFromPropertiesHelper;
import ch.post.it.evoting.cryptolib.primitives.messagedigest.configuration.ConfigMessageDigestAlgorithmAndProvider;
import ch.post.it.evoting.cryptolib.primitives.securerandom.configuration.ConfigSecureRandomAlgorithmAndProvider;

/**
 * A Maurer Unified Proof policy obtained from a properties source. For a key that is not set in the properties input, a default value will be used.
 * The default values are:
 * <ul>
 *     <li>proofs.hashbuilder.messagedigest=SHA256_SUN</li>
 *     <li>proofs.hashbuilder.charset=UTF8</li>
 *     <li>proofs.securerandom.unix=NATIVE_PRNG_SUN</li>
 *     <li>proofs.securerandom.windows=PRNG_SUN_MSCAPI</li>
 * </ul>
 */
public final class MaurerProofPolicyFromProperties implements MaurerProofPolicy {

	static final ConfigMessageDigestAlgorithmAndProvider DEFAULT_MESSAGE_DIGEST = ConfigMessageDigestAlgorithmAndProvider.SHA256_SUN;
	static final ConfigProofHashCharset DEFAULT_CHARSET = ConfigProofHashCharset.UTF8;
	static final ConfigSecureRandomAlgorithmAndProvider DEFAULT_UNIX_SECURE_RANDOM = ConfigSecureRandomAlgorithmAndProvider.NATIVE_PRNG_SUN;
	static final ConfigSecureRandomAlgorithmAndProvider DEFAULT_WINDOWS_SECURE_RANDOM = ConfigSecureRandomAlgorithmAndProvider.PRNG_SUN_MSCAPI;
	static final String MESSAGE_DIGEST_KEY = "proofs.hashbuilder.messagedigest";
	static final String CHARSET_KEY = "proofs.hashbuilder.charset";
	static final String SECURE_RANDOM_KEY = "proofs.securerandom";

	private final ConfigMessageDigestAlgorithmAndProvider configMessageDigestAlgorithmAndProvider;
	private final ConfigProofHashCharset configProofHashCharset;
	private final ConfigSecureRandomAlgorithmAndProvider configSecureRandomAlgorithmAndProvider;

	public MaurerProofPolicyFromProperties() {
		this(PolicyFromPropertiesHelper.loadCryptolibProperties());
	}

	/**
	 * Creates an instance of the class from the specified path.
	 *
	 * @param properties the properties that define the policy.
	 * @throws CryptoLibException if the required properties are invalid.
	 */
	public MaurerProofPolicyFromProperties(Properties properties) {

		PolicyFromPropertiesHelper helper = new PolicyFromPropertiesHelper(properties);
		configMessageDigestAlgorithmAndProvider = ConfigMessageDigestAlgorithmAndProvider
				.valueOf(helper.getNotBlankOrDefaultPropertyValue(MESSAGE_DIGEST_KEY, DEFAULT_MESSAGE_DIGEST.name()));

		configProofHashCharset = ConfigProofHashCharset.valueOf(helper.getNotBlankOrDefaultPropertyValue(CHARSET_KEY, DEFAULT_CHARSET.name()));

		configSecureRandomAlgorithmAndProvider = ConfigSecureRandomAlgorithmAndProvider.valueOf(
				helper.getNotBlankOrDefaultOSDependentPropertyValue(SECURE_RANDOM_KEY, DEFAULT_UNIX_SECURE_RANDOM.name(),
						DEFAULT_WINDOWS_SECURE_RANDOM.name()));
	}

	@Override
	public ConfigSecureRandomAlgorithmAndProvider getSecureRandomAlgorithmAndProvider() {
		return configSecureRandomAlgorithmAndProvider;
	}

	@Override
	public ConfigMessageDigestAlgorithmAndProvider getMessageDigestAlgorithmAndProvider() {
		return configMessageDigestAlgorithmAndProvider;
	}

	@Override
	public ConfigProofHashCharset getCharset() {
		return configProofHashCharset;
	}
}
