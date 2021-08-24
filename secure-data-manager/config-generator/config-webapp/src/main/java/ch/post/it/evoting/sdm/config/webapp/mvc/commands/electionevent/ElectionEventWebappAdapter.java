/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.webapp.mvc.commands.electionevent;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.springframework.web.bind.annotation.RequestBody;

import ch.post.it.evoting.domain.election.AuthenticationParams;
import ch.post.it.evoting.domain.election.ElectionInformationParams;
import ch.post.it.evoting.domain.election.VotingWorkflowContextData;
import ch.post.it.evoting.domain.election.helpers.ReplacementsHolder;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.domain.CreateElectionEventCertificatePropertiesContainer;
import ch.post.it.evoting.sdm.commons.domain.CreateElectionEventInput;
import ch.post.it.evoting.sdm.config.commands.electionevent.CreateElectionEventParametersHolder;
import ch.post.it.evoting.sdm.config.commands.electionevent.datapacks.beans.ElectionInputDataPack;

public class ElectionEventWebappAdapter {

	public CreateElectionEventParametersHolder adapt(
			@RequestBody
			final CreateElectionEventInput input) {
		ElectionInputDataPack electionInputDataPack = new ElectionInputDataPack();

		// get EEID parameter
		String eeid = input.getEeid();
		electionInputDataPack.setEeid(eeid);
		ReplacementsHolder replacementHolder = new ReplacementsHolder(eeid);
		electionInputDataPack.setReplacementsHolder(replacementHolder);

		String end = input.getEnd();
		Integer validityPeriod = input.getValidityPeriod();

		// ISO_INSTANT format => 2011-12-03T10:15:30Z
		ZonedDateTime startValidityPeriod = ZonedDateTime.now(ZoneOffset.UTC);
		ZonedDateTime electionEndDate = ZonedDateTime.ofInstant(Instant.parse(end), ZoneOffset.UTC);
		ZonedDateTime endValidityPeriod = electionEndDate.plusYears(validityPeriod);

		if (electionEndDate.isAfter(endValidityPeriod)) {
			throw new IllegalArgumentException("End date cannot be after Start date plus validity period.");
		}

		electionInputDataPack.setStartDate(startValidityPeriod);
		electionInputDataPack.setEndDate(endValidityPeriod);

		String challengeResExpTime = input.getChallengeResExpTime();
		String authTokenExpTime = input.getAuthTokenExpTime();
		String challengeLength = input.getChallengeLength();
		String numVotesPerVotingCard = input.getNumVotesPerVotingCard();
		String numVotesPerAuthToken = input.getNumVotesPerAuthToken();
		String maxNumberOfAttempts = input.getMaxNumberOfAttempts();

		ElectionInformationParams electionInformationParams = new ElectionInformationParams(numVotesPerVotingCard, numVotesPerAuthToken);

		AuthenticationParams authenticationParams = new AuthenticationParams(challengeResExpTime, authTokenExpTime, challengeLength);

		VotingWorkflowContextData votingWorkflowContextData = new VotingWorkflowContextData();
		votingWorkflowContextData.setMaxNumberOfAttempts(maxNumberOfAttempts);

		String outputPath = input.getOutputPath();

		Path electionFolder = Paths.get(outputPath, eeid);

		Path offlinePath = electionFolder.resolve(Constants.CONFIG_DIR_NAME_OFFLINE);

		Path onlinePath = electionFolder.resolve(Constants.CONFIG_DIR_NAME_ONLINE);

		Path autenticationPath = onlinePath.resolve(Constants.CONFIG_DIR_NAME_AUTHENTICATION);

		Path electionInformationPath = onlinePath.resolve(Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION);

		Path votingWorkflowPath = onlinePath.resolve(Constants.CONFIG_DIR_NAME_VOTINGWORKFLOW);

		String keyForProtectingKeystorePassword = input.getKeyForProtectingKeystorePassword();

		CreateElectionEventCertificatePropertiesContainer certificatePropertiesInput = input.getCertificatePropertiesInput();

		return new CreateElectionEventParametersHolder(electionInputDataPack, Paths.get(outputPath), electionFolder, offlinePath, autenticationPath,
				electionInformationPath, votingWorkflowPath, authenticationParams, electionInformationParams, votingWorkflowContextData,
				keyForProtectingKeystorePassword, certificatePropertiesInput);
	}
}
