/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch.writers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ItemWriter;

import ch.post.it.evoting.sdm.config.spring.batch.GeneratedVotingCardOutput;

class CompositeOutputWriterTest {

	@Test
	void writeIntoQueueAllItems() throws Exception {

		// given
		List<GeneratedVotingCardOutput> inputItems = new ArrayList<>();
		inputItems.add(createOutput());
		inputItems.add(createOutput());
		inputItems.add(createErrorOutput());
		inputItems.add(createOutput());

		List<GeneratedVotingCardOutput> outputItems = new ArrayList<>();
		ListOutputItemWriter listOutputItemWriter = new ListOutputItemWriter(outputItems);

		CompositeOutputWriter sut = new CompositeOutputWriter();
		sut.setDelegates(Collections.singletonList(listOutputItemWriter));
		sut.write(inputItems);
		sut.close();

		// then
		assertTrue(outputItems.size() < inputItems.size());
		// assert no error items in output
		outputItems.forEach(item -> assertFalse(item.isError()));

	}

	private GeneratedVotingCardOutput createOutput() {
		return GeneratedVotingCardOutput.success(null, null, null, null, null, null, null, null, null, null, null, null, null);
	}

	private GeneratedVotingCardOutput createErrorOutput() {
		return GeneratedVotingCardOutput.error(new Exception("For testing purposes only"));
	}

	private static class ListOutputItemWriter implements ItemWriter<GeneratedVotingCardOutput> {

		private final List<GeneratedVotingCardOutput> list;

		public ListOutputItemWriter(final List<GeneratedVotingCardOutput> list) {
			this.list = list;
		}

		@Override
		public void write(final List<? extends GeneratedVotingCardOutput> items) throws Exception {
			list.addAll(items);
		}
	}
}
