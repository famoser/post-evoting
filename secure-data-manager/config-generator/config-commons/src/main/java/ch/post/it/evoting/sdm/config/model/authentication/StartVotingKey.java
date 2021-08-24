/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.model.authentication;

/**
 * Class Representing the SVK entity
 */
public class StartVotingKey {

	private final String value;

	private StartVotingKey(final String value) {
		this.value = value;
	}

	public static StartVotingKey ofValue(String value) {
		return new StartVotingKey(value);
	}

	public String getValue() {
		return value;
	}
}
