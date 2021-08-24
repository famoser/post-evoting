/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.electoralauthority;

import static java.util.Collections.singletonMap;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.json.JsonArray;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.record.impl.ODocument;

import ch.post.it.evoting.sdm.application.exception.DatabaseException;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.electoralauthority.ElectoralAuthority;
import ch.post.it.evoting.sdm.domain.model.electoralauthority.ElectoralAuthorityRepository;
import ch.post.it.evoting.sdm.infrastructure.AbstractEntityRepository;
import ch.post.it.evoting.sdm.infrastructure.DatabaseManager;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * Implementation of the interface which offers operations on the repository of electoral repository.
 */
@Repository
public class ElectoralAuthorityRepositoryImpl extends AbstractEntityRepository implements ElectoralAuthorityRepository {

	@Autowired
	BallotBoxRepository ballotBoxRepository;

	/**
	 * The constructor.
	 *
	 * @param databaseManager the injected database manager
	 */
	@Autowired
	public ElectoralAuthorityRepositoryImpl(final DatabaseManager databaseManager) {
		super(databaseManager);
	}

	@PostConstruct
	@Override
	public void initialize() {
		super.initialize();
	}

	/**
	 * @see ElectoralAuthorityRepository#updateRelatedBallotBox(java.util.List)
	 */
	@Override
	public void updateRelatedBallotBox(final List<String> electoralAuthoritiesIds) {
		try {
			for (String id : electoralAuthoritiesIds) {
				ODocument authority = getDocument(id);
				List<String> aliases = getBallotBoxAliases(id);
				authority.field(JsonConstants.BALLOT_BOX_ALIAS, aliases);
				saveDocument(authority);
			}
		} catch (OException e) {
			throw new DatabaseException("Failed to update related ballot box.", e);
		}
	}

	@Override
	public String listByElectionEvent(final String electionEventId) {
		return list(singletonMap("electionEvent.id", electionEventId));
	}

	@Override
	protected String entityName() {
		return ElectoralAuthority.class.getSimpleName();
	}

	// Return the aliases of all the ballot boxes for the electoral authority
	// identified by electoralAuthorityId.
	private List<String> getBallotBoxAliases(final String electoralAuthorityId) {
		JsonArray ballotBoxesResult = JsonUtils.getJsonObject(ballotBoxRepository.findByElectoralAuthority(electoralAuthorityId))
				.getJsonArray(JsonConstants.RESULT);
		List<String> ballotBoxIds = new ArrayList<>();
		for (int index = 0; index < ballotBoxesResult.size(); index++) {
			ballotBoxIds.add(ballotBoxesResult.getJsonObject(index).getString(JsonConstants.ALIAS));
		}

		return ballotBoxIds;
	}
}
