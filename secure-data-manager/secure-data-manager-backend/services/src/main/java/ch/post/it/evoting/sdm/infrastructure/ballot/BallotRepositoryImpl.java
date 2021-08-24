/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.ballot;

import static java.util.Collections.singletonMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.record.impl.ODocument;

import ch.post.it.evoting.sdm.application.exception.DatabaseException;
import ch.post.it.evoting.sdm.domain.model.ballot.Ballot;
import ch.post.it.evoting.sdm.domain.model.ballot.BallotRepository;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.infrastructure.AbstractEntityRepository;
import ch.post.it.evoting.sdm.infrastructure.DatabaseManager;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;

/**
 * Implementation of the Ballot Repository
 */
@Repository
public class BallotRepositoryImpl extends AbstractEntityRepository implements BallotRepository {

	@Autowired
	@Lazy
	BallotBoxRepository ballotBoxRepository;

	/**
	 * Constructor
	 *
	 * @param databaseManager the injected database manager
	 */
	@Autowired
	public BallotRepositoryImpl(final DatabaseManager databaseManager) {
		super(databaseManager);
	}

	@PostConstruct
	@Override
	public void initialize() {
		super.initialize();
	}

	@Override
	public List<String> listAliases(final String id) {
		String sql = "select alias from " + entityName() + " where id=:id";
		Map<String, Object> parameters = singletonMap(JsonConstants.ID, id);
		List<ODocument> documents;
		try {
			documents = selectDocuments(sql, parameters, -1);
		} catch (OException e) {
			throw new DatabaseException("Failed to list aliases.", e);
		}
		List<String> aliases = new ArrayList<>(documents.size());
		for (ODocument document : documents) {
			aliases.add(document.field("alias", String.class));
		}
		return aliases;
	}

	@Override
	public void updateRelatedBallotBox(final List<String> ballotIds) {
		try {
			for (String id : ballotIds) {
				ODocument ballot = getDocument(id);
				List<String> relatedIds = ballotBoxRepository.listAliases(id);
				// to maintain compatibility with FE, save as comma-separated
				// string
				ballot.field(JsonConstants.BALLOT_BOXES, StringUtils.join(relatedIds, ","));
				saveDocument(ballot);
			}
		} catch (OException e) {
			throw new DatabaseException("Failed to update related ballot box.", e);
		}
	}

	@Override
	public void updateSignedBallot(final String ballotId, final String signedBallot) {
		try {
			ODocument ballot = getDocument(ballotId);
			ballot.field(JsonConstants.SIGNED_OBJECT, signedBallot);
			saveDocument(ballot);
		} catch (OException e) {
			throw new DatabaseException("Failed to update signed ballot.", e);
		}
	}

	@Override
	public String listByElectionEvent(final String electionEventId) {
		return list(singletonMap("electionEvent.id", electionEventId));
	}

	@Override
	protected String entityName() {
		return Ballot.class.getSimpleName();
	}
}
