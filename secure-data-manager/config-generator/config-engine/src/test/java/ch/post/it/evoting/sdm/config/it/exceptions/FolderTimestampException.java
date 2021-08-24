/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.it.exceptions;

import ch.post.it.evoting.sdm.config.exceptions.ConfigurationEngineException;

public class FolderTimestampException extends ConfigurationEngineException {

	private static final long serialVersionUID = -8547764137504103870L;

	public FolderTimestampException(final String message) {
		super(message);
	}

	public FolderTimestampException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public FolderTimestampException(final Throwable cause) {
		super(cause);
	}

}
