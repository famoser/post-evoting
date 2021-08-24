/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.returncodes;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

import ch.post.it.evoting.domain.election.model.messaging.SafeStreamDeserializationException;
import ch.post.it.evoting.domain.election.model.messaging.StreamSerializable;
import ch.post.it.evoting.domain.election.model.messaging.StreamSerializableClassType;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableUtil;

public class KeyCreationDTO extends CorrelatedSupport implements StreamSerializable, WithDTOResultsKeyFields {

	private String requestId;

	private String signature;

	private String resourceId;

	private String encryptionParameters;

	private String electionEventId;

	private ZonedDateTime from;

	private ZonedDateTime to;

	private List<CCPublicKey> publicKeys;

	public KeyCreationDTO() {
		super();
	}

	public KeyCreationDTO(KeyCreationDTO data) {
		super(data.getCorrelationId());
		this.requestId = data.requestId;
		this.resourceId = data.resourceId;
		this.encryptionParameters = data.encryptionParameters;
		this.electionEventId = data.electionEventId;
		this.from = data.from;
		this.to = data.to;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public void setElectionEventId(String electionEventId) {
		this.electionEventId = electionEventId;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public String getEncryptionParameters() {
		return encryptionParameters;
	}

	public void setEncryptionParameters(String encryptionParameters) {
		this.encryptionParameters = encryptionParameters;
	}

	public List<CCPublicKey> getPublicKeys() {
		return publicKeys;
	}

	public void setPublicKey(List<CCPublicKey> publicKeys) {
		this.publicKeys = publicKeys;
	}

	public ZonedDateTime getFrom() {
		return from;
	}

	public void setFrom(ZonedDateTime from) {
		this.from = from;
	}

	public ZonedDateTime getTo() {
		return to;
	}

	public void setTo(ZonedDateTime to) {
		this.to = to;
	}

	@Override
	public String[] getResultsKeyFields() {
		return new String[] { getElectionEventId(), getResourceId(), getRequestId() };
	}

	@Override
	public void serialize(MessagePacker packer) throws IOException {
		StreamSerializableUtil.storeStringValueWithNullCheck(packer, getCorrelationId().toString());
		StreamSerializableUtil.storeStringValueWithNullCheck(packer, requestId);
		StreamSerializableUtil.storeStringValueWithNullCheck(packer, signature);
		StreamSerializableUtil.storeStringValueWithNullCheck(packer, resourceId);
		StreamSerializableUtil.storeStringValueWithNullCheck(packer, encryptionParameters);
		StreamSerializableUtil.storeStringValueWithNullCheck(packer, electionEventId);

		StreamSerializableUtil.storeDateValueWithNullCheck(packer, from);
		StreamSerializableUtil.storeDateValueWithNullCheck(packer, to);

		if (publicKeys != null) {
			packer.packArrayHeader(publicKeys.size());
			for (CCPublicKey ccPublicKey : publicKeys) {
				ccPublicKey.serialize(packer);
			}
		} else {
			packer.packNil();
		}
	}

	@Override
	public void deserialize(MessageUnpacker unpacker) throws SafeStreamDeserializationException {
		try {
			setCorrelationId(UUID.fromString(StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker)));
			this.requestId = StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker);
			this.signature = StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker);
			this.resourceId = StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker);
			this.encryptionParameters = StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker);
			this.electionEventId = StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker);
			this.from = StreamSerializableUtil.retrieveDateValueWithNullCheck(unpacker);
			this.to = StreamSerializableUtil.retrieveDateValueWithNullCheck(unpacker);
			if (!unpacker.tryUnpackNil()) {
				int listSize = unpacker.unpackArrayHeader();
				this.publicKeys = new ArrayList<>(listSize);
				for (int i = 0; i < listSize; i++) {
					CCPublicKey key = new CCPublicKey();
					key.deserialize(unpacker);
					publicKeys.add(key);
				}
			} else {
				publicKeys = null;
			}
		} catch (IOException e) {
			throw new SafeStreamDeserializationException(e);
		}
	}

	@Override
	public StreamSerializableClassType type() {
		return StreamSerializableClassType.KEY_CREATION_DTO;
	}
}
