/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.mixing.service;

import java.io.IOException;
import java.security.KeyManagementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.domain.election.model.messaging.SafeStreamDeserializationException;
import ch.post.it.evoting.domain.election.model.messaging.StreamSerializable;
import ch.post.it.evoting.domain.returncodes.KeyCreationDTO;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectReader;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectReaderImpl;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectWriter;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectWriterImpl;

@Service
public class CcmjElectionKeyGenerationMessageConsumer {

	private static final Logger LOGGER = LoggerFactory.getLogger(CcmjElectionKeyGenerationMessageConsumer.class);
	private static final byte CONTENT_TYPE_STREAM_SERIALIZABLE = 1;

	private final RabbitTemplate rabbitTemplate;
	private final CcmjKeyRepository ccmjKeyRepository;

	private final String keyGenerationOutputQueue;

	public CcmjElectionKeyGenerationMessageConsumer(final RabbitTemplate rabbitTemplate, final CcmjKeyRepository ccmjKeyRepository,
			@Value("${keyGenerationResponseQueue}")
			final String keyGenerationOutputQueue) {

		this.keyGenerationOutputQueue = keyGenerationOutputQueue;
		this.rabbitTemplate = rabbitTemplate;
		this.ccmjKeyRepository = ccmjKeyRepository;
	}

	@RabbitListener(queues = "${keyGenerationRequestQueue}", autoStartup = "false")
	public void onMessage(final Message message) throws SafeStreamDeserializationException {

		final byte[] body = message.getBody();
		final StreamSerializableObjectReader<StreamSerializable> reader = new StreamSerializableObjectReaderImpl<>();

		//Skip Content type as we know its a StreamSerializable
		final StreamSerializable streamSerializable = reader.read(body, 1, body.length);

		final KeyCreationDTO receivedKeyCreationDTO = (KeyCreationDTO) streamSerializable;

		LOGGER.info("Generating CCM_j election key.");

		final StreamSerializableObjectWriter writer = new StreamSerializableObjectWriterImpl();
		try {
			final KeyCreationDTO keyCreationDTO = createKeyCreationDTO(receivedKeyCreationDTO);
			final byte[] serializedToBeReturned = writer.write(keyCreationDTO, 1);
			serializedToBeReturned[0] = CONTENT_TYPE_STREAM_SERIALIZABLE;

			final CorrelationData correlationData = new CorrelationData(keyCreationDTO.getCorrelationId().toString());
			rabbitTemplate.convertAndSend(keyGenerationOutputQueue, serializedToBeReturned, correlationData);
		} catch (GeneralCryptoLibException | KeyManagementException | IOException e) {
			LOGGER.error("Failed to send the CCM_j election key generation result.", e);
		}
	}

	private KeyCreationDTO createKeyCreationDTO(final KeyCreationDTO data) throws GeneralCryptoLibException, KeyManagementException {
		final KeyCreationDTO toBeReturned = new KeyCreationDTO(data);
		ccmjKeyRepository.addGeneratedKey(toBeReturned);

		return toBeReturned;
	}
}
