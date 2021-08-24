/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.symmetric.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.crypto.SecretKey;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.symmetric.SymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.symmetric.utils.SymmetricTestDataGenerator;

/**
 * Multithreaded tests of {@link SymmetricService}.
 */
class MultithreadSymmetricServiceIT {
	private static final byte[] DATA = "dataToEncrypt".getBytes(StandardCharsets.UTF_8);

	private static SecretKey secretKey;

	@BeforeAll
	public static void setup() throws GeneralCryptoLibException {
		secretKey = SymmetricTestDataGenerator.getSecretKeyForEncryption();
	}

	private static void assertServiceIsThreadSafe(SymmetricServiceAPI service) {
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

	private static Void invokeService(SymmetricServiceAPI service, int count) throws GeneralCryptoLibException {
		for (int i = 0; i < count; i++) {
			byte[] encrypted = service.encrypt(secretKey, DATA);
			byte[] decrypted = service.decrypt(secretKey, encrypted);
			if (!Arrays.equals(DATA, decrypted)) {
				throw new GeneralCryptoLibException("Data is corrupted.");
			}
		}
		return null;
	}

	@Test
	void okWhenThreadSafeServicesUsedPoolOf1Test() {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxTotal(1);
		SymmetricServiceAPI service = new PollingSymmetricServiceFactory(config).create();
		assertServiceIsThreadSafe(service);
	}

	@Test
	void usingHelperTest() throws GeneralCryptoLibException {
		SymmetricServiceAPI service = SymmetricServiceFactoryHelper.getFactoryOfThreadSafeServices().create();
		assertServiceIsThreadSafe(service);
	}

	@Test
	void usingHelperWithParamsTest() throws GeneralCryptoLibException {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxTotal(1);
		SymmetricServiceAPI service = SymmetricServiceFactoryHelper.getFactoryOfThreadSafeServices(config).create();
		assertServiceIsThreadSafe(service);
	}
}
