/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.infrastructure.persistence;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.VerificationDerivedKeys;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.VerificationDerivedKeysRepository;

/**
 * Implementation of the repository with JPA
 */
@Decorator
public abstract class VerificationDerivedKeysRepositoryDecorator implements VerificationDerivedKeysRepository {

	@Inject
	@Delegate
	private VerificationDerivedKeysRepository verificationDerivedKeysRepository;

	@Override
	public VerificationDerivedKeys findByTenantIdElectionEventIdVerificationCardId(final String tenantId, final String electionEventId,
			final String verificationCardId) throws ResourceNotFoundException {
		return verificationDerivedKeysRepository.findByTenantIdElectionEventIdVerificationCardId(tenantId, electionEventId, verificationCardId);
	}
}
