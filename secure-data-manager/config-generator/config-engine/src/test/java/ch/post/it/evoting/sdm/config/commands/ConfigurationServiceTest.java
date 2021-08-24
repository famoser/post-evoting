/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands;

import static ch.post.it.evoting.sdm.commons.Constants.ELECTION_EVENT_ID;
import static ch.post.it.evoting.sdm.commons.Constants.JOB_INSTANCE_ID;
import static ch.post.it.evoting.sdm.commons.Constants.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;

import ch.post.it.evoting.sdm.config.commands.api.ConfigurationService;
import ch.post.it.evoting.sdm.config.commands.api.output.BallotBoxesServiceOutput;
import ch.post.it.evoting.sdm.config.commands.api.output.ElectionEventServiceOutput;
import ch.post.it.evoting.sdm.config.commands.ballotbox.BallotBoxGenerator;
import ch.post.it.evoting.sdm.config.commands.ballotbox.BallotBoxHolderInitializer;
import ch.post.it.evoting.sdm.config.commands.ballotbox.BallotBoxParametersHolder;
import ch.post.it.evoting.sdm.config.commands.electionevent.CreateElectionEventGenerator;
import ch.post.it.evoting.sdm.config.commands.electionevent.CreateElectionEventHolderInitializer;
import ch.post.it.evoting.sdm.config.commands.electionevent.CreateElectionEventOutput;
import ch.post.it.evoting.sdm.config.commands.electionevent.CreateElectionEventParametersHolder;
import ch.post.it.evoting.sdm.config.commands.electionevent.CreateElectionEventSerializer;
import ch.post.it.evoting.sdm.config.commands.progress.ProgressManager;
import ch.post.it.evoting.sdm.config.commons.beans.VotingCardGenerationJobStatus;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

	@TempDir
	static Path temporaryFolder;
	@InjectMocks
	private final ConfigurationService configurationService = new ConfigurationService();
	@Mock
	CreateElectionEventHolderInitializer eventHolderInitializer;
	@Mock
	CreateElectionEventGenerator electionEventGenerator;
	@Mock
	CreateElectionEventSerializer electionEventSerializer;
	@Mock
	BallotBoxHolderInitializer ballotBoxHolderInitializer;
	@Mock
	BallotBoxGenerator ballotBoxGenerator;
	@Mock
	JobExplorer fakeJobExplorer;
	@Mock
	ProgressManager progressManager;

	@Test
	void generateCorrectOutputFoldersFromInputParametersWhenCreatingElectionEvent() throws Exception {
		// given
		CreateElectionEventParametersHolder inputParameters = getCreateElectionEventInputParameters();

		CreateElectionEventOutput mockOutput = mock(CreateElectionEventOutput.class);
		doAnswer(invocationOnMock -> null).when(mockOutput).clearPasswords();

		when(electionEventGenerator.generate(any())).thenReturn(mockOutput);
		doAnswer(invocationOnMock -> null).when(electionEventSerializer).serialize(any(), any());

		// when
		final ElectionEventServiceOutput output = configurationService.createElectionEvent(inputParameters);

		// then
		assertEquals(output.getOfflineFolder(), inputParameters.getOfflineFolder().toString());
		assertEquals(output.getOnlineAuthenticationFolder(), inputParameters.getOnlineAuthenticationFolder().toString());
		assertEquals(output.getOnlineElectionInformationFolder(), inputParameters.getOnlineElectionInformationFolder().toString());
	}

	@Test
	void generateCorrectOutputFromInputParametersWhenCreatingBallotBoxes() throws Exception {
		// given
		BallotBoxParametersHolder inputParameters = getCreateBallotBoxesInputParameters();
		Path fullBallotBoxPath = temporaryFolder.resolve("genBallot.txt");

		BallotBoxesServiceOutput mockOutput = mock(BallotBoxesServiceOutput.class);
		when(mockOutput.getOutputPath()).thenReturn(fullBallotBoxPath.toString());
		when(ballotBoxGenerator.generate(any())).thenReturn(mockOutput);

		// when
		final BallotBoxesServiceOutput output = configurationService.createBallotBoxes(inputParameters);

		// then
		assertEquals(output.getOutputPath(), fullBallotBoxPath.toString());
	}

	@Test
	void returnEmptyWhenAskingForJobStatusThatDoesNotExist() {

		// given
		when(fakeJobExplorer.findJobInstancesByJobName(anyString(), anyInt(), anyInt())).thenReturn(Collections.emptyList());

		// when
		final Optional<VotingCardGenerationJobStatus> status = configurationService
				.getVotingCardGenerationJobStatus("fake", "fake", UUID.randomUUID().toString());

		// then
		assertFalse(status.isPresent());
	}

	@Test
	void returnJobStatusAsStartingWhenJobIsCreated() {

		// given
		String tenantId = "fake";
		String electionEventid = "fake";
		UUID jobId = UUID.randomUUID();

		List<JobInstance> fakeJobInstances = new ArrayList<>();
		JobInstance fakeJobInstance = new JobInstance(1L, "fakeJob");
		fakeJobInstances.add(fakeJobInstance);

		JobParameters fakeJobParameters = getFakeJobParameters(tenantId, electionEventid, jobId.toString());
		JobExecution fakeJobExecution = new JobExecution(fakeJobInstance, 1L, fakeJobParameters, null);

		List<JobExecution> fakeJobExecutions = new ArrayList<>();
		fakeJobExecutions.add(fakeJobExecution);

		when(fakeJobExplorer.findJobInstancesByJobName(anyString(), anyInt(), anyInt())).thenReturn(fakeJobInstances);
		when(fakeJobExplorer.getJobExecutions(any())).thenReturn(fakeJobExecutions);
		when(progressManager.getJobProgress(any())).thenReturn(Optional.empty());

		// when

		final Optional<VotingCardGenerationJobStatus> status = configurationService
				.getVotingCardGenerationJobStatus(tenantId, electionEventid, jobId.toString());

		// then
		assertTrue(status.isPresent());
		VotingCardGenerationJobStatus jobStatus = status.get();
		assertEquals(jobId, jobStatus.getJobId());
		assertEquals(BatchStatus.STARTING.toString(), jobStatus.getStatus());
		assertEquals(0, jobStatus.getErrorCount());
		assertEquals(0, jobStatus.getGeneratedCount());
		assertNull(jobStatus.getProgressDetails());
	}

	@Test
	void returnListOfJobStatusAsStartingWhenJobsAreCreated() {

		// given
		String tenantId = "fake";
		String electionEventid = "fake";
		UUID jobId = UUID.randomUUID();

		List<JobInstance> fakeJobInstances = new ArrayList<>();
		JobInstance fakeJobInstance = new JobInstance(1L, "fakeJob");
		fakeJobInstances.add(fakeJobInstance);

		JobParameters fakeJobParameters = getFakeJobParameters(tenantId, electionEventid, jobId.toString());
		JobExecution fakeJobExecution = new JobExecution(fakeJobInstance, 1L, fakeJobParameters, null);

		List<JobExecution> fakeJobExecutions = new ArrayList<>();
		fakeJobExecutions.add(fakeJobExecution);

		when(fakeJobExplorer.findJobInstancesByJobName(anyString(), anyInt(), anyInt())).thenReturn(fakeJobInstances);
		when(fakeJobExplorer.getJobExecutions(any())).thenReturn(fakeJobExecutions);
		when(progressManager.getJobProgress(any())).thenReturn(Optional.empty());

		// when

		final List<VotingCardGenerationJobStatus> jobs = configurationService.getJobs();

		// then
		assertTrue(jobs.size() > 0);
		jobs.forEach(j -> {
			assertEquals(jobId, j.getJobId());
			assertEquals(BatchStatus.STARTING.toString(), j.getStatus());
			assertEquals(0, j.getErrorCount());
			assertEquals(0, j.getGeneratedCount());
			assertNull(j.getProgressDetails());
		});

	}

	@Test
	void returnCorrectListOfJobStatusWhenFilteringByStatus() {

		// given
		UUID jobId1 = UUID.randomUUID();
		UUID jobId2 = UUID.randomUUID();
		String tenantId = "fake";
		String electionEventid = "fake";

		List<JobInstance> fakeJobInstances = new ArrayList<>();
		JobInstance fakeJobInstance1 = new JobInstance(1L, "fakeJob1");
		JobInstance fakeJobInstance2 = new JobInstance(2L, "fakeJob2");
		fakeJobInstances.add(fakeJobInstance1);
		fakeJobInstances.add(fakeJobInstance2);

		JobParameters fakeJobParametersJob1 = getFakeJobParameters(tenantId, electionEventid, jobId1.toString());
		JobParameters fakeJobParametersJob2 = getFakeJobParameters(tenantId, electionEventid, jobId2.toString());

		JobExecution fakeJobExecution1 = new JobExecution(fakeJobInstance1, 1L, fakeJobParametersJob1, null);
		JobExecution fakeJobExecution2 = new JobExecution(fakeJobInstance2, 2L, fakeJobParametersJob2, null);
		fakeJobExecution2.setStatus(BatchStatus.COMPLETED);

		List<JobExecution> fakeJobExecutions1 = new ArrayList<>();
		fakeJobExecutions1.add(fakeJobExecution1);
		List<JobExecution> fakeJobExecutions2 = new ArrayList<>();
		fakeJobExecutions2.add(fakeJobExecution2);

		when(fakeJobExplorer.findJobInstancesByJobName(anyString(), anyInt(), anyInt())).thenReturn(fakeJobInstances);
		when(fakeJobExplorer.getJobExecutions(fakeJobInstance1)).thenReturn(fakeJobExecutions1);
		when(fakeJobExplorer.getJobExecutions(fakeJobInstance2)).thenReturn(fakeJobExecutions2);
		when(progressManager.getJobProgress(any())).thenReturn(Optional.empty());

		// when

		final List<VotingCardGenerationJobStatus> jobs = configurationService
				.getJobsWithStatus(tenantId, electionEventid, BatchStatus.COMPLETED.toString());

		// then
		assertEquals(1, jobs.size());
		jobs.forEach(j -> {
			assertEquals(jobId2, j.getJobId());
			assertEquals(BatchStatus.COMPLETED.toString(), j.getStatus());
			assertEquals(0, j.getErrorCount());
			assertEquals(0, j.getGeneratedCount());
			assertNull(j.getProgressDetails());
		});

	}

	private JobParameters getFakeJobParameters(final String tenantId, final String electionEventId, final String jobId) {
		JobParametersBuilder builder = new JobParametersBuilder();
		builder.addString(TENANT_ID, tenantId);
		builder.addString(ELECTION_EVENT_ID, electionEventId);
		builder.addString(JOB_INSTANCE_ID, jobId);
		return builder.toJobParameters();
	}

	private CreateElectionEventParametersHolder getCreateElectionEventInputParameters() {
		Path outputFolder = temporaryFolder;
		Path offlineFolder = outputFolder.resolve("OFFLINE");
		Path onlineAuthenticationFolder = outputFolder.resolve("ONLINE").resolve("authentication");
		Path onlineElectionInformationFolder = outputFolder.resolve("ONLINE").resolve("electionInformation");
		Path onlineVotingWorkflowFolder = outputFolder.resolve("ONLINE").resolve("votingWorkflow");

		return new CreateElectionEventParametersHolder(null, outputFolder, null, offlineFolder, onlineAuthenticationFolder,
				onlineElectionInformationFolder, onlineVotingWorkflowFolder, null, null, null, null, null);

	}

	private BallotBoxParametersHolder getCreateBallotBoxesInputParameters() {

		Path outputFolder = temporaryFolder;

		return new BallotBoxParametersHolder("", "", "", "", outputFolder, "", null, null, null, null, null);
	}
}
