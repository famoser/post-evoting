/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch.writers;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.core.io.FileSystemResource;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VerificationCardCodesDataPack;
import ch.post.it.evoting.sdm.config.exceptions.CreateVotingCardSetException;
import ch.post.it.evoting.sdm.config.model.authentication.AuthenticationKey;
import ch.post.it.evoting.sdm.config.model.authentication.ExtendedAuthChallenge;
import ch.post.it.evoting.sdm.config.model.authentication.ExtendedAuthInformation;
import ch.post.it.evoting.sdm.config.spring.batch.GeneratedVotingCardOutput;
import ch.post.it.evoting.sdm.utils.ConfigObjectMapper;

public class ChallengePrintingDataWriter extends FlatFileItemWriter<GeneratedVotingCardOutput> {

	private static final int EXPECTED_NUM_SECRETS = 1;

	private static final ConfigObjectMapper mapper = new ConfigObjectMapper();

	public ChallengePrintingDataWriter(final Path path) {

		setLineAggregator(lineAggregator());
		setTransactional(false);
		setAppendAllowed(false);
		setShouldDeleteIfExists(true);
		setResource(new FileSystemResource(path.toString()));
	}

	private LineAggregator<GeneratedVotingCardOutput> lineAggregator() {

		return item -> {
			final String votingCardId = item.getVotingCardId();
			final String verificationCardId = item.getVerificationCardId();
			final String electionEventId = item.getElectionEventId();
			final String ballotId = item.getBallotId();
			final VerificationCardCodesDataPack verificationCardCodesDataPack = item.getVerificationCardCodesDataPack();
			final String ballotCastingKey = verificationCardCodesDataPack.getBallotCastingKey();
			final String voteCastingCode = verificationCardCodesDataPack.getVoteCastingCode();
			ExtendedAuthInformation extendedAuthInformation = item.getExtendedAuthInformation();
			AuthenticationKey authKey = extendedAuthInformation.getAuthenticationKey();
			Optional<List<String>> secretsOptional = authKey.getSecrets();
			if (!secretsOptional.isPresent()) {
				throw new CreateVotingCardSetException("Secrets for the voter extended authentication information are not present.");
			}
			List<String> secrets = secretsOptional.get();
			if (EXPECTED_NUM_SECRETS != secrets.size()) {
				throw new CreateVotingCardSetException(
						"Secrets for the voter extended authentication information differ from the expected number: " + EXPECTED_NUM_SECRETS);
			}

			final String svk2 = secrets.get(0);

			Optional<ExtendedAuthChallenge> extAuthChallengeOptional = extendedAuthInformation.getExtendedAuthChallenge();

			if (!extAuthChallengeOptional.isPresent()) {
				throw new CreateVotingCardSetException("Challenge for the voter extended authentication information is not present.");
			}

			ExtendedAuthChallenge extendedAuthChallenge = extAuthChallengeOptional.get();

			Optional<String> aliasOptional = extendedAuthChallenge.getAlias();

			if (!aliasOptional.isPresent()) {
				throw new CreateVotingCardSetException("Alias for the voter extended authentication information is not present.");
			}

			String alias = aliasOptional.get();

			String mapChoicesCodesToVotingOptionsAsJSON;
			try {
				mapChoicesCodesToVotingOptionsAsJSON = mapper.fromJavaToJSON(verificationCardCodesDataPack.getMapChoiceCodesToVotingOption());
			} catch (JsonProcessingException e) {
				throw new CreateVotingCardSetException("Exception while trying to encode choice codes to voting options map to Json", e);
			}
			return String
					.format("%s;%s;%s;%s;%s;%s;%s;%s;%s", votingCardId, verificationCardId, electionEventId, mapChoicesCodesToVotingOptionsAsJSON,
							ballotCastingKey, voteCastingCode, ballotId, svk2, alias);
		};
	}
}
