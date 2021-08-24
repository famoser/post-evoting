/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.infrastructure.health;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HealthCheckTest {

	private final HealthCheck sut = spy(HealthCheck.class);

	@Test
	public void whenCheckThrowsResultShouldBeUnhealthy() {

		// given
		doThrow(new RuntimeException("exception")).when(sut).check();

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
		when(sut.check()).thenReturn(mockResult);

		// when
		HealthCheck.HealthCheckResult result = sut.execute();

		// then
		Assert.assertEquals(mockResult.getHealthy(), result.getHealthy());

	}
}
