/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.returncodes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.domain.MapperSetUp;
import ch.post.it.evoting.domain.election.Ballot;
import ch.post.it.evoting.domain.mixnet.SerializationUtils;

@DisplayName("A ReturnCodeGenerationRequestPayload")
class ReturnCodeGenerationRequestPayloadTest extends MapperSetUp {

	private static final String TENANT_ID = "100";
	private static final String ELECTION_EVENT_ID = "1234";
	private static final String VERIFICATION_CARD_SET_ID = "1234";
	private static final int CHUNK_ID = 1;
	private static final String BALLOT_JSON = "ballot.json";

	private static ReturnCodeGenerationRequestPayload requestPayload;
	private static ObjectNode rootNode;

	@BeforeAll
	static void setupAll() throws IOException {
		final Ballot ballot = getBallotFromResourceName();
		requestPayload = SerializationUtils.getRequestPayload(ballot, TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, CHUNK_ID);

		// Create expected json.
		rootNode = SerializationUtils.createRequestPayloadNode(requestPayload);
	}

	private static Ballot getBallotFromResourceName() throws IOException {
		return mapper.readValue(ReturnCodeGenerationRequestPayloadTest.class.getClassLoader().getResource(BALLOT_JSON), Ballot.class);
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializeReturnCodeGenerationRequestPayload() throws JsonProcessingException {
		final String serializedPayload = mapper.writeValueAsString(requestPayload);

		assertEquals(rootNode.toString(), serializedPayload);
	}

	@Test
	@DisplayName("deserialized gives expected payload")
	void deserializeReturnCodeGenerationRequestPayload() throws IOException {
		final ReturnCodeGenerationRequestPayload deserializedPayload = mapper
				.readValue(rootNode.toString(), ReturnCodeGenerationRequestPayload.class);

		assertEquals(requestPayload, deserializedPayload);
	}

	@Test
	@DisplayName("serialized then deserialized gives original payload")
	void cycle() throws IOException {
		final ReturnCodeGenerationRequestPayload deserializedPayload = mapper
				.readValue(mapper.writeValueAsString(requestPayload), ReturnCodeGenerationRequestPayload.class);

		assertEquals(requestPayload, deserializedPayload);
	}

}
