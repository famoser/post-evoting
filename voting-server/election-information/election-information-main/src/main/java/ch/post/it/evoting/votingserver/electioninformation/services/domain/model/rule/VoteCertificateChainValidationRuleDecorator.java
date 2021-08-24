/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.rule;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.votingserver.commons.domain.model.rule.AbstractRule;

/**
 * Decorator of the certificate chain validation rule.
 */
@Decorator
public class VoteCertificateChainValidationRuleDecorator implements AbstractRule<Vote> {

	@Inject
	@Delegate
	private VoteCertificateChainValidationRule voteCertificateChainValidationRule;

	/**
	 * @see AbstractRule#execute(Object)
	 */
	@Override
	public ValidationError execute(Vote vote) {
		return voteCertificateChainValidationRule.execute(vote);
	}

	/**
	 * @see AbstractRule#getName()
	 */
	@Override
	public String getName() {
		return voteCertificateChainValidationRule.getName();
	}

}
