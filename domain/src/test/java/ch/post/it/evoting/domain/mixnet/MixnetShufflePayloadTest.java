/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.DecryptionProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.VerifiableDecryptions;
import ch.post.it.evoting.domain.MapperSetUp;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;

class MixnetShufflePayloadTest extends MapperSetUp {

	private static final int NBR_CIPHERTEXT = 4;
	private static final SecureRandom secureRandom = new SecureRandom();
	private static final byte[] randomBytes = new byte[10];

	private static List<ElGamalMultiRecipientCiphertext> ciphertexts;
	private static VerifiableDecryptions verifiableDecryptions;
	private static ElGamalMultiRecipientPublicKey remainingElectionPublicKey;
	private static ElGamalMultiRecipientPublicKey previousRemainingElectionPublicKey;
	private static ElGamalMultiRecipientPublicKey nodeElectionPublicKey;
	private static CryptolibPayloadSignature signature;
	private static ObjectNode rootNode;
	private static GqGroup gqGroup;

	@BeforeAll
	static void setUpAll() throws JsonProcessingException {
		gqGroup = SerializationUtils.getGqGroup();

		ciphertexts = SerializationUtils.getCiphertexts(NBR_CIPHERTEXT);

		final GroupVector<DecryptionProof, ZqGroup> decryptionProofs = SerializationUtils.getDecryptionProofs(ciphertexts.size());
		verifiableDecryptions = new VerifiableDecryptions(GroupVector.from(ciphertexts), decryptionProofs);

		remainingElectionPublicKey = SerializationUtils.getPublicKey();
		previousRemainingElectionPublicKey = SerializationUtils.getPublicKey();
		nodeElectionPublicKey = SerializationUtils.getPublicKey();

		// Generate random bytes for signature content and create payload signature.
		secureRandom.nextBytes(randomBytes);
		final X509Certificate certificate = SerializationUtils.generateTestCertificate();
		signature = new CryptolibPayloadSignature(randomBytes, new X509Certificate[] { certificate });

		// Create expected json.
		rootNode = mapper.createObjectNode();

		final JsonNode encryptionGroupNode = mapper.readTree(mapper.writeValueAsString(gqGroup));
		rootNode.set("encryptionGroup", encryptionGroupNode);

		final JsonNode verifiableDecryptionNode = mapper.readTree(mapper.writeValueAsString(verifiableDecryptions));
		rootNode.set("verifiableDecryptions", verifiableDecryptionNode);

		final ObjectNode verifiableShuffleNode = mapper.createObjectNode();
		final ArrayNode shuffledCiphertextsNode = SerializationUtils.createCiphertextsNode(ciphertexts);
		verifiableShuffleNode.set("shuffledCiphertexts", shuffledCiphertextsNode);
		final JsonNode jsonNode = mapper.readTree(SerializationUtils.getShuffleArgumentJson());
		verifiableShuffleNode.set("shuffleArgument", jsonNode);
		rootNode.set("verifiableShuffle", verifiableShuffleNode);

		final ArrayNode remainingElectionPublicKeyNode = SerializationUtils.createPublicKeyNode(remainingElectionPublicKey);
		rootNode.set("remainingElectionPublicKey", remainingElectionPublicKeyNode);
		final ArrayNode previousRemainingElectionPublicKeyNode = SerializationUtils.createPublicKeyNode(previousRemainingElectionPublicKey);
		rootNode.set("previousRemainingElectionPublicKey", previousRemainingElectionPublicKeyNode);
		final ArrayNode nodeElectionPublicKeyNode = SerializationUtils.createPublicKeyNode(nodeElectionPublicKey);
		rootNode.set("nodeElectionPublicKey", nodeElectionPublicKeyNode);

		rootNode.put("nodeId", 0);

		final JsonNode signatureNode = SerializationUtils.createSignatureNode(signature);
		rootNode.set("signature", signatureNode);
	}

	@Nested
	@DisplayName("with VerifiableShuffle")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class WithVerifiableShuffle {

		private MixnetShufflePayload mixnetShufflePayload;

		@BeforeAll
		void setUp() {
			final VerifiableShuffle verifiableShuffle = new VerifiableShuffle(GroupVector.from(ciphertexts),
					SerializationUtils.createShuffleArgument());

			mixnetShufflePayload = new MixnetShufflePayload(gqGroup, verifiableDecryptions, verifiableShuffle, remainingElectionPublicKey,
					previousRemainingElectionPublicKey, nodeElectionPublicKey, 0, signature);
		}

		@Test
		@DisplayName("serialize ShufflePayload gives expected json")
		void serializeShufflePayload() throws JsonProcessingException {
			final String serializedShufflePayload = mapper.writeValueAsString(mixnetShufflePayload);

			assertEquals(rootNode.toString(), serializedShufflePayload);
		}

		@Test
		@DisplayName("deserialize ShufflePayload gives expected ShufflePayload")
		void deserializeShufflePayload() throws IOException {
			final MixnetShufflePayload deserializedPayload = mapper.readValue(rootNode.toString(), MixnetShufflePayload.class);

			assertEquals(mixnetShufflePayload, deserializedPayload);
		}

		@Test
		@DisplayName("serialize then deserialized gives original ShufflePayload")
		void cycle() throws IOException {
			final String serializedShufflePayload = mapper.writeValueAsString(mixnetShufflePayload);

			final MixnetShufflePayload deserializedPayload = mapper.readValue(serializedShufflePayload, MixnetShufflePayload.class);

			assertEquals(mixnetShufflePayload, deserializedPayload);
		}

	}

	@Nested
	@DisplayName("without VerifiableShuffle")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class WithoutVerifiableShuffle {

		private MixnetShufflePayload payloadWithoutVerifiableShuffle;
		private ObjectNode rootNodeCopy;

		@BeforeAll
		void setUpAll() {
			payloadWithoutVerifiableShuffle = new MixnetShufflePayload(gqGroup, verifiableDecryptions, null, remainingElectionPublicKey,
					previousRemainingElectionPublicKey, nodeElectionPublicKey, 0, signature);

			rootNodeCopy = rootNode.deepCopy();
			rootNodeCopy.remove("verifiableShuffle");
		}

		@Test
		@DisplayName("serialized gives expected json")
		void serializeWithoutVerifiableShuffle() throws JsonProcessingException {
			final String serializedShufflePayload = mapper.writeValueAsString(payloadWithoutVerifiableShuffle);

			assertEquals(rootNodeCopy.toString(), serializedShufflePayload);
		}

		@Test
		@DisplayName("deserialized gives expected ShufflePayload")
		void deserializeShufflePayload() throws IOException {
			final MixnetShufflePayload deserializedPayload = mapper.readValue(rootNodeCopy.toString(), MixnetShufflePayload.class);

			assertEquals(payloadWithoutVerifiableShuffle, deserializedPayload);
		}

		@Test
		@DisplayName("serialized then deserialized gives original ShufflePayload")
		void cycle() throws IOException {
			final String serializedShufflePayload = mapper.writeValueAsString(payloadWithoutVerifiableShuffle);

			final MixnetShufflePayload deserializedPayload = mapper.readValue(serializedShufflePayload, MixnetShufflePayload.class);

			assertEquals(payloadWithoutVerifiableShuffle, deserializedPayload);
		}

	}

}
