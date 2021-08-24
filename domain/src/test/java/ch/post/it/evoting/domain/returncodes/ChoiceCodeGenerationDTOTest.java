/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.returncodes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.domain.MapperSetUp;
import ch.post.it.evoting.domain.election.Ballot;
import ch.post.it.evoting.domain.mixnet.SerializationUtils;

@DisplayName("A ChoiceCodeGenerationDTO")
class ChoiceCodeGenerationDTOTest extends MapperSetUp {

	private static final String TENANT_ID = "100";
	private static final String ELECTION_EVENT_ID = "1234";
	private static final String VERIFICATION_CARD_SET_ID = "5678";
	private static final int CHUNK_ID = 1;
	private static final String BALLOT_JSON = "ballot.json";

	private static Ballot getBallotFromResourceName() throws IOException {
		return mapper.readValue(ChoiceCodeGenerationDTOTest.class.getClassLoader().getResource(BALLOT_JSON), Ballot.class);
	}

	@Nested
	@DisplayName("with a request payload")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class WithRequestPayload {

		private ChoiceCodeGenerationDTO<ReturnCodeGenerationRequestPayload> choiceCodeGenerationDTO;
		private ObjectNode rootNode;

		@BeforeAll
		void setupAll() throws IOException {
			final Ballot ballot = getBallotFromResourceName();
			final ReturnCodeGenerationRequestPayload requestPayload = SerializationUtils
					.getRequestPayload(ballot, TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, CHUNK_ID);

			final UUID randomUUID = UUID.randomUUID();
			final String requestId = "5555";
			choiceCodeGenerationDTO = new ChoiceCodeGenerationDTO<>(randomUUID, requestId, requestPayload);

			// Create expected json.
			rootNode = mapper.createObjectNode();
			rootNode.put("correlationId", randomUUID.toString());
			rootNode.put("requestId", requestId);

			final ObjectNode requestPayloadNode = SerializationUtils.createRequestPayloadNode(requestPayload);
			rootNode.set("payload", requestPayloadNode);
		}

		@Test
		@DisplayName("serialized gives expected json")
		void serializeDTOWithRequestPayload() throws JsonProcessingException {
			final String serializedDTO = mapper.writeValueAsString(choiceCodeGenerationDTO);

			assertEquals(rootNode.toString(), serializedDTO);
		}

		@Test
		@DisplayName("deserialized gives expected dto")
		void deserializeDTOWithRequestPayload() throws JsonProcessingException {
			final ChoiceCodeGenerationDTO<ReturnCodeGenerationRequestPayload> deserializedDTO = mapper
					.readValue(rootNode.toString(), new TypeReference<ChoiceCodeGenerationDTO<ReturnCodeGenerationRequestPayload>>() {
					});

			assertEquals(choiceCodeGenerationDTO, deserializedDTO);
		}

		@Test
		@DisplayName("serialized then deserialized gives original dto")
		void cycle() throws JsonProcessingException {
			final ChoiceCodeGenerationDTO<ReturnCodeGenerationRequestPayload> deserializedDTO = mapper
					.readValue(mapper.writeValueAsString(choiceCodeGenerationDTO),
							new TypeReference<ChoiceCodeGenerationDTO<ReturnCodeGenerationRequestPayload>>() {
							});

			assertEquals(choiceCodeGenerationDTO, deserializedDTO);
		}
	}

	@Nested
	@DisplayName("with a response payload")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class WithResponsePayload {

		private ChoiceCodeGenerationDTO<ReturnCodeGenerationResponsePayload> choiceCodeGenerationDTO;
		private ObjectNode rootNode;

		@BeforeAll
		void setupAll() throws IOException {
			final ReturnCodeGenerationResponsePayload responsePayload = SerializationUtils
					.getResponsePayload(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, CHUNK_ID);

			final UUID randomUUID = UUID.randomUUID();
			final String requestId = "5555";
			choiceCodeGenerationDTO = new ChoiceCodeGenerationDTO<>(randomUUID, requestId, responsePayload);

			// Create expected json.
			rootNode = mapper.createObjectNode();
			rootNode.put("correlationId", randomUUID.toString());
			rootNode.put("requestId", requestId);

			final ObjectNode requestPayloadNode = SerializationUtils.createResponsePayloadNode(responsePayload);
			rootNode.set("payload", requestPayloadNode);
		}

		@Test
		@DisplayName("serialized gives expected json")
		void serializeDTOWithRequestPayload() throws JsonProcessingException {
			final String serializedDTO = mapper.writeValueAsString(choiceCodeGenerationDTO);

			assertEquals(rootNode.toString(), serializedDTO);
		}

		@Test
		@DisplayName("deserialized gives expected dto")
		void deserializeDTOWithRequestPayload() throws JsonProcessingException {
			final ChoiceCodeGenerationDTO<ReturnCodeGenerationResponsePayload> deserializedDTO = mapper
					.readValue(rootNode.toString(), new TypeReference<ChoiceCodeGenerationDTO<ReturnCodeGenerationResponsePayload>>() {
					});

			assertEquals(choiceCodeGenerationDTO, deserializedDTO);
		}

		@Test
		@DisplayName("serialized then deserialized gives original dto")
		void cycle() throws JsonProcessingException {
			final ChoiceCodeGenerationDTO<ReturnCodeGenerationResponsePayload> deserializedDTO = mapper
					.readValue(mapper.writeValueAsString(choiceCodeGenerationDTO),
							new TypeReference<ChoiceCodeGenerationDTO<ReturnCodeGenerationResponsePayload>>() {
							});

			assertEquals(choiceCodeGenerationDTO, deserializedDTO);
		}

	}

}