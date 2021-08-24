/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.extendedkeystore.service;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore.PasswordProtection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.extendedkeystore.KeyStoreService;
import ch.post.it.evoting.cryptolib.extendedkeystore.cryptoapi.CryptoAPIExtendedKeyStore;

/**
 * Multithreaded tests of {@link ExtendedKeyStoreService}.
 */
class MultithreadExtendedKeyStoreServiceIT {

	private static final char[] PASSWORD = "01234567890abcdefghijk".toCharArray();
	private static final String ALIAS = "myaliassymmetric-2_";

	private static void assertServiceIsThreadSafe(KeyStoreService service) {
		int size = Runtime.getRuntime().availableProcessors();
		Collection<Callable<Void>> tasks = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			tasks.add(() -> invokeService(service, size * 2));
		}

		Collection<Future<Void>> futures;
		ExecutorService executor = Executors.newFixedThreadPool(size);
		try {
			futures = executor.invokeAll(tasks);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError(e);
		} finally {
			executor.shutdown();
		}

		try {
			for (Future<Void> future : futures) {
				future.get();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError(e);
		} catch (ExecutionException e) {
			throw new AssertionError(e.getCause());
		}
	}

	private static Void invokeService(KeyStoreService service, int count) throws GeneralCryptoLibException, IOException {
		for (int i = 0; i < count; i++) {
			CryptoAPIExtendedKeyStore store;
			try (InputStream stream = MultithreadExtendedKeyStoreServiceIT.class.getResourceAsStream("/keystoreSymmetric.sks")) {
				store = service.loadKeyStore(stream, new PasswordProtection(PASSWORD));
			}
			for (int j = 0; j < 10; j++) {
				store.getSecretKeyEntry(ALIAS, PASSWORD);
			}
		}
		return null;
	}

	@Test
	void okWhenThreadSafeServicesTest() {
		KeyStoreService service = new ExtendedKeyStoreService();
		assertServiceIsThreadSafe(service);
	}

	@Test
	void okWhenThreadSafeServicesUsedPoolTest() {
		KeyStoreService service = new PollingExtendedKeyStoreServiceFactory().create();
		assertServiceIsThreadSafe(service);
	}

	@Test
	void okWhenThreadSafeServicesUsedPoolOf1Test() {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxTotal(1);
		KeyStoreService service = new PollingExtendedKeyStoreServiceFactory(config).create();
		assertServiceIsThreadSafe(service);
	}
}
