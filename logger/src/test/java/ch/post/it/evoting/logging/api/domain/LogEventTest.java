/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.logging.api.domain;

/**
 * Test implementation
 */
public enum LogEventTest implements LogEvent {

	TEST_EVENT("TEST", "ACTION", "OUTCOME", "INFO");

	private final String layer;

	private final String action;

	private final String outcome;

	private final String info;

	LogEventTest(final String layer, final String action, final String outcome, final String info) {
		this.layer = layer;
		this.action = action;
		this.outcome = outcome;
		this.info = info;
	}

	/**
	 * @see LogEvent#getAction()
	 */
	@Override
	public String getAction() {
		return action;
	}

	/**
	 * @see LogEvent#getOutcome()
	 */
	@Override
	public String getOutcome() {
		return outcome;
	}

	/**
	 * @see LogEvent#getInfo()
	 */
	@Override
	public String getInfo() {
		return info;
	}

	/**
	 * @see LogEvent#getLayer()
	 */
	@Override
	public String getLayer() {
		return layer;
	}

}
