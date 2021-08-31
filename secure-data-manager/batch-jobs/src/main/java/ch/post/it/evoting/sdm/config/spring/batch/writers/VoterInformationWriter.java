/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch.writers;

import java.nio.file.Path;

import ch.post.it.evoting.sdm.config.spring.batch.GeneratedVotingCardOutput;

public class VoterInformationWriter extends MultiFileDataWriter<GeneratedVotingCardOutput> {

	public VoterInformationWriter(final Path basePath, final int maxNumCredentialsPerFile) {
		super(basePath, maxNumCredentialsPerFile);
	}

	@Override
	protected String getLine(GeneratedVotingCardOutput item) {
		final String votingCardId = item.getVotingCardId();
		final String ballotId = item.getBallotId();
		final String ballotBoxId = item.getBallotBoxId();
		final String credentialId = item.getCredentialId();
		final String electionEventId = item.getElectionEventId();
		final String votingCardSetId = item.getVotingCardSetId();
		final String verificationCardId = item.getVerificationCardId();
		final String verificationCardSetId = item.getVerificationCardSetId();

		return String.format("%s,%s,%s,%s,%s,%s,%s,%s", votingCardId, ballotId, ballotBoxId, credentialId, electionEventId, votingCardSetId,
				verificationCardId, verificationCardSetId);
	}
}
