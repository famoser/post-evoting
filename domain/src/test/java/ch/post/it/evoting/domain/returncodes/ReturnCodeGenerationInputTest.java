/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.returncodes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.MapperSetUp;
import ch.post.it.evoting.domain.mixnet.SerializationUtils;

@DisplayName("A ReturnCodeGenerationInput")
class ReturnCodeGenerationInputTest extends MapperSetUp {

	private static final String VERIFICATION_CARD_ID = "1234";

	private static ReturnCodeGenerationInput returnCodeGenerationInput;
	private static ObjectNode rootNode;
	private static GqGroup gqGroup;

	@BeforeAll
	static void setUpAll() {
		gqGroup = SerializationUtils.getGqGroup();

		final ElGamalMultiRecipientCiphertext encryptedHashedSquaredConfirmationKey = SerializationUtils.getSinglePhiCiphertext();
		final ElGamalMultiRecipientCiphertext encryptedHashedSquaredPartialChoiceReturnCodes = SerializationUtils.getCiphertexts(1).get(0);
		final ElGamalMultiRecipientPublicKey verificationCardPublicKey = SerializationUtils.getPublicKey();

		returnCodeGenerationInput = new ReturnCodeGenerationInput(VERIFICATION_CARD_ID, encryptedHashedSquaredConfirmationKey,
				encryptedHashedSquaredPartialChoiceReturnCodes, verificationCardPublicKey);

		// Create expected json.
		rootNode = mapper.createObjectNode();
		rootNode.put("verificationCardId", VERIFICATION_CARD_ID);

		final ObjectNode confirmationKeyNode = SerializationUtils.createCiphertextNode(encryptedHashedSquaredConfirmationKey);
		rootNode.set("encryptedHashedSquaredConfirmationKey", confirmationKeyNode);

		final ObjectNode partialChoiceCodesNode = SerializationUtils.createCiphertextNode(encryptedHashedSquaredPartialChoiceReturnCodes);
		rootNode.set("encryptedHashedSquaredPartialChoiceReturnCodes", partialChoiceCodesNode);

		final ArrayNode verificationCardPublicKeyNode = SerializationUtils.createPublicKeyNode(verificationCardPublicKey);
		rootNode.set("verificationCardPublicKey", verificationCardPublicKeyNode);
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializeReturnCodeGenerationInput() throws JsonProcessingException {
		final String serializedInput = mapper.writeValueAsString(returnCodeGenerationInput);

		assertEquals(rootNode.toString(), serializedInput);
	}

	@Test
	@DisplayName("deserialized gives expected input")
	void deserializeReturnCodeGenerationInput() throws IOException {
		final ReturnCodeGenerationInput deserializedInput = mapper.reader().withAttribute("group", gqGroup)
				.readValue(rootNode.toString(), ReturnCodeGenerationInput.class);

		assertEquals(returnCodeGenerationInput, deserializedInput);
	}

	@Test
	@DisplayName("serialized then deserialized gives original input")
	void cycle() throws IOException {
		final ReturnCodeGenerationInput deserializedInput = mapper.reader().withAttribute("group", gqGroup)
				.readValue(mapper.writeValueAsString(returnCodeGenerationInput), ReturnCodeGenerationInput.class);

		assertEquals(returnCodeGenerationInput, deserializedInput);
	}

}
