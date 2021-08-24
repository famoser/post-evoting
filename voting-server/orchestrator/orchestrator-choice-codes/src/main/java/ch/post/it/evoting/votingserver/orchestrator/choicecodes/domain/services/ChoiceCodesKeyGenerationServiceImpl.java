/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.services;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.domain.election.model.messaging.SafeStreamDeserializationException;
import ch.post.it.evoting.domain.returncodes.CCPublicKey;
import ch.post.it.evoting.domain.returncodes.KeyCreationDTO;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectReader;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectReaderImpl;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.messaging.MessageListener;
import ch.post.it.evoting.votingserver.commons.messaging.MessagingException;
import ch.post.it.evoting.votingserver.commons.messaging.MessagingService;
import ch.post.it.evoting.votingserver.commons.messaging.Queue;
import ch.post.it.evoting.votingserver.orchestrator.commons.config.QueuesConfig;
import ch.post.it.evoting.votingserver.orchestrator.commons.config.TopicsConfig;
import ch.post.it.evoting.votingserver.orchestrator.commons.polling.PollingService;

public class ChoiceCodesKeyGenerationServiceImpl implements ChoiceCodesKeyGenerationService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChoiceCodesKeyGenerationServiceImpl.class);
	private static final int INDEX_OF_CHOICE_CODES_GENERATION_KEY_IN_LIST = 0;
	private static final int INDEX_OF_CHOICE_CODES_DECRYPTION_KEY_IN_LIST = 1;
	private final StreamSerializableObjectReader<KeyCreationDTO> reader = new StreamSerializableObjectReaderImpl<>();
	@Inject
	private MessagingService messagingService;
	@Inject
	@ChoiceCodesKeyGeneration
	@Contributions
	private MessageListener contributionsConsumer;
	@Inject
	@ChoiceCodesKeyGeneration
	@ResultsReady
	private MessageListener resultsReadyConsumer;
	@Inject
	@ChoiceCodesKeyGeneration
	private PollingService<List<byte[]>> pollingService;

	@Override
	public ChoiceCodesKeysResponse requestChoiceCodesKeyGenerationSync(String trackingId, String tenantId, String electionEventId,
			final List<String> verificationCardSetIds, ZonedDateTime keysDateFrom, ZonedDateTime keysDateTo,
			ElGamalEncryptionParameters elGamalEncryptionParameters) throws ResourceNotFoundException, GeneralCryptoLibException {

		ChoiceCodesKeysResponse response = new ChoiceCodesKeysResponse();

		LOGGER.info("OR - Requesting Choice codes keys");

		Map<String, List<String>> choiceCodesGenerationKeys = new HashMap<>();
		Map<String, List<String>> choiceCodesDecryptionKeys = new HashMap<>();

		for (String verificationCardSetId : verificationCardSetIds) {

			KeyCreationDTO keyCreationDTO = new KeyCreationDTO();
			keyCreationDTO.setCorrelationId(UUID.randomUUID());
			keyCreationDTO.setElectionEventId(electionEventId);
			keyCreationDTO.setResourceId(verificationCardSetId);
			keyCreationDTO.setRequestId(trackingId);
			keyCreationDTO.setFrom(keysDateFrom);
			keyCreationDTO.setTo(keysDateTo);
			keyCreationDTO.setEncryptionParameters(elGamalEncryptionParameters.toJson());

			publishToReqQ(keyCreationDTO);

			List<KeyCreationDTO> collectedKeys = collectKeys(keyCreationDTO);

			List<String> generationKeysAsJsonString = groupKeysOfSameTypeTogether(collectedKeys, INDEX_OF_CHOICE_CODES_GENERATION_KEY_IN_LIST);
			choiceCodesGenerationKeys.put(verificationCardSetId, generationKeysAsJsonString);

			List<String> decryptionKeysAsJsonString = groupKeysOfSameTypeTogether(collectedKeys, INDEX_OF_CHOICE_CODES_DECRYPTION_KEY_IN_LIST);
			choiceCodesDecryptionKeys.put(verificationCardSetId, decryptionKeysAsJsonString);
		}

		response.setChoiceCodesGenerationKeys(choiceCodesGenerationKeys);
		response.setChoiceCodesDecryptionKeys(choiceCodesDecryptionKeys);

		return response;
	}

	public void startup() throws MessagingException {
		consumeResultsReady();
		LOGGER.info("OR - Consuming the results ready notifications choice codes key generation");
		consumeKeyGenerationContributions();
		LOGGER.info("OR - Consuming the choice codes key generation contributions");
	}

	public void shutdown() throws MessagingException {
		stopConsumingResultsReady();
		LOGGER.info("OR - Consuming the results ready notifications choice codes key generation is stopped");
		stopConsumingKeyGenerationContributions();
		LOGGER.info("OR - Consuming the choice codes key generation contributions is stopped");
	}

	private void consumeKeyGenerationContributions() throws MessagingException {

		Queue[] queues = QueuesConfig.CHOICE_CODES_KEY_GENERATION_RES_QUEUES;

		if (queues.length == 0) {
			throw new IllegalStateException("No choice codes compute contributions response queues provided to listen to.");
		}

		for (Queue queue : queues) {
			messagingService.createReceiver(queue, contributionsConsumer);
		}
	}

	private void consumeResultsReady() throws MessagingException {
		messagingService.createReceiver(TopicsConfig.HA_TOPIC, resultsReadyConsumer);
	}

	private void stopConsumingKeyGenerationContributions() throws MessagingException {

		Queue[] queues = QueuesConfig.CHOICE_CODES_KEY_GENERATION_RES_QUEUES;

		if (queues.length == 0) {
			throw new IllegalStateException("No choice codes key generation contributions response queues provided to listen to.");
		}

		for (Queue queue : queues) {
			messagingService.destroyReceiver(queue, contributionsConsumer);
		}
	}

	private void stopConsumingResultsReady() throws MessagingException {
		messagingService.destroyReceiver(TopicsConfig.HA_TOPIC, resultsReadyConsumer);
	}

	private void publishToReqQ(KeyCreationDTO keyCreationDTO) throws ResourceNotFoundException {

		Queue[] queues = QueuesConfig.CHOICE_CODES_KEY_GENERATION_REQ_QUEUES;

		if (queues == null || queues.length == 0) {
			throw new IllegalStateException("No Choice Codes key generation request queues provided to publish to.");
		}

		for (Queue queue : queues) {

			try {
				messagingService.send(queue, keyCreationDTO);
			} catch (MessagingException e) {
				throw new ResourceNotFoundException("Error publishing the Choice Codes key generation request to the nodes queues. ", e);
			}
		}

	}

	private List<KeyCreationDTO> collectKeys(KeyCreationDTO keyCreationDTO) throws ResourceNotFoundException {

		UUID correlationId = keyCreationDTO.getCorrelationId();

		List<byte[]> collectedKeys;
		try {

			LOGGER.info("OR - Waiting for the Choice Codes key generation from the nodes to be returned.");
			collectedKeys = pollingService.getResults(correlationId);

		} catch (TimeoutException e) {
			throw new ResourceNotFoundException("Error collecting the Choice Codes key generation from the nodes. ", e);
		}

		return collectedKeys.stream().map(this::deserializeKeyCreationDTO).collect(Collectors.toList());
	}

	private KeyCreationDTO deserializeKeyCreationDTO(byte[] bytes) {
		try {
			return reader.read(bytes);
		} catch (SafeStreamDeserializationException e) {
			throw new IllegalStateException("Failed to deserialize the key creation DTO.", e);
		}
	}

	private List<String> groupKeysOfSameTypeTogether(List<KeyCreationDTO> collectedKeys, int indexInListOfPublicKeys)
			throws GeneralCryptoLibException {

		List<String> jsons = new ArrayList<>(collectedKeys.size());

		for (KeyCreationDTO dto : collectedKeys) {
			CCPublicKey key = dto.getPublicKeys().get(indexInListOfPublicKeys);
			/*
			 * For each key the following JSON is created { publicKey: "<ElGamalPublicKey as JSON>",
			 * signature: "<Base64 encoded signature>" signerCertificate:
			 * "<Signer certificate in PEM format>", nodeCACertificate:
			 * "<CCN CA certificate in PEM format>" }
			 */
			JsonObjectBuilder json = Json.createObjectBuilder();
			json.add("publicKey", key.getPublicKey().toJson());
			json.add("signature", Base64.getEncoder().encodeToString(key.getKeySignature()));
			json.add("signerCertificate", PemUtils.certificateToPem(key.getSignerCertificate()));
			json.add("nodeCACertificate", PemUtils.certificateToPem(key.getNodeCACertificate()));
			jsons.add(json.build().toString());
		}

		return jsons;
	}
}
