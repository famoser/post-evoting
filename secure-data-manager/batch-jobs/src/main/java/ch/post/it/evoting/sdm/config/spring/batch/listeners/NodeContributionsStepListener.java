/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch.listeners;

import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import ch.post.it.evoting.sdm.commons.domain.VcIdCombinedReturnCodesGenerationValues;

public class NodeContributionsStepListener implements StepExecutionListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(VotingCardGenerationStepListener.class);

	private final BlockingQueue<VcIdCombinedReturnCodesGenerationValues> queue;

	public NodeContributionsStepListener(final BlockingQueue<VcIdCombinedReturnCodesGenerationValues> queue) {
		this.queue = queue;
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		// nothing to do.
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		try {
			queue.put(VcIdCombinedReturnCodesGenerationValues.poisonPill());
		} catch (InterruptedException e) {
			LOGGER.error("Unexpected state", e);
			Thread.currentThread().interrupt();
		}
		return ExitStatus.COMPLETED;
	}

}
