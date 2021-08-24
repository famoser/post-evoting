/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.cc;

/**
 * An error condition while storing or retrieving a payload.
 */
public class PayloadStorageException extends Exception {

	public PayloadStorageException(Throwable cause) {
		super(cause);
	}
}
