/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.service.crypto;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.proofs.ProofsServiceAPI;
import ch.post.it.evoting.cryptolib.proofs.service.ProofsServiceFactoryHelper;

/**
 * This is a producer for proofs service in order to have a thread safe one.
 */
public class ProofsServiceProducer {

	private static final String MAX_ELEMENTS_CRYPTO_POOL = "PROOFS_MAX_ELEMENTS_CRYPTO_POOL";

	/**
	 * Returns a instance of the service.
	 *
	 * @return an instance of proofs service.
	 */
	@Produces
	@ApplicationScoped
	public ProofsServiceAPI getInstance() {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxTotal(Integer.parseInt(System.getenv(MAX_ELEMENTS_CRYPTO_POOL)));
		try {
			return ProofsServiceFactoryHelper.getFactoryOfThreadSafeServices(config).create();
		} catch (GeneralCryptoLibException e) {
			throw new IllegalStateException("Exception while trying to create ProofsService", e);
		}
	}
}
