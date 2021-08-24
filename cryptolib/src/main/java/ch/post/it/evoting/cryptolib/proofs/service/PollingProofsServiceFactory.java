/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.service;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import ch.post.it.evoting.cryptolib.api.proofs.ProofsServiceAPI;
import ch.post.it.evoting.cryptolib.commons.concurrent.PooledProxiedServiceFactory;

/**
 * This Service factory creates thread-safe services. Thread-safe services proxy all requests to a pool of non thread-safe service instances.
 */
public class PollingProofsServiceFactory extends PooledProxiedServiceFactory<ProofsServiceAPI> {
	/**
	 * Constructor that uses default values.
	 */
	public PollingProofsServiceFactory() {
		super(new BasicProofsServiceFactory(), new GenericObjectPoolConfig());
	}

	/**
	 * Constructor that uses the default path to the cryptographic properties and the provided configuration of the pool to setup the pool of
	 * services.
	 *
	 * @param poolConfig the configuration of the pool.
	 */
	public PollingProofsServiceFactory(final GenericObjectPoolConfig poolConfig) {
		super(new BasicProofsServiceFactory(), poolConfig);
	}
}
