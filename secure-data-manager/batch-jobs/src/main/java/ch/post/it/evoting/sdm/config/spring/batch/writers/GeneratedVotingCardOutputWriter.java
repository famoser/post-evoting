/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch.writers;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.springframework.batch.item.ItemWriter;

import ch.post.it.evoting.sdm.config.spring.batch.GeneratedVotingCardOutput;

public class GeneratedVotingCardOutputWriter implements ItemWriter<GeneratedVotingCardOutput> {

	private final BlockingQueue<GeneratedVotingCardOutput> queue;

	public GeneratedVotingCardOutputWriter(final BlockingQueue<GeneratedVotingCardOutput> queue) {
		this.queue = queue;
	}

	@Override
	public void write(final List<? extends GeneratedVotingCardOutput> items) throws Exception {
		for (GeneratedVotingCardOutput item : items) {
			queue.put(item);
		}
	}
}
