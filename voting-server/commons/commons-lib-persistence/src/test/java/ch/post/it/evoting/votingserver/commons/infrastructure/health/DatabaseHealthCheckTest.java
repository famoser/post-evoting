/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.infrastructure.health;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DatabaseHealthCheckTest {

	@Mock
	DataSource mockDataSource;

	@Test
	public void whenDatabaseIsAvailableShouldReturnHealthy_WithoutValidationQuery() throws Exception {

		// given
		DatabaseHealthCheck sut = new DatabaseHealthCheck(mockDataSource);

		// when
		HealthCheck.HealthCheckResult result = sut.execute();

		// then
		Assert.assertTrue(result.getHealthy());
	}

	@Test
	public void whenDatabaseIsAvailableShouldReturnHealthy_WithValidationQuery() throws Exception {

		// given
		DatabaseHealthCheck sut = new DatabaseHealthCheck(mockDataSource, null, null, "select 1");
		Connection mockConnection = mock(Connection.class);
		Statement mockStatement = mock(Statement.class);
		when(mockStatement.execute(anyString())).thenReturn(true);
		when(mockConnection.createStatement()).thenReturn(mockStatement);
		when(mockDataSource.getConnection()).thenReturn(mockConnection);

		// when
		HealthCheck.HealthCheckResult result = sut.execute();

		// then
		Assert.assertTrue(result.getHealthy());
	}

	@Test
	public void whenDatabaseIsNotAvailableShouldReturnHealthy_WithValidationQuery() throws Exception {

		// given
		DatabaseHealthCheck sut = new DatabaseHealthCheck(mockDataSource, null, null, "select 1");
		Connection mockConnection = mock(Connection.class);
		Statement mockStatement = mock(Statement.class);
		doThrow(SQLException.class).when(mockStatement).execute(anyString());
		when(mockConnection.createStatement()).thenReturn(mockStatement);
		when(mockDataSource.getConnection()).thenReturn(mockConnection);
		// when
		HealthCheck.HealthCheckResult result = sut.execute();

		// then
		Assert.assertFalse(result.getHealthy());
	}
}
