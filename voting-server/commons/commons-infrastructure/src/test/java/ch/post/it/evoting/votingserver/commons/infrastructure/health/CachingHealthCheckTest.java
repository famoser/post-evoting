/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.infrastructure.health;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CachingHealthCheckTest {

	private static final int MS_IN_SECOND = 1000;
	private static final int CACHE_EXPIRATION_TIME_IN_SECONDS = 5;
	private final HealthCheck delegate = spy(new TestHealthCheck());
	private final CachingHealthCheck sut = spy(new CachingHealthCheck(delegate, CACHE_EXPIRATION_TIME_IN_SECONDS));

	@Test
	public void whenCheckThrowsResultShouldBeUnhealthy() {

		// given
		doThrow(new RuntimeException("exception")).when(delegate).check();

		// when
		HealthCheck.HealthCheckResult result = sut.execute();

		// then
		Assert.assertFalse(result.getHealthy());
	}

	@Test
	public void executeShouldReturnSameResultAsCheck() {
		HealthCheck.HealthCheckResult mockResult = mock(HealthCheck.HealthCheckResult.class);
		when(mockResult.getHealthy()).thenReturn(false);

		// given
		when(delegate.check()).thenReturn(mockResult);

		// when
		HealthCheck.HealthCheckResult result = sut.execute();

		// then
		Assert.assertEquals(mockResult.getHealthy(), result.getHealthy());

	}

	@Test
	public void executeExpirationShouldNotExecuteRealCheck() {

		// when
		sut.execute();
		// then
		verify(delegate, times(1)).execute();

		// when
		sut.execute();
		// then (executions should still be one)
		verify(delegate, times(1)).execute();
	}

	@Test
	public void executeAfterExpirationShouldExecuteRealCheck() throws InterruptedException {

		// when
		sut.execute();
		// then
		verify(delegate, times(1)).execute();

		waitForCacheExpiration();

		// then //then (executions should be 2)
		sut.execute();
		verify(delegate, times(2)).execute();
	}

	private void waitForCacheExpiration() throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Timer timer = new Timer();

		TimerTask delayedThreadStartTask = new TimerTask() {
			@Override
			public void run() {
				countDownLatch.countDown();
			}
		};
		timer.schedule(delayedThreadStartTask, CACHE_EXPIRATION_TIME_IN_SECONDS * MS_IN_SECOND);

		countDownLatch.await();
		timer.cancel();
	}

	public static class TestHealthCheck extends HealthCheck {

		@Override
		protected HealthCheckResult check() {
			return HealthCheckResult.healthy();
		}
	}
}
