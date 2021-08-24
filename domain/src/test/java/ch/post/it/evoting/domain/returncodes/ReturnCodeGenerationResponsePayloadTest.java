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
import ch.post.it.evoting.domain.mixnet.SerializationUtils;

@DisplayName("A ReturnCodeGenerationResponsePayload")
class ReturnCodeGenerationResponsePayloadTest extends MapperSetUp {

	private static final String TENANT_ID = "100";
	private static final String ELECTION_EVENT_ID = "1234";
	private static final String VERIFICATION_CARD_SET_ID = "1234";
	private static final int CHUNK_ID = 1;

	private static ReturnCodeGenerationResponsePayload responsePayload;
	private static ObjectNode rootNode;

	@BeforeAll
	static void setUpAll() throws JsonProcessingException {
		responsePayload = SerializationUtils.getResponsePayload(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, CHUNK_ID);

		// Create expected json.
		rootNode = SerializationUtils.createResponsePayloadNode(responsePayload);
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializeReturnCodeGenerationResponsePayload() throws JsonProcessingException {
		final String serializedResponsePayload = mapper.writeValueAsString(responsePayload);

		assertEquals(rootNode.toString(), serializedResponsePayload);
	}

	@Test
	@DisplayName("deserialized gives expected payload")
	void deserializeReturnCodeGenerationResponsePayload() throws IOException {
		final ReturnCodeGenerationResponsePayload deserializedResponsePayload = mapper
				.readValue(rootNode.toString(), ReturnCodeGenerationResponsePayload.class);

		assertEquals(responsePayload, deserializedResponsePayload);
	}

	@Test
	@DisplayName("serialized then deserialized gives original payload")
	void cycle() throws IOException {
		final ReturnCodeGenerationResponsePayload deserializedResponsePayload = mapper
				.readValue(mapper.writeValueAsString(responsePayload), ReturnCodeGenerationResponsePayload.class);

		assertEquals(responsePayload, deserializedResponsePayload);
	}

}