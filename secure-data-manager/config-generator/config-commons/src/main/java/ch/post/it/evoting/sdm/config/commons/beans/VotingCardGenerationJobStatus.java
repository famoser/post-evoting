/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commons.beans;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import ch.post.it.evoting.sdm.config.commons.progress.JobProgressDetails;

public class VotingCardGenerationJobStatus {

	public static final VotingCardGenerationJobStatus UNKNOWN = new VotingCardGenerationJobStatus(new UUID(0, 0), "UNKNOWN", Instant.EPOCH, null,
			JobProgressDetails.EMPTY, null, 0, 0);

	private final UUID jobId;
	private final String status;
	private final Instant startTime;
	private final String statusDetails;
	private final JobProgressDetails progressDetails;
	private final String verificationCardSetId;
	private final int generatedCount;
	private final int errorCount;

	public VotingCardGenerationJobStatus(final UUID jobId, final String status, final Instant startTime, final String statusDetails,
			final JobProgressDetails progressDetails, final String verificationCardSetId, final int generatedCount, final int errorCount) {

		this.jobId = jobId;
		this.status = status;
		this.startTime = startTime;
		this.statusDetails = statusDetails;
		this.progressDetails = progressDetails;
		this.verificationCardSetId = verificationCardSetId;
		this.generatedCount = generatedCount;
		this.errorCount = errorCount;
	}

	public UUID getJobId() {
		return jobId;
	}

	public String getStatus() {
		return status;
	}

	public String getStartTime() {
		return startTime.atZone(ZoneId.systemDefault()).toString();
	}

	public String getStatusDetails() {
		return statusDetails;
	}

	public JobProgressDetails getProgressDetails() {
		return progressDetails;
	}

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	public int getGeneratedCount() {
		return generatedCount;
	}

	public int getErrorCount() {
		return errorCount;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("{");
		sb.append("jobId=").append(jobId);
		sb.append(", status='").append(status).append('\'');
		sb.append(", startTime=").append(startTime);
		sb.append(", statusDetails='").append(statusDetails).append('\'');
		sb.append(", progressDetails=").append(progressDetails);
		sb.append(", verificationCardSetId='").append(verificationCardSetId).append('\'');
		sb.append(", generatedCount=").append(generatedCount);
		sb.append(", errorCount=").append(errorCount);
		sb.append('}');
		return sb.toString();
	}
}
