/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch.readers;

import java.util.concurrent.BlockingQueue;

import org.springframework.batch.item.ItemReader;

import ch.post.it.evoting.sdm.config.spring.batch.GeneratedVotingCardOutput;

public class OutputQueueReader implements ItemReader<GeneratedVotingCardOutput> {

	private final BlockingQueue<GeneratedVotingCardOutput> queue;

	public OutputQueueReader(final BlockingQueue<GeneratedVotingCardOutput> queue) {
		this.queue = queue;
	}

	@Override
	public GeneratedVotingCardOutput read() throws Exception {
		// wait until we get something. if poison pill then terminate by returning null
		try {
			final GeneratedVotingCardOutput output = queue.take();
			if (output.isPoisonPill()) {
				return null;
			}
			return output;
		} catch (InterruptedException e) {
			// signal the framework that we got interrupted.
			Thread.currentThread().interrupt();
			// nothing else we can do. signal termination
			return null;
		}
	}
}
