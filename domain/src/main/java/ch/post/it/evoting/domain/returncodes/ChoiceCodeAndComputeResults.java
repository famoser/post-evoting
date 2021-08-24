/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.returncodes;

public class ChoiceCodeAndComputeResults extends ComputeResults {

	/**
	 * The choice codes.
	 */
	private String choiceCodes;

	/**
	 * Returns the current value of the field choiceCodes.
	 *
	 * @return Returns the choiceCodes.
	 */
	public String getChoiceCodes() {
		return choiceCodes;
	}

	/**
	 * Sets the value of the field choiceCodes.
	 *
	 * @param choiceCodes The choiceCodes to set.
	 */
	public void setChoiceCodes(String choiceCodes) {
		this.choiceCodes = choiceCodes;
	}

}


