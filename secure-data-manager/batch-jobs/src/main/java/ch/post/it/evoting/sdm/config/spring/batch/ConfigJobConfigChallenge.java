/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch;

import java.nio.file.Path;
import java.util.Arrays;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ch.post.it.evoting.cryptolib.api.primitives.PrimitivesServiceAPI;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.commands.progress.ProgressManager;
import ch.post.it.evoting.sdm.config.spring.batch.writers.ChallengePrintingDataWriter;
import ch.post.it.evoting.sdm.config.spring.batch.writers.CompositeOutputWriter;

@Configuration("configuration-job-config-challenge")
@Profile("challenge")
public class ConfigJobConfigChallenge extends ConfigJobConfig {

	@Autowired
	private CommonBatchInfrastructure commonBatchInfrastructure;

	@Override
	@Bean
	Job job(PrimitivesServiceAPI primitivesService, ProgressManager progressManager) {

		return commonBatchInfrastructure
				.getJobBuilder(Constants.VOTING_CARD_SET_GENERATION + "-challenge", new RunIdIncrementer(), jobExecutionListener())
				.start(preProcessingFlow().build()) //
				.next(verificationCombinationAndGenerationFlow(primitivesService, progressManager).build()) //
				.next(postProcessingFlow().build()) //
				.end().build();
	}

	@Override
	@Bean
	@JobScope
	CompositeOutputWriter compositeOutputWriter(
			@Value("#{jobExecutionContext['" + Constants.BASE_PATH + "']}")
			final String outputPath,
			@Value("#{jobExecutionContext['" + Constants.VOTING_CARD_SET_ID + "']}")
			final String votingCardSetId,
			@Value("#{jobExecutionContext['" + Constants.VERIFICATION_CARD_SET_ID + "']}")
			final String verificationCardSetId) {

		final CompositeOutputWriter writer = new CompositeOutputWriter();
		writer.setDelegates(Arrays.asList(voterInformationWriter(outputPath, votingCardSetId, verificationCardSetId),
				credentialDataWriter(outputPath, votingCardSetId, verificationCardSetId),
				codesMappingTableWriter(outputPath, votingCardSetId, verificationCardSetId),
				challengePrintingDataWriter(outputPath, votingCardSetId, verificationCardSetId),
				verificationCardDataWriter(outputPath, votingCardSetId, verificationCardSetId),
				extendedAuthenticationWriter(outputPath, votingCardSetId, verificationCardSetId)));
		return writer;
	}

	private ChallengePrintingDataWriter challengePrintingDataWriter(final String baseOutputPath, final String votingCardSetId,
			final String verificationCardSetId) {

		final Path path = commonBatchInfrastructure.getDataSerializationProvider(baseOutputPath, votingCardSetId, verificationCardSetId)
				.getTempPrintingData("");

		return new ChallengePrintingDataWriter(path);
	}
}
