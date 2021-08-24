/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.services;

import java.time.Duration;
import java.util.List;

import javax.ejb.Stateless;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

import ch.post.it.evoting.votingserver.commons.messaging.MessageListener;
import ch.post.it.evoting.votingserver.orchestrator.commons.infrastructure.persistence.Jdbc;
import ch.post.it.evoting.votingserver.orchestrator.commons.infrastructure.persistence.PartialResultsRepository;
import ch.post.it.evoting.votingserver.orchestrator.commons.messaging.PartialResultsReadyListener;
import ch.post.it.evoting.votingserver.orchestrator.commons.polling.PollingService;
import ch.post.it.evoting.votingserver.orchestrator.commons.polling.PollingServiceImpl;
import ch.post.it.evoting.votingserver.orchestrator.commons.polling.ReactivePartialResultsHandlerImpl;
import ch.post.it.evoting.votingserver.orchestrator.commons.polling.ReactiveResultsHandler;

/**
 * CDI bean producer.
 */
@Stateless
public class ChoiceCodesBeanProducer {

	private static final Duration DECRYPTION_POLLING_TIMEOUT = getDuration("DECRYPT_POLLING_TIMEOUT_MS", Duration.ofMillis(20000));

	private static final Duration DECRYPTION_INTER_POLLS_DELAY = getDuration("DECRYPT_INTER_POLLS_DELAY_MS", Duration.ofMillis(100));

	private static final Duration COMPUTE_POLLING_TIMEOUT = getDuration("COMPUTE_POLLING_TIMEOUT_MS", Duration.ofMillis(10000));

	private static final Duration COMPUTE_INTER_POLLS_DELAY = getDuration("COMPUTE_INTER_POLLS_DELAY_MS", Duration.ofMillis(100));

	private static final Duration KEY_GENERATION_POLLING_TIMEOUT = getDuration("CHOICE_CODES_KEY_GENERATION_POLLING_TIMEOUT_MS",
			Duration.ofMillis(20000));

	private static final Duration KEY_GENERATION_INTER_POLLS_DELAY = getDuration("CHOICE_CODES_KEY_GENERATION_INTER_POLLS_DELAY_MS",
			Duration.ofMillis(100));

	private static Duration getDuration(String propertyName, Duration defaultValue) {
		String propertyValue = System.getenv(propertyName);
		if (propertyValue == null) {
			return defaultValue;
		}
		return Duration.ofMillis(Long.parseLong(propertyValue));
	}

	@Produces
	@ApplicationScoped
	@ChoiceCodesVerification
	public ReactiveResultsHandler<List<byte[]>> verificationResultsHandler(
			@Jdbc
					PartialResultsRepository<byte[]> repository) {
		Duration timeout = COMPUTE_POLLING_TIMEOUT.multipliedBy(2);
		ReactivePartialResultsHandlerImpl<byte[]> handler = new ReactivePartialResultsHandlerImpl<>(repository, timeout);
		handler.start();
		return handler;
	}

	public void stopVerificationResultsHandler(
			@Disposes
			@ChoiceCodesVerification
					ReactiveResultsHandler<List<byte[]>> handler) {
		((ReactivePartialResultsHandlerImpl<byte[]>) handler).stop();
	}

	@Produces
	@ChoiceCodesVerification
	public PollingService<List<byte[]>> verificationPollingService(
			@ChoiceCodesVerification
					ReactiveResultsHandler<List<byte[]>> handler) {
		return new PollingServiceImpl<>(handler, COMPUTE_POLLING_TIMEOUT, COMPUTE_INTER_POLLS_DELAY);
	}

	@Produces
	@ChoiceCodesVerification
	@ResultsReady
	public MessageListener verificationResultsReadyListener(
			@ChoiceCodesVerification
					ReactiveResultsHandler<List<byte[]>> handler) {
		return new PartialResultsReadyListener(handler);
	}

	@Produces
	@ApplicationScoped
	@ChoiceCodesDecryption
	public ReactiveResultsHandler<List<byte[]>> decryptionResultsHandler(
			@Jdbc
					PartialResultsRepository<byte[]> repository) {
		Duration timeout = DECRYPTION_POLLING_TIMEOUT.multipliedBy(2);
		ReactivePartialResultsHandlerImpl<byte[]> handler = new ReactivePartialResultsHandlerImpl<>(repository, timeout);
		handler.start();
		return handler;
	}

	public void stopDecryptionResultsHandler(
			@Disposes
			@ChoiceCodesDecryption
					ReactiveResultsHandler<List<byte[]>> handler) {
		((ReactivePartialResultsHandlerImpl<byte[]>) handler).stop();
	}

	@Produces
	@ChoiceCodesDecryption
	public PollingService<List<byte[]>> decryptionPollingService(
			@ChoiceCodesDecryption
					ReactiveResultsHandler<List<byte[]>> handler) {
		return new PollingServiceImpl<>(handler, DECRYPTION_POLLING_TIMEOUT, DECRYPTION_INTER_POLLS_DELAY);
	}

	@Produces
	@ChoiceCodesDecryption
	@ResultsReady
	public MessageListener decryptionResultsReadyListener(
			@ChoiceCodesDecryption
					ReactiveResultsHandler<List<byte[]>> handler) {
		return new PartialResultsReadyListener(handler);
	}

	@Produces
	@ApplicationScoped
	@ChoiceCodesKeyGeneration
	public ReactiveResultsHandler<List<byte[]>> keyGenerationResultsHandler(
			@Jdbc
					PartialResultsRepository<byte[]> repository) {
		Duration timeout = KEY_GENERATION_POLLING_TIMEOUT.multipliedBy(2);
		ReactivePartialResultsHandlerImpl<byte[]> handler = new ReactivePartialResultsHandlerImpl<>(repository, timeout);
		handler.start();
		return handler;
	}

	public void stopKeyGenerationResultsHandler(
			@Disposes
			@ChoiceCodesKeyGeneration
					ReactiveResultsHandler<List<byte[]>> handler) {
		((ReactivePartialResultsHandlerImpl<byte[]>) handler).stop();
	}

	@Produces
	@ChoiceCodesKeyGeneration
	public PollingService<List<byte[]>> keyGenerationPollingService(
			@ChoiceCodesKeyGeneration
					ReactiveResultsHandler<List<byte[]>> handler) {
		return new PollingServiceImpl<>(handler, KEY_GENERATION_POLLING_TIMEOUT, KEY_GENERATION_INTER_POLLS_DELAY);
	}

	@Produces
	@ChoiceCodesKeyGeneration
	@ResultsReady
	public MessageListener keyGenerationResultsReadyListener(
			@ChoiceCodesKeyGeneration
					ReactiveResultsHandler<List<byte[]>> handler) {
		return new PartialResultsReadyListener(handler);
	}
}
