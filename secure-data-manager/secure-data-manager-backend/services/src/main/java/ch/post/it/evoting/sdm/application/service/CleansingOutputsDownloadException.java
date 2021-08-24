/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

public class CleansingOutputsDownloadException extends Exception {

	public CleansingOutputsDownloadException(String message) {
		super(message);
	}

	public CleansingOutputsDownloadException(String message, Throwable cause) {
		super(message, cause);
	}
}
