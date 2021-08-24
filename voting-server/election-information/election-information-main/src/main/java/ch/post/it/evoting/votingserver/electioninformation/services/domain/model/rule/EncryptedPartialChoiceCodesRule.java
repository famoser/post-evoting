/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.rule;

import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.crypto.Constants;
import ch.post.it.evoting.votingserver.commons.domain.model.rule.AbstractRule;
import ch.post.it.evoting.votingserver.commons.util.JsonUtils;

/**
 * Validate if the number of allowed encrypted partial return codes depends on the ballot rules: for
 * votations, this number has to be equal to the number of questions in the ballot. For elections,
 * this number is equal to the number of candidates that can be selected in a list, plus one (for
 * the list identifier).
 */
public class EncryptedPartialChoiceCodesRule implements AbstractRule<Vote> {

	/**
	 * The separator used in the correctness ids string.
	 */
	public static final String SEPARATOR_ENCRYPTED_OPTIONS = ",";
	private static final Logger LOGGER = LoggerFactory.getLogger(EncryptedPartialChoiceCodesRule.class);

	/**
	 * @see AbstractRule#execute(Object)
	 */
	@Override
	public ValidationError execute(Vote vote) {
		ValidationError result = new ValidationError();
		if (StringUtils.isEmpty(vote.getEncryptedPartialChoiceCodes())) {
			result.setValidationErrorType(ValidationErrorType.FAILED);
			result.setErrorArgs(new String[] { "There are not encrypted partial choice codes in the vote" });
		} else if (StringUtils.isEmpty(vote.getCorrectnessIds())) {
			result.setValidationErrorType(ValidationErrorType.FAILED);
			result.setErrorArgs(new String[] { "There are no correctness attribute sets in the vote" });
		} else {
			try {
				String[] gammaAndPhis = vote.getEncryptedPartialChoiceCodes().split(Constants.SEPARATOR_ENCRYPTED_OPTIONS);
				int numberOfCorrectnessAttributeSets = JsonUtils.getJsonArray(vote.getCorrectnessIds()).size();
				if ((gammaAndPhis.length - 1) == numberOfCorrectnessAttributeSets) {
					result.setValidationErrorType(ValidationErrorType.SUCCESS);
				} else {
					result.setValidationErrorType(ValidationErrorType.FAILED);
					result.setErrorArgs(new String[] {
							"The number of encrypted partial choice codes (" + (gammaAndPhis.length - 1) + ") and correctness attribute sets ("
									+ numberOfCorrectnessAttributeSets + ") are different" });
				}
			} catch (PatternSyntaxException e) {
				LOGGER.error("Error validating encrypted partial choice codes: ", e);
				result.setValidationErrorType(ValidationErrorType.FAILED);
				result.setErrorArgs(new String[] { "Error validating encrypted partial choice codes " + ExceptionUtils.getRootCauseMessage(e) });
			}
		}
		return result;
	}

	/**
	 * @see AbstractRule#getName()
	 */
	@Override
	public String getName() {
		return RuleNames.VOTE_NUMBER_OF_PARTIAL_CHOICE_CODES.getText();
	}

}
