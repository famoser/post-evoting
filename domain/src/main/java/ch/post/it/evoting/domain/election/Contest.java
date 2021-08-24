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
 * Encapsulates the information related to an election.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Contest {

	private final String id;

	private final String defaultTitle;

	private final String defaultDescription;

	private final String alias;

	private final String template;

	private final boolean fullBlank;

	private final List<ElectionOption> options;

	private final List<ElectionAttributes> attributes;

	private final List<Question> questions;

	@JsonCreator
	public Contest(
			@JsonProperty("id")
			final String id,
			@JsonProperty("defaultTitle")
			final String defaultTitle,
			@JsonProperty("defaultDescription")
			final String defaultDescription,
			@JsonProperty("alias")
			final String alias,
			@JsonProperty("template")
			final String template,
			@JsonProperty("fullBlank")
			final String fullBlank,
			@JsonProperty("options")
			final List<ElectionOption> options,
			@JsonProperty("attributes")
			final List<ElectionAttributes> attributes,
			@JsonProperty("questions")
			final List<Question> questions) {
		super();
		this.id = id;
		this.defaultTitle = defaultTitle;
		this.defaultDescription = defaultDescription;
		this.alias = alias;
		this.template = template;
		this.fullBlank = Boolean.parseBoolean(fullBlank);
		this.options = options;
		this.attributes = attributes;
		this.questions = questions;
	}

	@JsonGetter("id")
	public String getId() {
		return id;
	}

	@JsonGetter("defaultTitle")
	public String getDefaultTitle() {
		return defaultTitle;
	}

	@JsonGetter("defaultDescription")
	public String getDefaultDescription() {
		return defaultDescription;
	}

	@JsonGetter("alias")
	public String getAlias() {
		return alias;
	}

	@JsonGetter("template")
	public String getTemplate() {
		return template;
	}

	@JsonGetter("fullBlank")
	public boolean isFullBlank() {
		return fullBlank;
	}

	@JsonGetter("options")
	public List<ElectionOption> getOptions() {
		return options;
	}

	@JsonGetter("attributes")
	public List<ElectionAttributes> getAttributes() {
		return attributes;
	}

	@JsonGetter("questions")
	public List<Question> getQuestions() {
		return questions;
	}

}
