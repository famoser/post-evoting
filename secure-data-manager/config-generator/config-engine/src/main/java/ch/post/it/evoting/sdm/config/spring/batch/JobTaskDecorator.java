/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch;

import org.springframework.core.task.TaskDecorator;

/**
 * Implementation of {@link TaskDecorator} which allows to run the supplied job task in appropriate context.
 */
class JobTaskDecorator implements TaskDecorator {
	private final String tenantId;

	/**
	 * Constructor.
	 *
	 * @param tenantId
	 */
	public JobTaskDecorator(final String tenantId) {
		this.tenantId = tenantId;
	}

	@Override
	public Runnable decorate(final Runnable task) {
		return () -> runTask(task);
	}

	private void runTask(final Runnable task) {
		task.run();
	}
}
