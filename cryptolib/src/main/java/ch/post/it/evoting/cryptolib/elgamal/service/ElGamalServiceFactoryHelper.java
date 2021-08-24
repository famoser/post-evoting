/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.service;

import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.services.ServiceFactory;
import ch.post.it.evoting.cryptolib.commons.concurrent.ServiceFactoryHelper;

/**
 * This class sets up default factories of {@link ElGamalServiceAPI} objects.
 */
public final class ElGamalServiceFactoryHelper {
	/**
	 * This is a helper class. It cannot be instantiated.
	 */
	private ElGamalServiceFactoryHelper() {
		super();
	}

	/**
	 * Retrieves a new factory of non thread-safe services.
	 *
	 * <p>Default factory is {@link BasicElGamalServiceFactory}.
	 *
	 * @param params parameters used in the creation of the default factory.
	 * @return the new factory.
	 */
	@SuppressWarnings("unchecked")
	public static ServiceFactory<ElGamalServiceAPI> getInstance(final Object... params) {
		return ServiceFactoryHelper.get(BasicElGamalServiceFactory.class, params);
	}

	/**
	 * Retrieves a new factory of thread-safe services.
	 *
	 * <p>Default factory is {link {@link PollingElGamalServiceFactory}
	 *
	 * @param params parameters used in the creation of the default factory.
	 * @return the new factory of thread-safe services.
	 */
	@SuppressWarnings("unchecked")
	public static ServiceFactory<ElGamalServiceAPI> getFactoryOfThreadSafeServices(final Object... params) {
		return ServiceFactoryHelper.get(PollingElGamalServiceFactory.class, params);
	}
}
