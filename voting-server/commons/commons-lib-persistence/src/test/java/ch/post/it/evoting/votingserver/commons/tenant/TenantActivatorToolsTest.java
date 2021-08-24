/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.tenant;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.extendedkeystore.cryptoapi.CryptoAPIExtendedKeyStore;
import ch.post.it.evoting.cryptolib.extendedkeystore.service.ExtendedKeyStoreService;
import ch.post.it.evoting.domain.election.model.tenant.TenantActivationData;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.CryptoException;

public class TenantActivatorToolsTest {

	private static final TenantActivatorTools _target = new TenantActivatorTools();

	@Test
	public void testGetListTenantsFromPasswordFiles() throws IOException, URISyntaxException {

		String path = getClass().getClassLoader().getResource("tenantDirTwoValidTenants").toURI().getPath();
		System.out.println(path);

		List<TenantActivationData> listTenants = _target.getListTenantsFromPasswordFiles(path, "AU");

		String errorMsg = "Failed to find the expected number of tenants";
		assertEquals(errorMsg, 2, listTenants.size());
	}

	@Test
	public void testGetListTenantsFromEmptyDirectory() throws IOException, URISyntaxException {

		String path = getClass().getClassLoader().getResource("properties").toURI().getPath();
		System.out.println(path);

		List<TenantActivationData> listTenants = _target.getListTenantsFromPasswordFiles(path, "AU");

		String errorMsg = "Expected there to be zero tenants found";
		assertEquals(errorMsg, 0, listTenants.size());
	}

	@Test
	public void testGetPasswordFromFile() throws URISyntaxException, IOException {

		Path path = Paths.get(getClass().getClassLoader().getResource("tenantDirTwoValidTenants").toURI());
		String tenantID = "100";
		String serviceName = "AU";
		int expectedNumberOfCharacters = 26;

		char[] password = _target.getPasswordFromFile(path.toAbsolutePath().toString(), tenantID, serviceName);

		String errorMsg = "Obtained password does not contain the expected number of characters";
		assertEquals(errorMsg, expectedNumberOfCharacters, password.length);
	}

	@Test
	public void testGetPrivateKeyFromKeystore() throws CryptoException, GeneralCryptoLibException {

		String pathSystemTanantKeystore = "/keystore/100_AU.sks";
		String systemTenantKeystorePassword = "RBW6WD4IPTA73VT4KASLILIIFJ";

		CryptoAPIExtendedKeyStore tenantKeystore = new ExtendedKeyStoreService()
				.loadKeyStore(readKeystoreAsStream(pathSystemTanantKeystore), systemTenantKeystorePassword.toCharArray());

		PrivateKey privateKey = _target.getPrivateKeys(tenantKeystore, systemTenantKeystorePassword.toCharArray()).get("encryptionkey");

		String errorMsg = "Failed to extract private key from keystore";
		assertEquals(errorMsg, "RSA", privateKey.getAlgorithm());
	}

	@Test
	public void testLoadPropertiesFromFile() throws URISyntaxException, IOException {

		Path path = Paths.get(getClass().getClassLoader().getResource("properties/test.properties").toURI());

		Properties props = _target.loadPropertiesFromFile(path.toAbsolutePath().toString());

		assertEquals(3, props.keySet().size());
		assertEquals("value1", props.getProperty("key1"));
		assertEquals("value2", props.getProperty("key2"));
		assertEquals("value3", props.getProperty("key3"));
	}

	@Test
	public void testAttemptToDeletePasswordsFiles() throws URISyntaxException, IOException {

		String testDirectoryName = "testDir";
		Path testDirectoryAsPath = Paths.get(testDirectoryName);
		File testDirectoryAsFile = testDirectoryAsPath.toFile();

		Files.createDirectories(testDirectoryAsPath);

		File testFileAsFile = new File(
				getClass().getClassLoader().getResource("tenantDirTwoValidTenants/tenant_AU_100.properties").toURI().getPath());
		File destinationFileAsFile = Paths.get(testDirectoryAsPath.toAbsolutePath().toString(), "tenant_AU_100.properties").toFile();

		FileUtils.copyFile(testFileAsFile, destinationFileAsFile);

		Collection<File> filesBeforeClean = FileUtils.listFiles(testDirectoryAsFile, null, false);
		String errorMsg = "Expected that there would be 1 file";
		assertEquals(errorMsg, 1, filesBeforeClean.size());

		_target.attemptToDeletePasswordsFiles(testDirectoryAsPath.toAbsolutePath().toString(), "AU", "100");

		Collection<File> filesAfterClean = FileUtils.listFiles(testDirectoryAsFile, null, false);
		errorMsg = "Expected that there would be 0 files";
		assertEquals(errorMsg, 0, filesAfterClean.size());
	}

	@Test
	public void testGetCertificateChainFromKeystore() throws CryptoException, GeneralCryptoLibException, CertificateException {

		String pathSystemTanantKeystore = "/keystore/100_AU.sks";
		String systemTenantKeystorePassword = "RBW6WD4IPTA73VT4KASLILIIFJ";

		CryptoAPIExtendedKeyStore tenantKeystore = new ExtendedKeyStoreService()
				.loadKeyStore(readKeystoreAsStream(pathSystemTanantKeystore), systemTenantKeystorePassword.toCharArray());

		X509Certificate[] certificateChain = _target.getCertificateChains(tenantKeystore).get("encryptionkey");

		assertEquals(2, certificateChain.length);
	}

	private InputStream readKeystoreAsStream(final String path) {
		return getClass().getResourceAsStream(path);
	}
}
