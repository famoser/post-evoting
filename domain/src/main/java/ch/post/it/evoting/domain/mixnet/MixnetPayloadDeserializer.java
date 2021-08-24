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

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;

/**
 * Allows to deserialize the {@link MixnetPayload} interface into its implementing classes.
 */
class MixnetPayloadDeserializer extends JsonDeserializer<MixnetPayload> {

	@Override
	public MixnetPayload deserialize(final JsonParser parser, final DeserializationContext context) throws IOException {
		final ObjectMapper mapper = (ObjectMapper) parser.getCodec();

		final JsonNode node = mapper.readTree(parser);
		final JsonNode encryptionGroupNode = node.get("encryptionGroup");
		final GqGroup gqGroup = mapper.readValue(encryptionGroupNode.toString(), GqGroup.class);

		if (node.has("verifiableDecryptions")) {
			return mapper.reader().withAttribute("group", gqGroup).readValue(node.toString(), MixnetShufflePayload.class);
		} else {
			return mapper.reader().withAttribute("group", gqGroup).readValue(node.toString(), MixnetInitialPayload.class);
		}
	}

}
