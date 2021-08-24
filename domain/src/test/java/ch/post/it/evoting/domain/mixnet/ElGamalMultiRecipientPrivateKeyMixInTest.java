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
import com.fasterxml.jackson.databind.node.ArrayNode;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.MapperSetUp;

@DisplayName("An ElGamalMultiRecipientPrivateKey")
class ElGamalMultiRecipientPrivateKeyMixInTest extends MapperSetUp {

	private static ArrayNode rootNode;

	private static GqGroup gqGroup;
	private static ElGamalMultiRecipientPrivateKey elGamalMultiRecipientPrivateKey;

	@BeforeAll
	static void setUpAll() {
		gqGroup = SerializationUtils.getGqGroup();
		elGamalMultiRecipientPrivateKey = SerializationUtils.getPrivateKey();

		// Create expected json.
		rootNode = SerializationUtils.createPrivateKeyNode(elGamalMultiRecipientPrivateKey);
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializeElGamalMultiRecipientPrivateKey() throws JsonProcessingException {
		final String serializedMessage = mapper.writeValueAsString(elGamalMultiRecipientPrivateKey);

		assertEquals(rootNode.toString(), serializedMessage);
	}

	@Test
	@DisplayName("deserialized gives expected private key")
	void deserializeElGamalMultiRecipientPrivateKey() throws IOException {
		final ElGamalMultiRecipientPrivateKey deserializedPrivateKey = mapper.reader().withAttribute("group", gqGroup)
				.readValue(rootNode.toString(), ElGamalMultiRecipientPrivateKey.class);

		assertEquals(elGamalMultiRecipientPrivateKey, deserializedPrivateKey);
	}

	@Test
	@DisplayName("serialized then deserialized gives original private key")
	void cycle() throws IOException {
		final ElGamalMultiRecipientPrivateKey deserializedPrivateKey = mapper.reader().withAttribute("group", gqGroup)
				.readValue(mapper.writeValueAsString(elGamalMultiRecipientPrivateKey), ElGamalMultiRecipientPrivateKey.class);

		assertEquals(elGamalMultiRecipientPrivateKey, deserializedPrivateKey);
	}

}