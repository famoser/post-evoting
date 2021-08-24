/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service.utils;

import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.activity.GroupElementsCompressor;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

/**
 * Combines ElGamal public keys, using compression when necessary.
 */
public final class ElGamalPublicKeyCombinerWithCompression {

	/**
	 * Combines the primary ElGamal publickey with all of the keys in the received list of ElGamal
	 * public keys.
	 * <p>
	 * The generated ElGamal publickey will have the same number of subkeys as the primary ElGamal
	 * publickey. If the list contains any ElGamal publickeys with more subkeys than the primary key,
	 * then the surplus subkeys will be compressed into the last subkey before combining, thus
	 * ensuring that all of the subkeys contribute to the output of the operation.
	 * <p>
	 * Each of the ElGamal keys in the list should have at least as many subkeys as the primary
	 * ElGamal key.
	 *
	 * @param primaryElGamalPublicKey the primary ElGamal publickey.
	 * @param keysToBeCombined        a list of ElGamal publickeys to be combined.
	 * @return the combined ElGamal publickey.
	 * @throws GeneralCryptoLibException if any cryptography exception happens while combining the
	 *                                   keys.
	 * @throw IllegalArgumentException if any of the inputs are null, if all the keys don't belong to
	 * the same group, or if any of the keys in the received list don't contain at least as
	 * many subkeys as the primary ElGamal key.
	 */
	public ElGamalPublicKey combine(ElGamalPublicKey primaryElGamalPublicKey, List<ElGamalPublicKey> keysToBeCombined)
			throws GeneralCryptoLibException {

		validateInputs(primaryElGamalPublicKey, keysToBeCombined);

		int numRequiredElements = primaryElGamalPublicKey.getKeys().size();

		ElGamalPublicKey combinedKey = primaryElGamalPublicKey;

		GroupElementsCompressor<ZpGroupElement> compressor = new GroupElementsCompressor<>();

		for (ElGamalPublicKey key : keysToBeCombined) {

			List<ZpGroupElement> subkeys = key.getKeys();

			if (subkeys.size() > numRequiredElements) {

				List<ZpGroupElement> compressedList = compressor.buildListWithCompressedFinalElement(numRequiredElements, subkeys);

				key = new ElGamalPublicKey(compressedList, key.getGroup());
			}

			combinedKey = combinedKey.multiply(key);
		}

		return combinedKey;
	}

	private void validateInputs(ElGamalPublicKey primaryPublicKey, List<ElGamalPublicKey> keysToBeCombined) {

		if (primaryPublicKey == null) {
			throw new IllegalArgumentException("The primary public key was null");
		} else if (keysToBeCombined == null) {
			throw new IllegalArgumentException("The list of public keys to be combined was null");
		}

		ZpSubgroup group = primaryPublicKey.getGroup();

		int numSubkeysInPrimaryKey = primaryPublicKey.getKeys().size();

		for (ElGamalPublicKey key : keysToBeCombined) {

			if (!group.equals(key.getGroup())) {
				throw new IllegalArgumentException("The keys don't belong to the same group");
			}

			int numSubKeysInThisKey = key.getKeys().size();
			if (numSubkeysInPrimaryKey > numSubKeysInThisKey) {
				String errorMsg = String
						.format("The primary key has more elements than one of the additional keys. Primary: %s, other: %s", numSubkeysInPrimaryKey,
								numSubKeysInThisKey);
				throw new IllegalArgumentException(errorMsg);
			}
		}
	}
}
