/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.persistence;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.DuplicateEntryException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionPublicKey;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionPublicKeyRepository;

/**
 * Election Public Key repository decorator
 */
@Decorator
public abstract class ElectionPublicKeyRepositoryDecorator implements ElectionPublicKeyRepository {

	@Inject
	@Delegate
	private ElectionPublicKeyRepository electionPublicKeyRepository;

	@Override
	public ElectionPublicKey find(final Integer electionPublicKeyId) {
		return electionPublicKeyRepository.find(electionPublicKeyId);
	}

	@Override
	public ElectionPublicKey save(final ElectionPublicKey entity) throws DuplicateEntryException {
		return electionPublicKeyRepository.save(entity);
	}

	@Override
	public ElectionPublicKey findByTenantIdElectionEventIdElectoralAuthorityId(final String tenantId, final String electionEventId,
			final String electoralAuthorityId) throws ResourceNotFoundException {
		return electionPublicKeyRepository.findByTenantIdElectionEventIdElectoralAuthorityId(tenantId, electionEventId, electoralAuthorityId);
	}
}
