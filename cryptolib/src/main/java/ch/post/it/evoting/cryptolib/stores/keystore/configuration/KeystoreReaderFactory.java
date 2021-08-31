/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.cryptolib.stores.keystore.configuration;

public class KeystoreReaderFactory {

	public KeystoreReader getInstance() {
		return KeystoreReaderImpl.getInstance();
	}
}
