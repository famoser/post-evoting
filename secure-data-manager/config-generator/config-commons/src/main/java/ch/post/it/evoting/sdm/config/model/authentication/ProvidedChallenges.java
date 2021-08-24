/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.model.authentication;

import java.util.List;

public class ProvidedChallenges {

	private final String alias;

	private final List<String> challenges;

	public ProvidedChallenges(final String alias, final List<String> challenges) {
		this.alias = alias;
		this.challenges = challenges;
	}

	public String getAlias() {
		return alias;
	}

	public List<String> getChallenges() {
		return challenges;
	}
}
