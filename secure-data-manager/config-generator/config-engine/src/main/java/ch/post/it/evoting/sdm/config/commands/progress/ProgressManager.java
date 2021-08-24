/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.progress;

import java.util.Optional;
import java.util.UUID;

import ch.post.it.evoting.sdm.config.commons.progress.JobProgressDetails;

public interface ProgressManager {

	void registerJob(UUID jobId, JobProgressDetails job);

	void unregisterJob(UUID jobId);

	Optional<JobProgressDetails> getJobProgress(UUID jobId);

	void updateProgress(UUID jobId, double deltaValue);
}
