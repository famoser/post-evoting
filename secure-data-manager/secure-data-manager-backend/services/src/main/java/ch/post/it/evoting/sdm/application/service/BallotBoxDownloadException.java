/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

public class BallotBoxDownloadException extends Exception {

	public BallotBoxDownloadException(String message) {
		super(message);
	}

	public BallotBoxDownloadException(String message, Throwable cause) {
		super(message, cause);
	}
}
