/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import java.security.PrivateKey;

public interface KeyStoreService {

	PrivateKey getPrivateKey();
}
