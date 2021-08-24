/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.commons.domain;

import java.util.List;
import java.util.Map;

public class CreateElectoralBoardKeyPairInput {

	private String outputFolder;

	private Map<String, List<String>> ballotMappings;

	public String getOutputFolder() {
		return outputFolder;
	}

	public void setOutputFolder(final String outputFolder) {
		this.outputFolder = outputFolder;
	}

	public Map<String, List<String>> getBallotMappings() {
		return ballotMappings;
	}

	public void setBallotMappings(final Map<String, List<String>> ballotMappings) {
		this.ballotMappings = ballotMappings;
	}
}
