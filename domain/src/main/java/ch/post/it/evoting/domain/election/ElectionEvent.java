/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Encapsulates the id of an Election Event.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ElectionEvent {

	private final String id;

	@JsonCreator
	public ElectionEvent(
			@JsonProperty("id")
			final String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
}
