/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.electionevent.readers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.domain.common.ConfigurationInput;
import ch.post.it.evoting.sdm.readers.ConfigurationInputReader;

class ConfigurationInputReaderTest {

	private final ConfigurationInputReader target = new ConfigurationInputReader();

	@Test
	void read_config_from_file() throws IOException {
		File src = new File("./src/test/resources", Constants.KEYS_CONFIG_FILENAME);
		ConfigurationInput configurationInput = target.fromFileToJava(src);
		assertEquals(4, configurationInput.getConfigProperties().values().size());
	}
}
