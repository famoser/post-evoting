/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.securelogger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.post.it.evoting.controlcomponents.commons.keymanagement.KeysManager;

@ExtendWith(MockitoExtension.class)
class SecureLogAppenderTest {

	public static final String SRC_TEST_RESOURCES_TEST_SIG_KEYSTORE_P_12 = "src/test/resources/test_sig_keystore.p12";
	public static final String SRC_TEST_RESOURCES_TEST_ENC_KEYSTORE_P_12 = "src/test/resources/test_enc_keystore.p12";
	public static final char[] TEST_KS_PWD = "this_is_a_test_key".toCharArray();
	private static final Logger LOGGER = LogManager.getLogger("SecureLog");
	private static final AtomicInteger counter = new AtomicInteger();
	private static PrivateKey signingPrivateKey;
	private static PublicKey encryptionPublicKey;

	@Mock
	KeysManager mockKeysManager;

	@BeforeAll
	static void setup() throws IOException, GeneralSecurityException {
		Security.addProvider(new BouncyCastleProvider());

		KeyStore signatureKeyStore = KeyStore.getInstance("PKCS12");
		signatureKeyStore.load(new FileInputStream(SRC_TEST_RESOURCES_TEST_SIG_KEYSTORE_P_12), TEST_KS_PWD);
		signingPrivateKey = (PrivateKey) signatureKeyStore.getKey("testSignatureKeyPair", TEST_KS_PWD);

		KeyStore encryptionKeyStore = KeyStore.getInstance("PKCS12");
		encryptionKeyStore.load(new FileInputStream(SRC_TEST_RESOURCES_TEST_ENC_KEYSTORE_P_12), TEST_KS_PWD);
		encryptionPublicKey = encryptionKeyStore.getCertificate("testEncryptKeyPair").getPublicKey();
	}

	@AfterAll
	static void tearDown() {
		Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
	}

	@Test
	void testConcurrentLoggingLogsTheRightNumberOfLines() throws InterruptedException {

		Mockito.when(mockKeysManager.nodeLogSigningPrivateKey()).thenReturn(signingPrivateKey);
		Mockito.when(mockKeysManager.nodeLogEncryptionPublicKey()).thenReturn(encryptionPublicKey);

		final SecureLogAppender secureLogAppender = SecureLogAppender.getAppender();
		secureLogAppender.setKeysManager(mockKeysManager);
		secureLogAppender.logInitialCheckpoint();

		int numberOfThreads = 2;
		int numberOfIterations = 10;
		ExecutorService service = Executors.newFixedThreadPool(10);
		CountDownLatch latch = new CountDownLatch(numberOfThreads);
		for (int i = 0; i < numberOfThreads; i++) {
			service.submit(() -> {
				for (int j = 0; j < numberOfIterations; j++) {
					final int current = counter.getAndIncrement();
					LOGGER.error(String.format("increment order is [%03d]", current));
				}
				latch.countDown();
			});
		}

		latch.await();
		assertEquals(numberOfThreads * numberOfIterations, counter.get());
	}
}