/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.webapp.mvc.commands.ballotbox;

import static ch.post.it.evoting.cryptolib.commons.validations.Validate.validateUUID;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Properties;

import ch.post.it.evoting.sdm.commons.domain.CreateBallotBoxesInput;
import ch.post.it.evoting.sdm.config.commands.ballotbox.BallotBoxParametersHolder;
import ch.post.it.evoting.sdm.config.commands.electionevent.datapacks.beans.ElectionInputDataPack;

public class BallotBoxWebappAdapter {

	public BallotBoxParametersHolder adapt(final CreateBallotBoxesInput input) {
		final BallotBoxParametersHolder holder;

		final String ballotID = input.getBallotID();

		final String electoralAuthorityId = input.getElectoralAuthorityID();

		final String ballotBoxID = input.getBallotBoxID();

		final String alias = input.getAlias();

		final String isTest = input.getTest();

		final String gracePeriod = input.getGracePeriod();

		final String outputFolder = input.getOutputFolder();

		validateOutPutFolder(outputFolder);

		final Path absolutePath = Paths.get(outputFolder).toAbsolutePath();

		final String eeID = absolutePath.getFileName().toString();

		final ElectionInputDataPack electionInputDataPack = new ElectionInputDataPack();

		electionInputDataPack.setEeid(eeID);

		final Integer validityPeriod = input.getValidityPeriod();

		final ZonedDateTime startValidityPeriod = ZonedDateTime.now(ZoneOffset.UTC);

		final ZonedDateTime electionStartDate = ZonedDateTime.ofInstant(Instant.parse(input.getStart()), ZoneOffset.UTC);
		final ZonedDateTime electionEndDate = ZonedDateTime.ofInstant(Instant.parse(input.getEnd()), ZoneOffset.UTC);

		final ZonedDateTime endValidityPeriod = electionEndDate.plusYears(validityPeriod);

		if (electionEndDate.isAfter(endValidityPeriod)) {
			throw new IllegalArgumentException("End date cannot be after Start date plus validity period.");
		}

		electionInputDataPack.setStartDate(startValidityPeriod);
		electionInputDataPack.setEndDate(endValidityPeriod);
		electionInputDataPack.setElectionStartDate(electionStartDate);
		electionInputDataPack.setElectionEndDate(electionEndDate);

		final String writeInAlphabet = input.getWriteInAlphabet();
		final Properties certificateProperties = input.getBallotBoxCertificateProperties();

		holder = new BallotBoxParametersHolder(ballotID, electoralAuthorityId, ballotBoxID, alias, absolutePath, eeID, electionInputDataPack, isTest,
				gracePeriod, writeInAlphabet, certificateProperties);
		holder.setKeyForProtectingKeystorePassword(input.getKeyForProtectingKeystorePassword());

		return holder;
	}

	private void validateOutPutFolder(final String outputFolder) {

		final Path outputPath = Paths.get(outputFolder).toAbsolutePath();

		if (!outputPath.toFile().exists()) {
			throw new IllegalArgumentException(String.format("The given output path: \"%s\" does not exist.", outputPath));
		}

		final String electionEventID = outputPath.getFileName().toString();
		validateUUID(electionEventID);
	}

}
