/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.primitives.messagedigest.configuration.ConfigMessageDigestAlgorithmAndProvider;
import ch.post.it.evoting.cryptolib.proofs.maurer.configuration.ConfigProofHashCharset;
import ch.post.it.evoting.cryptolib.proofs.maurer.configuration.MaurerProofPolicyFromProperties;

/**
 * Tests of MaurerProofPolicyFromProperties.
 */
class MaurerProofPolicyFromPropertiesTest {

	private static MaurerProofPolicyFromProperties maurerProofPolicyFromProperties;

	@BeforeAll
	public static void setUp() {
		maurerProofPolicyFromProperties = new MaurerProofPolicyFromProperties();
	}

	@Test
	void givenPolicyWhenGetAlgorithmThenExpected() {

		ConfigMessageDigestAlgorithmAndProvider expectedConfigProofHashAlgorithm = ConfigMessageDigestAlgorithmAndProvider.SHA256_SUN;

		Assertions.assertEquals(expectedConfigProofHashAlgorithm, maurerProofPolicyFromProperties.getMessageDigestAlgorithmAndProvider());
	}

	@Test
	void givenPolicyWhenGetCharsetThenExpected() {

		ConfigProofHashCharset expectedConfigProofHashCharset = ConfigProofHashCharset.UTF8;

		Assertions.assertEquals(expectedConfigProofHashCharset, maurerProofPolicyFromProperties.getCharset());
	}
}
