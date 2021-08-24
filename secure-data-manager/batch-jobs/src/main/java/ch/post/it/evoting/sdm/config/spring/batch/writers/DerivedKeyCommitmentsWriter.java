/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch.writers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.logging.api.writer.LoggingWriter;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.domain.VcIdCombinedReturnCodesGenerationValues;

public class DerivedKeyCommitmentsWriter implements ItemWriter<List<VcIdCombinedReturnCodesGenerationValues>> {

	private final Path derivedKeysFolderPath;
	private final int maxNumCredentialsPerFile;
	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	private LoggingWriter loggingWriter;
	private int numLinesRead;

	private int fileNumber;

	public DerivedKeyCommitmentsWriter(Path derivedKeysFolderPath, int maxNumCredentialsPerFile) {
		this.derivedKeysFolderPath = derivedKeysFolderPath;
		this.maxNumCredentialsPerFile = maxNumCredentialsPerFile;

		this.fileNumber = 0;
		this.numLinesRead = 0;
	}

	@Override
	public void write(List<? extends List<VcIdCombinedReturnCodesGenerationValues>> itemsList) throws Exception {
		for (List<VcIdCombinedReturnCodesGenerationValues> items : itemsList) {
			writeDerivedKeyCommitments(items);
		}
	}

	private void writeDerivedKeyCommitments(List<VcIdCombinedReturnCodesGenerationValues> computedValues) throws IOException {
		List<String> derivedKeysLines = new ArrayList<>();

		for (VcIdCombinedReturnCodesGenerationValues computedValuesEntry : computedValues) {
			derivedKeysLines.add(getDerivedKeysLine(computedValuesEntry.getVerificationCardId(),
					computedValuesEntry.getVoterChoiceReturnCodeGenerationPublicKey(),
					computedValuesEntry.getVoterVoteCastReturnCodeGenerationPublicKey()));
		}

		int numDerivedKeysToWrite = derivedKeysLines.size();
		int remainingLinesInFile = maxNumCredentialsPerFile - numLinesRead;
		if (remainingLinesInFile >= numDerivedKeysToWrite) {
			Files.write(derivedKeysFolderPath.resolve(Constants.CONFIG_FILE_NAME_DERIVED_KEYS + "." + fileNumber + Constants.CSV), derivedKeysLines,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			numLinesRead += numDerivedKeysToWrite;
		} else {
			if (remainingLinesInFile != 0) {
				Files.write(derivedKeysFolderPath.resolve(Constants.CONFIG_FILE_NAME_DERIVED_KEYS + "." + fileNumber + Constants.CSV),
						derivedKeysLines.subList(0, remainingLinesInFile), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			}
			this.fileNumber++;
			List<String> newFileKeys = derivedKeysLines.subList(remainingLinesInFile, derivedKeysLines.size());
			if (!newFileKeys.isEmpty()) {
				Files.write(derivedKeysFolderPath.resolve(Constants.CONFIG_FILE_NAME_DERIVED_KEYS + "." + fileNumber + Constants.CSV), newFileKeys,
						StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			}
			numLinesRead = newFileKeys.size();
		}
	}

	private String getDerivedKeysLine(String verificationCardId, List<String> choiceCodeDerivedKeyCommitments,
			List<String> ballotCastKeyDerivedExponentCommitments) throws JsonProcessingException {
		return String.join(";", verificationCardId, objectMapper.writeValueAsString(choiceCodeDerivedKeyCommitments),
				objectMapper.writeValueAsString(ballotCastKeyDerivedExponentCommitments));
	}
}
