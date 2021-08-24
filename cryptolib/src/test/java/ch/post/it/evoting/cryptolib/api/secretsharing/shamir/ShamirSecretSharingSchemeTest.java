/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.api.secretsharing.shamir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.secretsharing.Share;
import ch.post.it.evoting.cryptolib.mathematical.polynomials.Point;
import ch.post.it.evoting.cryptolib.secretsharing.service.ThresholdSecretSharingService;
import ch.post.it.evoting.cryptolib.secretsharing.shamir.ShamirShare;

class ShamirSecretSharingSchemeTest {

	private ThresholdSecretSharingService secretSharingService;

	@BeforeEach
	void initialize() {
		this.secretSharingService = new ThresholdSecretSharingService();
	}

	@Test
	void testSecretWithFirstBits0() {
		byte[] secret = new byte[] { (byte) 0x00, (byte) 0x01 };
		byte[] secretClone = secret.clone();
		BigInteger modulus = new BigInteger(1, secret).nextProbablePrime();

		Set<Share> split = secretSharingService.split(secretClone, 2, 1, modulus);
		byte[] recoveredSecret = secretSharingService.recover(split);

		// Make sure the secret is destroyed
		assertArrayEquals(new byte[secret.length], secretClone);

		// Check the recovered secret
		assertArrayEquals(secret, recoveredSecret);
	}

	@Test
	void testGoThereAndBackAgainWithSecretLeadingBit1() {
		// We generate a 2048 bit secret
		byte[] secret = new byte[256];
		Arrays.fill(secret, (byte) -128);

		byte[] clonedSecret = secret.clone();

		Set<Share> shares = secretSharingService.split(clonedSecret, 2, 1, new BigInteger(1, secret).nextProbablePrime());

		// Make sure the secret has been disposed
		assertArrayEquals(new byte[clonedSecret.length], clonedSecret);

		assertArrayEquals(secret, secretSharingService.recover(shares));
		Share[] shareArray = shares.toArray(new Share[0]);

		assertArrayEquals(secret, secretSharingService.recover(new HashSet<>(Arrays.asList(Arrays.copyOfRange(shareArray, 0, 1)))));
		assertArrayEquals(secret, secretSharingService.recover(new HashSet<>(Arrays.asList(Arrays.copyOfRange(shareArray, 1, 2)))));
	}

	@Test
	void testGoThereAndBackAgainWithThresholdOne() {
		// We generate a 2048 bit secret
		byte[] secret = new byte[256];
		Arrays.fill(secret, (byte) 1);

		byte[] clonedSecret = secret.clone();

		Set<Share> shares = secretSharingService.split(clonedSecret, 2, 1, new BigInteger(1, secret).nextProbablePrime());

		// Make sure the secret has been disposed
		assertArrayEquals(new byte[clonedSecret.length], clonedSecret);

		assertArrayEquals(secret, secretSharingService.recover(shares));
		Share[] shareArray = shares.toArray(new Share[0]);

		assertArrayEquals(secret, secretSharingService.recover(new HashSet<>(Arrays.asList(Arrays.copyOfRange(shareArray, 0, 1)))));
		assertArrayEquals(secret, secretSharingService.recover(new HashSet<>(Arrays.asList(Arrays.copyOfRange(shareArray, 1, 2)))));
	}

	@Test
	void testGoThereAndBackAgain() {
		// We generate a 2048 bit secret
		byte[] secret = new byte[256];
		Arrays.fill(secret, (byte) 1);

		byte[] clonedSecret = secret.clone();

		Set<Share> shares = secretSharingService.split(clonedSecret, 5, 3, new BigInteger(1, secret).nextProbablePrime());

		// Make sure the secret has been disposed
		assertArrayEquals(new byte[clonedSecret.length], clonedSecret);

		assertArrayEquals(secret, secretSharingService.recover(shares));
		Share[] shareArray = shares.toArray(new Share[0]);

		assertArrayEquals(secret, secretSharingService.recover(new HashSet<>(Arrays.asList(Arrays.copyOfRange(shareArray, 0, 3)))));
		assertArrayEquals(secret, secretSharingService.recover(new HashSet<>(Arrays.asList(Arrays.copyOfRange(shareArray, 1, 4)))));
		assertArrayEquals(secret, secretSharingService.recover(new HashSet<>(Arrays.asList(Arrays.copyOfRange(shareArray, 2, 5)))));
	}

	@Test
	void testMultipleGoThereAndBackAgain() {
		byte[][] secrets = new byte[][] { { 1, 1, 1 }, { 2, 2, 2 }, { 3, 3, 3 }, { 4, 4, 4 }, { 5, 5, 5 }, { 6, 6, 6 } };
		int numSecrets = secrets.length;

		// Make the modulus larger than the largest secret.
		BigInteger modulus = new BigInteger(1, secrets[secrets.length - 1]).nextProbablePrime();

		// Clone the secrets since they are going to be removed.
		byte[][] clonedSecrets = Stream.of(secrets).map(byte[]::clone).toArray(byte[][]::new);

		Set<Share> shares = secretSharingService.split(clonedSecrets, 5, 3, modulus);

		// Make sure the secrets has been disposed
		assertArrayEquals(new byte[clonedSecrets.length][clonedSecrets[0].length], clonedSecrets);

		// Test recover with all shares
		byte[][] recoveredSecrets = secretSharingService.recover(shares, numSecrets);
		assertArrayEquals(secrets, recoveredSecrets);

		// Test recover with subshares
		Share[] shareArray = shares.toArray(new Share[0]);

		assertArrayEquals(secrets, secretSharingService.recover(new HashSet<>(Arrays.asList(Arrays.copyOfRange(shareArray, 0, 3))), numSecrets));
		assertArrayEquals(secrets, secretSharingService.recover(new HashSet<>(Arrays.asList(Arrays.copyOfRange(shareArray, 1, 4))), numSecrets));
		assertArrayEquals(secrets, secretSharingService.recover(new HashSet<>(Arrays.asList(Arrays.copyOfRange(shareArray, 2, 5))), numSecrets));
	}

	@Test
	void testNegativeSecret() {
		byte[] secret = new byte[] { 0, -5 };
		byte[] secretClone = secret.clone();
		BigInteger modulus = new BigInteger(1, secret).nextProbablePrime();

		Set<Share> split = secretSharingService.split(secretClone, 2, 1, modulus);
		byte[] recoveredSecret = secretSharingService.recover(split);

		// Make sure the secret is destroyed
		assertArrayEquals(new byte[secret.length], secretClone);

		// Check the recovered secret.
		assertArrayEquals(secret, recoveredSecret);
	}

	@Test
	void testZeroSecret() {
		byte[] zeroSecret = new byte[2048];
		BigInteger modulus = new BigInteger(new byte[] { 2 });

		Set<Share> split = secretSharingService.split(zeroSecret, 2, 1, modulus);
		byte[] recoveredSecret = secretSharingService.recover(split);

		assertEquals(new BigInteger(zeroSecret), new BigInteger(recoveredSecret));
	}

	@Test
	void notEnoughShares() {

		// We generate a 2048 bit secret
		byte[] secret = new byte[256];
		Arrays.fill(secret, (byte) 1);

		byte[] clonedSecret = secret.clone();

		Set<Share> shares = secretSharingService.split(clonedSecret, 5, 3, new BigInteger(1, secret).nextProbablePrime());

		Share[] shareArray = shares.toArray(new Share[0]);

		assertThrows(IllegalArgumentException.class,
				() -> secretSharingService.recover(new HashSet<>(Arrays.asList(Arrays.copyOfRange(shareArray, 0, 1)))));
	}

	@Test
	void oversizedSecret() {
		byte[][] secrets = new byte[][] { { 45 }, { 46 }, { 47 }, { 48 }, { 49 }, { 52 } };
		BigInteger modulus = new BigInteger(new byte[] { 51 });

		// The method should complain about the last element.
		assertThrows(IllegalArgumentException.class, () -> secretSharingService.split(secrets, 3, 2, modulus));
	}

	@Test
	void slightlyOversizedSecret() {
		byte secretValue = 1;
		byte[][] secrets = new byte[][] { { secretValue } };
		BigInteger modulus = new BigInteger(new byte[] { secretValue });

		// The method should complain.
		assertThrows(IllegalArgumentException.class, () -> secretSharingService.split(secrets, 3, 2, modulus));
	}

	@Test
	void testDifferentLengthSecretsShouldThrow() {
		byte[][] secrets = new byte[][] { { 1, 1, 1 }, { 2, 2, 2 }, { 3, 3 }, { 4, 4, 4 }, { 5, 5, 5 }, { 6, 6, 6 } };

		// Make the modulus larger than the largest secret.
		BigInteger modulus = new BigInteger(1, secrets[secrets.length - 1]).nextProbablePrime();

		// Clone the secrets since they are going to be removed.
		byte[][] clonedSecrets = Stream.of(secrets).map(byte[]::clone).toArray(byte[][]::new);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> secretSharingService.split(clonedSecrets, 3, 2, modulus));
		assertEquals("Cannot create shares for secrets of different size.", exception.getMessage());
	}

	@Test
	void testNegativeRecoveredSecretException() {
		BigInteger secret = new BigInteger(new byte[] { (byte) -1 });
		Point point = new Point(BigInteger.ONE, secret);
		BigInteger modulus = new BigInteger(new byte[] { (byte) 1 });
		Share share = new ShamirShare(2, 1, modulus, 1, Collections.singletonList(point));

		Set<Share> shares = new HashSet<>();
		shares.add(share);

		assertThrows(IllegalArgumentException.class, () -> secretSharingService.recover(shares));
	}

	@Test
	void testTooBigRecoveredSecretException() {
		BigInteger secret = new BigInteger(1, new byte[] { (byte) 255, (byte) 255 });
		Point point = new Point(BigInteger.ONE, secret);
		BigInteger modulus = secret.nextProbablePrime();
		Share share = new ShamirShare(2, 1, modulus, 1, Collections.singletonList(point));

		Set<Share> shares = new HashSet<>();
		shares.add(share);

		assertThrows(IllegalArgumentException.class, () -> secretSharingService.recover(shares));
	}

	@Test
	void testCheckKOPrime() {
		Point point = new Point(BigInteger.ZERO, BigInteger.ZERO);
		ShamirShare share = new ShamirShare(0, 0, new BigInteger(new byte[] { 0 }), 0, Collections.singletonList(point));
		Set<Share> shares = new HashSet<>();
		shares.add(share);

		assertThrows(IllegalArgumentException.class, () -> secretSharingService.recover(shares));
	}

	@Test
	void testCheckKOThreshold() {
		Point point = new Point(new BigInteger(new byte[] { 1 }), new BigInteger(new byte[] { 1 }));
		ShamirShare share = new ShamirShare(0, 1, new BigInteger(new byte[] { 2 }), 0, Collections.singletonList(point));
		Set<Share> shares = new HashSet<>();
		shares.add(share);

		assertThrows(IllegalArgumentException.class, () -> secretSharingService.recover(shares));
	}

}
