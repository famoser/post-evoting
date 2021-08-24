/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.commons.destroy;

import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;

/**
 * A destroyer for the key bytes of a {@link RSAPrivateKey}. Since {@link RSAPrivateKey} contains {@link java.math.BigInteger} which are immutable, in
 * order to clean up any information stored inside, the user must call the {@link #destroy(RSAPrivateKey)} and {@link #destroy(RSAPrivateCrtKey)}
 * methods before dereferencing the keys.
 */
public class RSAPrivateKeyDestroyer {
	private final BigIntegerDestroyer bigIntegerDestroyer = new BigIntegerDestroyer();

	/**
	 * Destroy the content of the key.
	 *
	 * @param rsaPrivateCrtKey The key to destroy.
	 */
	public void destroy(final RSAPrivateCrtKey rsaPrivateCrtKey) {
		bigIntegerDestroyer
				.destroyInstances(rsaPrivateCrtKey.getCrtCoefficient(), rsaPrivateCrtKey.getModulus(), rsaPrivateCrtKey.getPrimeExponentP(),
						rsaPrivateCrtKey.getPrimeExponentQ(), rsaPrivateCrtKey.getPrimeP(), rsaPrivateCrtKey.getPrimeQ(),
						rsaPrivateCrtKey.getPrivateExponent());
	}

	/**
	 * Destroy the content of the key.
	 *
	 * @param rsaPrivateKey The key to destroy.
	 */
	public void destroy(final RSAPrivateKey rsaPrivateKey) {
		bigIntegerDestroyer.destroyInstances(rsaPrivateKey.getModulus(), rsaPrivateKey.getPrivateExponent());
	}
}
