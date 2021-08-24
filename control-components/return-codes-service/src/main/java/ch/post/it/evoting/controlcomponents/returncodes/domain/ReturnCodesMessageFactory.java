/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ReturnCodesMessageFactory {

	private final ObjectMapper mapper;

	@Autowired
	public ReturnCodesMessageFactory(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	public ReturnCodesMessage buildPartialDecryptPccExponentiationProofLogMessage(ControlComponentContext context,
			PartialDecryptPccExponentiationProof exponentiationProof) {

		return new ReturnCodesMessage(mapper, new PartialDecryptPccExponentiationProofLog(context, exponentiationProof));
	}

	public ReturnCodesMessage buildLongChoiceReturnCodesShareExponentiationProofLogMessage(ControlComponentContext context,
			LongChoiceReturnCodesShareExponentiationProof exponentiationProof) {
		return new ReturnCodesMessage(mapper, new LongChoiceReturnCodesShareExponentiationProofLog(context, exponentiationProof));
	}

	public ReturnCodesMessage buildLongVoteCastReturnCodesShareExponentiationProofLogMessage(ControlComponentContext context,
			LongVoteCastReturnCodesShareExponentiationProof exponentiationProof) {

		return new ReturnCodesMessage(mapper, new LongVoteCastReturnCodesShareExponentiationProofLog(context, exponentiationProof));
	}

	public ReturnCodesMessage buildEncryptedConfirmationKeyExponentiationLogMessage(ControlComponentContext context, String verificationCardId) {
		return new ReturnCodesMessage(mapper, new EncryptedConfirmationKeyExponentiationLog(context, verificationCardId));
	}

	public ReturnCodesMessage buildEncryptedPartialChoiceReturnCodeExponentiationLogMessage(ControlComponentContext context,
			String verificationCardId) {
		return new ReturnCodesMessage(mapper, new EncryptedPartialChoiceReturnCodeExponentiationLog(context, verificationCardId));
	}

}
