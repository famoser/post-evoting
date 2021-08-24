/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

/**
 * Tests of {@link Cache}.
 */
class CacheTest {
	private static final String ELECTION_EVENT_ID = "electionEventId";

	private static final String VERIFICATION_CARD_SET_ID = "verificationCardSetId";

	private static final Duration EXPIRY_PERIOD = Duration.ofSeconds(1);

	private TimerSpy timer;

	private Cache cache;

	@BeforeEach
	public void setUp() {
		timer = new TimerSpy();
		cache = new Cache(timer, EXPIRY_PERIOD);
		cache.startup();
	}

	@AfterEach
	public void tearDown() {
		timer.cancel();
	}

	@Test
	void testPutCcrjReturnCodesKeys() throws GeneralCryptoLibException {
		ZpSubgroup group = new ZpSubgroup(BigInteger.valueOf(4), BigInteger.valueOf(7), BigInteger.valueOf(3));
		Exponent exponent = new Exponent(group.getQ(), BigInteger.ONE);
		ElGamalPrivateKey privateKey = new ElGamalPrivateKey(singletonList(exponent), group);
		ZpGroupElement element = group.getGenerator();
		ElGamalPublicKey publicKey = new ElGamalPublicKey(singletonList(element), group);
		byte[] publicKeySignature = { 1, 2, 3 };

		CcrjReturnCodesKeys ccrjReturnCodesKeys = new CcrjReturnCodesKeys.Builder()
				.setCcrjChoiceReturnCodesEncryptionKeys(privateKey, publicKey, publicKeySignature)
				.setCcrjReturnCodesGenerationKeys(privateKey, publicKey, publicKeySignature).build();

		cache.putCcrjReturnCodesKeys(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, ccrjReturnCodesKeys);

		assertEquals(ccrjReturnCodesKeys, cache.getCcrjReturnCodesKeys(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID));
	}

	@Test
	void testPutElectionSigningKeys() {
		PrivateKey privateKey = mock(PrivateKey.class);
		X509Certificate[] certificateChain = { mock(X509Certificate.class) };

		ElectionSigningKeys electionSigningKeys = new ElectionSigningKeys(privateKey, certificateChain);

		cache.putElectionSigningKeys(ELECTION_EVENT_ID, electionSigningKeys);

		assertEquals(electionSigningKeys, cache.getElectionSigningKeys(ELECTION_EVENT_ID));
	}

	@Test
	void testPutCcmjElectionKeys() throws GeneralCryptoLibException {
		ZpSubgroup group = new ZpSubgroup(BigInteger.valueOf(4), BigInteger.valueOf(7), BigInteger.valueOf(3));
		Exponent exponent = new Exponent(group.getQ(), BigInteger.ONE);
		ElGamalPrivateKey privateKey = new ElGamalPrivateKey(singletonList(exponent), group);
		ZpGroupElement element = group.getGenerator();
		ElGamalPublicKey publicKey = new ElGamalPublicKey(singletonList(element), group);
		byte[] publicKeySignature = { 1, 2, 3 };

		CcmjElectionKeys ccmjElectionKeys = new CcmjElectionKeys(privateKey, publicKey, publicKeySignature);

		cache.putCcmjElectionKeys(ELECTION_EVENT_ID, ccmjElectionKeys);

		assertEquals(ccmjElectionKeys, cache.getCcmjElectionKeys(ELECTION_EVENT_ID));
	}

	@Test
	void testShutdown() {
		cache.shutdown();
		assertTrue(timer.isCancelled());
	}

	@Test
	void testStartup() {
		assertEquals(EXPIRY_PERIOD.toMillis(), timer.getDelay());
		assertEquals(EXPIRY_PERIOD.toMillis(), timer.getPeriod());
	}

	@Test
	void testExpiry() {
		PrivateKey privateKey = mock(PrivateKey.class);
		X509Certificate[] certificateChain = { mock(X509Certificate.class) };

		ElectionSigningKeys electionSigningKeys = new ElectionSigningKeys(privateKey, certificateChain);

		cache.putElectionSigningKeys(ELECTION_EVENT_ID, electionSigningKeys);

		// Put poll delay and interval to a value higher than the cache expiration time (EXPIRY_PERIOD). We have to do this because the condition
		// that is periodically checked, cache.getElectionSigningKeys(ELECTION_EVENT_ID), resets the expiration date of the KeysContainer.
		final Duration pollingDuration = EXPIRY_PERIOD.multipliedBy(2);
		Awaitility.await().with().pollDelay(pollingDuration).pollInterval(pollingDuration)
				.untilAsserted(() -> assertNull(cache.getElectionSigningKeys(ELECTION_EVENT_ID)));
	}

	private static class TimerSpy extends Timer {
		private boolean cancelled;

		private long delay;

		private long period;

		public boolean isCancelled() {
			return cancelled;
		}

		public long getDelay() {
			return delay;
		}

		public long getPeriod() {
			return period;
		}

		@Override
		public void cancel() {
			super.cancel();
			cancelled = true;
		}

		@Override
		public void schedule(TimerTask task, long delay, long period) {
			super.schedule(task, delay, period);
			this.delay = delay;
			this.period = period;
		}
	}
}
