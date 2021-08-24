/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.preconfiguration;

import java.io.IOException;

/**
 * Interface providing operations with contests configurations.
 */
public interface PreconfigurationRepository {

	/**
	 * Download configuration data from administration portal and save it on a json file.
	 *
	 * @param filename the name of the file where the data is stored.
	 * @return True if the preconfigurations are successfully downloaded. Otherwise, false.
	 * @throws IOException if there are any problem writing the configuration file.
	 */
	boolean download(String filename) throws IOException;

	/**
	 * Reads a json of configuration from a file and save the data related with each contests.
	 *
	 * @param filename the name of the file.
	 * @return The ids of the created election events.
	 * @throws IOException if there are any problem during json parsing.
	 */
	String readFromFileAndSave(String filename) throws IOException;
}
