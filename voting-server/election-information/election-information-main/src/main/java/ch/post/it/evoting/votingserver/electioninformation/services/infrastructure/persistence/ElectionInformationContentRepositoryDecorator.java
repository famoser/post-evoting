/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.persistence;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.DuplicateEntryException;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionInformationContent;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionInformationContentRepository;

/**
 * Decorator for the ballottext repository.
 */
@Decorator
public abstract class ElectionInformationContentRepositoryDecorator implements ElectionInformationContentRepository {

	@Inject
	@Delegate
	private ElectionInformationContentRepository electionInformationContentRepository;

	@Override
	public ElectionInformationContent save(final ElectionInformationContent entity) throws DuplicateEntryException {
		return electionInformationContentRepository.save(entity);
	}
}
