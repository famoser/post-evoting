/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.CryptoLibException;
import ch.post.it.evoting.cryptolib.commons.system.OperatingSystem;
import ch.post.it.evoting.cryptolib.primitives.securerandom.configuration.ConfigSecureRandomAlgorithmAndProvider;

class ElGamalPolicyFromPropertiesTest {

	private static ConfigSecureRandomAlgorithmAndProvider secureRandomAlgorithmProviderPair;
	private static ElGamalPolicy cryptoElGamalPolicyFromProperties;

	@BeforeAll
	static void setUp() {
		secureRandomAlgorithmProviderPair = getCryptoSecureRandomAlgorithmAndProvider();
	}

	private static ConfigSecureRandomAlgorithmAndProvider getCryptoSecureRandomAlgorithmAndProvider() {
		switch (OperatingSystem.current()) {
		case WINDOWS:
			return ConfigSecureRandomAlgorithmAndProvider.PRNG_SUN_MSCAPI;
		case UNIX:
			return ConfigSecureRandomAlgorithmAndProvider.NATIVE_PRNG_SUN;
		default:
			throw new CryptoLibException("OS not supported");
		}
	}

	@Test
	void givenPolicyFromPropertiesWhenGetFieldsThenExpectedValues() {
		cryptoElGamalPolicyFromProperties = new ElGamalPolicyFromProperties();

		assertPolicyHasExpectedValues(secureRandomAlgorithmProviderPair, cryptoElGamalPolicyFromProperties);
	}

	@Test
	void givenDefaultPolicyWhenGetFieldsThenExpectedValues() {
		cryptoElGamalPolicyFromProperties = new ElGamalPolicyFromProperties();

		assertPolicyHasExpectedValues(secureRandomAlgorithmProviderPair, cryptoElGamalPolicyFromProperties);
	}

	@Test
	void givenWrongPolicyWhenGetFieldsThenThrowException() {
		Properties properties = new Properties();
		properties.setProperty("elgamal.securerandom.unix", "WRONG_PROVIDER");
		properties.setProperty("elgamal.securerandom.windows", "WRONG_PROVIDER");

		assertThrows(CryptoLibException.class, () -> new ElGamalPolicyFromProperties(properties));
	}

	@Test
	void givenWrongSecureRandomProviderWhenGetFieldsThenThrowException() {
		Properties properties = new Properties();
		properties.setProperty("elgamal.securerandom.unix", ConfigSecureRandomAlgorithmAndProvider.PRNG_SUN_MSCAPI.name());
		properties.setProperty("elgamal.securerandom.windows", ConfigSecureRandomAlgorithmAndProvider.NATIVE_PRNG_SUN.name());

		assertThrows(CryptoLibException.class, () -> new ElGamalPolicyFromProperties(properties));
	}

	private void assertPolicyHasExpectedValues(final ConfigSecureRandomAlgorithmAndProvider pair, final ElGamalPolicy policy) {
		assertSecureRandomPairIsExpectedValue(pair, policy);
	}

	private void assertSecureRandomPairIsExpectedValue(final ConfigSecureRandomAlgorithmAndProvider pair, final ElGamalPolicy elGamalPolicy) {
		String errorMsg = "The SecureRandom algorithm and provider pair are not the expected values";

		assertEquals(pair, elGamalPolicy.getSecureRandomAlgorithmAndProvider(), errorMsg);
	}
}
