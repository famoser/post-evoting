/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.certificates.service;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.Executors.newFixedThreadPool;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.certificates.CertificatesServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateData;
import ch.post.it.evoting.cryptolib.certificates.bean.ValidityDates;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;

class MultithreadCertificatesServiceIT {
	private static final int TASK_COUNT = 1000;

	private static PrivateKey privateKey;

	private static PublicKey publicKey;

	@BeforeAll
	static void setUp() throws GeneralSecurityException, IOException {
		Security.addProvider(new BouncyCastleProvider());

		privateKey = getPrivateKey();
		publicKey = getPublicKey();
	}

	@AfterAll
	static void tearDown() {
		Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
	}

	private static void checkResults(Collection<Future<Void>> futures) {
		for (Future<Void> future : futures) {
			try {
				future.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new AssertionError("Test has been interrupted.", e);
			} catch (ExecutionException e) {
				throw new AssertionError("Test has failed.", e.getCause());
			}
		}
	}

	private static PrivateKey getPrivateKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		StringBuilder pem = new StringBuilder();
		try (InputStream stream = MultithreadCertificatesServiceIT.class.getResourceAsStream("/privateKey.pem");
				Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			char[] buffer = new char[2048];
			int count;
			while ((count = reader.read(buffer)) != -1) {
				pem.append(buffer, 0, count);
			}
		}
		String base64 = pem.toString().replaceAll("(-+BEGIN RSA PRIVATE KEY-+\\r?\\n|-+END RSA PRIVATE KEY-+\\r?\\n?)", "");
		byte[] bytes = Base64.getMimeDecoder().decode(base64);
		KeySpec spec = new PKCS8EncodedKeySpec(bytes);
		KeyFactory factory = KeyFactory.getInstance("RSA");
		return factory.generatePrivate(spec); // Requires BC provider.
	}

	private static PublicKey getPublicKey() throws CertificateException, IOException {
		X509Certificate certificate;
		CertificateFactory factory = CertificateFactory.getInstance("X.509");
		try (InputStream stream = MultithreadCertificatesServiceIT.class.getResourceAsStream("/cert.pem")) {
			certificate = (X509Certificate) factory.generateCertificate(stream);
		}
		return certificate.getPublicKey();
	}

	private static CertificateData newCertificateData(int index) throws GeneralCryptoLibException {
		CertificateData data = new CertificateData();
		data.setSubjectPublicKey(publicKey);
		data.setIssuerDn(newX509DistinguishedName("issuer" + index));
		data.setSubjectDn(newX509DistinguishedName("subject" + index));
		data.setValidityDates(newValidityDates());
		return data;
	}

	private static ExecutorService newExecutorService() {
		int threadCount = Runtime.getRuntime().availableProcessors();
		return newFixedThreadPool(threadCount);
	}

	private static Callable<Void> newTask(CertificatesServiceAPI service, int index) throws GeneralCryptoLibException {
		CertificateData data = newCertificateData(index);
		return () -> {
			try {
				service.createEncryptionX509Certificate(data, privateKey);
			} catch (GeneralCryptoLibException e) {
				throw e;
			} catch (Throwable e) {
				throw new GeneralCryptoLibException("Failed to call the service.", e);
			}
			return null;
		};
	}

	private static Collection<Callable<Void>> newTasks(CertificatesServiceAPI service) throws GeneralCryptoLibException {
		Collection<Callable<Void>> tasks = new ArrayList<>(TASK_COUNT);
		for (int i = 0; i < TASK_COUNT; i++) {
			tasks.add(newTask(service, i));
		}
		return tasks;
	}

	private static ValidityDates newValidityDates() throws GeneralCryptoLibException {
		return new ValidityDates(new Date(), new Date(currentTimeMillis() + 100000));
	}

	private static X509DistinguishedName newX509DistinguishedName(String commonName) throws GeneralCryptoLibException {
		return new X509DistinguishedName.Builder(commonName, "ES").build();
	}

	private static Collection<Future<Void>> runTasks(Collection<Callable<Void>> tasks) {
		Collection<Future<Void>> futures;
		ExecutorService executor = newExecutorService();
		try {
			futures = executor.invokeAll(tasks);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Test has been interrupted.", e);
		} finally {
			executor.shutdown();
		}
		return futures;
	}

	private static void test(CertificatesServiceAPI service) throws GeneralCryptoLibException {
		checkResults(runTasks(newTasks(service)));
	}

	@Test
	void okWhenThreadSafeServicesUsedDefaultPoolTest() throws GeneralCryptoLibException {
		test(new PollingCertificatesServiceFactory().create());
	}

	@Test
	void okWhenThreadSafeServicesUsedPoolOf1Test() throws GeneralCryptoLibException {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxTotal(1);
		test(new PollingCertificatesServiceFactory(config).create());
	}
}
