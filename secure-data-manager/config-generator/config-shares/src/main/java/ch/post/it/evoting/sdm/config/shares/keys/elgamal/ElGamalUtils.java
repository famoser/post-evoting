/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.shares.keys.elgamal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.sdm.config.constants.Constants;

/**
 * This util class is intended to be a bridge between cryptolib and shares functionalities.
 */
public final class ElGamalUtils {

	public byte[] serialize(final ElGamalPrivateKey privateKey) {

		if (privateKey == null) {
			throw new IllegalArgumentException("The given private key should be initialized");
		}

		List<Exponent> keys = privateKey.getKeys();

		byte[] concatenated = new byte[Constants.MAX_EXPONENT_SIZE * keys.size()];

		for (int i = 0; i < keys.size(); i++) {

			BigInteger expValue = keys.get(i).getValue();
			byte[] b = expValue.toByteArray();
			if (b.length > Constants.MAX_EXPONENT_SIZE) {
				throw new IllegalArgumentException("The BigInteger " + expValue + " doesn't fit in " + Constants.MAX_EXPONENT_SIZE + "bytes");
			}
			System.arraycopy(b, 0, concatenated, Constants.MAX_EXPONENT_SIZE * (i + 1) - b.length, b.length);
		}

		return concatenated;
	}

	public ElGamalPrivateKey reconstruct(final ElGamalPublicKey elGamalPublicKey, final byte[] privateKeys) throws GeneralCryptoLibException {

		if (elGamalPublicKey == null) {
			throw new IllegalArgumentException("The given public key should be initialized");
		}

		if (privateKeys == null) {
			throw new IllegalArgumentException("The given byte array should be initialized");
		}

		byte[] pKeys = privateKeys.clone();
		ZpSubgroup group = elGamalPublicKey.getGroup();

		List<Exponent> exponents = new ArrayList<Exponent>();

		int remainedBytesInFirstKey = pKeys.length % Constants.MAX_EXPONENT_SIZE;
		int numOfCompletedKeys = pKeys.length / Constants.MAX_EXPONENT_SIZE;

		byte[] dest;

		if (remainedBytesInFirstKey != 0) {
			dest = new byte[remainedBytesInFirstKey];
			System.arraycopy(pKeys, 0, dest, 0, remainedBytesInFirstKey);
			exponents.add(createExponent(dest, group));
		}

		int initOffset = remainedBytesInFirstKey;
		int init;
		for (int i = 0; i < numOfCompletedKeys; i++) {
			dest = new byte[Constants.MAX_EXPONENT_SIZE];
			init = (i * Constants.MAX_EXPONENT_SIZE) + initOffset;
			System.arraycopy(pKeys, init, dest, 0, Constants.MAX_EXPONENT_SIZE);
			exponents.add(createExponent(dest, group));
		}

		return new ElGamalPrivateKey(exponents, group);
	}

	private Exponent createExponent(final byte[] dest, final ZpSubgroup group) throws GeneralCryptoLibException {

		BigInteger value = new BigInteger(1, dest);

		return new Exponent(group.getQ(), value);
	}
}
