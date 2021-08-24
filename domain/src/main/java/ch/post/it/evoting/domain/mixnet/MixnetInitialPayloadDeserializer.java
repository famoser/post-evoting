/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;

/**
 * Deserializes a json into a {@link MixnetInitialPayload}. This deserializer is needed when deserializing a payload outside of a {@link
 * MixnetState}.
 */
class MixnetInitialPayloadDeserializer extends JsonDeserializer<MixnetInitialPayload> {

	@Override
	public MixnetInitialPayload deserialize(final JsonParser parser, final DeserializationContext context) throws IOException {
		final ObjectMapper mapper = (ObjectMapper) parser.getCodec();

		final JsonNode node = mapper.readTree(parser);
		final JsonNode encryptionGroupNode = node.get("encryptionGroup");
		final GqGroup gqGroup = mapper.readValue(encryptionGroupNode.toString(), GqGroup.class);
		final String groupAttribute = "group";

		final ElGamalMultiRecipientCiphertext[] encryptedVotesArray = mapper.reader().withAttribute(groupAttribute, gqGroup)
				.readValue(node.get("ciphertexts").toString(), ElGamalMultiRecipientCiphertext[].class);

		final ElGamalMultiRecipientPublicKey electionPublicKey = mapper.reader().withAttribute(groupAttribute, gqGroup)
				.readValue(node.get("electionPublicKey").toString(), ElGamalMultiRecipientPublicKey.class);

		final CryptolibPayloadSignature signature = mapper.reader().readValue(node.get("signature").toString(), CryptolibPayloadSignature.class);

		return new MixnetInitialPayload(gqGroup, Arrays.asList(encryptedVotesArray), electionPublicKey, signature);
	}
}
