/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;

/**
 * Deserializes a json into a {@link GroupVector} of {@link GqElement}s.
 */
class GqGroupVectorDeserializer extends JsonDeserializer<GroupVector<GqElement, GqGroup>> {

	/**
	 * The {@code context} must provide the {@link GqGroup} that will be used to reconstruct the various {@link GqElement}s.
	 *
	 * @inheritDoc
	 */
	@Override
	public GroupVector<GqElement, GqGroup> deserialize(final JsonParser parser, final DeserializationContext context) throws IOException {
		final GqGroup gqGroup = (GqGroup) context.getAttribute("group");
		final ObjectMapper mapper = new ObjectMapper();

		final GqElement[] elementsArray = mapper.addMixIn(GqElement.class, GqElementMixIn.class).reader().withAttribute("group", gqGroup)
				.readValue(parser, GqElement[].class);

		return GroupVector.from(Arrays.asList(elementsArray));
	}

}
