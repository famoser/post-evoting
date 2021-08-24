/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.MapperSetUp;

@DisplayName("An ElGamalMultiRecipientCiphertext list")
class ElGamalMultiRecipientCiphertextTest extends MapperSetUp {

	private static ArrayNode rootNode;

	private static List<ElGamalMultiRecipientCiphertext> ciphertexts;
	private static GqGroup gqGroup;

	@BeforeAll
	static void setUpAll() {
		ciphertexts = SerializationUtils.getCiphertexts(2);
		gqGroup = SerializationUtils.getGqGroup();
		rootNode = SerializationUtils.createCiphertextsNode(ciphertexts);
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializeElGamalMultiRecipientCiphertexts() throws JsonProcessingException {
		final String serializedCiphertexts = mapper.writeValueAsString(ciphertexts);

		assertEquals(rootNode.toString(), serializedCiphertexts);
	}

	@Test
	@DisplayName("deserialized gives expected ElGamalMultiRecipientCiphertext list")
	void deserializeElGamalMultiRecipientCiphertexts() throws IOException {
		final ElGamalMultiRecipientCiphertext[] deserializedCiphertexts = mapper.reader().withAttribute("group", gqGroup)
				.readValue(rootNode.toString(), ElGamalMultiRecipientCiphertext[].class);

		assertEquals(ciphertexts, Arrays.asList(deserializedCiphertexts));
	}

	@Test
	@DisplayName("serialized then deserialized gives original ElGamalMultiRecipientCiphertext list")
	void deserializeElGamalMultiRecipientCiphertext() throws IOException {
		final ElGamalMultiRecipientCiphertext ciphertext = ciphertexts.get(0);

		final String serializedCiphertext = mapper.writeValueAsString(ciphertext);

		final ElGamalMultiRecipientCiphertext deserializedCiphertext = mapper.reader().withAttribute("group", gqGroup)
				.readValue(serializedCiphertext, ElGamalMultiRecipientCiphertext.class);

		assertEquals(ciphertext, deserializedCiphertext);
	}

}
