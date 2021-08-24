/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.extendedkeystore.cryptoapi.CryptoAPIExtendedKeyStore;
import ch.post.it.evoting.sdm.config.exceptions.ConfigurationEngineException;

public class KeyStorePersister {

	private KeyStorePersister() {
	}

	public static void saveKeyStore(final CryptoAPIExtendedKeyStore keyStore, final Path path, final char[] keyStorePassword) {

		try (final OutputStream out = Files.newOutputStream(path)) {
			keyStore.store(out, keyStorePassword);
		} catch (IOException | GeneralCryptoLibException e) {
			throw new ConfigurationEngineException("An error occurred while persisting the keystore to " + path.toString(), e);
		}
	}

	public static void saveKeyStore(final KeyStore keyStore, final Path path, final char[] keyStorePassword) {

		try (final OutputStream out = Files.newOutputStream(path)) {
			keyStore.store(out, keyStorePassword);
		} catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
			throw new ConfigurationEngineException("An error occurred while persisting the keystore to " + path.toString(), e);
		}
	}
}
