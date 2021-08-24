/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.model.messaging;

/**
 * Implementation of the signature payload exception
 */
public class PayloadSignatureException extends Exception {

	private static final long serialVersionUID = -6738655844708081827L;

	public PayloadSignatureException(Throwable cause) {
		super("The payload could not be signed", cause);
	}

	public PayloadSignatureException(String cause) {
		super(cause);
	}
}
