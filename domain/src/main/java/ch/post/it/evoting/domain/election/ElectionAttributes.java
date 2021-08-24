/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Contains a list with the election option attributes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ElectionAttributes {

	private final String id;

	private final String alias;

	private final List<String> related;

	private final boolean correctness;

	@JsonCreator
	public ElectionAttributes(
			@JsonProperty("id")
			final String id,
			@JsonProperty("alias")
			final String alias,
			@JsonProperty("related")
			final List<String> related,
			@JsonProperty("correctness")
			final boolean correctness) {
		this.id = id;
		this.alias = alias;
		this.related = related;
		this.correctness = correctness;
	}

	public String getId() {
		return this.id;
	}

	public boolean isCorrectness() {
		return this.correctness;
	}

	@JsonGetter("alias")
	public String getAlias() {
		return alias;
	}

	public List<String> getRelated() {
		return related;
	}

}
