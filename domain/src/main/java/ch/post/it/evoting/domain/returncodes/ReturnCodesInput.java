/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.returncodes;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.msgpack.core.MessageFormat;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

import ch.post.it.evoting.domain.election.model.messaging.SafeStreamDeserializationException;
import ch.post.it.evoting.domain.election.model.messaging.StreamSerializable;
import ch.post.it.evoting.domain.election.model.messaging.StreamSerializableClassType;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableUtil;

/**
 * Encapsulates generic input - for instance encrypted and plaintext partial Choice Return Codes or the Confirmation Key - necessary for the
 * generation of long Choice Return Codes or the long Vote Cast Return Code
 */
public class ReturnCodesInput implements StreamSerializable {

	private List<BigInteger> returnCodesInputElements;

	private ConfirmationKeyVerificationInput confirmationKeyVerificationInput;

	private PartialChoiceReturnCodesVerificationInput partialChoiceReturnCodesVerificationInput;

	private String certificates;

	public List<BigInteger> getReturnCodesInputElements() {
		return returnCodesInputElements;
	}

	public void setReturnCodesInputElements(List<BigInteger> returnCodesInputElements) {
		this.returnCodesInputElements = returnCodesInputElements;
	}

	public ConfirmationKeyVerificationInput getConfirmationKeyVerificationInput() {
		return confirmationKeyVerificationInput;
	}

	public void setConfirmationKeyVerificationInput(ConfirmationKeyVerificationInput confirmationKeyVerificationInput) {
		this.confirmationKeyVerificationInput = confirmationKeyVerificationInput;
	}

	public PartialChoiceReturnCodesVerificationInput getPartialChoiceReturnCodesVerificationInput() {
		return partialChoiceReturnCodesVerificationInput;
	}

	public void setPartialChoiceReturnCodesVerificationInput(PartialChoiceReturnCodesVerificationInput partialChoiceReturnCodesVerificationInput) {
		this.partialChoiceReturnCodesVerificationInput = partialChoiceReturnCodesVerificationInput;
	}

	public String getCertificates() {
		return certificates;
	}

	public void setCertificates(String certificates) {
		this.certificates = certificates;
	}

	@Override
	public void serialize(MessagePacker packer) throws IOException {
		if (returnCodesInputElements == null) {
			packer.packNil();
		} else {
			packer.packArrayHeader(returnCodesInputElements.size());
			for (BigInteger bigInteger : returnCodesInputElements) {
				StreamSerializableUtil.storeBigIntegerValueWithNullCheck(packer, bigInteger);
			}
		}

		if (confirmationKeyVerificationInput == null) {
			packer.packNil();
		} else {
			StreamSerializableUtil.storeStringValueWithNullCheck(packer, confirmationKeyVerificationInput.getConfirmationMessage());
			StreamSerializableUtil.storeStringValueWithNullCheck(packer, confirmationKeyVerificationInput.getVotingCardId());
		}
		if (partialChoiceReturnCodesVerificationInput == null) {
			packer.packNil();
		} else {
			StreamSerializableUtil.storeStringValueWithNullCheck(packer, partialChoiceReturnCodesVerificationInput.getVerificationCardSetDataJwt());
			StreamSerializableUtil.storeStringValueWithNullCheck(packer, partialChoiceReturnCodesVerificationInput.getElectionPublicKeyJwt());
			StreamSerializableUtil.storeStringValueWithNullCheck(packer, partialChoiceReturnCodesVerificationInput.getVote());
		}

		StreamSerializableUtil.storeStringValueWithNullCheck(packer, certificates);
	}

	@Override
	public void deserialize(MessageUnpacker unpacker) throws SafeStreamDeserializationException {
		try {
			if (unpacker.tryUnpackNil()) {
				this.returnCodesInputElements = null;
			} else {
				int listSize = unpacker.unpackArrayHeader();
				returnCodesInputElements = new ArrayList<>(listSize);
				for (int i = 0; i < listSize; i++) {
					returnCodesInputElements.add(StreamSerializableUtil.retrieveBigIntegerValueWithNullCheck(unpacker));
				}
			}

			if (MessageFormat.NIL.equals(unpacker.getNextFormat())) {
				confirmationKeyVerificationInput = null;
				unpacker.unpackNil();
			} else {
				confirmationKeyVerificationInput = new ConfirmationKeyVerificationInput();
				confirmationKeyVerificationInput.setConfirmationMessage(StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker));
				confirmationKeyVerificationInput.setVotingCardId(StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker));
			}
			if (MessageFormat.NIL.equals(unpacker.getNextFormat())) {
				partialChoiceReturnCodesVerificationInput = null;
				unpacker.unpackNil();
			} else {
				partialChoiceReturnCodesVerificationInput = new PartialChoiceReturnCodesVerificationInput();
				partialChoiceReturnCodesVerificationInput
						.setVerificationCardSetDataJwt(StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker));
				partialChoiceReturnCodesVerificationInput.setElectionPublicKeyJwt(StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker));
				partialChoiceReturnCodesVerificationInput.setVote(StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker));
			}

			certificates = StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker);
		} catch (IOException e) {
			throw new SafeStreamDeserializationException(e);
		}
	}

	@Override
	public StreamSerializableClassType type() {
		return StreamSerializableClassType.RETURN_CODES_INPUT;
	}

}
