/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.commons;

public class PrefixPathResolverException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PrefixPathResolverException(Throwable cause) {
		super(cause);
	}

	public PrefixPathResolverException(String message) {
		super(message);
	}

	public PrefixPathResolverException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
