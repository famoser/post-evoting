/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import javax.ejb.Local;
import javax.persistence.EntityManager;

import ch.post.it.evoting.domain.election.model.ballotbox.BallotBoxId;
import ch.post.it.evoting.domain.election.model.vote.EncryptedVote;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.CleansedBallotBoxRepositoryException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.domain.model.BaseRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.votingcard.VotingCardWriter;

/**
 * Provides operations on the cleansed ballot box repository.
 */
@Local
public interface CleansedBallotBoxRepository extends BaseRepository<CleansedBallotBox, Integer> {

	/**
	 * Returns a stored vote in a cleansed ballot box.
	 *
	 * @param tenantId        - the tenant id.
	 * @param electionEventId - the election event id.
	 * @param votingCardId    - the voting card id.
	 * @return the vote stored in the ballot box.
	 * @throws ResourceNotFoundException if the vote is not found.
	 */
	CleansedBallotBox findByTenantIdElectionEventIdVotingCardId(String tenantId, String electionEventId, String votingCardId)
			throws ResourceNotFoundException;

	/**
	 * Returns the list of encrypted votes in the cleansed ballot box identified by tenantId, electionEventId and ballotBoxId.
	 *
	 * @param tenantId        - the identifier of the tenant.
	 * @param electionEventId - the identifier of the electionEvent.
	 * @param ballotBoxId     - the identifier of the ballot box.
	 * @param firstElement    -the first element to extract
	 * @param lastElement     -the last element to extract
	 * @return a list of encrypted votes in the ballot box in csv format (each encrypted vote is separated by semicolon).
	 */
	List<CleansedExportedBallotBoxItem> getEncryptedVotesByTenantIdElectionEventIdBallotBoxId(String tenantId, String electionEventId,
			String ballotBoxId, int firstElement, int lastElement);

	/**
	 * Returns the list of encrypted votes in the cleansed ballot box identified by tenantId, electionEventId and ballotBoxId.
	 *
	 * @param tenantId        - the identifier of the tenant.
	 * @param electionEventId - the identifier of the electionEvent.
	 * @param ballotBoxId     - the identifier of the ballot box.
	 * @return a list of encrypted votes in the ballot box in csv format (each encrypted vote is separated by semicolon).
	 */
	String getEncryptedVotesByTenantIdElectionEventIdBallotBoxIdCSV(String tenantId, String electionEventId, String ballotBoxId) throws IOException;

	/**
	 * Returns a list of clenased ballot boxes for a tenant and election event.
	 *
	 * @param tenantId        - the identifier of the tenant.
	 * @param electionEventId - the identifier of the electionEvent.
	 * @param ballotBoxId     - the identifier of the ballot box.
	 * @return a list of ballot boxes for the given parameters.
	 */
	List<CleansedBallotBox> findByTenantIdElectionEventIdBallotBoxId(String tenantId, String electionEventId, String ballotBoxId);

	/**
	 * Returns a stored vote in a cleansed ballot box for a given tenant, election event, voting card and ballot.
	 *
	 * @param tenantId        - the identifier of the tenant.
	 * @param electionEventId - the identifier of the electionEvent.
	 * @param votingCardId    - the identifier of a voting card.
	 * @param ballotBoxId     - the identifier of the ballot box.
	 * @param ballotId        - the identifier of the ballot.
	 * @return the votes stored in the ballot box.
	 */
	List<CleansedBallotBox> findByTenantIdElectionEventIdVotingCardIdBallotBoxIdBallotId(String tenantId, String electionEventId, String votingCardId,
			String ballotBoxId, String ballotId);

	/**
	 * Execute persistence actions with a specific entityManager
	 *
	 * @param entityManager to be used
	 */
	void with(EntityManager entityManager);

	/**
	 * Finds all the VotingCards that have votes saved in ballotboxes and writes the id's into the specified writer
	 *
	 * @param tenantId        - the tenant identifier
	 * @param electionEventId - the election event identifier
	 * @param writer          - the writer to write the used voting cards
	 * @throws IOException
	 */
	void findAndWriteUsedVotingCards(String tenantId, String electionEventId, VotingCardWriter writer) throws IOException;

	/**
	 * @param ballotBoxId the ballot box to count votes from
	 * @return the number of votes in the specified ballot box
	 * @throws CleansedBallotBoxRepositoryException
	 */
	int count(BallotBoxId ballotBoxId) throws CleansedBallotBoxRepositoryException;

	/**
	 * Checks whether the given ballot box exists.
	 *
	 * @param electionEventId the election event id.
	 * @param ballotBoxId     the ballot box id.
	 * @return {@code true} if there is at least one vote for the ballot box, {@code false} otherwise.
	 */
	boolean exists(final String electionEventId, final String ballotBoxId);

	/**
	 * Get a range of encrypted votes from a ballot box, according to the vote set size.`
	 *
	 * @param ballotBoxId a reference to the ballot box with the desired votes
	 * @param offset      starting point of the vote set within the ballot box
	 * @param size        size of the vote set
	 * @return a stream with the requested votes
	 */
	Stream<EncryptedVote> getVoteSet(BallotBoxId ballotBoxId, int offset, int size);
}
