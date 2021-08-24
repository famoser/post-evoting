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
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.MapperSetUp;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;

@DisplayName("A MixnetInitialPayload")
class MixnetInitialPayloadTest extends MapperSetUp {

	private static final int NBR_CIPHERTEXT = 10;
	private static final SecureRandom secureRandom = new SecureRandom();
	private static final byte[] randomBytes = new byte[10];

	private static MixnetPayload initialPayload;
	private static ObjectNode rootNode;

	@BeforeAll
	static void setUpAll() throws IOException {
		final GqGroup gqGroup = SerializationUtils.getGqGroup();

		final List<ElGamalMultiRecipientCiphertext> ciphertexts = SerializationUtils.getCiphertexts(NBR_CIPHERTEXT);
		final ElGamalMultiRecipientPublicKey electionPublicKey = SerializationUtils.getPublicKey();

		// Generate random bytes for signature content and create payload signature.
		secureRandom.nextBytes(randomBytes);
		final X509Certificate certificate = SerializationUtils.generateTestCertificate();
		final CryptolibPayloadSignature signature = new CryptolibPayloadSignature(randomBytes, new X509Certificate[] { certificate });

		initialPayload = new MixnetInitialPayload(gqGroup, ciphertexts, electionPublicKey, signature);

		// Create expected Json.
		rootNode = mapper.createObjectNode();

		final JsonNode encryptionGroupNode = mapper.readTree(mapper.writeValueAsString(gqGroup));
		rootNode.set("encryptionGroup", encryptionGroupNode);

		final ArrayNode ciphertextsNode = SerializationUtils.createCiphertextsNode(ciphertexts);
		rootNode.set("ciphertexts", ciphertextsNode);

		final ArrayNode electionPublicKeyNode = SerializationUtils.createPublicKeyNode(electionPublicKey);
		rootNode.set("electionPublicKey", electionPublicKeyNode);

		final JsonNode signatureNode = SerializationUtils.createSignatureNode(signature);
		rootNode.set("signature", signatureNode);
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializeMixnetInitialPayload() throws JsonProcessingException {
		final String serializedInitialPayload = mapper.writeValueAsString(initialPayload);

		assertEquals(rootNode.toString(), serializedInitialPayload);
	}

	@Test
	@DisplayName("deserialized gives expected payload")
	void deserializeMixnetInitialPayload() throws IOException {
		final MixnetInitialPayload deserializedRequest = mapper.readValue(rootNode.toString(), MixnetInitialPayload.class);

		assertEquals(initialPayload, deserializedRequest);
	}

	@Test
	@DisplayName("serialized and deserialized gives original payload")
	void cycle() throws IOException {
		final MixnetInitialPayload result = mapper.readValue(mapper.writeValueAsString(initialPayload), MixnetInitialPayload.class);

		assertEquals(initialPayload, result);
	}

}