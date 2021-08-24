/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.service;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import ch.post.it.evoting.domain.election.model.messaging.StreamSerializable;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectWriter;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectWriterImpl;

public class MessageSerialisation {

	private MessageSerialisation() {
		super();
	}

	public static Message getMessage(StreamSerializable message) {
		StreamSerializableObjectWriter writer = new StreamSerializableObjectWriterImpl();
		byte[] serialisedMessage;

		try {
			serialisedMessage = writer.write(message);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		byte[] typedSerialisedMessage = new byte[serialisedMessage.length + 1];
		System.arraycopy(serialisedMessage, 0, typedSerialisedMessage, 1, serialisedMessage.length);
		typedSerialisedMessage[0] = 1;

		return new Message(typedSerialisedMessage, new MessageProperties());
	}

}
