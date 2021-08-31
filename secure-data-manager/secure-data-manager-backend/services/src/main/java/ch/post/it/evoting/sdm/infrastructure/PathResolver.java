/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure;

import static ch.post.it.evoting.cryptolib.commons.validations.Validate.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.sdm.commons.Constants;

@Service
public class PathResolver {

	@Value("${sdm.workspace}")
	private String workspace;

	/**
	 * Provides the election information directory path in the SDM workspace for the given election event.
	 * <p>
	 * The path corresponds to the location {@value Constants#CONFIG_FILES_BASE_DIR}/{@code electionEventId}/{@value
	 * Constants#CONFIG_DIR_NAME_ONLINE}/{@value Constants#CONFIG_DIR_NAME_ELECTIONINFORMATION}.
	 *
	 * @param electionEventId the election event id for which to retrieve the election information directory. Must be non-null and a valid UUID.
	 * @return the election information path in the SDM workspace.
	 * @throws NullPointerException     if {@code electionEventId} is null.
	 * @throws IllegalArgumentException if {@code electionEventId} is not valid.
	 */
	public Path resolveElectionInformationPath(final String electionEventId) {
		checkNotNull(electionEventId);
		validateUUID(electionEventId);

		return Paths.get(workspace, Constants.CONFIG_FILES_BASE_DIR, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE,
				Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION);
	}

	/**
	 * Provides the ballot box directory path in the SDM workspace for the given election event, ballot and ballot box.
	 * <p>
	 * The path corresponds to the location {@value Constants#CONFIG_FILES_BASE_DIR}/{@code electionEventId}/{@value
	 * Constants#CONFIG_DIR_NAME_ONLINE}/{@value Constants#CONFIG_DIR_NAME_ELECTIONINFORMATION}/{@value Constants#CONFIG_DIR_NAME_BALLOTS}/ {@code
	 * ballotId}/{@value Constants#CONFIG_DIR_NAME_BALLOTBOXES}/{@code ballotBoxId}.
	 *
	 * @param electionEventId the election event the ballot box belongs to. Must be non-null and a valid UUID.
	 * @param ballotId        the ballot the ballot box belongs to. Must be non-null and a valid UUID.
	 * @param ballotBoxId     the expected ballot box. Must be non-null and a valid UUID.
	 * @return the ballot box path in the SDM workspace.
	 * @throws NullPointerException     if any of the inputs is null.
	 * @throws IllegalArgumentException if any of the inputs is not valid.
	 */
	public Path resolveBallotBoxPath(final String electionEventId, final String ballotId, final String ballotBoxId) {
		checkNotNull(electionEventId);
		checkNotNull(ballotId);
		checkNotNull(ballotBoxId);
		validateUUID(electionEventId);
		validateUUID(ballotId);
		validateUUID(ballotBoxId);

		return Paths.get(workspace, Constants.CONFIG_FILES_BASE_DIR, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE,
				Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION, Constants.CONFIG_DIR_NAME_BALLOTS, ballotId, Constants.CONFIG_DIR_NAME_BALLOTBOXES,
				ballotBoxId);
	}

	/**
	 * Provides the election event printing directory path in the SDM workspace for the given election event.
	 * <p>
	 * The path corresponds to the location {@value Constants#CONFIG_FILES_BASE_DIR}/{@code electionEventId}/{@value
	 * Constants#CONFIG_DIR_NAME_ONLINE}/{@value Constants#CONFIG_DIR_NAME_PRINTING}.
	 *
	 * @param electionEventId the election event the printing directory belongs to. Must be non-null and a valid UUID.
	 * @return the election event printing path in the SDM workspace.
	 * @throws NullPointerException     if input is null.
	 * @throws IllegalArgumentException if input is not valid.
	 */
	public Path resolvePrintingPath(final String electionEventId) {
		checkNotNull(electionEventId);
		validateUUID(electionEventId);

		return Paths.get(workspace, Constants.CONFIG_FILES_BASE_DIR, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE,
				Constants.CONFIG_DIR_NAME_PRINTING);
	}

	/**
	 * Provides the election event customer output directory path in the SDM workspace for the given election event.
	 * <p>
	 * The path corresponds to the location {@value Constants#CONFIG_FILES_BASE_DIR}/{@code electionEventId}/{@value
	 * Constants#CONFIG_DIR_NAME_CUSTOMER}/{@value Constants#CONFIG_DIR_NAME_OUTPUT}.
	 *
	 * @param electionEventId the election event the customer output directory belongs to. Must be non-null and a valid UUID.
	 * @return the election event customer output path in the SDM workspace.
	 * @throws NullPointerException     if input is null.
	 * @throws IllegalArgumentException if input is not valid.
	 */
	public Path resolveOutputPath(final String electionEventId) {
		checkNotNull(electionEventId);
		validateUUID(electionEventId);

		return Paths.get(workspace, Constants.CONFIG_FILES_BASE_DIR, electionEventId, Constants.CONFIG_DIR_NAME_CUSTOMER,
				Constants.CONFIG_DIR_NAME_OUTPUT);
	}

	/**
	 * Provides the election event customer input directory path in the SDM workspace for the given election event.
	 * <p>
	 * The path corresponds to the location {@value Constants#CONFIG_FILES_BASE_DIR}/{@code electionEventId}/{@value
	 * Constants#CONFIG_DIR_NAME_CUSTOMER}/{@value Constants#CONFIG_DIR_NAME_INPUT}.
	 *
	 * @param electionEventId the election event the customer input directory belongs to. Must be non-null and a valid UUID.
	 * @return the election event customer input path in the SDM workspace.
	 * @throws NullPointerException     if input is null.
	 * @throws IllegalArgumentException if input is not valid.
	 */
	public Path resolveInputPath(final String electionEventId) {
		checkNotNull(electionEventId);
		validateUUID(electionEventId);

		return Paths.get(workspace, Constants.CONFIG_FILES_BASE_DIR, electionEventId, Constants.CONFIG_DIR_NAME_CUSTOMER,
				Constants.CONFIG_DIR_NAME_INPUT);
	}

	/**
	 * Provides the integration election event output directory path in the SDM workspace for the given election alias.
	 * <p>
	 * The path corresponds to the location {@value Constants#INTEGRATION_FILES_BASE_DIR}/{@value Constants#INTEGRATION_DIR_NAME_ELECTION_EVENTS}/{@code
	 * electionEventAlias}/{@value Constants#INTEGRATION_DIR_NAME_OUTPUT}.
	 *
	 * @param electionEventAlias the election event the customer output directory belongs to. Must be non-null.
	 * @return the integration election event output path in the SDM workspace.
	 * @throws NullPointerException if electionEventAlias is null.
	 */
	public Path resolveIntegrationOutputPath(final String electionEventAlias) {
		checkNotNull(electionEventAlias);

		return Paths.get(workspace, Constants.INTEGRATION_FILES_BASE_DIR, Constants.INTEGRATION_DIR_NAME_ELECTION_EVENTS, electionEventAlias,
				Constants.INTEGRATION_DIR_NAME_OUTPUT);
	}

	/**
	 * Provides the integration election event input directory path in the SDM workspace for the given election alias.
	 * <p>
	 * The path corresponds to the location {@value Constants#INTEGRATION_FILES_BASE_DIR}/{@value Constants#INTEGRATION_DIR_NAME_ELECTION_EVENTS}/{@code
	 * electionEventAlias}/{@value Constants#INTEGRATION_DIR_NAME_INPUT}.
	 *
	 * @param electionEventAlias the election event the customer input directory belongs to. Must be non-null.
	 * @return the integration election event input path in the SDM workspace.
	 * @throws NullPointerException if electionEventAlias is null.
	 */
	public Path resolveIntegrationInputPath(final String electionEventAlias) {
		checkNotNull(electionEventAlias);

		return Paths.get(workspace, Constants.INTEGRATION_FILES_BASE_DIR, Constants.INTEGRATION_DIR_NAME_ELECTION_EVENTS, electionEventAlias,
				Constants.INTEGRATION_DIR_NAME_INPUT);
	}

}
