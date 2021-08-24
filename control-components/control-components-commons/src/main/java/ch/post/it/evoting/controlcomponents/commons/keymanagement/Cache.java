/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;

@Service
public class Cache {
	private final ConcurrentMap<KeysId, KeysContainer> containers = new ConcurrentHashMap<>();

	private final Timer timer;
	private final Duration expiryPeriod;

	@Autowired
	public Cache(
			@Value("${keys.cacheExpiryPeriod:60000}")
			final String cacheExpiryPeriod) {
		this.expiryPeriod = Duration.ofMillis(Long.parseLong(cacheExpiryPeriod));
		this.timer = new Timer();
	}

	@VisibleForTesting
	Cache(final Timer timer, final Duration expiryPeriod) {
		this.timer = timer;
		this.expiryPeriod = expiryPeriod;
	}

	@javax.annotation.Nullable
	public CcrjReturnCodesKeys getCcrjReturnCodesKeys(final String electionEventId, final String verificationCardSetId) {
		final KeysId id = KeysId.getCcrjReturnCodesKeysInstance(electionEventId, verificationCardSetId);
		return (CcrjReturnCodesKeys) doGet(id);
	}

	@javax.annotation.Nullable
	public ElectionSigningKeys getElectionSigningKeys(final String electionEventId) {
		final KeysId id = KeysId.getElectionSigningInstance(electionEventId);
		return (ElectionSigningKeys) doGet(id);
	}

	@javax.annotation.Nullable
	public CcmjElectionKeys getCcmjElectionKeys(final String electionEventId) {
		final KeysId id = KeysId.getCcmjElectionKeysInstance(electionEventId);
		return (CcmjElectionKeys) doGet(id);
	}

	public void putCcrjReturnCodesKeys(final String electionEventId, final String verificationCardSetId,
			final CcrjReturnCodesKeys ccrjReturnCodesKeys) {
		final KeysId id = KeysId.getCcrjReturnCodesKeysInstance(electionEventId, verificationCardSetId);
		doPut(id, ccrjReturnCodesKeys);
	}

	public void putElectionSigningKeys(final String electionEventId, final ElectionSigningKeys electionSigningKeys) {
		final KeysId id = KeysId.getElectionSigningInstance(electionEventId);
		doPut(id, electionSigningKeys);
	}

	public void putCcmjElectionKeys(final String electionEventId, final CcmjElectionKeys ccmjElectionKeys) {
		final KeysId id = KeysId.getCcmjElectionKeysInstance(electionEventId);
		doPut(id, ccmjElectionKeys);
	}

	public void shutdown() {
		timer.cancel();
	}

	public void startup() {
		final long period = expiryPeriod.toMillis();
		timer.schedule(new RemoveExpiredTask(), period, period);
	}

	private Object doGet(final KeysId id) {
		final KeysContainer container = containers.get(id);
		return container != null ? container.getKeys() : null;
	}

	private void doPut(final KeysId id, final Object keys) {
		containers.put(id, new KeysContainer(keys, expiryPeriod));
	}

	private class RemoveExpiredTask extends TimerTask {
		@Override
		public void run() {
			removeExpired();
		}

		private void removeExpired() {
			containers.values().removeIf(KeysContainer::isExpired);
		}
	}
}
