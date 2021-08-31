/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.extendedkeystore.service;

import ch.post.it.evoting.cryptolib.api.extendedkeystore.KeyStoreService;
import ch.post.it.evoting.cryptolib.api.services.ServiceFactory;
import ch.post.it.evoting.cryptolib.commons.concurrent.ServiceFactoryHelper;

/**
 * This class sets up default factories of {@link KeyStoreService} objects.
 */
public final class ExtendedKeyStoreServiceFactoryHelper {

	/**
	 * This is a helper class. It cannot be instantiated.
	 */
	private ExtendedKeyStoreServiceFactoryHelper() {
		super();
	}

	/**
	 * Retrieves a new factory of non thread-safe services.
	 *
	 * <p>Default factory is {@link BasicExtendedKeyStoreServiceFactory}.
	 *
	 * @param params a list of parameters used in the creation of the default factory.
	 * @return the new factory.
	 */
	@SuppressWarnings("unchecked")
	public static ServiceFactory<KeyStoreService> getInstance(final Object... params) {
		return ServiceFactoryHelper.get(BasicExtendedKeyStoreServiceFactory.class, params);
	}

	/**
	 * Retrieves a new factory of thread-safe services.
	 *
	 * <p>Default factory is {link {@link PollingExtendedKeyStoreServiceFactory}.
	 *
	 * @param params a list of parameters used in the creation of the default factory.
	 * @return the new factory of thread-safe services.
	 */
	@SuppressWarnings("unchecked")
	public static ServiceFactory<KeyStoreService> getFactoryOfThreadSafeServices(final Object... params) {
		return ServiceFactoryHelper.get(PollingExtendedKeyStoreServiceFactory.class, params);
	}
}
