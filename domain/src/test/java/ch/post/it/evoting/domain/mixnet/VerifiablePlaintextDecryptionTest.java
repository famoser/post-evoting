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
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientMessage;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.DecryptionProof;
import ch.post.it.evoting.domain.MapperSetUp;

@DisplayName("A VerifiablePlaintextDecryption")
class VerifiablePlaintextDecryptionTest extends MapperSetUp {

	private static ObjectNode rootNode;

	private static GqGroup gqGroup;
	private static VerifiablePlaintextDecryption verifiablePlaintextDecryption;

	@BeforeAll
	static void setUpAll() {
		final int nbrMessage = 2;
		gqGroup = SerializationUtils.getGqGroup();

		final GroupVector<ElGamalMultiRecipientMessage, GqGroup> messages = SerializationUtils.getMessages(nbrMessage);
		final GroupVector<DecryptionProof, ZqGroup> decryptionProofs = SerializationUtils.getDecryptionProofs(nbrMessage);
		verifiablePlaintextDecryption = new VerifiablePlaintextDecryption(messages, decryptionProofs);

		// Create expected json.
		rootNode = mapper.createObjectNode();

		final ArrayNode messagesNode = SerializationUtils.createMessagesNode(messages);
		rootNode.set("decryptedVotes", messagesNode);

		final ArrayNode decryptionProofsNode = SerializationUtils.createDecryptionProofsNode(decryptionProofs);
		rootNode.set("decryptionProofs", decryptionProofsNode);
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializeVerifiablePlaintextDecryption() throws JsonProcessingException {
		final String serializedVerifiablePlaintextDecryption = mapper.writeValueAsString(verifiablePlaintextDecryption);

		assertEquals(rootNode.toString(), serializedVerifiablePlaintextDecryption);
	}

	@Test
	@DisplayName("deserialized gives expected verifiablePlaintextDecryption")
	void deserializeVerificationPlaintextDecryption() throws IOException {
		final VerifiablePlaintextDecryption deserializedVerifiablePlaintextDecryption = mapper.reader().withAttribute("group", gqGroup)
				.readValue(rootNode.toString(), VerifiablePlaintextDecryption.class);

		assertEquals(verifiablePlaintextDecryption, deserializedVerifiablePlaintextDecryption);
	}

	@Test
	@DisplayName("serialized then deserialized gives original verifiablePlaintextDecryption")
	void cycle() throws IOException {
		final VerifiablePlaintextDecryption result = mapper.reader().withAttribute("group", gqGroup)
				.readValue(mapper.writeValueAsString(verifiablePlaintextDecryption), VerifiablePlaintextDecryption.class);

		assertEquals(verifiablePlaintextDecryption, result);
	}

}
