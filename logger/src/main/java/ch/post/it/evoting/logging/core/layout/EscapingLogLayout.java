/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.logging.core.layout;

import java.nio.charset.Charset;
import java.util.regex.Pattern;

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;

@Plugin(name = "EscapingLogLayout", category = Core.CATEGORY_NAME, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class EscapingLogLayout extends AbstractStringLayout {

	private static final String N_REPLACEMENT = "(n)";
	private static final String R_REPLACEMENT = "(r)";
	private static final String B_REPLACEMENT = "(b)";
	private static final String F_REPLACEMENT = "(f)";
	private final Pattern nEscapedPattern = Pattern.compile("\n");
	private final Pattern rEscapedPattern = Pattern.compile("\r");
	private final Pattern bEscapedPattern = Pattern.compile("\b");
	private final Pattern fEscapedPattern = Pattern.compile("\f");
	private final PatternLayout innerLayout;

	protected EscapingLogLayout(Charset charset, PatternLayout innerLayout) {
		super(charset);
		this.innerLayout = innerLayout;
	}

	@PluginBuilderFactory
	public static <B extends Builder<B>> B newBuilder() {
		return new Builder<B>().asBuilder();
	}

	@Override
	public String toSerializable(LogEvent event) {
		String message = innerSerializable(event);
		message = rEscapedPattern.matcher(message).replaceAll(R_REPLACEMENT);
		message = nEscapedPattern.matcher(message).replaceAll(N_REPLACEMENT);
		message = bEscapedPattern.matcher(message).replaceAll(B_REPLACEMENT);
		message = fEscapedPattern.matcher(message).replaceAll(F_REPLACEMENT);

		return message + System.getProperty("line.separator");
	}

	public String innerSerializable(LogEvent event) {
		return innerLayout.toSerializable(event);
	}

	public static class Builder<B extends Builder<B>> extends AbstractStringLayout.Builder<B>
			implements org.apache.logging.log4j.core.util.Builder<EscapingLogLayout> {
		@PluginElement("innerLayout")
		@Required
		PatternLayout innerLayout;

		Builder<B> withInnerLayout(PatternLayout innerLayout) {
			this.innerLayout = innerLayout;
			return this;
		}

		@Override
		public EscapingLogLayout build() {
			return new EscapingLogLayout(getCharset(), innerLayout);
		}
	}
}
