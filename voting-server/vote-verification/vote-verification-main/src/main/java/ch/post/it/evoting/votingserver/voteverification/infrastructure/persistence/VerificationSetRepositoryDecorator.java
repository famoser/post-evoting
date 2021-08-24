/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.infrastructure.persistence;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.DuplicateEntryException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetEntity;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetRepository;

/**
 * Implementation of the repository with JPA
 */
@Decorator
public abstract class VerificationSetRepositoryDecorator implements VerificationSetRepository {

	@Inject
	@Delegate
	private VerificationSetRepository verificationSetRepository;

	@Override
	public VerificationSetEntity save(final VerificationSetEntity entity) throws DuplicateEntryException {
		return verificationSetRepository.save(entity);
	}

	@Override
	public VerificationSetEntity findByTenantIdElectionEventIdVerificationCardSetId(final String tenantId, final String electionEventId,
			final String verificationCardSetId) throws ResourceNotFoundException {
		return verificationSetRepository.findByTenantIdElectionEventIdVerificationCardSetId(tenantId, electionEventId, verificationCardSetId);
	}
}
