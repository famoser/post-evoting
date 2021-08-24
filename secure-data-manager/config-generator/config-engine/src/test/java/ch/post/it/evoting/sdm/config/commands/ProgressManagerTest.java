/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.post.it.evoting.sdm.config.commands.progress.ProgressManager;
import ch.post.it.evoting.sdm.config.commands.progress.ProgressManagerImpl;
import ch.post.it.evoting.sdm.config.commons.progress.JobProgressDetails;

@ExtendWith(MockitoExtension.class)
class ProgressManagerTest {

	@InjectMocks
	ProgressManager sut = new ProgressManagerImpl();

	@Test
	void returnEmptyIfJobNotRegistered() {

		final Optional<JobProgressDetails> progress = sut.getJobProgress(UUID.randomUUID());
		assertFalse(progress.isPresent());
	}

	@Test
	void returnProgressIfJobRegistered() {

		UUID jobId = UUID.randomUUID();
		sut.registerJob(jobId, JobProgressDetails.EMPTY);

		final Optional<JobProgressDetails> progress = sut.getJobProgress(jobId);

		assertTrue(progress.isPresent());
	}

	@Test
	void removeEmptyAfterUnregisterJob() {

		UUID jobId = UUID.randomUUID();
		sut.registerJob(jobId, JobProgressDetails.EMPTY);
		final Optional<JobProgressDetails> progress = sut.getJobProgress(jobId);
		assertTrue(progress.isPresent());

		sut.unregisterJob(jobId);

		final Optional<JobProgressDetails> progress2 = sut.getJobProgress(jobId);
		assertFalse(progress2.isPresent());

	}

	@Test
	void returnUpdatedWorkCompletedAfterUpdateJob() {

		UUID jobId = UUID.randomUUID();
		long totalWorkAmount = 1;
		JobProgressDetails progressDetails = new JobProgressDetails(jobId, totalWorkAmount);
		sut.registerJob(jobId, progressDetails);
		final Optional<JobProgressDetails> progress = sut.getJobProgress(jobId);
		assertTrue(progress.isPresent());
		assertEquals(totalWorkAmount, (long) progress.get().getTotalWorkAmount());

		long workCompleted = 1;
		sut.updateProgress(jobId, workCompleted);

		final Optional<JobProgressDetails> updatedProgress = sut.getJobProgress(jobId);
		assertTrue(updatedProgress.isPresent());
		assertEquals(0, (long) progress.get().getRemainingWork());

	}
}
