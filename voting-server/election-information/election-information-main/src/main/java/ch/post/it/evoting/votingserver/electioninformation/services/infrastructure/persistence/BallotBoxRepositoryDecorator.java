/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.persistence;

import java.util.List;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.DuplicateEntryException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBox;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.ExportedBallotBoxItem;

/**
 * Decorator of the ballot box repository.
 */
@Decorator
public abstract class BallotBoxRepositoryDecorator implements BallotBoxRepository {

	@Inject
	@Delegate
	private BallotBoxRepository ballotBoxRepository;

	@Override
	public BallotBox save(BallotBox ballotBox) throws DuplicateEntryException {
		return ballotBoxRepository.save(ballotBox);
	}

	/**
	 * @see BallotBoxRepository#findByTenantIdElectionEventIdVotingCardId(String, String, String)
	 */
	@Override
	public BallotBox findByTenantIdElectionEventIdVotingCardId(String tenantId, String electionEventId, String votingCardId)
			throws ResourceNotFoundException {
		return ballotBoxRepository.findByTenantIdElectionEventIdVotingCardId(tenantId, electionEventId, votingCardId);
	}

	@Override
	public List<ExportedBallotBoxItem> getEncryptedVotesByTenantIdElectionEventIdBallotBoxId(final String tenantId, final String electionEventId,
			final String ballotBoxId, final int firstElement, final int lastElement) {
		return ballotBoxRepository
				.getEncryptedVotesByTenantIdElectionEventIdBallotBoxId(tenantId, electionEventId, ballotBoxId, firstElement, lastElement);
	}

	/**
	 * @see BallotBoxRepository#findByTenantIdElectionEventIdBallotBoxId(String, String, String)
	 */
	@Override
	public List<BallotBox> findByTenantIdElectionEventIdBallotBoxId(String tenantId, String electionEventId, String ballotBoxId) {
		return ballotBoxRepository.findByTenantIdElectionEventIdBallotBoxId(tenantId, electionEventId, ballotBoxId);
	}

}
