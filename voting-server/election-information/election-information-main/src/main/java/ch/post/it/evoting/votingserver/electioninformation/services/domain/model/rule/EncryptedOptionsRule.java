/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.rule;

import java.math.BigInteger;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.crypto.Constants;
import ch.post.it.evoting.votingserver.commons.crypto.Utils;
import ch.post.it.evoting.votingserver.commons.domain.model.rule.AbstractRule;

/**
 * Validate if the encrypted voting options are two big integers.
 */
public class EncryptedOptionsRule implements AbstractRule<Vote> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EncryptedOptionsRule.class);

	/**
	 * @see AbstractRule#execute(Object)
	 */
	@Override
	public ValidationError execute(Vote vote) {
		ValidationError result = new ValidationError();
		if (vote.getEncryptedOptions() != null) {
			try {
				String[] splitedEncryptedOptions = vote.getEncryptedOptions().split(Constants.SEPARATOR_ENCRYPTED_OPTIONS);
				if (splitedEncryptedOptions.length == 2) {
					new BigInteger(Utils.getC0FromEncryptedOptions(vote.getEncryptedOptions()));
					new BigInteger(Utils.getC1FromEncryptedOptions(vote.getEncryptedOptions()));
					result.setValidationErrorType(ValidationErrorType.SUCCESS);
				} else {
					result.setValidationErrorType(ValidationErrorType.FAILED);
					result.setErrorArgs(new String[] { "Incorrect number of encrypted options" });
				}
			} catch (PatternSyntaxException | NumberFormatException e) {
				LOGGER.error("Error validating encrypted options: ", e);
				result.setValidationErrorType(ValidationErrorType.FAILED);
				result.setErrorArgs(new String[] { "Error validating encrypted options " + ExceptionUtils.getRootCauseMessage(e) });
			}
		} else {
			result.setValidationErrorType(ValidationErrorType.FAILED);
			result.setErrorArgs(new String[] { "There are not encrypted options in the vote" });
		}
		return result;
	}

	/**
	 * @see AbstractRule#getName()
	 */
	@Override
	public String getName() {
		return RuleNames.VOTE_OPTIONS_BIG_INTEGERS.getText();
	}

}
