/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.mixdec.domain.services;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationException;
import ch.post.it.evoting.votingserver.orchestrator.commons.config.QueuesConfig;
import ch.post.it.evoting.votingserver.orchestrator.mixdec.domain.model.BallotBoxStatus;
import ch.post.it.evoting.votingserver.orchestrator.mixdec.domain.model.MixDecBallotBoxStatus;
import ch.post.it.evoting.votingserver.orchestrator.mixdec.domain.model.MixDecStatus;
import ch.post.it.evoting.votingserver.orchestrator.mixdec.infrastructure.persistence.CleansedBallotBoxRepository;
import ch.post.it.evoting.votingserver.orchestrator.mixdec.infrastructure.persistence.MixDecBallotBoxStatusRepository;
import ch.post.it.evoting.votingserver.orchestrator.mixdec.infrastructure.persistence.MixDecNodeOutputRepository;

@RunWith(MockitoJUnitRunner.class)
public class MixDecBallotBoxServiceTest {

	@InjectMocks
	private static final MixDecBallotBoxServiceImpl sut = new MixDecBallotBoxServiceImpl();
	@ClassRule
	public static EnvironmentVariables environmentVariables = new EnvironmentVariables();
	@Mock
	private MixDecBallotBoxStatusRepository mixDecBallotBoxStatusRepository;
	@Mock
	private CleansedBallotBoxRepository cleansedBallotBoxRepository;
	@Mock
	private MixDecNodeOutputRepository nodeOutputRepository;
	@Mock
	private Logger logger;
	private String ccQueueNames;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(getClass());
		ccQueueNames = System.getenv("CC_QUEUE_NAMES");
		environmentVariables.set("CC_QUEUE_NAMES",
				"{\"g4\": {\"cg-comp\": {\"res\": \"cg-comp-g4-res\", \"req\": \"cg-comp-g4-req\"}, \"cg-keygen\": {\"res\": \"cg-keygen-g4-res\", \"req\": \"cg-keygen-g4-req\"}}, \"g3\": {\"cg-comp\": {\"res\": \"cg-comp-g3-res\", \"req\": \"cg-comp-g3-req\"}, \"cg-keygen\": {\"res\": \"cg-keygen-g3-res\", \"req\": \"cg-keygen-g3-req\"}}, \"g2\": {\"cg-comp\": {\"res\": \"cg-comp-g2-res\", \"req\": \"cg-comp-g2-req\"}, \"cg-keygen\": {\"res\": \"cg-keygen-g2-res\", \"req\": \"cg-keygen-g2-req\"}}, \"g1\": {\"cg-comp\": {\"res\": \"cg-comp-g1-res\", \"req\": \"cg-comp-g1-req\"}, \"cg-keygen\": {\"res\": \"cg-keygen-g1-res\", \"req\": \"cg-keygen-g1-req\"}}, \"v1\": {\"cv-comp\": {\"res\": \"cv-comp-v1-res\", \"req\": \"cv-comp-v1-req\"}, \"cv-dec\": {\"res\": \"cv-dec-v1-res\", \"req\": \"cv-dec-v1-req\"}}, \"v2\": {\"cv-comp\": {\"res\": \"cv-comp-v2-res\", \"req\": \"cv-comp-v2-req\"}, \"cv-dec\": {\"res\": \"cv-dec-v2-res\", \"req\": \"cv-dec-v2-req\"}}, \"v3\": {\"cv-comp\": {\"res\": \"cv-comp-v3-res\", \"req\": \"cv-comp-v3-req\"}, \"cv-dec\": {\"res\": \"cv-dec-v3-res\", \"req\": \"cv-dec-v3-req\"}}, \"m1\": {\"md-keygen\": {\"res\": \"md-keygen-m1-res\", \"req\": \"md-keygen-m1-req\"}, \"md-mixdec\": {\"res\": \"md-mixdec-m1-res\", \"req\": \"md-mixdec-m1-req\"}}, \"m3\": {\"md-keygen\": {\"res\": \"md-keygen-m3-res\", \"req\": \"md-keygen-m3-req\"}, \"md-mixdec\": {\"res\": \"md-mixdec-m3-res\", \"req\": \"md-mixdec-m3-req\"}}, \"m2\": {\"md-keygen\": {\"res\": \"md-keygen-m2-res\", \"req\": \"md-keygen-m2-req\"}, \"md-mixdec\": {\"res\": \"md-mixdec-m2-res\", \"req\": \"md-mixdec-m2-req\"}}, \"v4\": {\"cv-comp\": {\"res\": \"cv-comp-v4-res\", \"req\": \"cv-comp-v4-req\"}, \"cv-dec\": {\"res\": \"cv-dec-v4-res\", \"req\": \"cv-dec-v4-req\"}}}");
	}

	@After
	public void tearDown() {
		if (ccQueueNames == null) {
			System.clearProperty("CC_QUEUE_NAMES");
		} else {
			environmentVariables.set("CC_QUEUE_NAMES", ccQueueNames);
		}
	}

	@Test
	public void testBlankBallotBox() throws ApplicationException {
		String tenantId = "tenant";
		String electionEventId = "election_event";
		String ballotBoxId = "1b17408618934e6493d9bbc8a8aca82b";
		List<String> ballotBoxIds = Collections.singletonList(ballotBoxId);
		String trackingId = "tracking";

		// Blank ballot box
		MixDecBallotBoxStatus mixDecBallotBoxStatus = mock(MixDecBallotBoxStatus.class);

		Assert.assertNotNull(QueuesConfig.MIX_DEC_COMPUTATION_REQ_QUEUES);
		when(cleansedBallotBoxRepository.isBallotBoxEmpty(any(), any())).thenReturn(true);

		List<BallotBoxStatus> result = sut.processBallotBoxes(electionEventId, ballotBoxIds, trackingId);

		MatcherAssert.assertThat(result.get(0).getProcessStatus(), is(MixDecStatus.PROCESSING));
	}

}
