/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.electionevent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import ch.post.it.evoting.sdm.domain.common.ConfigurationInput;
import ch.post.it.evoting.sdm.readers.ConfigurationInputReader;

public class CreateElectionEventHolderInitializer {

	private final ConfigurationInputReader configurationInputReader;

	/**
	 * @param configurationInputReader
	 */
	public CreateElectionEventHolderInitializer(final ConfigurationInputReader configurationInputReader) {
		this.configurationInputReader = configurationInputReader;
	}

	public void init(final CreateElectionEventParametersHolder holder, final File configurationInputFile) throws IOException {

		ConfigurationInput configurationInput = configurationInputReader.fromFileToJava(configurationInputFile);
		holder.setConfigurationInput(configurationInput);
	}

	public void init(final CreateElectionEventParametersHolder holder, final InputStream configurationInputStream) throws IOException {

		ConfigurationInput configurationInput = configurationInputReader.fromStreamToJava(configurationInputStream);
		holder.setConfigurationInput(configurationInput);
	}
}
