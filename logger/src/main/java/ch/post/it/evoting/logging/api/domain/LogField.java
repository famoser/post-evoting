/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.logging.api.domain;

public enum LogField {

	OBJECTTYPE("ObjectType"),
	OBJECTID("ObjectId"),
	USER("User"),
	ELECTIONEVENT("ElectionEvent");

	private final String fieldName;

	LogField(String name) {
		fieldName = name;
	}

	public String getFieldName() {
		return fieldName;
	}
}
