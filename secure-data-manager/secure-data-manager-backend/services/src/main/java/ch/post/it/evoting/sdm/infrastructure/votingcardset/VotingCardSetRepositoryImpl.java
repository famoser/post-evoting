/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.votingcardset;

import static ch.post.it.evoting.cryptolib.commons.validations.Validate.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.record.impl.ODocument;

import ch.post.it.evoting.sdm.application.exception.DatabaseException;
import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.domain.model.ballot.BallotRepository;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.votingcardset.VotingCardSet;
import ch.post.it.evoting.sdm.domain.model.votingcardset.VotingCardSetRepository;
import ch.post.it.evoting.sdm.infrastructure.AbstractEntityRepository;
import ch.post.it.evoting.sdm.infrastructure.DatabaseManager;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * Implementation of operations with voting card set.
 */
@Repository
public class VotingCardSetRepositoryImpl extends AbstractEntityRepository implements VotingCardSetRepository {

	@Autowired
	BallotBoxRepository ballotBoxRepository;

	@Autowired
	BallotRepository ballotRepository;

	/**
	 * Constructor
	 *
	 * @param databaseManager the injected database manager
	 */
	@Autowired
	public VotingCardSetRepositoryImpl(final DatabaseManager databaseManager) {
		super(databaseManager);
	}

	@PostConstruct
	@Override
	public void initialize() {
		super.initialize();
	}

	/**
	 * @see VotingCardSetRepository#getBallotBoxId(java.lang.String)
	 */
	@Override
	public String getBallotBoxId(final String votingCardSetId) {
		String votingCardSetAsJson = find(votingCardSetId);
		// simple check if there is a voting card set data returned
		if (JsonConstants.EMPTY_OBJECT.equals(votingCardSetAsJson)) {
			return "";
		}

		JsonObject votingCardSet = JsonUtils.getJsonObject(votingCardSetAsJson);
		JsonObject ballotBox = votingCardSet.getJsonObject(JsonConstants.BALLOT_BOX);

		String ballotBoxId = ballotBox.getString(JsonConstants.ID);
		return ballotBoxId;
	}

	@Override
	public void updateRelatedBallot(final List<String> votingCardsIds) {
		try {
			for (String id : votingCardsIds) {
				String ballotBoxId = getBallotBoxId(id);
				JsonObject ballotBoxObject = JsonUtils.getJsonObject(ballotBoxRepository.find(ballotBoxId));
				String ballotBoxAlias = ballotBoxObject.getString(JsonConstants.ALIAS, "");
				String ballotId = ballotBoxObject.getJsonObject(JsonConstants.BALLOT).getString(JsonConstants.ID);
				JsonObject ballotObject = JsonUtils.getJsonObject(ballotRepository.find(ballotId));
				String ballotAlias = ballotObject.getString(JsonConstants.ALIAS, "");

				ODocument set = getDocument(id);
				set.field(JsonConstants.BALLOT_ALIAS, ballotAlias);
				set.field(JsonConstants.BALLOT_BOX_ALIAS, ballotBoxAlias);
				saveDocument(set);
			}
		} catch (OException e) {
			throw new DatabaseException("Failed to update related ballot.", e);
		}
	}

	/**
	 * @see VotingCardSetRepository#updateRelatedVerificationCardSet(java.lang.String, java.lang.String)
	 */
	@Override
	public void updateRelatedVerificationCardSet(final String votingCardSetId, final String verificationCardSetId) {
		try {
			ODocument set = getDocument(votingCardSetId);
			set.field(JsonConstants.VERIFICATION_CARD_SET_ID, verificationCardSetId);
			saveDocument(set);
		} catch (OException e) {
			throw new DatabaseException("Failed to update related verification card set.", e);
		}
	}

	@Override
	public String listByElectionEvent(final String electionEventId) {
		return list(singletonMap("electionEvent.id", electionEventId));
	}

	/**
	 * @see AbstractEntityRepository#entityName()
	 */
	@Override
	protected String entityName() {
		return VotingCardSet.class.getSimpleName();
	}

	@Override
	public String getVerificationCardSetId(final String votingCardSetId) {
		String votingCardSetAsJson = find(votingCardSetId);
		if (JsonConstants.EMPTY_OBJECT.equals(votingCardSetAsJson)) {
			return "";
		}

		JsonObject votingCardSet = JsonUtils.getJsonObject(votingCardSetAsJson);

		return votingCardSet.getString(JsonConstants.VERIFICATION_CARD_SET_ID);
	}

	// get voting card set in json object format. This method assumes that there
	// is just one element as result of the
	// search.
	public JsonObject getVotingCardSetJson(final String electionEventId, final String votingCardSetId) throws ResourceNotFoundException {
		JsonObject votingCardSet;
		Map<String, Object> attributeValueMap = new HashMap<>();
		attributeValueMap.put(JsonConstants.ELECTION_EVENT_DOT_ID, electionEventId);
		attributeValueMap.put(JsonConstants.ID, votingCardSetId);
		String votingCardSetResultListAsJson = list(attributeValueMap);
		if (StringUtils.isEmpty(votingCardSetResultListAsJson)) {
			throw new ResourceNotFoundException("Voting card set not found");
		} else {
			JsonArray votingCardSetResultList = JsonUtils.getJsonObject(votingCardSetResultListAsJson).getJsonArray(JsonConstants.RESULT);

			// Assume that there is just one element as result of the search.
			if (votingCardSetResultList != null && !votingCardSetResultList.isEmpty()) {
				votingCardSet = votingCardSetResultList.getJsonObject(0);
			} else {
				throw new ResourceNotFoundException("Voting card set not found");
			}
		}

		return votingCardSet;
	}

	@Override
	public String getVotingCardSetAlias(final String votingCardSetId) {
		checkNotNull(votingCardSetId);
		validateUUID(votingCardSetId);

		final String sql = "select alias from " + entityName() + " where id = :id";
		final Map<String, Object> parameters = singletonMap(JsonConstants.ID, votingCardSetId);
		final List<ODocument> documents;
		try {
			documents = selectDocuments(sql, parameters, 1);
		} catch (OException e) {
			throw new DatabaseException("Failed to get voting card set alias.", e);
		}
		return documents.isEmpty() ? "" : documents.get(0).field("alias", String.class);
	}

}
