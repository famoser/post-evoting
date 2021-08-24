/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.operation;

public enum OperationsOutputCode {
	GENERAL_ERROR(4000, "General Error"),
	MISSING_PARAMETER(4001, "Missing parameter"),
	ERROR_IO_OPERATIONS(4002, "I/O operations error"),
	ERROR_PARSING_FILE(4003, "Error parsing file"),
	ERROR_STATUS_NAME(4004, "Illegal argument status name"),
	MISSING_COMMANDS_FOR_PHASE(4005, "Missing commands for phase"),
	SIGNATURE_VERIFICATION_FAILED(4006, "Verification of signature failed"),
	CHAIN_VALIDATION_FAILED(4007, "Chain validation failed"),
	ERROR_CERTIFICATE_PARSING(4008, "Error parsing certificate"),
	KEYSTORE_READING_FAILED(4009, "Error reading keystore"),
	CONSISTENCY_ERROR(4010, "Consistency check failed"),
	ERROR_SIGNING_OPERATIONS(4011, "Error signing files");

	private final int value;
	private final String reasonPhrase;

	OperationsOutputCode(int value, String reasonPhrase) {
		this.value = value;
		this.reasonPhrase = reasonPhrase;
	}

	public int value() {
		return value;
	}

	public String getReasonPhrase() {
		return reasonPhrase;
	}
}
