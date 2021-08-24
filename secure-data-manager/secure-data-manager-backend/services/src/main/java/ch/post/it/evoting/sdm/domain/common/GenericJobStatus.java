/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.common;

import java.util.UUID;

public class GenericJobStatus {

	public static final GenericJobStatus UNKNOWN = new GenericJobStatus(new UUID(0, 0).toString(), JobStatus.UNKNOWN, "", JobProgressDetails.UNKNOWN);

	private String jobId;
	private JobStatus status;
	private String statusDetails;
	private JobProgressDetails progressDetails;

	protected GenericJobStatus() {
	}

	public GenericJobStatus(final String jobId, final JobStatus jobStatus, final String statusDetails, final JobProgressDetails progressDetails) {

		this.jobId = jobId;
		this.status = jobStatus;
		this.statusDetails = statusDetails;
		this.progressDetails = progressDetails;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(final String jobId) {
		this.jobId = jobId;
	}

	public JobStatus getStatus() {
		return status;
	}

	public void setStatus(final JobStatus status) {
		this.status = status;
	}

	public JobProgressDetails getProgressDetails() {
		return progressDetails;
	}

	public void setProgressDetails(final JobProgressDetails progressDetails) {
		this.progressDetails = progressDetails;
	}

	public String getStatusDetails() {
		return statusDetails;
	}

	public void setStatusDetails(final String statusDetails) {
		this.statusDetails = statusDetails;
	}
}
