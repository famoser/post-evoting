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
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;

/**
 * Deserializes a json into a {@link GroupVector} of {@link ElGamalMultiRecipientCiphertext}s.
 */
class CiphertextGroupVectorDeserializer extends JsonDeserializer<GroupVector<ElGamalMultiRecipientCiphertext, GqGroup>> {

	/**
	 * The {@code context} must provide the {@link GqGroup} that will be used to reconstruct the various {@link GqElement}s.
	 *
	 * @inheritDoc
	 */
	@Override
	public GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> deserialize(final JsonParser parser, final DeserializationContext context)
			throws IOException {

		final ObjectMapper mapper = ObjectMapperMixnetConfig.getNewInstance();
		final GqGroup gqGroup = (GqGroup) context.getAttribute("group");

		final ElGamalMultiRecipientCiphertext[] elementsArray = mapper.reader().withAttribute("group", gqGroup)
				.readValue(parser, ElGamalMultiRecipientCiphertext[].class);

		return GroupVector.from(Arrays.asList(elementsArray));
	}

}
