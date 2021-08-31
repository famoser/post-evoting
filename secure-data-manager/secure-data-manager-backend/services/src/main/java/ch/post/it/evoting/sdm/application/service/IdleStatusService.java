/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

/**
 * Service that handles if a certain ID is performing any operation
 */
@Service
public class IdleStatusService {

	private final ConcurrentHashMap<String, AtomicBoolean> lockedIds = new ConcurrentHashMap<>();

	/**
	 * Checks if the id is ready for executing an operation. The purpose is to avoid parallel executions due to extra quickly clicks on the front end
	 *
	 * @param id
	 * @return
	 */
	public synchronized boolean getIdLock(String id) {
		if (lockedIds.containsKey(id)) {
			return lockedIds.get(id).compareAndSet(false, true);
		} else {
			AtomicBoolean atomicBoolean = new AtomicBoolean(true);
			lockedIds.put(id, atomicBoolean);
			return true;
		}

	}

	/**
	 * Marks the id as ready to be executed
	 *
	 * @param id
	 */
	public void freeIdLock(String id) {
		lockedIds.get(id).set(false);
	}

}
