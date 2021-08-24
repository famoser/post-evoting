/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.mixdec.infrastructure.persistence;

import static ch.post.it.evoting.cryptolib.commons.validations.Validate.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.domain.election.exceptions.LambdaException;
import ch.post.it.evoting.domain.mixnet.BallotBoxDetails;
import ch.post.it.evoting.domain.mixnet.MixnetPayload;
import ch.post.it.evoting.domain.mixnet.MixnetState;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.DuplicateEntryException;
import ch.post.it.evoting.votingserver.commons.infrastructure.persistence.BaseRepositoryImpl;
import ch.post.it.evoting.votingserver.orchestrator.mixdec.domain.model.MixDecNodeOutput;
import ch.post.it.evoting.votingserver.orchestrator.mixdec.domain.model.MixDecNodeOutputId;

/**
 * Implementation of the mix-dec node output repository based on BaseRepositoryImpl.
 */
@Stateless
public class MixDecNodeOutputRepositoryImpl extends BaseRepositoryImpl<MixDecNodeOutput, MixDecNodeOutputId> implements MixDecNodeOutputRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(MixDecNodeOutputRepositoryImpl.class);
	private static final String PARAMETER_ELECTION_EVENT_ID = "electionEventId";
	private static final String PARAMETER_BALLOT_BOX_ID = "ballotBoxId";

	@Inject
	private ObjectMapper mapper;

	@Override
	public MixDecNodeOutput save(final MixnetState mixnetState) throws DuplicateEntryException, MixDecNodeOutputRepositoryException {
		// Create a partial results object from the mixing DTO and store it.
		final BallotBoxDetails ballotBoxDetails = mixnetState.getBallotBoxDetails();
		MixDecNodeOutputId id = new MixDecNodeOutputId(ballotBoxDetails.getElectionEventId(), ballotBoxDetails.getBallotBoxId(),
				String.valueOf(mixnetState.getNodeToVisit()));

		LOGGER.info("Starting to serialize mixnetState payload...");

		final MixnetPayload payload = mixnetState.getPayload();
		byte[] serializedPayload;
		try {
			serializedPayload = mapper.writeValueAsBytes(payload);
		} catch (IOException e) {
			throw new MixDecNodeOutputRepositoryException("Error serializing the mixnet payload.", e);
		}

		LOGGER.debug("Storing node output ({} bytes)...", serializedPayload.length);

		final MixDecNodeOutput entity = new MixDecNodeOutput();
		entity.setId(id);
		entity.setPayload(serializedPayload);

		return save(entity);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<MixnetPayload> getBallotBoxPayloadList(final String electionEventId, final String ballotBoxId) {
		checkNotNull(electionEventId);
		checkNotNull(ballotBoxId);
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		LOGGER.info("Query database for payloads of electionEvent:{}, ballotBox:{}", electionEventId, ballotBoxId);

		final Query query = entityManager.createNativeQuery(
				"SELECT " + " election_event_id, ballot_box_id, node_id, payload " + " FROM mixdec_node_output "
						+ " WHERE election_event_id = :electionEventId " + " AND ballot_box_id = :ballotBoxId ");

		query.setParameter(PARAMETER_ELECTION_EVENT_ID, electionEventId);
		query.setParameter(PARAMETER_BALLOT_BOX_ID, ballotBoxId);

		return (List<MixnetPayload>) query.getResultList().stream().map(record -> {
			final Object[] fields = (Object[]) record;
			final Blob payloadBlob = (Blob) fields[3];
			LOGGER.info("Deserialize payload of electionEvent:{}, ballotBox:{}", electionEventId, ballotBoxId);
			try {
				final byte[] payloadBytes = payloadBlob.getBytes(1L, (int) payloadBlob.length());
				return mapper.readValue(payloadBytes, MixnetPayload.class);
			} catch (SQLException e) {
				throw new LambdaException(e);
			} catch (IOException e) {
				throw new UncheckedIOException("Unable to read the mixnet payload from database", e);
			}
		}).collect(Collectors.toList());
	}
}
