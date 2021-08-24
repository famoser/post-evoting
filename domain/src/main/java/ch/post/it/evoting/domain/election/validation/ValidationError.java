/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.validation;

/**
 * A class for storing errors from vote validations.
 */
public class ValidationError {

	private ValidationErrorType validationErrorType = ValidationErrorType.FAILED;

	private String[] errorArgs;

	public ValidationError(ValidationErrorType validationErrorType) {
		this.validationErrorType = validationErrorType;
	}

	public ValidationError() {
	}

	/**
	 * Returns the current value of the field validationErrorType.
	 *
	 * @return Returns the validationErrorType.
	 */
	public ValidationErrorType getValidationErrorType() {
		return validationErrorType;
	}

	/**
	 * Sets the value of the field validationErrorType.
	 *
	 * @param validationErrorType The validationErrorType to set.
	 */
	public void setValidationErrorType(ValidationErrorType validationErrorType) {
		this.validationErrorType = validationErrorType;
	}

	/**
	 * Returns the current value of the field errorArgs.
	 *
	 * @return Returns the errorArgs.
	 */
	public String[] getErrorArgs() {
		return errorArgs;
	}

	/**
	 * Sets the value of the field errorArgs.
	 *
	 * @param errorArgs The errorArgs to set.
	 */
	public void setErrorArgs(String[] errorArgs) {
		this.errorArgs = errorArgs;
	}

}
