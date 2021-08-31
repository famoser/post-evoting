/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.model.authentication.service;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import ch.post.it.evoting.sdm.config.exceptions.ConfigurationEngineException;
import ch.post.it.evoting.sdm.config.exceptions.SequentialProvidedChallengeSourceException;
import ch.post.it.evoting.sdm.config.model.authentication.ProvidedChallenges;

public class SequentialProvidedChallengeSource implements ProvidedChallengeSource {

	private static final int PROVIDED_CHALLENGE_FILE_ALIAS_INDEX = 0;

	private static final Logger LOGGER = LoggerFactory.getLogger(SequentialProvidedChallengeSource.class);

	private final CSVReader providedChallengeCSVReader;

	public SequentialProvidedChallengeSource(final Path providedChallengePath) {
		try {

			// Ignore 'Resources should be closed' Sonar's rule since the closing is handled by the closeProvidedChallengeCSVReader()'s method.
			@SuppressWarnings("squid:S2095")
			final FileReader fReader = new FileReader(providedChallengePath.toString());
			final CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
			providedChallengeCSVReader = new CSVReaderBuilder(fReader).withCSVParser(parser).build();

		} catch (FileNotFoundException e) {
			closeProvidedChallengeCSVReader();
			LOGGER.error("Could not find provided challenge input data file: {}", providedChallengePath);
			throw new SequentialProvidedChallengeSourceException(
					String.format("Could not find provided challenge input data file: %s", providedChallengePath.toString()), e);
		} catch (RuntimeException e) {
			closeProvidedChallengeCSVReader();
			throw e;
		}
	}

	@Override
	public ProvidedChallenges next() {

		String alias;
		List<String> challenges;
		String[] nextLine;

		try {
			synchronized (providedChallengeCSVReader) {
				nextLine = providedChallengeCSVReader.readNext();
			}

			if ((nextLine) != null) {
				alias = nextLine[PROVIDED_CHALLENGE_FILE_ALIAS_INDEX];
				challenges = Arrays.asList(Arrays.copyOfRange(nextLine, PROVIDED_CHALLENGE_FILE_ALIAS_INDEX + 1, nextLine.length));
			} else {
				closeProvidedChallengeCSVReader();
				return null;
			}
		} catch (IOException | CsvValidationException e) {
			closeProvidedChallengeCSVReader();
			throw new ConfigurationEngineException("Error retrieving the next challenge", e);
		} catch (RuntimeException e) {
			closeProvidedChallengeCSVReader();
			throw e;
		}

		return new ProvidedChallenges(alias, challenges);
	}

	private void closeProvidedChallengeCSVReader() {
		try {
			if (providedChallengeCSVReader != null) {
				providedChallengeCSVReader.close();
			}
		} catch (IOException e) {
			LOGGER.error("Error trying to close file reader.");
			throw new SequentialProvidedChallengeSourceException("Error trying to close file reader.", e);
		}
	}
}
