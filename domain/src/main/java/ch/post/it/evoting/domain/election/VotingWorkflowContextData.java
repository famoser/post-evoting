/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election;

public class VotingWorkflowContextData {

	private String maxNumberOfAttempts;

	public String getMaxNumberOfAttempts() {
		return maxNumberOfAttempts;
	}

	public void setMaxNumberOfAttempts(final String maxNumberOfAttempts) {
		this.maxNumberOfAttempts = maxNumberOfAttempts;
	}
}
