/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import java.time.Duration;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Keys container.
 */
@ThreadSafe
class KeysContainer {
	private final Object keys;

	private final long expiryPeriod;

	private volatile long lastUsedTime = System.currentTimeMillis();

	public KeysContainer(Object keys, Duration expiryPeriod) {
		this.keys = keys;
		this.expiryPeriod = expiryPeriod.toMillis();
	}

	public boolean isExpired() {
		return System.currentTimeMillis() - lastUsedTime > expiryPeriod;
	}

	public Object getKeys() {
		lastUsedTime = System.currentTimeMillis();
		return keys;
	}
}
