/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientMessage;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.MapperSetUp;

@DisplayName("An ElGamalMultiRecipientMessage")
class ElGamalMultiRecipientMessageMixInTest extends MapperSetUp {

	private static ObjectNode rootNode;

	private static GqGroup gqGroup;
	private static ElGamalMultiRecipientMessage elGamalMultiRecipientMessage;

	@BeforeAll
	static void setUpAll() {
		gqGroup = SerializationUtils.getGqGroup();
		elGamalMultiRecipientMessage = SerializationUtils.getMessage();

		// Create expected json.
		rootNode = SerializationUtils.createMessageNode(elGamalMultiRecipientMessage);
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializeElGamalMultiRecipientMessage() throws JsonProcessingException {
		final String serializedMessage = mapper.writeValueAsString(elGamalMultiRecipientMessage);

		assertEquals(rootNode.toString(), serializedMessage);
	}

	@Test
	@DisplayName("deserialized gives expected message")
	void deserializeElGamalMultiRecipientMessage() throws IOException {
		final ElGamalMultiRecipientMessage deserializedMessage = mapper.reader().withAttribute("group", gqGroup)
				.readValue(rootNode.toString(), ElGamalMultiRecipientMessage.class);

		assertEquals(elGamalMultiRecipientMessage, deserializedMessage);
	}

	@Test
	@DisplayName("serialized then deserialized gives original message")
	void cycle() throws IOException {
		final ElGamalMultiRecipientMessage result = mapper.reader().withAttribute("group", gqGroup)
				.readValue(mapper.writeValueAsString(elGamalMultiRecipientMessage), ElGamalMultiRecipientMessage.class);

		assertEquals(elGamalMultiRecipientMessage, result);
	}

}
