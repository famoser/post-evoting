/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.shares.keys;

import java.security.KeyException;
import java.security.KeyPair;

public interface KeyPairGenerator {

	KeyPair generate() throws KeyException;
}
