/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

package ch.post.it.evoting.cryptolib.cmssigner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.cms.CMSException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.stores.bean.KeyStoreType;
import ch.post.it.evoting.cryptolib.stores.service.StoresService;

/**
 * Test for FileSigner
 */
class CMSSignerTest {

	private static final String P12_FILENAME = "src/test/resources/ca.p12";
	private static final String P12_PWD = "R3p37vk0";
	private static final String DATA_CONTENT = "Random data to write into the data file.";

	@TempDir
	Path tempDir;

	private File data;
	private File signature;
	private PrivateKey privateKey;
	private Certificate certificate;
	private File envelope;

	@BeforeEach
	public void setUp() throws GeneralSecurityException, IOException, GeneralCryptoLibException {

		// Load keyStore
		StoresService storesService = new StoresService();
		KeyStore keyStore = storesService.loadKeyStore(KeyStoreType.PKCS12, new FileInputStream(P12_FILENAME), P12_PWD.toCharArray());

		// Prepare private key, certificate chain and signer certificate
		Enumeration<String> keyAliases = keyStore.aliases();
		String keyAlias = keyAliases.nextElement(); // NoSuchElement exception is thrown if there is no alias in the KeyStore
		if (keyAliases.hasMoreElements()) {
			throw new IllegalArgumentException("There should be exactly one private key in the keystore");
		}
		privateKey = (PrivateKey) keyStore.getKey(keyAlias, P12_PWD.toCharArray());
		certificate = keyStore.getCertificate(keyAlias);

		// Prepare imput/output files
		data = tempDir.resolve("data").toFile();
		FileUtils.writeStringToFile(data, DATA_CONTENT, StandardCharsets.UTF_8);
		signature = tempDir.resolve("signature").toFile();
		envelope = tempDir.resolve("envelope").toFile();
	}

	@Test
	void testSignSelf() throws IOException, CMSException {
		CMSSigner.sign(data, signature, certificate, privateKey);
		try (InputStream is = new FileInputStream(data)) {
			assertEquals(CMSSigner.verify(FileUtils.readFileToByteArray(signature), is)[0][0], certificate);
		}
	}

	@Test
	void testSignSelfFiles() throws IOException, CMSException {
		CMSSigner.sign(data, signature, certificate, privateKey);

		try (InputStream is = new FileInputStream(data)) {
			assertEquals(CMSSigner.verify(FileUtils.readFileToByteArray(signature), is)[0][0], certificate);
		}
	}

	@Test
	void testEnvelopSelf() throws IOException, CMSException {
		CMSEnvelope cmsEnvelope = new CMSEnvelope();
		try (InputStream is = new FileInputStream(data); FileOutputStream os = new FileOutputStream(envelope)) {
			cmsEnvelope.generateEnvelope(is, os, certificate);
		}
		try (InputStream is = new FileInputStream(envelope)) {
			InputStream envelopeInputStream = cmsEnvelope.openEnvelope(is, privateKey);
			String envelopeData = new BufferedReader(new InputStreamReader(envelopeInputStream, StandardCharsets.UTF_8)).lines()
					.collect(Collectors.joining("\n"));

			assertEquals(DATA_CONTENT, envelopeData);
		}
	}
}
