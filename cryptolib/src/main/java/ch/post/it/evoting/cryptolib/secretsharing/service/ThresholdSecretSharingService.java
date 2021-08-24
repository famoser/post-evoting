/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.secretsharing.service;

import java.math.BigInteger;
import java.util.Set;

import ch.post.it.evoting.cryptolib.CryptolibService;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.secretsharing.Share;
import ch.post.it.evoting.cryptolib.api.secretsharing.ShareSerializer;
import ch.post.it.evoting.cryptolib.api.secretsharing.ThresholdSecretSharingScheme;
import ch.post.it.evoting.cryptolib.api.secretsharing.ThresholdSecretSharingSchemeAPI;
import ch.post.it.evoting.cryptolib.api.secretsharing.ThresholdSecretSharingServiceAPI;
import ch.post.it.evoting.cryptolib.secretsharing.configuration.ThresholdSecretSharingPolicy;
import ch.post.it.evoting.cryptolib.secretsharing.configuration.ThresholdSecretSharingPolicyFactory;
import ch.post.it.evoting.cryptolib.secretsharing.shamir.ShamirSecretSharingScheme;
import ch.post.it.evoting.cryptolib.secretsharing.shamir.ShamirShareSerializer;

/**
 * A service that exposes secret sharing operations.
 */
public final class ThresholdSecretSharingService extends CryptolibService implements ThresholdSecretSharingServiceAPI {

	private final ThresholdSecretSharingSchemeAPI thresholdSecretSharingScheme;

	private final ShareSerializer shareSerializer;

	/**
	 * Creates and instance of service and initializes all properties according to the properties specified by {@link
	 * ch.post.it.evoting.cryptolib.commons.configuration.PolicyFromPropertiesHelper#CRYPTOLIB_POLICY_PROPERTIES_FILE_PATH}.
	 */
	public ThresholdSecretSharingService() {
		ThresholdSecretSharingPolicy policy = ThresholdSecretSharingPolicyFactory.fromDefaults();

		thresholdSecretSharingScheme = getThresholdSecretSharingScheme(policy);

		shareSerializer = createSerializer(policy);
	}

	private static ShareSerializer createSerializer(ThresholdSecretSharingPolicy policy) {
		if (policy.getThresholdSecretSharingScheme().equals(ThresholdSecretSharingScheme.SHAMIR)) {
			return new ShamirShareSerializer();
		}
		throw new IllegalStateException("Threshold Secret Sharing scheme is not correctly defined");
	}

	private static ThresholdSecretSharingSchemeAPI getThresholdSecretSharingScheme(ThresholdSecretSharingPolicy policy) {
		if (policy.getThresholdSecretSharingScheme().equals(ThresholdSecretSharingScheme.SHAMIR)) {
			return new ShamirSecretSharingScheme();
		}
		throw new IllegalStateException("Threshold Secret Sharing scheme is not correctly defined");
	}

	@Override
	public Set<Share> split(byte[] secret, int number, int threshold, BigInteger modulus) {
		return split(new byte[][] { secret }, number, threshold, modulus);
	}

	@Override
	public Set<Share> split(byte[][] secrets, int number, int threshold, BigInteger modulus) {
		return thresholdSecretSharingScheme.split(secrets, number, threshold, modulus);
	}

	@Override
	public byte[] recover(Set<Share> shares) {
		return recover(shares, 1)[0];
	}

	@Override
	public byte[][] recover(Set<Share> shares, int expectedSecrets) {
		return thresholdSecretSharingScheme.recover(shares, expectedSecrets);
	}

	@Override
	public byte[] serialize(Share share) {
		return shareSerializer.toByteArray(share);
	}

	@Override
	public Share deserialize(byte[] shareBytes) throws GeneralCryptoLibException {
		return shareSerializer.fromByteArray(shareBytes);
	}
}
