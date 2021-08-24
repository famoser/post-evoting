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
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.ShuffleArgument;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.domain.MapperSetUp;

@DisplayName("A VerifiableShuffle")
class VerifiableShuffleTest extends MapperSetUp {

	private static final int NBR_CIPHERTEXT = 4;

	private static ObjectNode rootNode;
	private static VerifiableShuffle verifiableShuffle;
	private static GqGroup gqGroup;

	@BeforeAll
	static void setUpAll() throws JsonProcessingException {
		gqGroup = SerializationUtils.getGqGroup();

		final List<ElGamalMultiRecipientCiphertext> ciphertexts = SerializationUtils.getCiphertexts(NBR_CIPHERTEXT);
		final ShuffleArgument shuffleArgument = SerializationUtils.createShuffleArgument();

		verifiableShuffle = new VerifiableShuffle(GroupVector.from(ciphertexts), shuffleArgument);

		// Create expected json.
		rootNode = SerializationUtils.createVerifiableShuffleNode(verifiableShuffle);
	}

	@Test
	@DisplayName("serialized with mixIn gives expected json")
	void serializeVerifiableShuffleWithMixIn() throws JsonProcessingException {
		final String serializedShuffle = mapper.writeValueAsString(verifiableShuffle);

		assertEquals(rootNode.toString(), serializedShuffle);
	}

	@Test
	@DisplayName("deserialized with mixIn gives expected json")
	void deserializeVerifiableShuffleWithMixIn() throws IOException {
		final VerifiableShuffle deserializedVerifiableShuffle = mapper.reader().withAttribute("group", gqGroup)
				.readValue(rootNode.toString(), VerifiableShuffle.class);

		assertEquals(verifiableShuffle, deserializedVerifiableShuffle);
	}

	@Test
	@DisplayName("serialized then deserialized with mixIn gives original VerifiableShuffle")
	void mixInCycle() throws IOException {
		final String serializedShuffle = mapper.writeValueAsString(verifiableShuffle);

		final VerifiableShuffle deserializedVerifiableShuffle = mapper.reader().withAttribute("group", gqGroup)
				.readValue(serializedShuffle, VerifiableShuffle.class);

		assertEquals(verifiableShuffle, deserializedVerifiableShuffle);
	}

}
