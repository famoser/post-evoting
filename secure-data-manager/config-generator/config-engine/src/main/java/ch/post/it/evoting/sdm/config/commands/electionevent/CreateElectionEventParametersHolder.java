/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.electionevent;

import java.nio.file.Path;

import ch.post.it.evoting.domain.election.AuthenticationParams;
import ch.post.it.evoting.domain.election.ElectionInformationParams;
import ch.post.it.evoting.domain.election.VotingWorkflowContextData;
import ch.post.it.evoting.sdm.commons.domain.CreateElectionEventCertificatePropertiesContainer;
import ch.post.it.evoting.sdm.config.commands.electionevent.datapacks.beans.ElectionInputDataPack;
import ch.post.it.evoting.sdm.domain.common.ConfigurationInput;

/**
 * The container with all the info needed by the CreateElectionEventGenerator.
 */
public class CreateElectionEventParametersHolder {

	private final ElectionInputDataPack electionInputDataPack;

	private final Path electionFolder;

	private final Path outputFolder;

	private final Path offlineFolder;

	private final Path onlineAuthenticationFolder;

	private final Path onlineElectionInformationFolder;

	private final Path onlineVotingWorkflowFolder;

	private final AuthenticationParams authenticationParams;

	private final ElectionInformationParams electionInformationParams;

	private final VotingWorkflowContextData votingWorkflowContextData;
	private final String keyForProtectingKeystorePassword;
	private final CreateElectionEventCertificatePropertiesContainer certificatePropertiesInput;
	private ConfigurationInput configurationInput;

	public CreateElectionEventParametersHolder(final ElectionInputDataPack electionInputDataPack, final Path outputFolder, final Path electionFolder,
			final Path offlineFolder, final Path onlineAuthenticationFolder, final Path onlineElectionInformationFolder,
			final Path onlineVotingWorkflowFolder, final AuthenticationParams authenticationParams,
			final ElectionInformationParams electionInformationParams, final VotingWorkflowContextData votingWorkflowContextData,
			final String keyForProtectingKeystorePassword, final CreateElectionEventCertificatePropertiesContainer certificatePropertiesInput) {

		this.electionInputDataPack = electionInputDataPack;
		this.outputFolder = outputFolder;
		this.electionFolder = electionFolder;
		this.offlineFolder = offlineFolder;
		this.onlineAuthenticationFolder = onlineAuthenticationFolder;
		this.onlineElectionInformationFolder = onlineElectionInformationFolder;
		this.onlineVotingWorkflowFolder = onlineVotingWorkflowFolder;
		this.authenticationParams = authenticationParams;
		this.electionInformationParams = electionInformationParams;
		this.votingWorkflowContextData = votingWorkflowContextData;
		this.keyForProtectingKeystorePassword = keyForProtectingKeystorePassword;
		this.certificatePropertiesInput = certificatePropertiesInput;
	}

	/**
	 * @return Returns the onlineAuthenticationFolder.
	 */
	public Path getOnlineAuthenticationFolder() {
		return onlineAuthenticationFolder;
	}

	/**
	 * @return Returns the onlineElectionInformationFolder.
	 */
	public Path getOnlineElectionInformationFolder() {
		return onlineElectionInformationFolder;
	}

	/**
	 * @return Returns the onlinevotingWorkflowFolder.
	 */
	public Path getOnlineVotingWorkflowFolder() {
		return onlineVotingWorkflowFolder;
	}

	/**
	 * @return Returns the outputFolder.
	 */
	public Path getOutputFolder() {
		return outputFolder;
	}

	/**
	 * @return Returns the offlineFolder.
	 */
	public Path getOfflineFolder() {
		return offlineFolder;
	}

	/**
	 * @return Returns the authenticationParams.
	 */
	public AuthenticationParams getAuthenticationParams() {
		return authenticationParams;
	}

	/**
	 * @return Returns the electionInformationParams.
	 */
	public ElectionInformationParams getElectionInformationParams() {
		return electionInformationParams;
	}

	/**
	 * @return Returns the inputDataPack.
	 */
	public ElectionInputDataPack getInputDataPack() {
		return electionInputDataPack;
	}

	/**
	 * @return Returns the votingWorkflowContextData.
	 */
	public VotingWorkflowContextData getVotingWorkflowContextData() {
		return votingWorkflowContextData;
	}

	/**
	 * @return Returns the configurationInput.
	 */
	public ConfigurationInput getConfigurationInput() {
		return configurationInput;
	}

	/**
	 * @param configurationInput The configurationInput to set.
	 */
	public void setConfigurationInput(final ConfigurationInput configurationInput) {
		this.configurationInput = configurationInput;
	}

	public String getKeyForProtectingKeystorePassword() {
		return keyForProtectingKeystorePassword;
	}

	/**
	 * @return Returns the electionFolder.
	 */
	public Path getElectionFolder() {
		return electionFolder;
	}

	/**
	 * Get all the certificate properties.
	 *
	 * @return all the certificate properties.
	 */
	public CreateElectionEventCertificatePropertiesContainer getCertificatePropertiesInput() {
		return certificatePropertiesInput;
	}
}
