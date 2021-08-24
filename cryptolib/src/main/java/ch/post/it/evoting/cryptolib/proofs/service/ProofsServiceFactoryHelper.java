/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.service;

import ch.post.it.evoting.cryptolib.api.proofs.ProofsServiceAPI;
import ch.post.it.evoting.cryptolib.api.services.ServiceFactory;
import ch.post.it.evoting.cryptolib.commons.concurrent.ServiceFactoryHelper;

/**
 * This class sets up default factories of {@link ProofsServiceAPI} objects.
 */
public final class ProofsServiceFactoryHelper {

	/**
	 * This is a helper class. It cannot be instantiated.
	 */
	private ProofsServiceFactoryHelper() {
		super();
	}

	/**
	 * Retrieves a new factory of non thread-safe services.
	 *
	 * <p>Default factory is {@link BasicProofsServiceFactory}.
	 *
	 * @param params List of parameters used in the creation of the default factory.
	 * @return the new factory.
	 */
	@SuppressWarnings("unchecked")
	public static ServiceFactory<ProofsServiceAPI> getInstance(final Object... params) {
		return ServiceFactoryHelper.get(BasicProofsServiceFactory.class, params);
	}

	/**
	 * Retrieves a new factory of thread-safe services.
	 *
	 * <p>Default factory is {link {@link PollingProofsServiceFactory}
	 *
	 * @param params a list of parameters used in the creation of the default factory.
	 * @return the new factory of thread-safe services.
	 */
	@SuppressWarnings("unchecked")
	public static ServiceFactory<ProofsServiceAPI> getFactoryOfThreadSafeServices(final Object... params) {
		return ServiceFactoryHelper.get(PollingProofsServiceFactory.class, params);
	}
}
