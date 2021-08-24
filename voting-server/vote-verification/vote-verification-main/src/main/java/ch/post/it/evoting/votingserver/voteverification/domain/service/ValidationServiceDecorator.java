/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.domain.service;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationResult;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationException;

/**
 * Decorator of the ballot box information repository.
 */
@Decorator
public abstract class ValidationServiceDecorator implements ValidationService {

	@Inject
	@Delegate
	private ValidationService validationService;

	/**
	 * @see ValidationService#validate(Vote)
	 */
	@Override
	public ValidationResult validate(Vote vote) throws ApplicationException {
		return validationService.validate(vote);
	}
}
