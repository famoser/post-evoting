/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.it.verifiers;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.it.exceptions.FolderTimestampException;
import ch.post.it.evoting.sdm.utils.ConfigObjectMapper;
import ch.post.it.evoting.sdm.utils.TimestampGenerator;

/**
 * A class to execute all validations related with PreConfig command
 */
public class PreConfigVerifier {

	private static final String OUTPUT_FOLDER = "target/it/output";
	private static final File OUTPUT_FOLDER_FILE = new File(OUTPUT_FOLDER);

	private final TimestampGenerator timestampGenerator = new TimestampGenerator();

	private final ConfigObjectMapper configObjectMapper = new ConfigObjectMapper();

	public void preconfigValidations() throws IOException {

		final File timestampFolder = getLastFolderTimestamp(OUTPUT_FOLDER_FILE);
		assertEquals(1, timestampFolder.list().length);

		for (File file : timestampFolder.listFiles()) {
			if (file.getName().equals(Constants.CONFIG_FILE_NAME_ENCRYPTION_PARAMETERS_JSON)) {
				final ElGamalEncryptionParameters elGamalEncryptionParameters = configObjectMapper
						.fromJSONFileToJava(file, ElGamalEncryptionParameters.class);

				assertAll(() -> assertTrue(elGamalEncryptionParameters.getP().compareTo(BigInteger.ZERO) > 0),
						() -> assertTrue(elGamalEncryptionParameters.getQ().compareTo(BigInteger.ZERO) > 0),
						() -> assertTrue(elGamalEncryptionParameters.getG().compareTo(BigInteger.ZERO) > 0),

						() -> assertTrue(elGamalEncryptionParameters.getP().compareTo(elGamalEncryptionParameters.getG()) > 0 && (
								elGamalEncryptionParameters.getP().compareTo(elGamalEncryptionParameters.getQ()) > 0)),

						() -> assertEquals(BigInteger.ONE,
								elGamalEncryptionParameters.getG().modPow(elGamalEncryptionParameters.getQ(), elGamalEncryptionParameters.getP())));

			} else {
				// listPrimes.txt file case
				final List<String> lines = Files.readAllLines(file.toPath());
				for (String line : lines) {
					assertTrue(new BigInteger(line).isProbablePrime(100));
				}
				assertEquals(20, lines.size());
			}
		}

	}

	private File getLastFolderTimestamp(final File parentFolder) {

		final List<LocalDateTime> timestamps = new ArrayList<>();

		for (final File timestampFolder : parentFolder.listFiles()) {
			final LocalDateTime parsedFromPattern = LocalDateTime.parse(timestampFolder.getName(), timestampGenerator.getDateFormat());
			timestamps.add(parsedFromPattern);
		}

		if (timestamps.isEmpty()) {
			throw new FolderTimestampException("Folder empty: no timestamps on " + parentFolder.getAbsolutePath());
		}

		final String lastTimeStamp = getLastTimestamp(timestamps);

		return new File(parentFolder, lastTimeStamp);
	}

	private String getLastTimestamp(final List<LocalDateTime> dates) {

		final LocalDateTime moreRecentDate = Collections.max(dates);

		return timestampGenerator.getDateFormat().format(moreRecentDate);
	}

}
