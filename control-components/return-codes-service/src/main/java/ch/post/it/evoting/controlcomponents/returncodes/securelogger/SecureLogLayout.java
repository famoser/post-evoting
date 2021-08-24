/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.securelogger;

import java.nio.charset.Charset;
import java.util.SortedMap;

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;

import ch.post.it.evoting.controlcomponents.returncodes.securelogger.events.SecureLogEvent;
import ch.post.it.evoting.controlcomponents.returncodes.securelogger.events.SecureLogProperty;

@Plugin(name = "SecureLogLayout", category = Core.CATEGORY_NAME, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class SecureLogLayout extends AbstractStringLayout {
	private static final String PREFIX = " {*";
	private static final String ASSIGN = "::";
	private static final String SEPARATOR = ",";
	private static final String SUFFIX = "*}";
	private final PatternLayout innerLayout;

	protected SecureLogLayout(Charset charset, PatternLayout innerLayout) {
		super(charset);
		this.innerLayout = innerLayout;
	}

	@PluginBuilderFactory
	public static <B extends Builder<B>> B newBuilder() {
		return new Builder<B>().asBuilder();
	}

	@Override
	public String toSerializable(LogEvent event) {
		StringBuilder builder = new StringBuilder();
		builder.append(innerSerializable(event));

		if (event instanceof SecureLogEvent) {
			SortedMap<SecureLogProperty, String> properties = ((SecureLogEvent) event).getProperties();
			if (properties.size() > 0) {
				builder.append(PREFIX);
				properties.forEach((k, v) -> {
					builder.append(k.label);
					builder.append(ASSIGN);
					builder.append(v);
					if (properties.lastKey() != k) {
						builder.append(SEPARATOR);
					}
				});
				builder.append(SUFFIX);
			}
		}

		builder.append(System.lineSeparator());

		return builder.toString();
	}

	public String innerSerializable(LogEvent event) {
		return innerLayout.toSerializable(event);
	}

	public static class Builder<B extends Builder<B>> extends AbstractStringLayout.Builder<B>
			implements org.apache.logging.log4j.core.util.Builder<SecureLogLayout> {
		@PluginElement("innerLayout")
		@Required
		PatternLayout innerLayout;

		Builder<B> withInnerLayout(PatternLayout innerLayout) {
			this.innerLayout = innerLayout;
			return this;
		}

		@Override
		public SecureLogLayout build() {
			return new SecureLogLayout(getCharset(), innerLayout);
		}
	}
}
