/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.domain.election.VerificationCardSetData;
import ch.post.it.evoting.domain.election.VoteVerificationContextData;
import ch.post.it.evoting.sdm.config.commands.voters.JobExecutionObjectContext;
import ch.post.it.evoting.sdm.config.commands.voters.VotersParametersHolder;
import ch.post.it.evoting.sdm.config.commands.voters.VotersSerializationDestProvider;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VerificationCardSetCredentialDataPack;
import ch.post.it.evoting.sdm.config.exceptions.CreateVotingCardSetException;
import ch.post.it.evoting.sdm.utils.ConfigObjectMapper;

class WriteVerificationDataTasklet implements Tasklet {

	private static final Logger LOGGER = LoggerFactory.getLogger(WriteVerificationDataTasklet.class);

	private final VotersSerializationDestProvider destProvider;

	private final JobExecutionObjectContext objectContext;

	public WriteVerificationDataTasklet(final VotersSerializationDestProvider destProvider, final JobExecutionObjectContext objectContext) {
		this.destProvider = destProvider;
		this.objectContext = objectContext;
	}

	@Override
	public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {

		final VotingCardGenerationJobExecutionContext jobExecutionContext = new VotingCardGenerationJobExecutionContext(
				chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext());

		try {

			final String jobInstanceId = jobExecutionContext.getJobInstanceId();
			final String electionEventId = jobExecutionContext.getElectionEventId();
			final String verificationCardSetId = jobExecutionContext.getVerificationCardSetId();
			final String electoralAuthorityId = jobExecutionContext.getElectoralAuthorityId();

			final VotersParametersHolder votersParametersHolder = objectContext.get(jobInstanceId, VotersParametersHolder.class);

			final VerificationCardSetCredentialDataPack verificationCardSetCredentialDataPack = objectContext
					.get(jobInstanceId, VerificationCardSetCredentialDataPack.class);

			final VerificationCardSetData verificationCardSetData = new VerificationCardSetData();
			verificationCardSetData.setVerificationCardSetId(verificationCardSetId);
			verificationCardSetData.setElectionEventId(electionEventId);

			final ElGamalPublicKey choiceCodesPublicKey = verificationCardSetCredentialDataPack.getChoiceCodesEncryptionPublicKey();
			String choiceCodesPublicKeyB64;
			try {
				choiceCodesPublicKeyB64 = Base64.getEncoder().encodeToString(choiceCodesPublicKey.toJson().getBytes(StandardCharsets.UTF_8));
			} catch (GeneralCryptoLibException e) {
				throw new CreateVotingCardSetException(
						"An error occurred while attempting to serialize the choices code publickey: " + e.getMessage(), e);
			}
			verificationCardSetData.setChoicesCodesEncryptionPublicKey(choiceCodesPublicKeyB64);

			final String verificationCardSetIssuerCert = getPemEncodedCertificate(
					verificationCardSetCredentialDataPack.getVerificationCardSetIssuerCert());
			verificationCardSetData.setVerificationCardSetIssuerCert(verificationCardSetIssuerCert);

			final ConfigObjectMapper configObjectMapper = new ConfigObjectMapper();

			final Path verDataSetDataJSON = destProvider.getVerificationCardSetData();
			final File verificationCardSetDataFile = verDataSetDataJSON.toFile();
			configObjectMapper.fromJavaToJSONFile(verificationCardSetData, verificationCardSetDataFile);

			final VoteVerificationContextData voteVerificationContextData = new VoteVerificationContextData();
			voteVerificationContextData.setElectionEventId(electionEventId);
			voteVerificationContextData.setVerificationCardSetId(verificationCardSetId);
			voteVerificationContextData.setEncryptionParameters(votersParametersHolder.getEncryptionParameters());

			voteVerificationContextData.setElectoralAuthorityId(electoralAuthorityId);

			final ElGamalPublicKey[] nonCombinedChoiceCodesEncryptionPublicKeys = verificationCardSetCredentialDataPack
					.getNonCombinedChoiceCodesEncryptionPublicKeys();
			StringJoiner nonCombinedChoiceCodesPublicKeysB64Builder = new StringJoiner(";");
			for (ElGamalPublicKey nonCombinedElGamalPublicKey : nonCombinedChoiceCodesEncryptionPublicKeys) {

				try {
					String nonCombinedChoiceCodesPublicKeyB64 = Base64.getEncoder()
							.encodeToString(nonCombinedElGamalPublicKey.toJson().getBytes(StandardCharsets.UTF_8));
					nonCombinedChoiceCodesPublicKeysB64Builder.add(nonCombinedChoiceCodesPublicKeyB64);
				} catch (GeneralCryptoLibException e) {
					throw new CreateVotingCardSetException(
							"An error occurred while attempting to serialize one of the non combined choices code publickeys: " + e.getMessage(), e);
				}
			}
			voteVerificationContextData.setNonCombinedChoiceCodesEncryptionPublicKeys(nonCombinedChoiceCodesPublicKeysB64Builder.toString());

			final Path voteVerContextDataJSON = destProvider.getVoteVerificationContextData();
			final File voteVerificationContextDataFile = voteVerContextDataJSON.toFile();
			configObjectMapper.fromJavaToJSONFile(voteVerificationContextData, voteVerificationContextDataFile);

		} catch (Exception e) {
			LOGGER.error("Write verification data task failed.", e);
			throw e;
		}
		return RepeatStatus.FINISHED;
	}

	private String getPemEncodedCertificate(final CryptoAPIX509Certificate certificate) {
		final byte[] certPEMArray = certificate.getPemEncoded();
		return new String(certPEMArray, StandardCharsets.UTF_8);
	}
}
