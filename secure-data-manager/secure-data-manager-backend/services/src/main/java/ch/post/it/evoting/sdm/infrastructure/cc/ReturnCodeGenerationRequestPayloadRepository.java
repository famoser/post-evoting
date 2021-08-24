/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.cc;

import ch.post.it.evoting.domain.returncodes.ReturnCodeGenerationRequestPayload;

/**
 * Manages the storage and retrieval of return code generation (for both choice return codes and vote cast return codes) request payloads.
 */
public interface ReturnCodeGenerationRequestPayloadRepository {
	/**
	 * Stores a return code generation request payload.
	 *
	 * @param payload the payload to store.
	 * @throws PayloadStorageException if the storage did not succeed
	 */
	void store(ReturnCodeGenerationRequestPayload payload) throws PayloadStorageException;

	/**
	 * Retrieves a return code generation request payload.
	 *
	 * @param electionEventId       the identifier of the election event the verification card set belongs to
	 * @param verificationCardSetId the identifier of the verification card set the payload is for
	 * @param chunkId               the chunk identifier
	 * @return the requested return code generation request payload
	 * @throws PayloadStorageException if retrieving the payload did not succeed
	 */
	ReturnCodeGenerationRequestPayload retrieve(String electionEventId, String verificationCardSetId, int chunkId) throws PayloadStorageException;

	/**
	 * Removes all the payloads for given election event and verification card set.
	 *
	 * @param electionEventId       the election event identifier
	 * @param verificationCardSetId the verification card set identifier
	 * @throws PayloadStorageException failed to remove payloads
	 */
	void remove(String electionEventId, String verificationCardSetId) throws PayloadStorageException;

	/**
	 * Returns the number of payloads for given election event and verification card set.
	 *
	 * @param electionEventId       the election event identifier
	 * @param verificationCardSetId the verification card set identifier
	 * @return the number of payloads
	 * @throws PayloadStorageException failed to get the number of payloads
	 */
	int getCount(String electionEventId, String verificationCardSetId) throws PayloadStorageException;
}
