/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.progress;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.commons.progress.JobProgressDetails;

@Component
public class ProgressManagerImpl implements ProgressManager {

	private final ConcurrentMap<UUID, JobProgressDetails> jobMap = new ConcurrentHashMap<>();

	@Override
	public void registerJob(final UUID jobId, final JobProgressDetails job) {
		Objects.requireNonNull(jobId, Constants.JOB_ID_CANNOT_BE_NULL);
		jobMap.put(jobId, job);
	}

	@Override
	public void unregisterJob(final UUID jobId) {
		Objects.requireNonNull(jobId, Constants.JOB_ID_CANNOT_BE_NULL);
		jobMap.remove(jobId);
	}

	@Override
	public Optional<JobProgressDetails> getJobProgress(final UUID jobId) {
		Objects.requireNonNull(jobId, Constants.JOB_ID_CANNOT_BE_NULL);
		return Optional.ofNullable(jobMap.get(jobId));
	}

	@Override
	public void updateProgress(final UUID jobId, final double deltaValue) {
		Objects.requireNonNull(jobId, Constants.JOB_ID_CANNOT_BE_NULL);
		getJobProgress(jobId).ifPresent(jobProgress -> jobProgress.incrementWorkCompleted(deltaValue));
	}
}
