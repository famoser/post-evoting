/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.persistence;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.slf4j.Logger;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.DuplicateEntryException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.infrastructure.persistence.BaseRepositoryImpl;
import ch.post.it.evoting.votingserver.commons.logging.service.I18nLoggerMessages;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.castcode.VoteCastCode;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.castcode.VoteCastCodeRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.votingcard.VotingCard;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.votingcard.VotingCardWriter;

/**
 * Implementation of VoteCastCodeRepository.
 */
@Stateless
public class VoteCastCodeRepositoryImpl extends BaseRepositoryImpl<VoteCastCode, Integer> implements VoteCastCodeRepository {
	private static final I18nLoggerMessages I18N = I18nLoggerMessages.getInstance();

	// The name of the parameter which identifies the tenantId
	private static final String PARAMETER_TENANT_ID = "tenantId";

	// The name of the parameter which identifies the electionEventId
	private static final String PARAMETER_ELECTION_EVENT_ID = "electionEventId";

	// The name of the parameter which identifies the voting card id
	private static final String PARAMETER_VOTING_CARD_ID = "votingCardId";

	@Inject
	private Logger logger;

	/**
	 * Searches for a vote cast code with the given tenant, election event and voting card id. This implementation uses database access by executing a
	 * SQL-query to select the data to be retrieved.
	 *
	 * @param tenantId        - the identifier of the tenant.
	 * @param electionEventId - the identifier of the election event.
	 * @param votingCardId    - the voting card identifier.
	 * @return a entity representing the vote cast code.
	 */
	@Override
	public VoteCastCode findByTenantIdElectionEventIdVotingCardId(String tenantId, String electionEventId, String votingCardId)
			throws ResourceNotFoundException {
		TypedQuery<VoteCastCode> query = entityManager.createQuery(
				"SELECT vcc FROM VoteCastCode vcc WHERE vcc.tenantId = :tenantId AND vcc.electionEventId = :electionEventId AND vcc.votingCardId = :votingCardId",
				VoteCastCode.class);
		query.setParameter(PARAMETER_TENANT_ID, tenantId);
		query.setParameter(PARAMETER_ELECTION_EVENT_ID, electionEventId);
		query.setParameter(PARAMETER_VOTING_CARD_ID, votingCardId);
		try {
			return query.getSingleResult();
		} catch (NoResultException e) {
			throw new ResourceNotFoundException("", e);
		}
	}

	/**
	 * Stores a vote cast code.
	 *
	 * @param tenantId        - the identifier of the tenant id.
	 * @param electionEventId - the identifier of the election event id.
	 * @param votingCardId    - the voting card id.
	 * @param voteCastCode    - the vote cast code.
	 * @throws DuplicateEntryException if the object exists for the given tenant, election event and voting card.
	 */
	@Override
	public void save(String tenantId, String electionEventId, String votingCardId, VoteCastCode voteCastCode) throws DuplicateEntryException {
		// set some values to object
		voteCastCode.setTenantId(tenantId);
		voteCastCode.setElectionEventId(electionEventId);
		voteCastCode.setVotingCardId(votingCardId);
		super.save(voteCastCode);
	}

	/**
	 * Find and write voting cards that had been casted.
	 *
	 * @param tenantId        the tenant id
	 * @param electionEventId the election event id
	 * @param writer          the writer
	 */
	@Override
	public void findAndWriteCastVotingCards(String tenantId, String electionEventId, VotingCardWriter writer) {

		final Session session = entityManager.unwrap(Session.class);
		final Query query = session.createQuery("SELECT vcc.votingCardId FROM VoteCastCode vcc " + "WHERE vcc.tenantId = ? "
				+ "AND vcc.electionEventId = ? ORDER BY vcc.votingCardId");
		query.setParameter(0, tenantId);
		query.setParameter(1, electionEventId);
		query.setFetchSize(1000);
		query.setReadOnly(true);
		query.setLockMode("vcc", LockMode.NONE);
		ScrollableResults results = query.scroll(ScrollMode.FORWARD_ONLY);

		try {
			while (results.next()) {
				writer.write(new VotingCard((String) results.get(0)));
			}
			results.close();
		} catch (NoResultException nrE) {
			logger.info(I18N.getMessage("VoteCastCodeRepositoryImpl.findAndWriteCastVotingCards.noResult"), tenantId, electionEventId, nrE);
		}
	}

}
