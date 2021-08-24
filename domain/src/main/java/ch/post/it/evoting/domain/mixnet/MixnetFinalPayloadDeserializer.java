/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;

/**
 * Deserializes a json into a {@link MixnetFinalPayload}.
 */
class MixnetFinalPayloadDeserializer extends JsonDeserializer<MixnetFinalPayload> {

	@Override
	public MixnetFinalPayload deserialize(final JsonParser parser, final DeserializationContext context) throws IOException {
		final ObjectMapper mapper = (ObjectMapper) parser.getCodec();

		final JsonNode node = mapper.readTree(parser);
		final JsonNode encryptionGroupNode = node.get("encryptionGroup");
		final GqGroup gqGroup = mapper.readValue(encryptionGroupNode.toString(), GqGroup.class);
		final String groupAttribute = "group";

		VerifiableShuffle verifiableShuffle = null;
		if (!node.path("verifiableShuffle").isMissingNode()) {
			verifiableShuffle = mapper.reader().withAttribute(groupAttribute, gqGroup)
					.readValue(node.get("verifiableShuffle").toString(), VerifiableShuffle.class);
		}

		final VerifiablePlaintextDecryption verifiablePlaintextDecryption = mapper.reader().withAttribute(groupAttribute, gqGroup)
				.readValue(node.get("verifiablePlaintextDecryption").toString(), VerifiablePlaintextDecryption.class);

		final ElGamalMultiRecipientPublicKey previousRemainingElectionPublicKey = mapper.reader().withAttribute(groupAttribute, gqGroup)
				.readValue(node.get("previousRemainingElectionPublicKey").toString(), ElGamalMultiRecipientPublicKey.class);

		final CryptolibPayloadSignature signature = mapper.reader().readValue(node.get("signature").toString(), CryptolibPayloadSignature.class);

		return new MixnetFinalPayload(gqGroup, verifiableShuffle, verifiablePlaintextDecryption, previousRemainingElectionPublicKey, signature);
	}
}
