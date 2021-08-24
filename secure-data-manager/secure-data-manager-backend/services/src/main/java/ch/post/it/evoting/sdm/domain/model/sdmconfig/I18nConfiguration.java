/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.sdmconfig;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class I18nConfiguration {

	private final List<Language> supportedLanguages = new ArrayList<>();
	private Language defaultLanguage;

	public void addLanguage(final Language language, boolean isDefault) {
		if (!languageExists(language)) {
			supportedLanguages.add(language);
		}

		if (isDefault) {
			defaultLanguage = language;
		}
	}

	private boolean languageExists(Language language) {
		return supportedLanguages.stream().anyMatch(l -> l.getCode().equals(language.getCode()));
	}

	@JsonProperty("default")
	public String getDefaultLanguage() {
		return defaultLanguage.getCode();
	}

	@JsonProperty("languages")
	public List<Language> getSupportedLanguages() {
		return supportedLanguages;
	}
}
