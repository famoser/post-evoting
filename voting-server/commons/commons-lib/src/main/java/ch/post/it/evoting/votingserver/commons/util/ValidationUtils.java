/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.util;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import ch.post.it.evoting.domain.election.errors.SemanticErrorGroup;
import ch.post.it.evoting.domain.election.errors.SyntaxErrorGroup;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.SemanticErrorException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.SyntaxErrorException;

/**
 * Class used to provide validation utilities.
 */
public final class ValidationUtils {

	private ValidationUtils() {
		// empty constructor.
	}

	/**
	 * Applies javax.validations in a programmatically way by applying the set of validations defined in the class using annotations like @NotNull,
	 *
	 * @param object - the object to be validated.
	 * @throws ch.post.it.evoting.votingserver.commons.beans.exceptions.SemanticErrorException if the object does not pass validations. This exception
	 *                                                                                         is handled by ValidationExceptionHandler and returns a
	 *                                                                                         status code 422 for unprocessable entity.
	 * @throws ch.post.it.evoting.votingserver.commons.beans.exceptions.SyntaxErrorException
	 */
	public static <T> void validate(T object) throws SyntaxErrorException, SemanticErrorException {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();
		Set<ConstraintViolation<Object>> syntaxErrors = validator.validate(object, SyntaxErrorGroup.class);
		if (!syntaxErrors.isEmpty()) {
			throw new SyntaxErrorException(syntaxErrors);
		}

		Set<ConstraintViolation<Object>> semanticErrors = validator.validate(object, SemanticErrorGroup.class);
		if (!semanticErrors.isEmpty()) {
			throw new SemanticErrorException(semanticErrors);
		}
	}
}
