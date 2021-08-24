/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.infrastructure.health;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HealthCheckRegistryTest {

	private final HealthCheckRegistry sut = new HealthCheckRegistry();

	@Test
	public void whenRegisterHealthCheckThenItShouldRunHealthCheck() {

		// given
		HealthCheck mockHealthCheck = mock(HealthCheck.class);

		// when
		sut.register(HealthCheckValidationType.DATABASE, mockHealthCheck);
		sut.runAllChecks();

		// then
		verify(mockHealthCheck, times(1)).execute();
	}

	@Test
	public void whenUnregisterHealthCheckThenItShouldNotRunHealthCheck() {
		// given
		HealthCheck mockHealthCheck = mock(HealthCheck.class);

		sut.register(HealthCheckValidationType.LOGGING_INITIALIZED, mockHealthCheck);
		// when

		sut.unregister(HealthCheckValidationType.LOGGING_INITIALIZED);
		sut.runAllChecks();

		// then
		verify(mockHealthCheck, times(0)).execute();

	}

	@Test
	public void testRunOnlySomeChecks() {

		// given
		HealthCheck mockHealthCheck = mock(HealthCheck.class);

		sut.register(HealthCheckValidationType.LOGGING_INITIALIZED, mockHealthCheck);
		sut.register(HealthCheckValidationType.DATABASE, mockHealthCheck);

		sut.runChecksDifferentFrom(Arrays.asList(HealthCheckValidationType.LOGGING_INITIALIZED));

		// then
		verify(mockHealthCheck, times(1)).execute();

	}

}
