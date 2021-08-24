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

@DisplayName("A MixnetState")
class MixnetStateTest extends MapperSetUp {

	private static final String BALLOT_BOX_ID = "0d31a1148f95488fae6827391425dc08";
	private static final String ELECTION_EVENT_ID = "f8ba3dd3844a4815af39c63570c12006";

	private static final int NBR_CIPHERTEXT = 4;
	private static final SecureRandom secureRandom = new SecureRandom();

	private static GqGroup gqGroup;
	private static ObjectNode rootNode;
	private static MixnetState mixnetState;

	@BeforeAll
	static void setUpAll() {
		// Create ciphertexts list.
		gqGroup = SerializationUtils.getGqGroup();

		// Create expected Json.
		rootNode = mapper.createObjectNode();

		final ObjectNode ballotBoxDetailsNode = mapper.createObjectNode();
		ballotBoxDetailsNode.put("ballotBoxId", BALLOT_BOX_ID);
		ballotBoxDetailsNode.put("electionEventId", ELECTION_EVENT_ID);
		rootNode.set("ballotBoxDetails", ballotBoxDetailsNode);

		rootNode.put("nodeToVisit", 0);
	}

	@Nested
	@DisplayName("with a MixnetInitialPayload")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class WithInitialPayload {

		private final byte[] randomBytes = new byte[10];

		@BeforeAll
		void setUpAll() throws JsonProcessingException {
			final List<ElGamalMultiRecipientCiphertext> ciphertexts = SerializationUtils.getCiphertexts(NBR_CIPHERTEXT);
			final ElGamalMultiRecipientPublicKey electionPublicKey = SerializationUtils.getPublicKey();

			// Generate random bytes for signature content and create payload signature.
			secureRandom.nextBytes(randomBytes);
			final X509Certificate certificate = SerializationUtils.generateTestCertificate();
			final CryptolibPayloadSignature signature = new CryptolibPayloadSignature(randomBytes, new X509Certificate[] { certificate });

			final BallotBoxDetails ballotBoxDetails = new BallotBoxDetails(BALLOT_BOX_ID, ELECTION_EVENT_ID);
			final MixnetInitialPayload initialPayload = new MixnetInitialPayload(gqGroup, ciphertexts, electionPublicKey, signature);
			mixnetState = new MixnetState(ballotBoxDetails, initialPayload);

			// Expected MixnetInitialPayload.
			final ObjectNode payloadNode = mapper.createObjectNode();
			final JsonNode encryptionGroupNode = mapper.readTree(mapper.writeValueAsString(gqGroup));
			payloadNode.set("encryptionGroup", encryptionGroupNode);
			final ArrayNode ciphertextsNode = SerializationUtils.createCiphertextsNode(ciphertexts);
			payloadNode.set("ciphertexts", ciphertextsNode);
			final ArrayNode electionPublicKeyNode = SerializationUtils.createPublicKeyNode(electionPublicKey);
			payloadNode.set("electionPublicKey", electionPublicKeyNode);
			final JsonNode signatureNode = SerializationUtils.createSignatureNode(signature);
			payloadNode.set("signature", signatureNode);
			rootNode.set("payload", payloadNode);

			rootNode.put("retryCount", 5);
		}

		@Test
		@DisplayName("serialized gives expected json")
		void serializeState() throws JsonProcessingException {
			final String serializedState = mapper.writeValueAsString(mixnetState);

			assertEquals(rootNode.toString(), serializedState);
		}

		@Test
		@DisplayName("deserialized gives expected state")
		void deserializeState() throws IOException {
			final MixnetState deserializedState = mapper.readValue(rootNode.toString(), MixnetState.class);

			assertEquals(mixnetState, deserializedState);
		}

		@Test
		@DisplayName("serialized then deserialized gives original state")
		void cycle() throws IOException {
			final MixnetState result = mapper.readValue(mapper.writeValueAsString(mixnetState), MixnetState.class);

			assertEquals(mixnetState, result);
		}

		@Test
		@DisplayName("serialized with error gives expected json")
		void serializedWithError() throws JsonProcessingException {
			mixnetState.setMixnetError("An error has occurred during mixing.");
			rootNode.put("mixnetError", "An error has occurred during mixing.");

			final String serializedState = mapper.writeValueAsString(mixnetState);

			assertEquals(rootNode.toString(), serializedState);
		}

		@Test
		@DisplayName("deserialized with error gives expected MixnetState")
		void deserializedWithError() throws JsonProcessingException {
			mixnetState.setMixnetError("An error has occurred during mixing.");
			rootNode.put("mixnetError", "An error has occurred during mixing.");

			final MixnetState deserializedState = mapper.readValue(rootNode.toString(), MixnetState.class);

			assertEquals(mixnetState, deserializedState);
		}

		@Test
		@DisplayName("serialized then deserialized with error gives original state")
		void cycleWithError() throws IOException {
			mixnetState.setMixnetError("An error has occurred during mixing.");
			rootNode.put("mixnetError", "An error has occurred during mixing.");

			final MixnetState result = mapper.readValue(mapper.writeValueAsString(mixnetState), MixnetState.class);

			assertEquals(mixnetState, result);
		}

	}

	@Nested
	@DisplayName("with a MixnetShufflePayload")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class WithShufflePayload {

		private final byte[] randomBytes = new byte[10];

		@BeforeAll
		void setUpAll() throws JsonProcessingException {
			final List<ElGamalMultiRecipientCiphertext> ciphertexts = SerializationUtils.getCiphertexts(NBR_CIPHERTEXT);
			final VerifiableShuffle verifiableShuffle = new VerifiableShuffle(GroupVector.from(ciphertexts),
					SerializationUtils.createShuffleArgument());

			final ElGamalMultiRecipientPublicKey remainingElectionPublicKey = SerializationUtils.getPublicKey();
			final ElGamalMultiRecipientPublicKey previousRemainingElectionPublicKey = SerializationUtils.getPublicKey();
			final ElGamalMultiRecipientPublicKey nodeElectionPublicKey = SerializationUtils.getPublicKey();

			// Generate random bytes for signature content and create payload signature.
			secureRandom.nextBytes(randomBytes);
			final X509Certificate certificate = SerializationUtils.generateTestCertificate();
			final CryptolibPayloadSignature signature = new CryptolibPayloadSignature(randomBytes, new X509Certificate[] { certificate });

			// VerifiableDecryptions.
			final GroupVector<DecryptionProof, ZqGroup> decryptionProofs = SerializationUtils.getDecryptionProofs(ciphertexts.size());
			final VerifiableDecryptions verifiableDecryptions = new VerifiableDecryptions(GroupVector.from(ciphertexts), decryptionProofs);

			final BallotBoxDetails ballotBoxDetails = new BallotBoxDetails(BALLOT_BOX_ID, ELECTION_EVENT_ID);
			final MixnetShufflePayload mixnetShufflePayload = new MixnetShufflePayload(gqGroup, verifiableDecryptions, verifiableShuffle,
					remainingElectionPublicKey, previousRemainingElectionPublicKey, nodeElectionPublicKey, 0, signature);
			mixnetState = new MixnetState(ballotBoxDetails, mixnetShufflePayload);

			// Create expected MixnetShufflePayload.
			final ObjectNode payloadNode = mapper.createObjectNode();
			final JsonNode encryptionGroupNode = mapper.readTree(mapper.writeValueAsString(gqGroup));
			payloadNode.set("encryptionGroup", encryptionGroupNode);

			final JsonNode verifiableDecryptionNode = mapper.readTree(mapper.writeValueAsString(verifiableDecryptions));
			payloadNode.set("verifiableDecryptions", verifiableDecryptionNode);

			final ObjectNode verifiableShuffleNode = mapper.createObjectNode();
			final ArrayNode shuffledCiphertextsNode = SerializationUtils.createCiphertextsNode(ciphertexts);
			verifiableShuffleNode.set("shuffledCiphertexts", shuffledCiphertextsNode);
			final JsonNode jsonNode = mapper.readTree(SerializationUtils.getShuffleArgumentJson());
			verifiableShuffleNode.set("shuffleArgument", jsonNode);
			payloadNode.set("verifiableShuffle", verifiableShuffleNode);

			final ArrayNode remainingElectionPublicKeyNode = SerializationUtils.createPublicKeyNode(remainingElectionPublicKey);
			payloadNode.set("remainingElectionPublicKey", remainingElectionPublicKeyNode);
			final ArrayNode previousRemainingElectionPublicKeyNode = SerializationUtils.createPublicKeyNode(previousRemainingElectionPublicKey);
			payloadNode.set("previousRemainingElectionPublicKey", previousRemainingElectionPublicKeyNode);
			final ArrayNode nodeElectionPublicKeyNode = SerializationUtils.createPublicKeyNode(nodeElectionPublicKey);
			payloadNode.set("nodeElectionPublicKey", nodeElectionPublicKeyNode);

			payloadNode.put("nodeId", 0);

			final JsonNode signatureNode = SerializationUtils.createSignatureNode(signature);
			payloadNode.set("signature", signatureNode);

			rootNode.set("payload", payloadNode);

			rootNode.put("retryCount", 5);
		}

		@Test
		@DisplayName("serialized gives expected json")
		void serializeState() throws JsonProcessingException {
			final String serializedState = mapper.writeValueAsString(mixnetState);

			assertEquals(rootNode.toString(), serializedState);
		}

		@Test
		@DisplayName("deserialized gives expected state")
		void deserializeState() throws IOException {
			final MixnetState deserializedState = mapper.readValue(rootNode.toString(), MixnetState.class);

			assertEquals(mixnetState, deserializedState);
		}

		@Test
		@DisplayName("serialized then deserialized gives original state")
		void cycle() throws IOException {
			final MixnetState result = mapper.readValue(mapper.writeValueAsString(mixnetState), MixnetState.class);

			assertEquals(mixnetState, result);
		}

	}

}
