/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch.readers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.Test;

import ch.post.it.evoting.sdm.config.spring.batch.GeneratedVotingCardOutput;

class OutputQueueReaderTest {

	@Test
	void returnNullWhenReceivedPoisonPill() throws Exception {

		// given
		BlockingQueue<GeneratedVotingCardOutput> queue = new LinkedBlockingQueue<>();
		queue.add(createOutput());
		queue.add(createOutput());
		queue.add(createPoisonPillOutput());
		OutputQueueReader sut = new OutputQueueReader(queue);

		GeneratedVotingCardOutput output1 = sut.read();
		GeneratedVotingCardOutput output2 = sut.read();
		GeneratedVotingCardOutput output3 = sut.read();

		assertNotNull(output1);
		assertNotNull(output2);
		assertNull(output3);
	}

	private GeneratedVotingCardOutput createOutput() {
		return GeneratedVotingCardOutput.success(null, null, null, null, null, null, null, null, null, null, null, null, null);
	}

	private GeneratedVotingCardOutput createPoisonPillOutput() {
		return GeneratedVotingCardOutput.poisonPill();
	}
}
