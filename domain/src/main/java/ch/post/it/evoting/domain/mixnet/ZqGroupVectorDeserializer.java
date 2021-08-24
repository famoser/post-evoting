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
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;

/**
 * Deserializes a json into a {@link GroupVector} of {@link ZqElement}s.
 */
class ZqGroupVectorDeserializer extends JsonDeserializer<GroupVector<ZqElement, ZqGroup>> {

	/**
	 * The {@code context} must provide the {@link GqGroup} that will be used to reconstruct the various {@link ZqElement}s.
	 *
	 * @inheritDoc
	 */
	@Override
	public GroupVector<ZqElement, ZqGroup> deserialize(final JsonParser parser, final DeserializationContext context) throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		final GqGroup gqGroup = (GqGroup) context.getAttribute("group");

		final ZqElement[] elementsArray = mapper.addMixIn(ZqElement.class, ZqElementMixIn.class).reader().withAttribute("group", gqGroup)
				.readValue(parser, ZqElement[].class);

		return GroupVector.from(Arrays.asList(elementsArray));
	}

}
