/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch.writers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.SecurityLevel;
import ch.post.it.evoting.cryptoprimitives.SecurityLevelConfig;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.logging.api.writer.LoggingWriter;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.domain.VcIdCombinedReturnCodesGenerationValues;

@ExtendWith(MockitoExtension.class)
class DerivedKeyCommitmentWriterTest {

	private static final int DEFAULT_MAX_NUM_CREDENTIALS_PER_FILE = 1000;
	@TempDir
	Path tempFolder;
	@Mock
	private LoggingWriter mockLoggingWriter;

	@Test
	void generateOutputFileWithCorrectDataFormat() throws Exception {

		int numCredentials = 3;

		DerivedKeyCommitmentsWriter derivedKeyCommitmentsWriter = new DerivedKeyCommitmentsWriter(tempFolder, DEFAULT_MAX_NUM_CREDENTIALS_PER_FILE);

		derivedKeyCommitmentsWriter.objectMapper = new ObjectMapper();

		List<List<VcIdCombinedReturnCodesGenerationValues>> items = new ArrayList<>();
		List<VcIdCombinedReturnCodesGenerationValues> innerItems = new ArrayList<>();
		innerItems.add(createOutput());
		items.add(innerItems);
		for (int i = 0; i < numCredentials; i++) {
			derivedKeyCommitmentsWriter.write(items);
		}

		Path tempCredentialDataPath = tempFolder.resolve(Constants.CONFIG_FILE_NAME_DERIVED_KEYS + ".0" + Constants.CSV);
		final List<String> lineStrings = Files.readAllLines(tempCredentialDataPath);
		assertEquals(numCredentials, lineStrings.size());
	}

	@Test
	void generateMultipleOutputFilesWithCorrectDataFormat() throws Exception {

		int numCredentials = 300;
		int maxNumCredentialsPerFile = 9;
		int nFiles = (numCredentials * 2) / maxNumCredentialsPerFile;
		int numCredentialsRemaining = (numCredentials * 2) % maxNumCredentialsPerFile;

		DerivedKeyCommitmentsWriter derivedKeyCommitmentsWriter = new DerivedKeyCommitmentsWriter(tempFolder, maxNumCredentialsPerFile);

		derivedKeyCommitmentsWriter.objectMapper = new ObjectMapper();

		List<List<VcIdCombinedReturnCodesGenerationValues>> items = new ArrayList<>();
		List<VcIdCombinedReturnCodesGenerationValues> innerItems = new ArrayList<>();
		innerItems.add(createOutput());
		items.add(innerItems);
		items.add(innerItems);
		for (int i = 0; i < numCredentials; i++) {
			derivedKeyCommitmentsWriter.write(items);
		}

		for (int i = 0; i < (nFiles - 1); i++) {
			Path tempPath = tempFolder.resolve(Constants.CONFIG_FILE_NAME_DERIVED_KEYS + "." + i + Constants.CSV);
			final List<String> lineStrings = Files.readAllLines(tempPath);
			assertEquals(maxNumCredentialsPerFile, lineStrings.size());
		}

		Path tempPath = tempFolder.resolve(Constants.CONFIG_FILE_NAME_DERIVED_KEYS + "." + nFiles + Constants.CSV);
		final List<String> lineStrings = Files.readAllLines(tempPath);
		assertEquals(numCredentialsRemaining, lineStrings.size());
	}

	private VcIdCombinedReturnCodesGenerationValues createOutput() {
		try (MockedStatic<SecurityLevelConfig> mockedSecurityLevel = mockStatic(SecurityLevelConfig.class)) {
			mockedSecurityLevel.when(SecurityLevelConfig::getSystemSecurityLevel).thenReturn(SecurityLevel.TESTING_ONLY);
			List<String> list = new ArrayList<>();
			list.add("5");
			final GqGroup gqGroup = new GqGroup(BigInteger.valueOf(11), BigInteger.valueOf(5), BigInteger.valueOf(3));
			final GqElement gqElement = GqElement.create(BigInteger.valueOf(4), gqGroup);
			final ElGamalMultiRecipientCiphertext encryptedPreChoiceReturnCodes = ElGamalMultiRecipientCiphertext
					.create(gqElement, Collections.singletonList(gqElement));
			final ElGamalMultiRecipientCiphertext encryptedPreVoteCastReturnCode = ElGamalMultiRecipientCiphertext
					.create(gqElement, Collections.singletonList(gqElement));

			return new VcIdCombinedReturnCodesGenerationValues("1", encryptedPreChoiceReturnCodes, encryptedPreVoteCastReturnCode, list, list);
		}
	}

}
