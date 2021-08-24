/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Encapsulates the information related to an election option.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ElectionOption {

	private final String id;
	private final String attribute;
	private String representation;

	@JsonCreator
	public ElectionOption(
			@JsonProperty("id")
			final String id,
			@JsonProperty("representation")
			final String representation,
			@JsonProperty("attribute")
			final String attribute) {
		this.id = id;
		this.representation = representation;
		this.attribute = attribute;
	}

	public String getId() {
		return this.id;
	}

	public String getRepresentation() {
		return this.representation;
	}

	public void setRepresentation(final String representation) {
		this.representation = representation;
	}

	public boolean hasRepresentation() {
		return (representation != null) && (representation.length() > 0);
	}

	public String getAttribute() {
		return this.attribute;
	}

}
