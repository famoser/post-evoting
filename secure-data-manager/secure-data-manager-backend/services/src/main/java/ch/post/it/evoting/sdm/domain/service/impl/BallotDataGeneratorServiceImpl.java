/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service.impl;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.write;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.sdm.application.service.ConsistencyCheckService;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.ballot.BallotRepository;
import ch.post.it.evoting.sdm.domain.model.generator.DataGeneratorResponse;
import ch.post.it.evoting.sdm.domain.service.BallotDataGeneratorService;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;

/**
 * This implementation contains a call to the configuration engine for generating the ballot data.
 */
@Service
public class BallotDataGeneratorServiceImpl implements BallotDataGeneratorService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BallotDataGeneratorServiceImpl.class);
	private final BallotRepository repository;
	private final PathResolver resolver;
	private final ConsistencyCheckService consistencyCheckService;

	/**
	 * Constructor.
	 *
	 * @param repository
	 * @param resolver
	 */
	@Autowired
	public BallotDataGeneratorServiceImpl(BallotRepository repository, PathResolver resolver, ConsistencyCheckService consistencyCheckService) {
		this.repository = repository;
		this.resolver = resolver;
		this.consistencyCheckService = consistencyCheckService;
	}

	/**
	 * This method generates a file with the ballot content in json format.
	 *
	 * @see BallotDataGeneratorService#generate(String, String)
	 */
	@Override
	public DataGeneratorResponse generate(String id, String electionEventId) {
		DataGeneratorResponse result = new DataGeneratorResponse();

		// basic validation of input
		if (StringUtils.isEmpty(id)) {
			result.setSuccessful(false);
			return result;
		}

		// read the ballot from the database
		String json = repository.find(id);

		// simple check if there is a voting card set data returned
		if (JsonConstants.EMPTY_OBJECT.equals(json)) {
			result.setSuccessful(false);
			return result;
		}

		// check consistency for the ballot
		try {
			// Get the representations file for the Election Event
			Path representationsFile = resolver
					.resolve(Constants.SDM_DIR_NAME, Constants.CONFIG_DIR_NAME, electionEventId, Constants.CONFIG_DIR_NAME_CUSTOMER,
							Constants.CONFIG_DIR_NAME_OUTPUT, Constants.CONFIG_FILE_NAME_REPRESENTATIONS_CSV);
			if (!consistencyCheckService.representationsConsistent(json, representationsFile)) {
				String errMsg = "Consistency check of the representations used on the ballot options failed.";
				LOGGER.error(errMsg);
				result.setResult(errMsg);
				result.setSuccessful(false);
				return result;
			}
		} catch (IOException e) {
			LOGGER.error("Failed to read representations file.", e);
			result.setSuccessful(false);
		}

		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

		Path file = resolver.resolve(Constants.CONFIG_FILES_BASE_DIR).resolve(electionEventId).resolve(Constants.CONFIG_DIR_NAME_ONLINE)
				.resolve(Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION).resolve(Constants.CONFIG_DIR_NAME_BALLOTS).resolve(id)
				.resolve(Constants.CONFIG_FILE_NAME_BALLOT_JSON);
		try {
			createDirectories(file.getParent());
			// ballot.json must not be written for each ballot box from the
			// ballot because already working voting card generator can read
			// corrupted content, thus ballot.json must be written only once.
			// Write ballot.json only if the file does not exist or has a
			// different content. The content is pretty small so it is possible
			// compare everything in memory. Synchronized block protects the
			// operation from a concurrent call, because services in Spring are
			// normally singletons.
			synchronized (this) {
				if (!exists(file) || !Arrays.equals(bytes, readAllBytes(file))) {
					write(file, bytes);
				}
			}
		} catch (IOException e) {
			LOGGER.error("Failed to write ballot to file.", e);
			result.setSuccessful(false);
		}
		return result;
	}

	@Override
	public void cleanAll(String electionEventId) {
		Path ballotsPath = resolver.resolve(Constants.CONFIG_FILES_BASE_DIR).resolve(electionEventId).resolve(Constants.CONFIG_DIR_NAME_ONLINE)
				.resolve(Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION).resolve(Constants.CONFIG_DIR_NAME_BALLOTS);

		if (!exists(ballotsPath)) {
			throw new IllegalStateException("Ballot files have not been generated yet");
		}

		File[] ballotPath = ballotsPath.toFile().listFiles();

		for (File ballotFolder : ballotPath) {
			if (ballotFolder.isDirectory()) {
				Path file = ballotFolder.toPath().resolve(Constants.CONFIG_FILE_NAME_BALLOT_JSON);
				try {
					Files.deleteIfExists(file);
				} catch (IOException e) {
					LOGGER.error("Failed to delete ballot file.", e);
				}
			}
		}
	}
}
