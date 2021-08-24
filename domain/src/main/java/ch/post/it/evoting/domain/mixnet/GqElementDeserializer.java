/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import static ch.post.it.evoting.domain.mixnet.ConversionUtils.hexToBigInteger;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;

/**
 * Deserializes a json to a {@link GqElement}.
 */
class GqElementDeserializer extends JsonDeserializer<GqElement> {

	/**
	 * The {@code context} must provide the {@link GqGroup} that will be used to reconstruct the various {@link GqElement}s.
	 *
	 * @inheritDoc
	 */
	@Override
	public GqElement deserialize(final JsonParser parser, final DeserializationContext context) throws IOException {
		final GqGroup gqGroup = (GqGroup) context.getAttribute("group");
		final JsonNode node = new ObjectMapper().readTree(parser);
		final String value = node.asText();

		return GqElement.create(hexToBigInteger(value), gqGroup);
	}

}
