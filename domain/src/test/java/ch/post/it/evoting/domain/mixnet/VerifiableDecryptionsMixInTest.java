/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.DecryptionProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.VerifiableDecryptions;
import ch.post.it.evoting.domain.MapperSetUp;

@DisplayName("VerifiableDecryptions")
class VerifiableDecryptionsMixInTest extends MapperSetUp {

	private static final int NBR_CIPHERTEXT = 2;

	private static GqGroup gqGroup;
	private static VerifiableDecryptions verifiableDecryptions;
	private static ObjectNode rootNode;

	@BeforeAll
	static void setUpAll() {
		gqGroup = SerializationUtils.getGqGroup();

		final List<ElGamalMultiRecipientCiphertext> ciphertexts = SerializationUtils.getCiphertexts(NBR_CIPHERTEXT);

		final GroupVector<DecryptionProof, ZqGroup> decryptionProofs = SerializationUtils.getDecryptionProofs(ciphertexts.size());
		verifiableDecryptions = new VerifiableDecryptions(GroupVector.from(ciphertexts), decryptionProofs);

		// Create expected json.
		rootNode = mapper.createObjectNode();
		final ArrayNode ciphertextsNode = SerializationUtils.createCiphertextsNode(ciphertexts);
		rootNode.set("ciphertexts", ciphertextsNode);

		final ArrayNode decryptionProofsNode = SerializationUtils.createDecryptionProofsNode(decryptionProofs);
		rootNode.set("decryptionProofs", decryptionProofsNode);
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializedVerifiableDecryption() throws JsonProcessingException {
		final String serializedVerifiableDecryption = mapper.writeValueAsString(verifiableDecryptions);

		assertEquals(rootNode.toString(), serializedVerifiableDecryption);
	}

	@Test
	@DisplayName("deserialized gives expected VerifiableDecryptions")
	void deserializedVerifiableDecryption() throws IOException {
		final VerifiableDecryptions deserializedVerifiableDecryption = mapper.reader()
				.withAttribute("group", gqGroup)
				.readValue(rootNode.toString(), VerifiableDecryptions.class);

		assertEquals(verifiableDecryptions, deserializedVerifiableDecryption);
	}

	@Test
	@DisplayName("serialized then deserialized gives original VerifiableDecryptions")
	void cycle() throws IOException {
		final String serializedVerifiableDecryption = mapper.writeValueAsString(verifiableDecryptions);

		final VerifiableDecryptions deserializedVerifiableDecryption = mapper.reader()
				.withAttribute("group", gqGroup)
				.readValue(serializedVerifiableDecryption, VerifiableDecryptions.class);

		assertEquals(verifiableDecryptions, deserializedVerifiableDecryption);
	}

}
