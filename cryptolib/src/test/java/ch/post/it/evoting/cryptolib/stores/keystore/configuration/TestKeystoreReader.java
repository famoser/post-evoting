/*
 * (c) Copyright 2020 Swiss Post Ltd.
 */
package ch.post.it.evoting.cryptolib.stores.keystore.configuration;

public class TestKeystoreReader extends AbstractKeystoreReader {

	private TestKeystoreReader() {
		super(TestKeystoreResources.getInstance());
	}

	public static TestKeystoreReader getInstance() {
		return new TestKeystoreReader();
	}

}
