/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.KeyManagementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.domain.election.model.messaging.SafeStreamDeserializationException;
import ch.post.it.evoting.domain.returncodes.KeyCreationDTO;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectReader;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectReaderImpl;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectWriter;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectWriterImpl;

@Service
public class ReturnCodesKeyGenerationConsumer {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReturnCodesKeyGenerationConsumer.class);

	private final RabbitTemplate rabbitTemplate;
	private final ReturnCodesKeyRepository returnCodesKeyRepository;
	private final String keyGenerationOutputQueue;

	public ReturnCodesKeyGenerationConsumer(final RabbitTemplate rabbitTemplate, final ReturnCodesKeyRepository returnCodesKeyRepository,
			@Value("${generation.keygen.response.queue}")
			final String keyGenerationOutputQueue) {

		this.rabbitTemplate = rabbitTemplate;
		this.keyGenerationOutputQueue = keyGenerationOutputQueue;
		this.returnCodesKeyRepository = returnCodesKeyRepository;
	}

	@RabbitListener(queues = "${generation.keygen.request.queue}", autoStartup = "false")
	public void onMessage(final Message message) throws GeneralCryptoLibException, KeyManagementException, SafeStreamDeserializationException {

		final StreamSerializableObjectReader<KeyCreationDTO> reader = new StreamSerializableObjectReaderImpl<>();
		final KeyCreationDTO data = reader.read(message.getBody(), 1, message.getBody().length);

		LOGGER.info("Generating CCR_j Return Codes keys (CCR_j Return Codes Generation key pair and CCR_j Choice Return Codes Encryption key pair).");

		final KeyCreationDTO toBeReturned = createKeyCreationDTO(data);

		final StreamSerializableObjectWriter writer = new StreamSerializableObjectWriterImpl();
		final byte[] serialisedMessage;
		try {
			serialisedMessage = writer.write(toBeReturned);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		final byte[] typedSerialisedMessage = new byte[serialisedMessage.length + 1];
		System.arraycopy(serialisedMessage, 0, typedSerialisedMessage, 1, serialisedMessage.length);
		typedSerialisedMessage[0] = 1;

		final Message amqpMessage = new Message(typedSerialisedMessage, new MessageProperties());
		rabbitTemplate.send(keyGenerationOutputQueue, amqpMessage);

	}

	private KeyCreationDTO createKeyCreationDTO(final KeyCreationDTO data) throws GeneralCryptoLibException, KeyManagementException {
		final KeyCreationDTO toBeReturned = new KeyCreationDTO(data);
		returnCodesKeyRepository.addGeneratedKey(toBeReturned);

		return toBeReturned;
	}

}
