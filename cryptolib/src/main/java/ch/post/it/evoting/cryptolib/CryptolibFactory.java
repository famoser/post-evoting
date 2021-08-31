/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.cryptolib;

@SuppressWarnings("squid:S1610")
// This is a misfire of this warning, we want the constructor to be called by all subclasses, during class initialization.
// There is no equivalent way of doing this in an interface.
public abstract class CryptolibFactory {
	public CryptolibFactory() {
		CryptolibInitializer.initialize();
	}
}
