/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.domain.model.rule;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.votingserver.commons.domain.model.rule.AbstractRule;

/**
 * Decorator of the encrypted options id rule.
 */
@Decorator
public class VerifyVerificationCardPKSignatureRuleDecorator implements AbstractRule<Vote> {

	@Inject
	@Delegate
	private VerifyVerificationCardPKSignatureRule encryptedPartialChoiceCodesRule;

	/**
	 * @see AbstractRule#execute(Object)
	 */
	@Override
	public ValidationError execute(Vote vote) {
		return encryptedPartialChoiceCodesRule.execute(vote);
	}

	/**
	 * @see AbstractRule#getName()
	 */
	@Override
	public String getName() {
		return encryptedPartialChoiceCodesRule.getName();
	}
}
