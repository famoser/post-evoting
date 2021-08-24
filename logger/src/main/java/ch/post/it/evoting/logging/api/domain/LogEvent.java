/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.logging.api.domain;

/**
 * Defines mandatory methods necessary to identify a log
 */
public interface LogEvent {

	String getLayer();

	String getAction();

	String getOutcome();

	String getInfo();
}
