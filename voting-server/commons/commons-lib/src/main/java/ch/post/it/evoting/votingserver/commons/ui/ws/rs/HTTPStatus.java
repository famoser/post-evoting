/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.ui.ws.rs;

/**
 * Constants for HTTP Status, that are not defined in HTTP standard.
 */
public enum HTTPStatus {

	/**
	 * The 422 (Unprocessable Entity) status code means the server understands the content type of the request entity (hence a 415(Unsupported Media
	 * Type) status code is inappropriate), and the syntax of the request entity is correct (thus a 400 (Bad Request) status code is inappropriate)
	 * but was unable to process the contained instructions. For example, this error condition may occur if an XML request body contains well-formed
	 * (i.e., syntactically correct), but semantically erroneous, XML instructions.
	 */
	UNPROCESSABLE_ENTITY(422);

	/**
	 * The value of the http status.
	 */
	private final int value;

	/**
	 * To avoid instantiation.
	 */
	HTTPStatus(int value) {
		this.value = value;
	}

	/**
	 * Returns the current value of the field value.
	 *
	 * @return Returns the value.
	 */
	public int getValue() {
		return value;
	}
}
