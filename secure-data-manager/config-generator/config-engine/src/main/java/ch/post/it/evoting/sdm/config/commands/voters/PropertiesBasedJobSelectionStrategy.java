/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.voters;

public class PropertiesBasedJobSelectionStrategy implements JobSelectionStrategy {

	private final String prefix;
	private final String qualifier;

	public PropertiesBasedJobSelectionStrategy(final String prefix, final String qualifier) {
		this.prefix = prefix;
		this.qualifier = qualifier;
	}

	@Override
	public String select() {
		return String.format("%s-%s", prefix, qualifier);
	}
}
