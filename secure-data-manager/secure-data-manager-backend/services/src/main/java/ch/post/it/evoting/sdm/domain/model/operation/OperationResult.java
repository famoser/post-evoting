/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.operation;

public class OperationResult {

	private int error;
	private String message;
	private String exception;

	public int getError() {
		return error;
	}

	public void setError(int errorCode) {
		this.error = errorCode;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getException() {
		return exception;
	}

	public void setException(String exceptionName) {
		this.exception = exceptionName;
	}

}
