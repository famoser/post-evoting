/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.model.messaging;

import ch.post.it.evoting.domain.returncodes.CCPublicKey;
import ch.post.it.evoting.domain.returncodes.ChoiceCodeGenerationDTO;
import ch.post.it.evoting.domain.returncodes.ChoiceCodesVerificationDecryptResPayload;
import ch.post.it.evoting.domain.returncodes.KeyCreationDTO;
import ch.post.it.evoting.domain.returncodes.ReturnCodeComputationDTO;
import ch.post.it.evoting.domain.returncodes.ReturnCodeGenerationRequestPayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodeGenerationResponsePayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodesExponentiationResponsePayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodesInput;

/**
 * Classes that are supported by MsgPack serialization/deserialization.
 */
public enum StreamSerializableClassType {

	KEY_CREATION_DTO(KeyCreationDTO.class.getName()),
	RETURN_CODES_EXPONENTIATION_RESPONSE_PAYLOAD(ReturnCodesExponentiationResponsePayload.class.getName()),
	CHOICE_CODES_VERIFICATION_DECRYPT_RES_PAYLOAD(ChoiceCodesVerificationDecryptResPayload.class.getName()),
	RETURN_CODE_GENERATION_RES_PAYLOAD(ReturnCodeGenerationResponsePayload.class.getName()),
	RETURN_CODE_GENERATION_REQUEST_PAYLOAD(ReturnCodeGenerationRequestPayload.class.getName()),
	RETURN_CODE_COMPUTATION_DTO(ReturnCodeComputationDTO.class.getName()),
	CHOICE_CODE_GENERATION_DTO(ChoiceCodeGenerationDTO.class.getName()),
	RETURN_CODES_INPUT(ReturnCodesInput.class.getName()),
	CC_PUBLIC_KEY(CCPublicKey.class.getName());

	private final String className;

	StreamSerializableClassType(String className) {
		this.className = className;
	}

	public String getClassName() {
		return className;
	}
}
