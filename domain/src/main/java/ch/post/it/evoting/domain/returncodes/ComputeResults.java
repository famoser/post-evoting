/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.returncodes;

/**
 * Class that contains all computation results of choice codes for the vote and the ballot casting key (aka. vote cast code)
 */
public class ComputeResults {

	private String computationResults;

	private String decryptionResults;

	public String getComputationResults() {
		return computationResults;
	}

	public void setComputationResults(String computationResults) {
		this.computationResults = computationResults;
	}

	public String getDecryptionResults() {
		return decryptionResults;
	}

	public void setDecryptionResults(String decryptionResults) {
		this.decryptionResults = decryptionResults;
	}

}


