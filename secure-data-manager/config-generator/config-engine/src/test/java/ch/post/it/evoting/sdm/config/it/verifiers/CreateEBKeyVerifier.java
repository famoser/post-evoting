/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.it.verifiers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import ch.post.it.evoting.domain.election.BallotBox;
import ch.post.it.evoting.domain.election.VoteVerificationContextData;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.utils.ConfigObjectMapper;
import ch.post.it.evoting.sdm.utils.IDsParser;

/**
 * A class to execute all validations related with Create Electoral Board Key command
 */
public class CreateEBKeyVerifier {

	private final ConfigObjectMapper configObjectMapper = new ConfigObjectMapper();

	private final IDsParser idsParser = new IDsParser();

	public void createEBKeyPairValidations(final File eeidFolder, final Path ebPropertiesPath, String vcsids) throws IOException {

		offlineFolderValidations(eeidFolder, ebPropertiesPath);

		onlineFolderValidations(eeidFolder, ebPropertiesPath, vcsids);
	}

	private void offlineFolderValidations(final File eeidFolder, final Path ebPropertiesPath) throws IOException {

		final Map<String, List<String>> ballotMappings = getBallotMappings(ebPropertiesPath);

		for (final String ballotId : ballotMappings.keySet()) {

			for (final String ballotBoxID : ballotMappings.get(ballotId)) {

				final Path ballotIdPath = Paths
						.get(eeidFolder.getAbsolutePath(), Constants.CONFIG_DIR_NAME_OFFLINE, Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION,
								Constants.CONFIG_DIR_NAME_BALLOTS, ballotId, Constants.CONFIG_DIR_NAME_BALLOTBOXES, ballotBoxID);

				final Path ebPrivateKeyPath = Paths.get(ballotIdPath.toString(), Constants.EB_PRIVATE_KEY_FILENAME);
				assertTrue(Files.exists(ebPrivateKeyPath));
			}
		}
	}

	private void onlineFolderValidations(final File eeidFolder, final Path ebPropertiesPath, String vcsids) throws IOException {

		Map<String, List<String>> ballotMappings = getBallotMappings(ebPropertiesPath);
		for (String ballotId : ballotMappings.keySet()) {

			Path ballotsBoxesFolder = Paths
					.get(eeidFolder.getAbsolutePath(), Constants.CONFIG_DIR_NAME_ONLINE, Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION,
							Constants.CONFIG_DIR_NAME_BALLOTS, ballotId, Constants.CONFIG_DIR_NAME_BALLOTBOXES);

			for (String ballotBoxId : ballotMappings.get(ballotId)) {

				Path ballotBoxIdFolder = Paths.get(ballotsBoxesFolder.toString(), ballotBoxId);
				Path ballotBoxJSONPath = Paths.get(ballotBoxIdFolder.toString(), Constants.CONFIG_DIR_NAME_BALLOTBOX_JSON);

				BallotBox ballotBox = configObjectMapper.fromJSONFileToJava(ballotBoxJSONPath.toFile(), BallotBox.class);
				assertTrue(StringUtils.isNotEmpty(ballotBox.getElectoralAuthorityId()));
			}
		}

		for (String vcsid : idsParser.parse(vcsids)) {
			Path voteVerificationContextDataPath = Paths
					.get(eeidFolder.getAbsolutePath(), Constants.CONFIG_DIR_NAME_ONLINE, Constants.CONFIG_DIR_NAME_VOTERVERIFICATION, vcsid,
							Constants.VOTE_VERIFICATION_CONTEXT_DATA + Constants.JSON);
			assertTrue(Files.exists(voteVerificationContextDataPath));

			VoteVerificationContextData voteVerificationContextData = configObjectMapper
					.fromJSONFileToJava(voteVerificationContextDataPath.toFile(), VoteVerificationContextData.class);
			assertTrue(StringUtils.isNotEmpty(voteVerificationContextData.getElectoralAuthorityId()));
		}
	}

	private Map<String, List<String>> getBallotMappings(final Path ebPropertiesPath) throws IOException {
		Properties props = new Properties();
		try (BufferedReader bufferedReader = Files.newBufferedReader(ebPropertiesPath)) {
			props.load(bufferedReader);
		}

		Map<String, List<String>> ballotMappings = new HashMap<>();
		for (String key : props.stringPropertyNames()) {

			String values = props.getProperty(key);
			String[] ballotBoxesIds = values.split(",");

			ballotMappings.put(key, Arrays.asList(ballotBoxesIds));
		}

		return ballotMappings;
	}

}
