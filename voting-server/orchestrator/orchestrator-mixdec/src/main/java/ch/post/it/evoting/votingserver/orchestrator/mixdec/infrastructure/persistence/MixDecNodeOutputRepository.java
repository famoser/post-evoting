/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.mixdec.infrastructure.persistence;

import java.util.List;

import ch.post.it.evoting.domain.mixnet.MixnetPayload;
import ch.post.it.evoting.domain.mixnet.MixnetState;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.DuplicateEntryException;
import ch.post.it.evoting.votingserver.orchestrator.mixdec.domain.model.MixDecNodeOutput;

/**
 * Repository that manages the partial results of mixing and decryption, i.e. what comes out of each node after processing.
 */
public interface MixDecNodeOutputRepository {

	/**
	 * Saves the results of mixing and decrypting a ballot box.
	 *
	 * @param mixnetState the results of mixing and decrypting a ballot box
	 * @return the saved partial results entity
	 */
	MixDecNodeOutput save(final MixnetState mixnetState) throws DuplicateEntryException, MixDecNodeOutputRepositoryException;

	/**
	 * Provides a list of payloads for a ballot box.
	 *
	 * @param electionEventId the election event the ballot box belongs to
	 * @param ballotBoxId     the identifier of the ballot box the payloads belong to
	 */
	List<MixnetPayload> getBallotBoxPayloadList(final String electionEventId, final String ballotBoxId);
}
