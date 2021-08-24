/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.shares.keys.elgamal;

import java.util.List;

import ch.post.it.evoting.cryptolib.commons.destroy.BigIntegerDestroyer;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;

public final class ElGamalPrivateKeyDestroyer {

	private final BigIntegerDestroyer bigIntegerDestroyer;

	public ElGamalPrivateKeyDestroyer() {
		bigIntegerDestroyer = new BigIntegerDestroyer();
	}

	public void destroy(final ElGamalPrivateKey keyAsElGamalPrivateKey) {

		List<Exponent> keys = keyAsElGamalPrivateKey.getKeys();
		for (Exponent exponent : keys) {
			bigIntegerDestroyer.destroyInstances(exponent.getValue());
		}
	}
}
