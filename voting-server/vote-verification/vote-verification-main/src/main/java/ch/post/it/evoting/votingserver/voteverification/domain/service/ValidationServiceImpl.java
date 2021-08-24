/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.domain.service;

import java.util.ArrayList;
import java.util.Collection;

import javax.ejb.Stateless;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.domain.election.validation.ValidationResult;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationException;
import ch.post.it.evoting.votingserver.commons.domain.model.rule.AbstractRule;
import ch.post.it.evoting.votingserver.commons.domain.service.RuleExecutor;

/**
 * This service handles the validations.
 */
@Stateless(name = "vv-validationService")
public class ValidationServiceImpl implements ValidationService {

	private final Collection<AbstractRule<Vote>> rules = new ArrayList<>();

	@Inject
	private RuleExecutor<Vote> ruleExecutor;

	@Inject
	@Any
	void setRules(Instance<AbstractRule<Vote>> instance) {
		for (AbstractRule<Vote> rule : instance) {
			rules.add(rule);
		}
	}

	@Override
	public ValidationResult validate(final Vote vote) throws ApplicationException {
		// execute the rules that exist
		ValidationError executorRulesResult = ruleExecutor.execute(rules, vote);
		ValidationResult validationResult = new ValidationResult();
		validationResult.setResult(executorRulesResult.getValidationErrorType().equals(ValidationErrorType.SUCCESS));
		validationResult.setValidationError(executorRulesResult);
		return validationResult;
	}

}
