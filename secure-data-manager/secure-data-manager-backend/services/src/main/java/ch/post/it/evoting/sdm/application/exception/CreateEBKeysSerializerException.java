/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.exception;

public class CreateEBKeysSerializerException extends RuntimeException {

	private static final long serialVersionUID = 4330090041872284503L;

	public CreateEBKeysSerializerException(Throwable cause) {
		super(cause);
	}

	public CreateEBKeysSerializerException(String message) {
		super(message);
	}

	public CreateEBKeysSerializerException(String message, Throwable cause) {
		super(message, cause);
	}
}
