/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.electionevent;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.record.impl.ODocument;

import ch.post.it.evoting.domain.election.ElectionEvent;
import ch.post.it.evoting.sdm.application.exception.DatabaseException;
import ch.post.it.evoting.sdm.domain.model.electionevent.ElectionEventRepository;
import ch.post.it.evoting.sdm.infrastructure.AbstractEntityRepository;
import ch.post.it.evoting.sdm.infrastructure.DatabaseManager;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;

/**
 * Implementation of operations with election event.
 */
@Repository
public class ElectionEventRepositoryImpl extends AbstractEntityRepository implements ElectionEventRepository {

	/**
	 * Constructor
	 *
	 * @param databaseManager the injected database manager
	 */
	@Autowired
	public ElectionEventRepositoryImpl(final DatabaseManager databaseManager) {
		super(databaseManager);
	}

	@PostConstruct
	@Override
	public void initialize() {
		super.initialize();
	}

	@Override
	public String getElectionEventAlias(final String electionEventId) {
		String sql = "select alias from " + entityName() + " where id = :id";
		Map<String, Object> parameters = singletonMap(JsonConstants.ID, electionEventId);
		List<ODocument> documents;
		try {
			documents = selectDocuments(sql, parameters, 1);
		} catch (OException e) {
			throw new DatabaseException("Failed to get election event alias.", e);
		}
		return documents.isEmpty() ? "" : documents.get(0).field("alias", String.class);
	}

	@Override
	public List<String> listIds() {
		String sql = "select id from " + entityName();
		Map<String, Object> parameters = emptyMap();
		List<ODocument> documents;
		try {
			documents = selectDocuments(sql, parameters, -1);
		} catch (OException e) {
			throw new DatabaseException("Failed to list identifiers.", e);
		}
		List<String> ids = new ArrayList<>(documents.size());
		for (ODocument document : documents) {
			ids.add(document.field(JsonConstants.ID, String.class));
		}
		return ids;
	}

	@Override
	protected String entityName() {
		return ElectionEvent.class.getSimpleName();
	}
}
