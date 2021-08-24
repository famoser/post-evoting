/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch.writers;

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;

import ch.post.it.evoting.logging.api.domain.Level;
import ch.post.it.evoting.logging.api.domain.LogContent;
import ch.post.it.evoting.sdm.config.exceptions.specific.GenerateVerificationCardCodesException;
import ch.post.it.evoting.sdm.config.logevents.ConfigGeneratorLogEvents;
import ch.post.it.evoting.sdm.config.spring.batch.GeneratedVotingCardOutput;
import ch.post.it.evoting.sdm.datapacks.beans.SerializedCredentialDataPack;

public class CredentialDataWriter extends MultiFileDataWriter<GeneratedVotingCardOutput> {

	public CredentialDataWriter(final Path basePath, final int maxNumCredentialsPerFile) {
		super(basePath, maxNumCredentialsPerFile);
	}

	@Override
	protected String getLine(GeneratedVotingCardOutput item) {
		final String credentialId = item.getCredentialId();
		final String votingCardSetId = item.getVotingCardSetId();
		final String electionEventId = item.getElectionEventId();
		final SerializedCredentialDataPack voterCredentialDataPack = item.getVoterCredentialDataPack();

		final String credentialSerializedKeyStoreB64 = voterCredentialDataPack.getSerializedKeyStore();
		if (StringUtils.isNotBlank(credentialSerializedKeyStoreB64)) {
			loggingWriter.log(Level.DEBUG, new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENCREDAT_SUCCESS_KEYSTORE_GENERATED)
					.electionEvent(electionEventId).user("adminID").objectId(votingCardSetId).additionalInfo("c_id", credentialId).createLogInfo());
		} else {
			String errorMsg = "Error - the keyStore that is being written to file is invalid";
			loggingWriter.log(Level.ERROR, new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENCREDAT_ERROR_GENERATING_KEYSTORE)
					.objectId(votingCardSetId).electionEvent(electionEventId).user("adminID").additionalInfo("c_id", credentialId)
					.additionalInfo("err_desc", errorMsg).createLogInfo());
			throw new GenerateVerificationCardCodesException(errorMsg);
		}

		return String.format("%s,%s", credentialId, credentialSerializedKeyStoreB64);
	}
}
