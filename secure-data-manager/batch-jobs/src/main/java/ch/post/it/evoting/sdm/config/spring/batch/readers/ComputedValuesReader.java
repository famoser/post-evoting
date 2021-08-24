/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch.readers;

import java.util.concurrent.BlockingQueue;

import org.springframework.batch.item.ItemReader;

import ch.post.it.evoting.sdm.commons.domain.VcIdCombinedReturnCodesGenerationValues;

public class ComputedValuesReader implements ItemReader<VcIdCombinedReturnCodesGenerationValues> {

	private final BlockingQueue<VcIdCombinedReturnCodesGenerationValues> computedValuesQueue;

	public ComputedValuesReader(BlockingQueue<VcIdCombinedReturnCodesGenerationValues> computedValuesQueue) {
		this.computedValuesQueue = computedValuesQueue;
	}

	@Override
	public VcIdCombinedReturnCodesGenerationValues read() throws InterruptedException {
		VcIdCombinedReturnCodesGenerationValues computedValues = computedValuesQueue.take();
		if (!computedValues.isPoisonPill()) {
			return computedValues;
		} else {
			// Add again the poison pill to the queue to ensure all remaining
			// threads will receive it
			computedValuesQueue.add(VcIdCombinedReturnCodesGenerationValues.poisonPill());
			return null;
		}
	}

}
