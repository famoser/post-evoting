/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.commons.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.sql.DataSource;

/**
 * <p>
 * Implementation of {@link PartialResultsRepository} which stores partial results in a relational database.
 * <p>
 * Client is responsible for transaction management.
 */
@Singleton
@Local(PartialResultsRepository.class)
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Jdbc
public final class JdbcPartialResultsRepository implements PartialResultsRepository<byte[]> {
	private DataSource dataSource;

	@Override
	public void deleteAll(UUID correlationId) {
		String sql = "delete from or_partial_results where correlation_id_hi = ? and correlation_id_lo = ?";
		try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, correlationId.getMostSignificantBits());
			statement.setLong(2, correlationId.getLeastSignificantBits());
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to delete partial results.", e);
		}

	}

	@Override
	public boolean hasAll(UUID correlationId, int count) {
		try (Connection connection = dataSource.getConnection()) {
			return count == count(connection, correlationId);
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to check partial result count.", e);
		}
	}

	@Override
	public List<byte[]> listAll(UUID correlationId) {
		try (Connection connection = dataSource.getConnection()) {
			return list(connection, correlationId);
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to list partial results.", e);
		}
	}

	@Override
	public Optional<List<byte[]>> listIfHasAll(UUID correlationId, int count) {
		try (Connection connection = dataSource.getConnection()) {
			if (count != count(connection, correlationId)) {
				return Optional.empty();
			}
			return Optional.of(list(connection, correlationId));
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to list partial results.", e);
		}
	}

	@Override
	public void save(UUID correlationId, byte[] result) {
		String sql = "insert into or_partial_results(correlation_id_hi, correlation_id_lo, result) values (?, ?, ?)";
		try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, correlationId.getMostSignificantBits());
			statement.setLong(2, correlationId.getLeastSignificantBits());
			statement.setBytes(3, result);
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to save partial result.", e);
		}
	}

	/**
	 * Sets the data source.
	 *
	 * @param dataSource the data source.
	 */
	@Resource
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	private int count(Connection connection, UUID correlationId) throws SQLException {
		String sql = "select count(*) from or_partial_results where correlation_id_hi = ? and correlation_id_lo = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, correlationId.getMostSignificantBits());
			statement.setLong(2, correlationId.getLeastSignificantBits());
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return 0;
				}
				return resultSet.getInt(1);
			}
		}
	}

	private List<byte[]> list(Connection connection, UUID correlationId) throws SQLException {
		List<byte[]> results = new ArrayList<>();
		String sql = "select result from or_partial_results where correlation_id_hi = ? and correlation_id_lo = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, correlationId.getMostSignificantBits());
			statement.setLong(2, correlationId.getLeastSignificantBits());
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					results.add(resultSet.getBytes(1));
				}
			}
		}
		return results;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Optional<List<byte[]>> deleteListIfHasAll(UUID correlationId, int count) {
		Optional<List<byte[]>> results = listIfHasAll(correlationId, count);
		if (results.isPresent()) {
			deleteAll(correlationId);
		}
		return results;
	}
}
