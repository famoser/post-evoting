/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

public class HashServiceException extends Exception {
	public HashServiceException(String message) {
		super(message);
	}

	public HashServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public HashServiceException(Throwable cause) {
		super(cause);
	}
}
