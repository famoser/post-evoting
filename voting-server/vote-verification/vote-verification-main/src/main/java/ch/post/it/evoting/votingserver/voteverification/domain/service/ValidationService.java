/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.domain.service;

import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationResult;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationException;

/**
 *
 */
public interface ValidationService {

	/**
	 * This method validates the given vote using all the rules that exist for a vote.
	 *
	 * @param vote to be validated.
	 * @return a ValidationResult containing the result and the list of the rules that failed if any.
	 * @throws ApplicationException if the execution of the rules fails.
	 */
	ValidationResult validate(Vote vote) throws ApplicationException;

}
