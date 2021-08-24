/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.domain.mixnet.exceptions.FailedValidationException;

@DisplayName("A BallotBoxDetails")
class BallotBoxDetailsTest {

	private static final String BALLOT_BOX_ID = "0d31a1148f95488fae6827391425dc08";
	private static final String ELECTION_EVENT_ID = "f8ba3dd3844a4815af39c63570c12006";
	private static final ObjectMapper mapper = new ObjectMapper();

	private static ObjectNode rootNode;
	private static BallotBoxDetails ballotBoxDetails;

	@BeforeAll
	static void setUpAll() {
		rootNode = mapper.createObjectNode();
		rootNode.put("ballotBoxId", BALLOT_BOX_ID);
		rootNode.put("electionEventId", ELECTION_EVENT_ID);

		ballotBoxDetails = new BallotBoxDetails(BALLOT_BOX_ID, ELECTION_EVENT_ID);
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializeDetails() throws JsonProcessingException {
		final String serializedDetails = mapper.writeValueAsString(ballotBoxDetails);

		assertEquals(rootNode.toString(), serializedDetails);
	}

	@Test
	@DisplayName("deserialized gives expected details")
	void deserializeDetails() throws JsonProcessingException {
		final BallotBoxDetails deserializedDetails = mapper.readValue(rootNode.toString(), BallotBoxDetails.class);

		assertEquals(ballotBoxDetails, deserializedDetails);
	}

	@Test
	@DisplayName("serialized then deserialized gives original details")
	void cycle() throws JsonProcessingException {
		final BallotBoxDetails result = mapper.readValue(mapper.writeValueAsString(BallotBoxDetailsTest.ballotBoxDetails), BallotBoxDetails.class);

		assertEquals(ballotBoxDetails, result);
	}

	@Test
	@DisplayName("instantiated with a non valid UUID throws a FailedValidationException.")
	void nonValidUUID() {
		assertThrows(FailedValidationException.class, () -> new BallotBoxDetails("ballotBoxId", ELECTION_EVENT_ID));
		assertThrows(FailedValidationException.class, () -> new BallotBoxDetails(BALLOT_BOX_ID, "electionEventId"));
	}

	@Test
	@DisplayName("instantiated with a null value throws a NullPointerException.")
	void nullValue() {
		assertThrows(NullPointerException.class, () -> new BallotBoxDetails(null, ELECTION_EVENT_ID));
		assertThrows(NullPointerException.class, () -> new BallotBoxDetails(BALLOT_BOX_ID, null));
	}

}
