/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.returncodes.safestream;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import ch.post.it.evoting.domain.election.model.messaging.SafeStreamDeserializationException;
import ch.post.it.evoting.domain.election.model.messaging.StreamSerializable;

public class StreamSerializableObjectReaderImpl<T extends StreamSerializable> implements StreamSerializableObjectReader<T> {

	@SuppressWarnings("unchecked")
	@Override
	public T read(byte[] serializedObject, int offset, int length) throws SafeStreamDeserializationException {
		try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(new ByteArrayInputStream(serializedObject, offset, length))) {
			String className = unpacker.unpackString();

			T deserializedObject = (T) StreamSerializableUtil.resolveByName(className);
			deserializedObject.deserialize(unpacker);
			return deserializedObject;
		} catch (IOException e) {
			throw new SafeStreamDeserializationException(e);
		}
	}

	@Override
	public T read(byte[] serializedObject) throws SafeStreamDeserializationException {
		return read(serializedObject, 0, serializedObject.length);
	}

}
