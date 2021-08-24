/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch;

import static ch.post.it.evoting.sdm.commons.Constants.CREDENTIAL_ID;
import static ch.post.it.evoting.sdm.commons.Constants.KEYSTORE_PIN;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.primitives.PrimitivesServiceAPI;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.commands.voters.JobExecutionObjectContext;
import ch.post.it.evoting.sdm.config.commands.voters.VotersHolderInitializer;
import ch.post.it.evoting.sdm.config.commands.voters.VotersParametersHolder;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VerificationCardSetCredentialDataPack;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VerificationCardSetCredentialInputDataPack;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.generators.VerificationCardSetCredentialDataPackGenerator;
import ch.post.it.evoting.sdm.config.exceptions.CreateVotingCardSetException;

class PrepareVotingCardGenerationDataTasklet implements Tasklet {

	private static final Logger LOGGER = LoggerFactory.getLogger(PrepareVotingCardGenerationDataTasklet.class);

	@Autowired
	private JobExecutionObjectContext stepExecutionObjectContext;

	@Autowired
	private VerificationCardSetCredentialDataPackGenerator verificationCardSetCredentialDataPackGenerator;

	@Autowired
	private VotersHolderInitializer votersHolderInitializer;

	@Autowired
	private PrimitivesServiceAPI primitivesService;

	@Override
	public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {

		try {
			final VotingCardGenerationJobExecutionContext jobExecutionContext = new VotingCardGenerationJobExecutionContext(
					chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext());

			final String electionEventId = jobExecutionContext.getElectionEventId();
			final String jobInstanceId = jobExecutionContext.getJobInstanceId();
			final VotersParametersHolder parametersHolder = stepExecutionObjectContext.get(jobInstanceId, VotersParametersHolder.class);
			final String verificationCardSetId = jobExecutionContext.getVerificationCardSetId();
			VotersParametersHolder updatedParametersHolder;
			try (InputStream is = getKeysConfiguration()) {
				updatedParametersHolder = votersHolderInitializer.init(parametersHolder, is);
			}
			stepExecutionObjectContext.put(jobInstanceId, updatedParametersHolder, VotersParametersHolder.class);

			String choiceCodesEncryptionKey = jobExecutionContext.getChoiceCodesEncryptionKey();

			final VerificationCardSetCredentialDataPack verificationCardSetCredentialDataPack = getVerificationCardSetCredentialDataPack(
					updatedParametersHolder, verificationCardSetId, choiceCodesEncryptionKey);
			stepExecutionObjectContext.put(jobInstanceId, verificationCardSetCredentialDataPack, VerificationCardSetCredentialDataPack.class);

			byte[] saltCredentialId = primitivesService.getHash((CREDENTIAL_ID + electionEventId).getBytes(StandardCharsets.UTF_8));
			jobExecutionContext.setSaltCredentialId(Base64.getEncoder().encodeToString(saltCredentialId));
			byte[] saltKeystoreSymmetricEncryptionKey = primitivesService.getHash((KEYSTORE_PIN + electionEventId).getBytes(StandardCharsets.UTF_8));
			jobExecutionContext.setSaltKeystoreSymmetricEncryptionKey(Base64.getEncoder().encodeToString(saltKeystoreSymmetricEncryptionKey));

		} catch (CreateVotingCardSetException e) {
			LOGGER.error("Failed to generate card set data pack.", e);
			throw e;
		} catch (GeneralCryptoLibException e) {
			LOGGER.error("Failed salt (credential|pin) hash values.", e);
			throw e;
		} catch (Exception e) {
			LOGGER.error("Prepare voting card generation task failed.", e);
			throw e;
		}

		return RepeatStatus.FINISHED;
	}

	private InputStream getKeysConfiguration() {
		return getClass().getClassLoader().getResourceAsStream(Constants.KEYS_CONFIG_FILENAME);
	}

	private VerificationCardSetCredentialDataPack getVerificationCardSetCredentialDataPack(final VotersParametersHolder holder,
			final String verificationCardSetID, String choiceCodesEncryptionKey) {

		final VerificationCardSetCredentialInputDataPack verificationCardSetCredentialInputDataPack = holder
				.getVerificationCardSetCredentialInputDataPack();

		return verificationCardSetCredentialDataPackGenerator
				.generate(verificationCardSetCredentialInputDataPack, verificationCardSetID, choiceCodesEncryptionKey,
						holder.getCreateVotingCardSetCertificateProperties().getVerificationCardSetCertificateProperties());
	}

}
