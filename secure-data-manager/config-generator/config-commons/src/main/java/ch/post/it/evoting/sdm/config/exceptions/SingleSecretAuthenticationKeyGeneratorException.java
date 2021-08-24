/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.exceptions;

public class SingleSecretAuthenticationKeyGeneratorException extends ConfigurationEngineException {

	private static final long serialVersionUID = 6847341172518146890L;

	public SingleSecretAuthenticationKeyGeneratorException(final String message) {
		super(message);
	}

	public SingleSecretAuthenticationKeyGeneratorException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public SingleSecretAuthenticationKeyGeneratorException(final Throwable cause) {
		super(cause);
	}

}
