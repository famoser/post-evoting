/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.webapp.mvc.commands.voters;

import static ch.post.it.evoting.cryptolib.commons.validations.Validate.validateUUID;
import static java.nio.file.Files.notExists;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.RequestBody;

import ch.post.it.evoting.domain.election.Ballot;
import ch.post.it.evoting.sdm.commons.domain.CreateVotingCardSetInput;
import ch.post.it.evoting.sdm.config.commands.voters.VotersParametersHolder;
import ch.post.it.evoting.sdm.utils.ConfigObjectMapper;

public class VotersWebappAdapter {

	private final ConfigObjectMapper mapper;

	public VotersWebappAdapter(final ConfigObjectMapper mapper) {
		this.mapper = mapper;
	}

	public VotersParametersHolder adapt(
			@RequestBody
			final CreateVotingCardSetInput input) {

		final int numberVotingCards;
		final String ballotID;
		final String ballotBoxID;
		final String votingCardSetID;
		final String verificationCardSetID;
		final String electoralAuthorityID;
		final Path absoluteBasePath;
		final int numCredentialsPerFile;
		final int numProcessors;
		final String eeID;
		final Ballot ballot;
		List<String> choiceCodesEncryptionKeyAsListStrings;
		VotersParametersHolder holder;

		numberVotingCards = input.getNumberVotingCards();
		ballotID = input.getBallotID();
		ballotBoxID = input.getBallotBoxID();
		votingCardSetID = input.getVotingCardSetID();
		verificationCardSetID = input.getVerificationCardSetID();
		electoralAuthorityID = input.getElectoralAuthorityID();
		choiceCodesEncryptionKeyAsListStrings = input.getChoiceCodesEncryptionKey();

		checkGivenIDsAreUUIDs(ballotID, ballotBoxID, votingCardSetID);

		absoluteBasePath = parseBaseToAbsolutePath(input.getBasePath());
		eeID = absoluteBasePath.getFileName().toString();

		ballot = getBallot(input.getBallotPath());

		validateBallotAndBallotIDMatch(ballot, ballotID);

		numCredentialsPerFile = input.getNumCredentialsPerFile();

		numProcessors = getNumProcessors(input.getNumProcessors());

		final ZonedDateTime startValidityPeriod;
		final ZonedDateTime endValidityPeriod;

		final String end = input.getEnd();

		final Integer validityPeriod = input.getValidityPeriod();

		startValidityPeriod = ZonedDateTime.now(ZoneOffset.UTC);

		final ZonedDateTime electionEndDate = ZonedDateTime.ofInstant(Instant.parse(end), ZoneOffset.UTC);

		final String platformRootCACertificate = input.getPlatformRootCACertificate();

		endValidityPeriod = electionEndDate.plusYears(validityPeriod);

		if (electionEndDate.isAfter(endValidityPeriod)) {
			throw new IllegalArgumentException("End date cannot be after Start date plus validity period.");
		}

		String keyForProtectingKeystorePassword = input.getKeyForProtectingKeystorePassword();

		holder = new VotersParametersHolder(numberVotingCards, ballotID, ballot, ballotBoxID, votingCardSetID, verificationCardSetID,
				electoralAuthorityID, absoluteBasePath, numCredentialsPerFile, numProcessors, eeID, startValidityPeriod, endValidityPeriod,
				keyForProtectingKeystorePassword, input.getVotingCardSetAlias(), choiceCodesEncryptionKeyAsListStrings, platformRootCACertificate,
				input.getCreateVotingCardSetCertificateProperties());

		return holder;
	}

	private int getNumProcessors(final int numProcessorsInput) {
		final int numProcessors;

		if (numProcessorsInput != 0) {
			numProcessors = numProcessorsInput;
			if (numProcessors < 1) {
				throw new IllegalArgumentException("The minimum number of processors should be 1.");
			}
			if (numProcessors > Runtime.getRuntime().availableProcessors()) {
				throw new IllegalArgumentException(
						"The given number of processors is higher than the maximum allowed, which is: " + Runtime.getRuntime().availableProcessors()
								+ ". ");
			}
		} else {
			numProcessors = 1;
		}
		return numProcessors;
	}

	private Path parseBaseToAbsolutePath(final String basePath) {

		final Path baseAbsolutePath = Paths.get(basePath).toAbsolutePath();

		final String prefixErrorMessage = "The given base path: \"" + basePath;

		checkFile(basePath, baseAbsolutePath);

		final String eeid = baseAbsolutePath.getFileName().toString();
		try {
			validateUUID(eeid);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(prefixErrorMessage + "\" requires an election event id in UUID format.", e);
		}

		return baseAbsolutePath;
	}

	private void checkGivenIDsAreUUIDs(final String ballotID, final String ballotBoxID, final String votingCardSetID) {

		validateUUID(ballotID);
		validateUUID(ballotBoxID);
		validateUUID(votingCardSetID);
	}

	private Ballot getBallot(final String ballotPath) {

		final Path ballotAbsolutePath = Paths.get(ballotPath).toAbsolutePath();

		final File ballotFile = ballotAbsolutePath.toFile();

		checkFile(ballotPath, ballotAbsolutePath);

		return getBallotFromFile(ballotPath, ballotFile);
	}

	private Ballot getBallotFromFile(final String ballotPath, final File ballotFile) {
		Ballot ballot;
		try {
			ballot = mapper.fromJSONFileToJava(ballotFile, Ballot.class);
		} catch (final IOException e) {
			throw new IllegalArgumentException("An error occurred while mapping \"" + ballotPath + "\" to a Ballot.", e);
		}
		return ballot;
	}

	private void validateBallotAndBallotIDMatch(final Ballot ballot, final String ballotID) {
		if (!ballot.getId().equals(ballotID)) {
			throw new IllegalArgumentException("The given Ballot with ID: " + ballot.getId() + " and the given ballotID: " + ballotID
					+ " are different. They must be the same.");
		}
	}

	private void checkFile(final String path, final Path absolutePath) {
		final String errorMessageBallot = "The given file: \"" + path + "\"";

		final String errorHelpMessage = " The given path should be either (1) relative to the execution path or (2) absolute.";

		if (notExists(absolutePath)) {
			throw new IllegalArgumentException(errorMessageBallot + " could not be found." + errorHelpMessage);
		}
	}

}
