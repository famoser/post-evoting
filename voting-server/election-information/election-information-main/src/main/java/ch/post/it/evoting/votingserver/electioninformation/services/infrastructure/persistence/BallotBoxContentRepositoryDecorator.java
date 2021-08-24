/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.persistence;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.DuplicateEntryException;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxContent;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxContentRepository;

/**
 * Decorator of the ballot box content repository.
 */
@Decorator
public abstract class BallotBoxContentRepositoryDecorator implements BallotBoxContentRepository {

	@Inject
	@Delegate
	private BallotBoxContentRepository ballotBoxContentRepository;

	@Override
	public BallotBoxContent save(final BallotBoxContent ballotBoxContent) throws DuplicateEntryException {
		return ballotBoxContentRepository.save(ballotBoxContent);
	}
}
