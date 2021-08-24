/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

public class AdminBoardServiceException extends RuntimeException {

	private static final long serialVersionUID = -5601384812692612498L;

	public AdminBoardServiceException(Throwable cause) {
		super(cause);
	}

	public AdminBoardServiceException(String message) {
		super(message);
	}

	public AdminBoardServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}
