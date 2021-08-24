/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.infrastructure.remote.filter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.Certificate;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.stores.keystore.configuration.KeystoreReader;
import ch.post.it.evoting.cryptolib.stores.keystore.configuration.KeystoreReaderFactory;
import ch.post.it.evoting.cryptolib.stores.keystore.configuration.NodeIdentifier;
import ch.post.it.evoting.votingserver.commons.infrastructure.exception.OvCommonsInfrastructureException;
import ch.post.it.evoting.votingserver.commons.keystore.TestKeystoreReader;

import mockit.Mock;
import mockit.MockUp;

public class SignedRequestKeyManagerTest {

	@BeforeClass
	public static void setUpAll() {
		// Make sure the SignedRequestKeyManager has not already been instantiated by another JUnit test.
		SignedRequestKeyManager.instance = null;
	}

	private static Path getPathOfFileInResources(final Path path) throws URISyntaxException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		URL resource = classLoader.getResource(path.toString());
		return Paths.get(resource.toURI());
	}

	@Test
	public void testSingletonInstance() throws OvCommonsInfrastructureException, IOException, URISyntaxException {

		copyTestFiles();

		new MockUp<KeystoreReaderFactory>() {
			@Mock
			public KeystoreReader getInstance() {
				return TestKeystoreReader.getInstance();
			}
		};

		SignedRequestKeyManager signedRequestKeyManager = SignedRequestKeyManager.getInstance();

		PublicKey sdmPublicKey = signedRequestKeyManager.getPublicKeyFromOriginator(NodeIdentifier.SECURE_DATA_MANAGER);
		Assert.assertNotNull(sdmPublicKey);

		PublicKey cprPublicKey = signedRequestKeyManager.getPublicKeyFromOriginator(NodeIdentifier.CONFIG_PLATFORM_ROOT);
		Assert.assertNotNull(cprPublicKey);

		PublicKey apPublicKey = signedRequestKeyManager.getPublicKeyFromOriginator(NodeIdentifier.ADMIN_PORTAL);
		Assert.assertNotNull(apPublicKey);

		PublicKey agPublicKey = signedRequestKeyManager.getPublicKeyFromOriginator(NodeIdentifier.API_GATEWAY);
		Assert.assertNotNull(agPublicKey);
	}

	@Test
	public void testUpdateKeyForOriginator() throws OvCommonsInfrastructureException {

		new MockUp<KeystoreReaderFactory>() {
			@Mock
			public KeystoreReader getInstance() {
				return TestKeystoreReader.getInstance();
			}
		};

		SignedRequestKeyManager signedRequestKeyManager = SignedRequestKeyManager.getInstance();

		PublicKey originalPublicKey = signedRequestKeyManager.getPublicKeyFromOriginator(NodeIdentifier.CONFIG_PLATFORM_ROOT);

		AsymmetricService asymmetricService = new AsymmetricService();
		KeyPair keyPair = asymmetricService.getKeyPairForSigning();
		PublicKey newKey = keyPair.getPublic();

		signedRequestKeyManager.setPublicKeyForOriginator(NodeIdentifier.CONFIG_PLATFORM_ROOT, newKey);

		PublicKey finalPublicKey = signedRequestKeyManager.getPublicKeyFromOriginator(NodeIdentifier.CONFIG_PLATFORM_ROOT);

		Assert.assertNotEquals(originalPublicKey, finalPublicKey);
	}

	@Test
	public void testGetPublicKeyFromCertificateString() throws GeneralCryptoLibException {
		String certificateContentStr = "-----BEGIN CERTIFICATE-----\n" + "MIIDjzCCAnegAwIBAgIVALxc4pAxqTzXp83vQTNQKwHaERTvMA0GCSqGSIb3DQEB\n"
				+ "CwUAMFwxFjAUBgNVBAMMDVRlbmFudCAxMDAgQ0ExFjAUBgNVBAsMDU9ubGluZSBW\n"
				+ "b3RpbmcxEjAQBgNVBAoMCVN3aXNzUG9zdDEJMAcGA1UEBwwAMQswCQYDVQQGEwJD\n"
				+ "SDAeFw0xNjA5MDcxMTE2MThaFw0xNjEwMTkyMjU5NTlaMIGDMT0wOwYDVQQDDDRB\n"
				+ "ZG1pbmlzdHJhdGlvbkJvYXJkIDNhM2M1MWNkZDE0NDRjOGJhYjA2NjQwZTM3ZTA0\n"
				+ "NzA4MRYwFAYDVQQLDA1PbmxpbmUgVm90aW5nMRIwEAYDVQQKDAlTd2lzc1Bvc3Qx\n"
				+ "CTAHBgNVBAcMADELMAkGA1UEBhMCQ0gwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAw\n"
				+ "ggEKAoIBAQCLjRvSUWwAkNAvyGwnksccYJ0XMSa/LmYbE2caVaUTJgkhfkt7uMi2\n"
				+ "e+LjCqEVRbfvcqcuH2SF9dsYrfgCdm/FHQadeciu66BV6vBntc3dCw1GJa4LJQcp\n"
				+ "tTRJBL1ca7FVHl7u3onfIez/o9Jy07P8P2iv+ol8Xvvx4PBa6BvvJkIlukQy+Ayt\n"
				+ "/zggF9QFSzJ+jywse5MLEYwh4oT53uETYHP3pVDGa5crxSOuCfZEk73tyJmgH0ML\n"
				+ "enrbu2oR4yNVfm6qIhijiJ9on05uLXSuYjLBAvhNwTrgYJBVtS8RKzOb5oMqNMKa\n"
				+ "mib7MVuY57bELJe9hsUp2cWNKrF5D3IxAgMBAAGjIDAeMA4GA1UdDwEB/wQEAwIG\n"
				+ "wDAMBgNVHRMBAf8EAjAAMA0GCSqGSIb3DQEBCwUAA4IBAQB9vJEPCixxoDPZThUo\n"
				+ "b6t41iILWBkdAExvQlVyZ/VfQ6/eXSmyviuZLcO13SDa3zp8g1DEe5H9XIfVgyqF\n"
				+ "C7aZ4pV88aI8GNqxnkdO4GmXOZM5mQZ0c9eGPKr0RJ6jUIL6iJmBSSj/gt4I9XoD\n"
				+ "lKBL3xivNMJxw5BVhyqCB5vM1xO6j5fzyfwc7EiJZF4axMly8yg/Ip7tBnZvtZM3\n"
				+ "cFWi0kCM755AFfAMVWPqA7pak4HcTT6c2En4ok+1RB4mVwjirfJJOSPxUsPwFvw4\n"
				+ "vC5KMoy46gcZO/bx6e4wjSNWz9NqwbWxLYfESo55ZNREtPb9rTnlb4JN7GMmvd9y\n" + "QvCO\n" + "-----END CERTIFICATE-----\n";

		Certificate certificateToValidate = PemUtils.certificateFromPem(certificateContentStr);

		CryptoAPIX509Certificate cryptoAPIX509Certificate = SignedRequestKeyManager.getCryptoX509Certificate(certificateToValidate);

		PublicKey publicKey = cryptoAPIX509Certificate.getPublicKey();

		Assert.assertNotNull(publicKey);

	}

	private void copyTestFiles() throws IOException, URISyntaxException {
		String sourceTestFilesFolderName = "SignedRequestKeyManagerTest";
		Path sourceTestFilesPath = getPathOfFileInResources(Paths.get(sourceTestFilesFolderName));
		Path tempDirectory = Files.createTempDirectory(sourceTestFilesFolderName);
		FileUtils.copyDirectory(sourceTestFilesPath.toFile(), tempDirectory.toFile());
	}

}
