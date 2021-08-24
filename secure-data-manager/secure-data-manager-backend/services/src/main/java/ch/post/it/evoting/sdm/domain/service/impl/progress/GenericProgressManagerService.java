/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service.impl.progress;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ch.post.it.evoting.sdm.domain.common.GenericJobStatus;
import ch.post.it.evoting.sdm.domain.common.JobStatus;
import ch.post.it.evoting.sdm.domain.service.ProgressManagerService;

/**
 * This implementation contains a call to the configuration engine for progressManager
 */
public abstract class GenericProgressManagerService<T extends GenericJobStatus> implements ProgressManagerService<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenericProgressManagerService.class);
	private static final String BASE_JOBS_URL_PATH = "/{tenantId}/{electionEventId}/jobs";
	private final Map<String, ProgressData<T>> jobMap;
	private final ThreadPoolTaskScheduler taskScheduler;
	@Autowired
	protected RestTemplate restClient;
	private URI serviceUri;
	private boolean isCheckProgressTaskStopped = true;
	private ScheduledFuture<?> checkProgressTaskFuture;

	protected GenericProgressManagerService() {
		taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(1);
		taskScheduler.afterPropertiesSet();
		jobMap = new ConcurrentHashMap<>();
	}

	@Override
	public Future<T> registerJob(final String jobId, final URI statusUri) {
		Objects.requireNonNull(jobId, "JobId parameter is required");
		LOGGER.info("Registered job [id={}, uri={}]", jobId, statusUri);
		// create a future that the client will 'wait' on, until we signal the job is complete
		CompletableFuture<T> future = new CompletableFuture<>();
		ProgressData<T> progressData = new ProgressData<>(statusUri, future, defaultData(jobId));
		jobMap.put(jobId, progressData);

		synchronized (this) {
			if (isCheckProgressTaskStopped) {
				LOGGER.debug("Starting 'check progress' task....");
				checkProgressTaskFuture = taskScheduler.scheduleWithFixedDelay(this::checkProgress, 2000);
				isCheckProgressTaskStopped = false;
			}

			return future;
		}
	}

	@Override
	public T getForJob(final String jobId) {
		Objects.requireNonNull(jobId, "JobId parameter is required");

		final ProgressData<T> progressData = jobMap.get(jobId);
		if (progressData == null) {
			LOGGER.warn("Job '{}' was not registered, returning default data.", jobId);
			return null;
		}

		T jobStatus = progressData.getProgressStatus();
		processIfCompleted(jobStatus);
		return jobStatus;
	}

	@Override
	public List<T> getAllByStatus(final String status) {
		Objects.requireNonNull(status, "Status parameter is required");
		JobStatus wantedStatus = JobStatus.valueOf(status.toUpperCase());
		// get only jobStatus that match the 'status' we were supplied
		List<T> registeredJobsStatus = findRegisteredJobs(pd -> wantedStatus.equals(pd.getProgressStatus().getStatus()));
		registeredJobsStatus.forEach(j -> processIfCompleted(j));
		return registeredJobsStatus;
	}

	@Override
	public List<T> getAll() {
		// do not filter, return all
		final List<T> registeredJobsStatus = findRegisteredJobs(ignored -> true);
		registeredJobsStatus.forEach(j -> processIfCompleted(j));
		return registeredJobsStatus;
	}

	private List<T> findRegisteredJobs(final Predicate<ProgressData<T>> jobFilter) {
		final Map<String, ProgressData<T>> registeredJobs = Collections.unmodifiableMap(jobMap);
		return registeredJobs.values().stream().filter(jobFilter).map(filteredJobs -> filteredJobs.getProgressStatus()).collect(Collectors.toList());
	}

	protected abstract T defaultData(final String jobId);

	protected abstract List<T> doCall(final URI uri);

	protected void processJobStatus(final T jobStatus) {
		final ProgressData<T> progressData = jobMap.get(jobStatus.getJobId());
		if (progressData == null) {
			LOGGER.warn("Job '{}' was not found in the 'job registry'. If this was a job you started it will not be updated"
					+ "correctly and you should restart it.", jobStatus.getJobId());
		} else {
			// update in-memory 'cache'
			progressData.setProgressStatus(jobStatus);
			// check if the job has completed
			if (JobStatus.COMPLETED.equals(jobStatus.getStatus()) || JobStatus.FAILED.equals(jobStatus.getStatus())) {
				// unblock future to update the job in the database
				progressData.getFuture().complete(jobStatus);
			}
		}
	}

	// @Scheduled(initialDelay = 5000, fixedDelay = 2000)
	protected void checkProgress() {
		int registeredJobsCount = jobMap.size();
		if (registeredJobsCount == 0) {
			LOGGER.debug("No remaining jobs registered, stopping 'check progress' task....");
			checkProgressTaskFuture.cancel(false);
			isCheckProgressTaskStopped = true;
		}

		LOGGER.debug("Registered jobs remaining: {}", registeredJobsCount);
		doCheckProgress();
	}

	protected void doCheckProgress() {

		try {
			final List<T> jobList = doCall(serviceUri);
			// from the complete list of jobs, keep the ones we know and that are not yet completed or
			// failed
			jobList.stream().filter(job -> jobMap.containsKey(job.getJobId()))
					// we only want the jobs in our map that are not 'terminated'.
					.filter(job -> !(JobStatus.COMPLETED.equals(jobMap.get(job.getJobId()).getProgressStatus().getStatus()) || JobStatus.FAILED
							.equals(jobMap.get(job.getJobId()).getProgressStatus().getStatus()))).forEach(this::processJobStatus);
		} catch (RestClientException e) {
			LOGGER.error("Failed to retrieve job progress status. Check if the server is up and running. Retrying...", e);
		}
	}

	protected void processIfCompleted(final T jobStatus) {
		// we remove it here because the front-end will receive this data and should stop asking for progress
		// of this job
		if (JobStatus.COMPLETED.equals(jobStatus.getStatus()) || JobStatus.FAILED.equals(jobStatus.getStatus())) {
			jobMap.remove(jobStatus.getJobId());
		}
	}

	protected void setServiceUrl(final String serviceUrl) {
		this.serviceUri = UriComponentsBuilder.fromUriString(serviceUrl + BASE_JOBS_URL_PATH).buildAndExpand("ignored", "ignored").toUri();
	}

}

