/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationException;
import ch.post.it.evoting.votingserver.commons.domain.model.rule.AbstractRule;
import ch.post.it.evoting.votingserver.commons.logging.service.I18nLoggerMessages;

/**
 * Handler of the execution of rules.
 *
 * @param <T> - the generic type of the object which is subject to be validated by a set of rules.
 */
public class RuleExecutor<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(RuleExecutor.class);

	private static final I18nLoggerMessages I18N = I18nLoggerMessages.getInstance();

	/**
	 * Applies the rules from the given list to the given object.
	 *
	 * @param rulesList The rules to be applied.
	 * @param object    The object to which the rules are applied.
	 * @return a ValidationResult containing information about the result of execution of rule.
	 * @throws ApplicationException in case the given object is null.
	 */
	public ValidationError execute(Iterable<AbstractRule<T>> rulesList, T object) throws ApplicationException {
		if (object == null) {
			LOGGER.error(I18N.getMessage("RuleExecutorImpl.execute.objectNull"));
			throw new ApplicationException("Object to validate is null!");
		}

		ValidationError result = new ValidationError();
		result.setValidationErrorType(ValidationErrorType.SUCCESS);
		if (rulesList == null) {
			LOGGER.info(I18N.getMessage("RuleExecutorImpl.execute.ruleListNull"));
			return result;
		}

		for (AbstractRule<T> rule : rulesList) {
			LOGGER.info(I18N.getMessage("RuleExecutorImpl.execute.executingRule"), rule.getName());
			result = rule.execute(object);
			if (!result.getValidationErrorType().equals(ValidationErrorType.SUCCESS)) {
				break;
			}
		}
		return result;
	}
}
