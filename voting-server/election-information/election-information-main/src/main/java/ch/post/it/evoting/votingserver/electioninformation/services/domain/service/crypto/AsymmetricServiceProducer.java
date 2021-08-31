/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.crypto;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricServiceFactoryHelper;

/**
 * Producer for asymmetric service.
 */
public class AsymmetricServiceProducer {

	private static final String MAX_ELEMENTS_CRYPTO_POOL = "ASYMMETRIC_MAX_ELEMENTS_CRYPTO_POOL";

	/**
	 * Returns a instance of the service.
	 *
	 * @return an instance of asymmetric service.
	 */
	@Produces
	@ApplicationScoped
	public AsymmetricServiceAPI getInstance() {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxTotal(Integer.parseInt(System.getenv(MAX_ELEMENTS_CRYPTO_POOL)));
		try {
			return AsymmetricServiceFactoryHelper.getFactoryOfThreadSafeServices(config).create();
		} catch (GeneralCryptoLibException e) {
			throw new IllegalStateException("Exception while trying to create AsymmetricService", e);
		}
	}
}
