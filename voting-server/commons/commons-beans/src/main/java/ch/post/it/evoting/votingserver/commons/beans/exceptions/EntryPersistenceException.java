/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.beans.exceptions;

/**
 * Exception class for handling a errors when persisting an entry in database.
 */
public class EntryPersistenceException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 636897026027663656L;

	// resource which provokes the exception
	private String resource;

	// error code of the exception
	private String errorCode;

	/**
	 * Constructs a new exception with the specified detail message.
	 *
	 * @param message the detail message.
	 */
	public EntryPersistenceException(String message) {
		super(message);
	}

	/**
	 * Constructs a new exception with the specified detail message and cause.
	 *
	 * @param message the detail message.
	 * @param cause   the cause of the exception.
	 */
	public EntryPersistenceException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new exception with the specified detail message, resource and errorCode.
	 *
	 * @param message   The detail message.
	 * @param resource  The resource which has provoked the exception.
	 * @param errorCode The error code of the exception.
	 */
	public EntryPersistenceException(String message, String resource, String errorCode) {
		super(message);
		this.resource = resource;
		this.errorCode = errorCode;
	}

	/**
	 * Gets the value of field resource.
	 *
	 * @return the resource.
	 */
	public String getResource() {
		return resource;
	}

	/**
	 * gets the value of field errorCode.
	 *
	 * @return the field error Code
	 */
	public String getErrorCode() {
		return errorCode;
	}
}
