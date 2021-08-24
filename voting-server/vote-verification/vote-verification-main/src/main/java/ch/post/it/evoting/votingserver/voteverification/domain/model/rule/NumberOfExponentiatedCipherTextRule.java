/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.domain.model.rule;

import org.apache.commons.lang3.StringUtils;

import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.crypto.Utils;
import ch.post.it.evoting.votingserver.commons.domain.model.rule.AbstractRule;

public class NumberOfExponentiatedCipherTextRule implements AbstractRule<Vote> {

	/**
	 * This method validates from the given vote that the number of encrypted options and the number
	 * of the cipher text exponentations have the same number of elements
	 *
	 * @see AbstractRule#execute(Object)
	 */
	@Override
	public ValidationError execute(Vote vote) {
		ValidationError result = new ValidationError();

		if (!StringUtils.isEmpty(vote.getEncryptedOptions()) && !StringUtils.isEmpty(vote.getCipherTextExponentiations())) {
			String[] cipherTextElements = Utils.getCiphertextElementsFromEncryptedOptions(vote.getEncryptedOptions());
			String[] cipherTextExponentiations = Utils.getCiphertextElementsFromEncryptedOptions(vote.getCipherTextExponentiations());

			if (cipherTextElements.length != cipherTextExponentiations.length) {
				result.setValidationErrorType(ValidationErrorType.INVALID_NUMER_COMPONENTS_EXPONENTIATED_CIPHER_TEXT);
			} else {
				result.setValidationErrorType(ValidationErrorType.SUCCESS);
			}
		} else {
			result.setValidationErrorType(ValidationErrorType.FAILED);
			result.setErrorArgs(new String[] { "There are not encrypted options or exponentiations in the vote" });
		}

		return result;
	}

	/**
	 * @see AbstractRule#getName()
	 */
	@Override
	public String getName() {
		return RuleNames.VOTE_EXPONENTIATED_CIPHER_TEXT.getText();
	}
}
