/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.services;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChoiceCodesKeysResponse {

	private Map<String, List<String>> choiceCodesGenerationKeys;

	private Map<String, List<String>> choiceCodesDecryptionKeys;

	private List<String> errors;

	public Map<String, List<String>> getChoiceCodesGenerationKeys() {
		return choiceCodesGenerationKeys;
	}

	public void setChoiceCodesGenerationKeys(Map<String, List<String>> choiceCodesGenerationKeys) {
		this.choiceCodesGenerationKeys = choiceCodesGenerationKeys;
	}

	public Map<String, List<String>> getChoiceCodesDecryptionKeys() {
		return choiceCodesDecryptionKeys;
	}

	public void setChoiceCodesDecryptionKeys(Map<String, List<String>> choiceCodesDecryptionKeys) {
		this.choiceCodesDecryptionKeys = choiceCodesDecryptionKeys;
	}

	/**
	 * Gets the errors.
	 *
	 * @return the errors
	 */
	public List<String> getErrors() {
		return errors;
	}

	/**
	 * Sets the errors.
	 *
	 * @param errors the errors to set
	 */
	public void setErrors(final List<String> errors) {
		this.errors = errors;
	}
}
