/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.readers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import ch.post.it.evoting.sdm.domain.common.ConfigurationInput;

class ConfigurationInputReaderTest {

	private ConfigurationInputReader configurationInputReader;

	@BeforeEach
	void init() {
		configurationInputReader = new ConfigurationInputReader();
	}

	@Test
	void readGivenFileCorrectly() {

		final File file = Paths.get("src/test/resources/keys_config.json").toFile();
		final ConfigurationInput configurationInput = assertDoesNotThrow(() -> configurationInputReader.fromFileToJava(file));

		assertEquals("privatekey", configurationInput.getBallotBox().getAlias().get("privateKey"));
	}

	@Test
	void throwAnExceptionIfGivenFileIsNotConsistent() {

		final File file = Paths.get("src/test/resources/not_consistent.json").toFile();

		assertThrows(UnrecognizedPropertyException.class, () -> configurationInputReader.fromFileToJava(file));
	}

}
