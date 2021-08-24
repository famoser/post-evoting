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
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.domain.MapperSetUp;
import ch.post.it.evoting.domain.mixnet.SerializationUtils;

@DisplayName("A ReturnCodeGenerationOutput")
class ReturnCodeGenerationOutputTest extends MapperSetUp {

	private static final String VERIFICATION_CARD_ID = "1234";

	private static ReturnCodeGenerationOutput returnCodeGenerationOutput;
	private static ObjectNode rootNode;
	private static GqGroup gqGroup;

	@BeforeAll
	static void setUpAll() {
		gqGroup = SerializationUtils.getGqGroup();

		final ElGamalMultiRecipientPublicKey voterChoiceReturnCodeGenerationPublicKey = SerializationUtils.getPublicKey();
		final ElGamalMultiRecipientPublicKey voterVoteCastReturnCodeGenerationPublicKey = SerializationUtils.getPublicKey();
		final ElGamalMultiRecipientCiphertext exponentiatedEncryptedPartialChoiceReturnCodes = SerializationUtils.getCiphertexts(1).get(0);
		final ExponentiationProof encryptedPartialChoiceReturnCodeExponentiationProof = SerializationUtils.createExponentiationProof();
		final ElGamalMultiRecipientCiphertext exponentiatedEncryptedConfirmationKey = SerializationUtils.getSinglePhiCiphertext();
		final ExponentiationProof encryptedConfirmationKeyExponentiationProof = SerializationUtils.createExponentiationProof();

		returnCodeGenerationOutput = new ReturnCodeGenerationOutput(VERIFICATION_CARD_ID, voterChoiceReturnCodeGenerationPublicKey,
				voterVoteCastReturnCodeGenerationPublicKey, exponentiatedEncryptedPartialChoiceReturnCodes,
				encryptedPartialChoiceReturnCodeExponentiationProof, exponentiatedEncryptedConfirmationKey,
				encryptedConfirmationKeyExponentiationProof);

		// Creat expected json.
		rootNode = mapper.createObjectNode();
		rootNode.put("verificationCardId", VERIFICATION_CARD_ID);

		final ArrayNode voterChoicePublicKey = SerializationUtils.createPublicKeyNode(voterChoiceReturnCodeGenerationPublicKey);
		rootNode.set("voterChoiceReturnCodeGenerationPublicKey", voterChoicePublicKey);

		final ArrayNode voterVoteCastPublicKey = SerializationUtils.createPublicKeyNode(voterVoteCastReturnCodeGenerationPublicKey);
		rootNode.set("voterVoteCastReturnCodeGenerationPublicKey", voterVoteCastPublicKey);

		final ObjectNode partialChoiceCodeNode = SerializationUtils.createCiphertextNode(exponentiatedEncryptedPartialChoiceReturnCodes);
		rootNode.set("exponentiatedEncryptedPartialChoiceReturnCodes", partialChoiceCodeNode);

		final ObjectNode partialChoiceCodeProofNode = SerializationUtils
				.createExponentiationProofNode(encryptedPartialChoiceReturnCodeExponentiationProof);
		rootNode.set("encryptedPartialChoiceReturnCodeExponentiationProof", partialChoiceCodeProofNode);

		final ObjectNode confirmationKeyNode = SerializationUtils.createCiphertextNode(exponentiatedEncryptedConfirmationKey);
		rootNode.set("exponentiatedEncryptedConfirmationKey", confirmationKeyNode);

		final ObjectNode confirmationKeyProofNode = SerializationUtils.createExponentiationProofNode(encryptedConfirmationKeyExponentiationProof);
		rootNode.set("encryptedConfirmationKeyExponentiationProof", confirmationKeyProofNode);
	}

	@Test
	@DisplayName("serialized gives expected json")
	void serializeReturnCodeGenerationOutput() throws JsonProcessingException {
		final String serializedOutput = mapper.writeValueAsString(returnCodeGenerationOutput);

		assertEquals(rootNode.toString(), serializedOutput);
	}

	@Test
	@DisplayName("deserialized gives expected output")
	void deserializeReturnCodeGenerationOutput() throws IOException {
		final ReturnCodeGenerationOutput deserializedOutput = mapper.reader().withAttribute("group", gqGroup)
				.readValue(rootNode.toString(), ReturnCodeGenerationOutput.class);

		assertEquals(returnCodeGenerationOutput, deserializedOutput);
	}

	@Test
	@DisplayName("serialized then deserialized gives original output")
	void cycle() throws IOException {
		final ReturnCodeGenerationOutput deserializedOutput = mapper.reader().withAttribute("group", gqGroup)
				.readValue(mapper.writeValueAsString(returnCodeGenerationOutput), ReturnCodeGenerationOutput.class);

		assertEquals(returnCodeGenerationOutput, deserializedOutput);
	}

}