/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.returncodes;

import java.io.IOException;
import java.util.UUID;

import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

import ch.post.it.evoting.domain.election.model.messaging.SafeStreamDeserializationException;
import ch.post.it.evoting.domain.election.model.messaging.StreamSerializable;
import ch.post.it.evoting.domain.election.model.messaging.StreamSerializableClassType;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableUtil;

public class ReturnCodeComputationDTO<T> extends CorrelatedSupport implements StreamSerializable, WithDTOResultsKeyFields {

	private String electionEventId;

	private String verificationCardSetId;

	private String verificationCardId;

	private String requestId;

	private T payload;

	public ReturnCodeComputationDTO() {
	}

	public ReturnCodeComputationDTO(UUID correlationId, String requestId, String electionEventId, String verificationCardSetId,
			String verificationCardId, T payload) {
		super(correlationId);
		this.requestId = requestId;
		this.payload = payload;
		this.electionEventId = electionEventId;
		this.verificationCardSetId = verificationCardSetId;
		this.verificationCardId = verificationCardId;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public void setElectionEventId(String electionEventId) {
		this.electionEventId = electionEventId;
	}

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	public void setVerificationCardSetId(String verificationCardSetId) {
		this.verificationCardSetId = verificationCardSetId;
	}

	public String getVerificationCardId() {
		return verificationCardId;
	}

	public void setVerificationCardId(String verificationCardId) {
		this.verificationCardId = verificationCardId;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public T getPayload() {
		return payload;
	}

	public void setPayload(T payload) {
		this.payload = payload;
	}

	@Override
	public String[] getResultsKeyFields() {
		return new String[] { getElectionEventId(), getVerificationCardId(), getRequestId() };
	}

	@Override
	public void serialize(MessagePacker packer) throws IOException {
		StreamSerializableUtil.storeStringValueWithNullCheck(packer, getCorrelationId().toString());
		StreamSerializableUtil.storeStringValueWithNullCheck(packer, electionEventId);
		StreamSerializableUtil.storeStringValueWithNullCheck(packer, verificationCardSetId);
		StreamSerializableUtil.storeStringValueWithNullCheck(packer, verificationCardId);
		StreamSerializableUtil.storeStringValueWithNullCheck(packer, requestId);
		if (payload instanceof StreamSerializable) {
			packer.packString(((StreamSerializable) payload).type().name());
			((StreamSerializable) payload).serialize(packer);
		} else if (payload instanceof String) {
			packer.packString(String.class.getName());
			StreamSerializableUtil.storeStringValueWithNullCheck(packer, (String) payload);
		} else {
			throw new IOException(payload.getClass().getName() + " type is not supported");
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	public void deserialize(MessageUnpacker unpacker) throws SafeStreamDeserializationException {
		try {
			setCorrelationId(UUID.fromString(StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker)));
			this.electionEventId = StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker);
			this.verificationCardSetId = StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker);
			this.verificationCardId = StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker);
			this.requestId = StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker);
			String unpackString = unpacker.unpackString();
			if (String.class.getName().equals(unpackString)) {
				payload = (T) StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker);
			} else {
				StreamSerializable resolveByName = (StreamSerializable) StreamSerializableUtil.resolveByName(unpackString);
				resolveByName.deserialize(unpacker);
				payload = (T) resolveByName;
			}
		} catch (IOException e) {
			throw new SafeStreamDeserializationException(e);
		}

	}

	@Override
	public StreamSerializableClassType type() {
		return StreamSerializableClassType.RETURN_CODE_COMPUTATION_DTO;
	}
}
