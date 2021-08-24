/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

package ch.post.it.evoting.sdm.config.shares;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.secretsharing.Share;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.secretsharing.service.ThresholdSecretSharingService;
import ch.post.it.evoting.cryptolib.secretsharing.shamir.ShamirShare;

public abstract class AbstractWithSharesTest {

	protected static KeyPair keyPair;
	protected static Set<Share> shares;
	protected static byte[] originalSecretBytes;
	protected static byte[] clonedSecretBytes;
	protected static ThresholdSecretSharingService thresholdSecretSharingService;
	protected static AsymmetricServiceAPI asymmetricService;

	@BeforeAll
	public static void initialize() {
		thresholdSecretSharingService = new ThresholdSecretSharingService();
		asymmetricService = new AsymmetricService();
		keyPair = asymmetricService.getKeyPairForEncryption();

		final BigInteger privateExponent = ((RSAPrivateKey) keyPair.getPrivate()).getPrivateExponent();
		originalSecretBytes = privateExponent.toByteArray();

		// The split method clears the original secret bytes.
		clonedSecretBytes = originalSecretBytes.clone();

		shares = thresholdSecretSharingService.split(clonedSecretBytes, 5, 3, privateExponent.nextProbablePrime());
	}

	@AfterAll
	public static void cleanUp() {
		for (Share share : shares) {
			share.destroy();
			assertEquals(0, share.getModulus().intValue());
			assertEquals(0, ((ShamirShare) share).getPoints().get(0).getX().intValue());
			assertEquals(0, ((ShamirShare) share).getPoints().get(0).getY().intValue());
		}
	}

	protected void assertEmpty(final byte[] bytes) {
		for (byte b : bytes) {
			assertEquals(0x00, b);
		}
	}

}
