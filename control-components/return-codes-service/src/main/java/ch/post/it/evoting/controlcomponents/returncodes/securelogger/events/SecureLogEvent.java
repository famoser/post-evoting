/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.securelogger.events;

import java.util.SortedMap;

import org.apache.logging.log4j.core.LogEvent;

public interface SecureLogEvent extends LogEvent {
	SortedMap<SecureLogProperty, String> getProperties();
}
