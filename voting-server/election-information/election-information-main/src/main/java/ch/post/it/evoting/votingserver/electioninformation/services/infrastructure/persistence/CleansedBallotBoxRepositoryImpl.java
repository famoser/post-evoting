/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.persistence;

import java.io.IOException;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.domain.election.model.ballotbox.BallotBoxId;
import ch.post.it.evoting.domain.election.model.vote.EncryptedVote;
import ch.post.it.evoting.domain.election.model.vote.StringEncryptedVote;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.CleansedBallotBoxRepositoryException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.infrastructure.persistence.BaseRepositoryImpl;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.CleansedBallotBox;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.CleansedBallotBoxRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.CleansedExportedBallotBoxItem;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.votingcard.VotingCard;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.votingcard.VotingCardWriter;

/**
 * Implementation of CleansedBallotBoxRepository.
 */
@Stateless
public class CleansedBallotBoxRepositoryImpl extends BaseRepositoryImpl<CleansedBallotBox, Integer> implements CleansedBallotBoxRepository {

	private static final String PARAMETER_VOTING_CARD_ID = "votingCardId";

	private static final String PARAMETER_TENANT_ID = "tenantId";

	private static final String PARAMETER_ELECTION_EVENT_ID = "electionEventId";

	private static final String PARAMETER_BALLOT_BOX_ID = "ballotBoxId";

	private static final String PARAMETER_BALLOT_ID = "ballotId";

	private static final String MAX_ROW_TO_FETCH = "maxRowToFetch";

	private static final String MIN_ROW_TO_FETCH = "minRowToFetch";

	private static final int FETCH_SIZE = 1000;

	@Inject
	private Logger logger;

	/**
	 * Searches for a vote with the given tenant, election event and voting card id. This implementation uses database access by executing a SQL-query
	 * to select the data to be retrieved.
	 *
	 * @param tenantId        - the identifier of the tenant.
	 * @param electionEventId - the identifier of the election event.
	 * @param votingCardId    - the identifier of the voting card.
	 * @return a entity representing the vote stored in the ballot box.
	 * @throws ResourceNotFoundException if the vote is not found.
	 */
	@Override
	public CleansedBallotBox findByTenantIdElectionEventIdVotingCardId(final String tenantId, final String electionEventId, final String votingCardId)
			throws ResourceNotFoundException {
		final TypedQuery<CleansedBallotBox> query = entityManager.createQuery(
				"SELECT b FROM CleansedBallotBox b WHERE b.tenantId = :tenantId AND b.electionEventId = :electionEventId AND b.votingCardId = :votingCardId ORDER BY b.id DESC",
				CleansedBallotBox.class);
		query.setParameter(PARAMETER_TENANT_ID, tenantId);
		query.setParameter(PARAMETER_ELECTION_EVENT_ID, electionEventId);
		query.setParameter(PARAMETER_VOTING_CARD_ID, votingCardId);
		final List<CleansedBallotBox> listBallotBox = query.getResultList();
		if (!listBallotBox.isEmpty()) {
			return listBallotBox.get(0);
		}
		throw new ResourceNotFoundException("");
	}

	@Override
	public List<CleansedExportedBallotBoxItem> getEncryptedVotesByTenantIdElectionEventIdBallotBoxId(final String tenantId,
			final String electionEventId, final String ballotBoxId, final int firstElement, final int lastElement) {

		final List<CleansedExportedBallotBoxItem> exportedBallotBoxList = new ArrayList<>();

		final Session session = entityManager.unwrap(Session.class);

		final String exportedBallotBoxSQL =
				"select fr.tenant_id, fr.election_event_id, fr.voting_card_id, fr.ballot_id, fr.ballot_box_id, fr.encrypted_vote, fr.vote_cast_code from "
						+ "( select  ft.*, ROWNUM rnum from (select rownum, B.TENANT_ID,B.ELECTION_EVENT_ID,B.VOTING_CARD_ID,B.BALLOT_ID,B.BALLOT_BOX_ID,B.ENCRYPTED_VOTE, VC.VOTE_CAST_CODE FROM CLEANSED_BALLOT_BOX B LEFT OUTER JOIN VOTE_CAST_CODE VC ON B.VOTING_CARD_ID = VC.VOTING_CARD_ID AND B.TENANT_ID = VC.TENANT_ID AND B.ELECTION_EVENT_ID = VC.ELECTION_EVENT_ID WHERE B.TENANT_ID = :tenantId AND B.ELECTION_EVENT_ID = :electionEventId AND B.BALLOT_BOX_ID = :ballotBoxId order by B.ID) ft where ROWNUM <= :maxRowToFetch ) fr where rnum  >= :minRowToFetch";

		final SQLQuery createSQLQuery = session.createSQLQuery(exportedBallotBoxSQL);
		createSQLQuery.setFetchSize(500);
		createSQLQuery.setParameter(PARAMETER_TENANT_ID, tenantId);
		createSQLQuery.setParameter(PARAMETER_ELECTION_EVENT_ID, electionEventId);
		createSQLQuery.setParameter(PARAMETER_BALLOT_BOX_ID, ballotBoxId);
		createSQLQuery.setParameter(MIN_ROW_TO_FETCH, firstElement);
		createSQLQuery.setParameter(MAX_ROW_TO_FETCH, lastElement);

		@SuppressWarnings("unchecked")
		final List<Object[]> resultSet = createSQLQuery.list();

		resultSet.forEach(record -> {

			String vote = clobToString((Clob) record[5]);
			exportedBallotBoxList.add(new CleansedExportedBallotBoxItem.CleansedExportedBallotBoxItemBuilder().setVote(vote).build());
		});

		return exportedBallotBoxList;

	}

	@Override
	public String getEncryptedVotesByTenantIdElectionEventIdBallotBoxIdCSV(final String tenantId, final String electionEventId,
			final String ballotBoxId) throws IOException {
		final TypedQuery<String> query = entityManager.createQuery(
				"SELECT b.encrypted_vote FROM CleansedBallotBox b WHERE b.tenantId = :tenantId AND b.electionEventId = :electionEventId AND b.ballotBoxId = :ballotBoxId",
				String.class);
		query.setParameter(PARAMETER_TENANT_ID, tenantId);
		query.setParameter(PARAMETER_ELECTION_EVENT_ID, electionEventId);
		query.setParameter(PARAMETER_BALLOT_BOX_ID, ballotBoxId);
		final List<String> resultList = query.getResultList();
		final ObjectMapper mapper = new ObjectMapper();

		final List<String> result = new ArrayList<>();
		for (final String s : resultList) {
			final JsonNode jsonNode = mapper.readTree(s);
			final String textValue = jsonNode.get("vote").get("encryptedOptions").textValue();
			result.add(textValue);
		}
		return StringUtils.join(result, System.lineSeparator());
	}

	private String clobToString(final Clob clob) {
		try {
			return clob.getSubString(1, (int) clob.length());
		} catch (final SQLException sE) {
			logger.warn("Error handling clob to String.", sE);
			return "";
		}
	}

	@Override
	public List<CleansedBallotBox> findByTenantIdElectionEventIdBallotBoxId(final String tenantId, final String electionEventId,
			final String ballotBoxId) {
		final TypedQuery<CleansedBallotBox> query = entityManager.createQuery(
				"SELECT b FROM CleansedBallotBox b WHERE b.tenantId = :tenantId AND b.electionEventId = :electionEventId AND b.ballotBoxId = :ballotBoxId",
				CleansedBallotBox.class);
		query.setParameter(PARAMETER_TENANT_ID, tenantId);
		query.setParameter(PARAMETER_ELECTION_EVENT_ID, electionEventId);
		query.setParameter(PARAMETER_BALLOT_BOX_ID, ballotBoxId);
		return query.getResultList();
	}

	@Override
	public List<CleansedBallotBox> findByTenantIdElectionEventIdVotingCardIdBallotBoxIdBallotId(final String tenantId, final String electionEventId,
			final String votingCardId, final String ballotBoxId, final String ballotId) {
		final TypedQuery<CleansedBallotBox> query = entityManager.createQuery(
				"SELECT b FROM CleansedBallotBox b WHERE b.tenantId = :tenantId AND b.electionEventId = :electionEventId "
						+ "AND b.votingCardId = :votingCardId AND b.ballotId = :ballotId AND b.ballotBoxId = :ballotBoxId", CleansedBallotBox.class);
		query.setParameter(PARAMETER_TENANT_ID, tenantId);
		query.setParameter(PARAMETER_ELECTION_EVENT_ID, electionEventId);
		query.setParameter(PARAMETER_VOTING_CARD_ID, votingCardId);
		query.setParameter(PARAMETER_BALLOT_ID, ballotId);
		query.setParameter(PARAMETER_BALLOT_BOX_ID, ballotBoxId);
		return query.getResultList();
	}

	@Override
	public void with(final EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/**
	 * Find and write voting cards that have votes in ballot boxes (used voting cards).
	 *
	 * @param tenantId        the tenant id
	 * @param electionEventId the election event id
	 * @param writer          the writer
	 * @throws IOException
	 */
	@Override
	public void findAndWriteUsedVotingCards(final String tenantId, final String electionEventId, final VotingCardWriter writer) throws IOException {
		final Session session = entityManager.unwrap(Session.class);
		final Query query = session.createQuery("SELECT bb.votingCardId FROM CleansedBallotBox bb WHERE bb.tenantId = ? AND bb.electionEventId = ?");
		query.setParameter(0, tenantId);
		query.setParameter(1, electionEventId);
		query.setFetchSize(FETCH_SIZE);
		query.setReadOnly(true);
		query.setLockMode("bb", LockMode.NONE);

		try {
			ScrollableResults results = query.scroll(ScrollMode.FORWARD_ONLY);
			while (results.next()) {
				writer.write(new VotingCard((String) results.get(0)));
			}
			results.close();
		} catch (HibernateException hE) {
			throw new IOException("Error getting used voting cards", hE);
		}
	}

	@Override
	public int count(BallotBoxId ballotBoxId) throws CleansedBallotBoxRepositoryException {
		TypedQuery<Long> query = entityManager.createQuery(
				"SELECT count(cbb.tenantId) FROM CleansedBallotBox cbb " + "WHERE cbb.tenantId = :tenantId "
						+ "AND cbb.electionEventId = :electionEventId " + "AND cbb.ballotBoxId = :ballotBoxId", Long.class);
		query.setParameter(PARAMETER_TENANT_ID, ballotBoxId.getTenantId());
		query.setParameter(PARAMETER_ELECTION_EVENT_ID, ballotBoxId.getElectionEventId());
		query.setParameter(PARAMETER_BALLOT_BOX_ID, ballotBoxId.getId());

		long result = query.getSingleResult();
		if (result > Integer.MAX_VALUE) {
			throw new CleansedBallotBoxRepositoryException("Too many votes! The application-imposed limit is Integer.MAX_VALUE");
		}

		return query.getSingleResult().intValue();
	}

	@Override
	public boolean exists(final String electionEventId, final String ballotBoxId) {
		TypedQuery<Long> query = entityManager.createQuery(
				"SELECT count(*) FROM CleansedBallotBox cbb " + "WHERE cbb.electionEventId = :electionEventId "
						+ "AND cbb.ballotBoxId = :ballotBoxId " + "AND rownum = 1", Long.class);
		query.setParameter(PARAMETER_ELECTION_EVENT_ID, electionEventId);
		query.setParameter(PARAMETER_BALLOT_BOX_ID, ballotBoxId);

		return query.getSingleResult() == 1;
	}

	@Override
	public Stream<EncryptedVote> getVoteSet(BallotBoxId ballotBoxId, int offset, int pageSize) {
		String voteSetSQL = "SELECT cbb.encryptedVote FROM CleansedBallotBox cbb " + "WHERE cbb.tenantId = :tenantId "
				+ "AND cbb.electionEventId = :electionEventId " + "AND cbb.ballotBoxId = :ballotBoxId " + "ORDER BY cbb.votingCardId ";
		return entityManager.createQuery(voteSetSQL, String.class).setParameter(PARAMETER_TENANT_ID, ballotBoxId.getTenantId())
				.setParameter(PARAMETER_ELECTION_EVENT_ID, ballotBoxId.getElectionEventId())
				.setParameter(PARAMETER_BALLOT_BOX_ID, ballotBoxId.getId()).setFirstResult(offset).setMaxResults(pageSize).getResultList().stream()
				.map(StringEncryptedVote::new);
	}
}
