/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.electionevent.datapacks.beans;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Properties;

/**
 * Provides functionality for manipulating instances of {@link ElectionInputDataPack}.
 */
public class ElectionInputDataPackUtils {

	/**
	 * Sets the start and end dates of the received InputDataPack using the current and date, and also using data extracted from the received
	 * properties file.
	 *
	 * @param propertiesFile        properties file containing data to be used to set the end and start start dates.
	 * @param electionInputDataPack the {@link ElectionInputDataPack} to be modified.
	 * @throws IOException
	 */
	public void setInputDataPackDatesFromProperties(final String propertiesFile, final ElectionInputDataPack electionInputDataPack)
			throws IOException {

		final Properties props = new Properties();
		try (BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(propertiesFile))) {
			props.load(bufferedReader);
		}

		final String startElection = (String) props.get("startElection");
		final String endElection = (String) props.get("endElection");

		final Integer validityPeriod = Integer.parseInt((String) props.get("validityPeriod"));

		final ZonedDateTime startValidityPeriod = ZonedDateTime.now(ZoneOffset.UTC);

		final ZonedDateTime electionStartDate = ZonedDateTime.ofInstant(Instant.parse(startElection), ZoneOffset.UTC);
		final ZonedDateTime electionEndDate = ZonedDateTime.ofInstant(Instant.parse(endElection), ZoneOffset.UTC);

		final ZonedDateTime endValidityPeriod = electionStartDate.plusYears(validityPeriod);

		if (electionEndDate.isAfter(endValidityPeriod)) {
			throw new IllegalArgumentException("End date cannot be after Start date plus validity period.");
		}

		electionInputDataPack.setStartDate(startValidityPeriod);
		electionInputDataPack.setEndDate(endValidityPeriod);
		electionInputDataPack.setElectionStartDate(electionStartDate);
		electionInputDataPack.setElectionEndDate(electionEndDate);
	}
}
