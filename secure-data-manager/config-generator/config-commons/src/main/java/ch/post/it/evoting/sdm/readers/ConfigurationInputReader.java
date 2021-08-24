/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.readers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import ch.post.it.evoting.sdm.domain.common.ConfigurationInput;
import ch.post.it.evoting.sdm.utils.ConfigObjectMapper;

/**
 * A ConfigObjectMapper wrapper to get from a file ConfigurationInput objects.
 */
public class ConfigurationInputReader {

	private final ConfigObjectMapper configObjectMapper = new ConfigObjectMapper();

	public ConfigurationInput fromFileToJava(final File src) throws IOException {
		return configObjectMapper.fromJSONFileToJava(src, ConfigurationInput.class);
	}

	public ConfigurationInput fromStreamToJava(final InputStream src) throws IOException {
		return configObjectMapper.fromJSONStreamToJava(src, ConfigurationInput.class);
	}

}
