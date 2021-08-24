/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.exception;

public class ConsistencyCheckException extends Exception {

	private static final long serialVersionUID = 4330290741872284503L;

	public ConsistencyCheckException(String msg) {
		super(msg);
	}

	public ConsistencyCheckException(Throwable t) {
		super(t);
	}

	public ConsistencyCheckException(String msg, Throwable t) {
		super(msg, t);
	}
}
