/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.ballotbox;

import static java.util.Collections.singletonMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.record.impl.ODocument;

import ch.post.it.evoting.domain.election.BallotBox;
import ch.post.it.evoting.sdm.application.exception.DatabaseException;
import ch.post.it.evoting.sdm.domain.model.ballot.BallotRepository;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.infrastructure.AbstractEntityRepository;
import ch.post.it.evoting.sdm.infrastructure.DatabaseManager;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * Implementation of operations with ballot box.
 */
@Repository
public class BallotBoxRepositoryImpl extends AbstractEntityRepository implements BallotBoxRepository {

	// The name of the json parameter ballot.
	private static final String JSON_NAME_PARAM_BALLOT = "ballot";

	// the name of the ballot alias
	private static final String JSON_NAME_PARAM_BALLOT_ALIAS = "ballotAlias";

	@Autowired
	@Lazy
	BallotRepository ballotRepository;

	/**
	 * Constructor
	 *
	 * @param databaseManager the injected database manager
	 */
	@Autowired
	public BallotBoxRepositoryImpl(final DatabaseManager databaseManager) {
		super(databaseManager);
	}

	@PostConstruct
	@Override
	public void initialize() {
		super.initialize();
	}

	/**
	 * @see BallotBoxRepository#getBallotId(java.lang.String)
	 */
	@Override
	public String getBallotId(final String ballotBoxId) {
		String ballotBoxAsJson = find(ballotBoxId);
		// simple check if there is a voting card set data returned
		if (JsonConstants.EMPTY_OBJECT.equals(ballotBoxAsJson)) {
			return "";
		}

		JsonObject ballotBox = JsonUtils.getJsonObject(ballotBoxAsJson);
		return ballotBox.getJsonObject(JSON_NAME_PARAM_BALLOT).getString(JsonConstants.ID);
	}

	@Override
	public List<String> listAliases(final String ballotId) {
		String sql = "select alias from " + entityName() + " where ballot.id = :ballotId";
		Map<String, Object> parameters = singletonMap("ballotId", ballotId);
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

	/**
	 * @see BallotBoxRepository#updateRelatedBallotAlias(java.util.List)
	 */
	@Override
	public void updateRelatedBallotAlias(final List<String> ballotBoxIds) {
		try {
			for (String id : ballotBoxIds) {
				ODocument ballotBox = getDocument(id);
				String ballotId = ballotBox.field("ballot.id", String.class);
				List<String> aliases = ballotRepository.listAliases(ballotId);
				// should be only one alias. to maintain compatibility with FE,
				// save
				// as comma-separated string
				ballotBox.field(JSON_NAME_PARAM_BALLOT_ALIAS, StringUtils.join(aliases, ","));
				saveDocument(ballotBox);
			}
		} catch (OException e) {
			throw new DatabaseException("Failed to update related ballot aliases.", e);
		}
	}

	@Override
	public String findByElectoralAuthority(final String id) {
		Map<String, Object> attributes = singletonMap(JsonConstants.ELECTORAL_AUTHORITY_DOT_ID, id);
		return list(attributes);
	}

	@Override
	public String listByElectionEvent(final String electionEventId) {
		return list(singletonMap("electionEvent.id", electionEventId));
	}

	@Override
	protected String entityName() {
		return BallotBox.class.getSimpleName();
	}
}
