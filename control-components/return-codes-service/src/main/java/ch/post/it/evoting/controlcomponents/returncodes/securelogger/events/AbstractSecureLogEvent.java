/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.securelogger.events;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

public abstract class AbstractSecureLogEvent implements SecureLogEvent {
	private final LogEvent delegate;
	private final SortedMap<SecureLogProperty, String> properties;

	protected AbstractSecureLogEvent(LogEvent delegate, SortedMap<SecureLogProperty, String> properties) {
		this.delegate = delegate.toImmutable();
		this.properties = new TreeMap<>(properties);
	}

	@Override
	public SortedMap<SecureLogProperty, String> getProperties() {
		return new TreeMap<>(properties);
	}

	@Override
	public LogEvent toImmutable() {
		return this;
	}

	/**
	 * Present for compliance to the interface, itself deprecated.
	 *
	 * @deprecated use {@link #getContextData()} instead
	 */
	@Override
	@Deprecated
	public Map<String, String> getContextMap() {
		return delegate.getContextMap();
	}

	@Override
	public ReadOnlyStringMap getContextData() {
		return delegate.getContextData();
	}

	@Override
	public ThreadContext.ContextStack getContextStack() {
		return delegate.getContextStack();
	}

	@Override
	public String getLoggerFqcn() {
		return delegate.getLoggerFqcn();
	}

	@Override
	public Level getLevel() {
		return delegate.getLevel();
	}

	@Override
	public String getLoggerName() {
		return delegate.getLoggerName();
	}

	@Override
	public Marker getMarker() {
		return delegate.getMarker();
	}

	@Override
	public Message getMessage() {
		return delegate.getMessage();
	}

	@Override
	public long getTimeMillis() {
		return delegate.getTimeMillis();
	}

	@Override
	public Instant getInstant() {
		return delegate.getInstant();
	}

	@Override
	public StackTraceElement getSource() {
		return delegate.getSource();
	}

	@Override
	public String getThreadName() {
		return delegate.getThreadName();
	}

	@Override
	public long getThreadId() {
		return delegate.getThreadId();
	}

	@Override
	public int getThreadPriority() {
		return delegate.getThreadPriority();
	}

	@Override
	public Throwable getThrown() {
		return delegate.getThrown();
	}

	@Override
	public ThrowableProxy getThrownProxy() {
		return delegate.getThrownProxy();
	}

	@Override
	public boolean isEndOfBatch() {
		return delegate.isEndOfBatch();
	}

	@Override
	public void setEndOfBatch(boolean endOfBatch) {
		delegate.setEndOfBatch(endOfBatch);
	}

	@Override
	public boolean isIncludeLocation() {
		return delegate.isIncludeLocation();
	}

	@Override
	public void setIncludeLocation(boolean locationRequired) {
		delegate.setIncludeLocation(locationRequired);
	}

	@Override
	public long getNanoTime() {
		return delegate.getNanoTime();
	}
}
