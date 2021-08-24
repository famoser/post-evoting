/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch.writers;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.springframework.batch.item.ItemWriter;

import ch.post.it.evoting.sdm.commons.domain.VcIdCombinedReturnCodesGenerationValues;

public class ComputedValuesOutputWriter implements ItemWriter<List<VcIdCombinedReturnCodesGenerationValues>> {

	private final BlockingQueue<VcIdCombinedReturnCodesGenerationValues> queue;

	public ComputedValuesOutputWriter(final BlockingQueue<VcIdCombinedReturnCodesGenerationValues> queue) {
		this.queue = queue;
	}

	@Override
	public void write(List<? extends List<VcIdCombinedReturnCodesGenerationValues>> itemsList) throws Exception {
		for (List<VcIdCombinedReturnCodesGenerationValues> items : itemsList) {
			for (VcIdCombinedReturnCodesGenerationValues item : items) {
				queue.put(item);
			}
		}
	}

}
