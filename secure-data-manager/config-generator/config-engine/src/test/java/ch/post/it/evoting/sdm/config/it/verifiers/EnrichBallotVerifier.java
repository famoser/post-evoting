/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.it.verifiers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;

import ch.post.it.evoting.domain.election.Ballot;
import ch.post.it.evoting.domain.election.Contest;
import ch.post.it.evoting.domain.election.ElectionOption;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.utils.ConfigObjectMapper;

/**
 * A class to execute all validations related with Create Ballot Boxes command
 */
public class EnrichBallotVerifier {

	private final ConfigObjectMapper configObjectMapper = new ConfigObjectMapper();

	public void enrichBallotValidations(final File eeidFolder, final Path inputBallotJSONPath) throws IOException {

		Ballot oldBallot = configObjectMapper.fromJSONFileToJava(inputBallotJSONPath.toFile(), Ballot.class);
		for (Contest contest : oldBallot.getContests()) {
			for (ElectionOption option : contest.getOptions()) {
				assertTrue(StringUtils.isEmpty(option.getRepresentation()));
			}
		}

		Path ballotIdFolder = Paths.get(eeidFolder.getAbsolutePath(), Constants.CONFIG_DIR_NAME_ONLINE, Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION,
				Constants.CONFIG_DIR_NAME_BALLOTS, oldBallot.getId());
		File updatedBallotJSONFile = Paths.get(ballotIdFolder.toString(), Constants.CONFIG_FILE_NAME_BALLOT_JSON).toFile();

		Ballot updatedBallot = configObjectMapper.fromJSONFileToJava(updatedBallotJSONFile, Ballot.class);
		for (Contest contest : updatedBallot.getContests()) {
			for (ElectionOption option : contest.getOptions()) {
				String representation = option.getRepresentation();
				assertTrue(StringUtils.isNotEmpty(representation));
				assertTrue(new BigInteger(representation).isProbablePrime(100));
			}
		}

		Path copiedBallotTextsPath = Paths.get(ballotIdFolder.toString(), Constants.CONFIG_FILE_NAME_BALLOT_JSON);
		assertTrue(Files.exists(copiedBallotTextsPath));
	}
}
