/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.service;

import ch.post.it.evoting.cryptolib.api.proofs.ProofsServiceAPI;
import ch.post.it.evoting.cryptolib.commons.concurrent.BaseFactory;

/**
 * Factory of services that are not thread-safe.
 */
public class BasicProofsServiceFactory extends BaseFactory<ProofsServiceAPI> {

	/**
	 * This factory will create a service configured with default values.
	 */
	public BasicProofsServiceFactory() {
		super(ProofsServiceAPI.class);
	}

	@Override
	public ProofsServiceAPI create() {
		return new ProofsService();
	}
}
