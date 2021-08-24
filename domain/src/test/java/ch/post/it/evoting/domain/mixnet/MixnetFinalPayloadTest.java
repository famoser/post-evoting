/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.domain.MapperSetUp;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;

@DisplayName("A MixnetFinalPayload")
class MixnetFinalPayloadTest extends MapperSetUp {

	private static final SecureRandom secureRandom = new SecureRandom();
	private static final byte[] randomBytes = new byte[10];

	private static ObjectNode rootNode;
	private static GqGroup gqGroup;
	private static ElGamalMultiRecipientPublicKey previousRemainingPublicKey;
	private static VerifiablePlaintextDecryption verifiablePlaintextDecryption;
	private static CryptolibPayloadSignature signature;
	private static MixnetFinalPayload mixnetFinalPayload;

	@BeforeAll
	static void setUpAll() {
		final int nbrMessage = 4;
		gqGroup = SerializationUtils.getGqGroup();

		final VerifiableShuffle verifiableShuffle = SerializationUtils.getVerifiableShuffle(nbrMessage);
		previousRemainingPublicKey = SerializationUtils.getPublicKey();
		verifiablePlaintextDecryption = SerializationUtils.getVerifiablePlaintextDecryption(nbrMessage);

		// Generate random bytes for signature content and create payload signature.
		secureRandom.nextBytes(randomBytes);
		final X509Certificate certificate = SerializationUtils.generateTestCertificate();
		signature = new CryptolibPayloadSignature(randomBytes, new X509Certificate[] { certificate });

		mixnetFinalPayload = new MixnetFinalPayload(gqGroup, verifiableShuffle, verifiablePlaintextDecryption, previousRemainingPublicKey, signature);

		// Create expected json.
		rootNode = mapper.createObjectNode();

		final JsonNode encryptionGroupNode = SerializationUtils.createEncryptionGroupNode(gqGroup);
		rootNode.set("encryptionGroup", encryptionGroupNode);

		final ObjectNode verifiableShuffleNode = SerializationUtils.createVerifiableShuffleNode(verifiableShuffle);
		rootNode.set("verifiableShuffle", verifiableShuffleNode);

		final ObjectNode verifiablePlaintextDecryptionNode = SerializationUtils
				.createVerifiablePlaintextDecryptionNode(verifiablePlaintextDecryption);
		rootNode.set("verifiablePlaintextDecryption", verifiablePlaintextDecryptionNode);

		final ArrayNode previousRemainingPublicKeyNode = SerializationUtils.createPublicKeyNode(previousRemainingPublicKey);
		rootNode.set("previousRemainingElectionPublicKey", previousRemainingPublicKeyNode);

		final JsonNode signatureNode = SerializationUtils.createSignatureNode(signature);
		rootNode.set("signature", signatureNode);
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializeMixnetFinalPayload() throws JsonProcessingException {
		final String serializedMixnetFinalPayload = mapper.writeValueAsString(mixnetFinalPayload);

		assertEquals(rootNode.toString(), serializedMixnetFinalPayload);
	}

	@Test
	@DisplayName("deserialized gives expected MixnetFinalPayload")
	void deserializeMixnetFinalPayload() throws IOException {
		final MixnetFinalPayload deserializedMixnetFinalPayload = mapper.readValue(rootNode.toString(), MixnetFinalPayload.class);

		assertEquals(mixnetFinalPayload, deserializedMixnetFinalPayload);
	}

	@Test
	@DisplayName("serialized then deserialized gives original MixnetFinalPayload")
	void cycle() throws IOException {
		final MixnetFinalPayload result = mapper.readValue(mapper.writeValueAsString(mixnetFinalPayload), MixnetFinalPayload.class);

		assertEquals(mixnetFinalPayload, result);
	}

	@Test
	@DisplayName("serialized then deserialized without VerifiableShuffle")
	void cycleWithoutVerifiableShuffle() throws IOException {
		final MixnetFinalPayload mixnetFinalPayload = new MixnetFinalPayload(gqGroup, null, verifiablePlaintextDecryption, previousRemainingPublicKey,
				signature);

		// Create expected json.
		final ObjectNode rootNode = mapper.createObjectNode();

		final JsonNode encryptionGroupNode = SerializationUtils.createEncryptionGroupNode(gqGroup);
		rootNode.set("encryptionGroup", encryptionGroupNode);

		final ObjectNode verifiablePlaintextDecryptionNode = SerializationUtils
				.createVerifiablePlaintextDecryptionNode(verifiablePlaintextDecryption);
		rootNode.set("verifiablePlaintextDecryption", verifiablePlaintextDecryptionNode);

		final ArrayNode previousRemainingPublicKeyNode = SerializationUtils.createPublicKeyNode(previousRemainingPublicKey);
		rootNode.set("previousRemainingElectionPublicKey", previousRemainingPublicKeyNode);

		final JsonNode signatureNode = SerializationUtils.createSignatureNode(signature);
		rootNode.set("signature", signatureNode);

		final MixnetFinalPayload result = mapper.readValue(mapper.writeValueAsString(mixnetFinalPayload), MixnetFinalPayload.class);

		assertEquals(mixnetFinalPayload, result);
	}

}
