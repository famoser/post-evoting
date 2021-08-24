/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch.listeners;

import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import ch.post.it.evoting.sdm.config.spring.batch.GeneratedVotingCardOutput;

public class VotingCardGenerationStepListener implements StepExecutionListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(VotingCardGenerationStepListener.class);

	private final Queue<GeneratedVotingCardOutput> outputQueue;

	public VotingCardGenerationStepListener(final Queue<GeneratedVotingCardOutput> outputQueue) {
		this.outputQueue = outputQueue;
	}

	@Override
	public void beforeStep(final StepExecution stepExecution) {
		// nothing to do.
	}

	@Override
	public ExitStatus afterStep(final StepExecution stepExecution) {
		LOGGER.debug("Voting card generation step has terminated. Inject 'poison pill' to signal end of work.");
		outputQueue.add(GeneratedVotingCardOutput.poisonPill());
		return ExitStatus.COMPLETED;
	}

}
