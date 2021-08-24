/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.mixnetpayload;

import static ch.post.it.evoting.cryptolib.commons.validations.Validate.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;

import ch.post.it.evoting.domain.mixnet.MixnetShufflePayload;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.infrastructure.PathResolver;

@Repository
public class MixnetShufflePayloadFileRepository {

	private final String filePrefix;
	private final ObjectMapper objectMapper;
	private final PathResolver payloadResolver;

	@Autowired
	public MixnetShufflePayloadFileRepository(
			@Value("${shufflePayload.filePrefix:mixnetShufflePayload_}")
			final String filePrefix, final ObjectMapper objectMapper, final PathResolver payloadResolver) {

		this.filePrefix = filePrefix;
		this.objectMapper = objectMapper;
		this.payloadResolver = payloadResolver;
	}

	/**
	 * Gets the mixnet payload stored on the filesystem for the given election, ballot, ballot box, control component combination.
	 *
	 * @return the MixnetShufflePayload object read from the stored file.
	 * @throws NullPointerException     if any of the inputs is null.
	 * @throws IllegalArgumentException if any of the inputs is not valid.
	 * @see PathResolver to get the resolved file Path.
	 */
	public MixnetShufflePayload getPayload(final String electionEventId, final String ballotId, final String ballotBoxId,
			final int controlComponentNodeId) {
		checkNotNull(electionEventId);
		checkNotNull(ballotId);
		checkNotNull(ballotBoxId);
		validateUUID(electionEventId);
		validateUUID(ballotId);
		validateUUID(ballotBoxId);
		checkArgument(controlComponentNodeId >= 1 && controlComponentNodeId <= 3);

		final Path payloadPath = payloadPath(electionEventId, ballotId, ballotBoxId, controlComponentNodeId);

		try {
			return objectMapper.readValue(payloadPath.toFile(), MixnetShufflePayload.class);
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to read the mixnet payload file", e);
		}
	}

	/**
	 * Saves the mixnet payload to the filesystem for the given election, ballot, ballot box, control component combination.
	 *
	 * @return the path of the saved file.
	 * @throws NullPointerException     if any of the inputs is null.
	 * @throws IllegalArgumentException if any of the inputs is not valid.
	 * @see PathResolver to get the resolved file Path.
	 */
	public Path savePayload(final String electionEventId, final String ballotId, final String ballotBoxId, final MixnetShufflePayload payload) {
		checkNotNull(electionEventId);
		checkNotNull(ballotId);
		checkNotNull(ballotBoxId);
		checkNotNull(payload);
		validateUUID(electionEventId);
		validateUUID(ballotId);
		validateUUID(ballotBoxId);

		final Path payloadPath = payloadPath(electionEventId, ballotId, ballotBoxId, payload.getNodeId());

		try {
			final Path filePath = Files.createFile(payloadPath);
			objectMapper.writeValue(filePath.toFile(), payload);
			return filePath;
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to write the mixnet payload file", e);
		}
	}

	@VisibleForTesting
	Path payloadPath(final String electionEventId, final String ballotId, final String ballotBoxId, final int nodeId) {
		final Path ballotBoxPath = payloadResolver.resolveBallotBoxPath(electionEventId, ballotId, ballotBoxId);
		return ballotBoxPath.resolve(filePrefix + nodeId + Constants.JSON);
	}
}