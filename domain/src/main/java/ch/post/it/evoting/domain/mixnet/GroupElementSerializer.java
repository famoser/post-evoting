/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import static ch.post.it.evoting.domain.mixnet.ConversionUtils.bigIntegerToHex;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import ch.post.it.evoting.cryptoprimitives.math.GroupElement;

/**
 * Serializes a {@link GroupElement} to a standalone string, i.e. by itself it does not return a valid JSON. We consider the GroupElement as a
 * primitive and not an object, hence it cannot be directly serialized to a JSON with this method.
 */
class GroupElementSerializer extends JsonSerializer<GroupElement<?>> {

	@Override
	public void serialize(final GroupElement element, final JsonGenerator gen, final SerializerProvider serializers) throws IOException {
		gen.writeString(bigIntegerToHex(element.getValue()));
	}

}
