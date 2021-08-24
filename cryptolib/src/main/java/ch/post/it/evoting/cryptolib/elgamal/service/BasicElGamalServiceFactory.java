/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.service;

import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.services.ServiceFactory;
import ch.post.it.evoting.cryptolib.commons.concurrent.BaseFactory;

/**
 * Factory of {@link ElGamalService} objects which are non thread-safe.
 */
public class BasicElGamalServiceFactory extends BaseFactory<ElGamalServiceAPI> implements ServiceFactory<ElGamalServiceAPI> {
	/**
	 * Creates an instance of factory that will create services configured with default values.
	 */
	public BasicElGamalServiceFactory() {
		super(ElGamalServiceAPI.class);
	}

	@Override
	public ElGamalServiceAPI create() {
		return new ElGamalService();
	}
}
