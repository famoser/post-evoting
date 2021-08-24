/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.config;

import ch.post.it.evoting.sdm.domain.common.GenericJobStatus;
import ch.post.it.evoting.sdm.domain.common.JobProgressDetails;
import ch.post.it.evoting.sdm.domain.common.JobStatus;

public class VotingCardGenerationJobStatus extends GenericJobStatus {

	private String verificationCardSetId;
	private int generatedCount;
	private int errorCount;

	protected VotingCardGenerationJobStatus() {
	}

	public VotingCardGenerationJobStatus(final String jobId) {
		super(jobId, JobStatus.UNKNOWN, "", JobProgressDetails.UNKNOWN);
	}

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	protected void setVerificationCardSetId(final String verificationCardSetId) {
		this.verificationCardSetId = verificationCardSetId;
	}

	public int getGeneratedCount() {
		return generatedCount;
	}

	protected void setGeneratedCount(final int generatedCount) {
		this.generatedCount = generatedCount;
	}

	public int getErrorCount() {
		return errorCount;
	}

	protected void setErrorCount(final int errorCount) {
		this.errorCount = errorCount;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("{");
		sb.append("status=").append(getStatus());
		sb.append(", statusDetails=").append(getStatusDetails());
		sb.append(", verificationCardSetId=").append(verificationCardSetId);
		sb.append(", generatedCount=").append(generatedCount);
		sb.append(", errorCount=").append(errorCount);
		sb.append('}');
		return sb.toString();
	}
}
