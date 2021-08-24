/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.voters;

import java.nio.file.Path;

public class NodeContributionsPath {

	private Path input;

	private Path output;

	public NodeContributionsPath(Path input, Path output) {
		this.setInput(input);
		this.setOutput(output);
	}

	public Path getInput() {
		return input;
	}

	public void setInput(Path input) {
		this.input = input;
	}

	public Path getOutput() {
		return output;
	}

	public void setOutput(Path output) {
		this.output = output;
	}

}
