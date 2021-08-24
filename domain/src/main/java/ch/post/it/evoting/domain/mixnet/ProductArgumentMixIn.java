/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.HadamardArgument;
import ch.post.it.evoting.cryptoprimitives.mixnet.ProductArgument;
import ch.post.it.evoting.cryptoprimitives.mixnet.SingleValueProductArgument;

@SuppressWarnings({ "java:S116", "java:S117", "unused" })
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "c_b", "hadamardArgument", "singleValueProductArgument" })
@JsonDeserialize(using = ProductArgumentMixIn.ProductArgumentDeserializer.class)
public abstract class ProductArgumentMixIn {

	@JsonProperty
	GqElement c_b;

	@JsonProperty
	HadamardArgument hadamardArgument;

	@JsonProperty
	SingleValueProductArgument singleValueProductArgument;

	static class ProductArgumentDeserializer extends JsonDeserializer<ProductArgument> {

		@Override
		public ProductArgument deserialize(final JsonParser parser, final DeserializationContext context) throws IOException {
			final ObjectMapper mapper = (ObjectMapper) parser.getCodec();
			final String groupAttribute = "group";
			final GqGroup gqGroup = (GqGroup) context.getAttribute(groupAttribute);

			final JsonNode node = mapper.readTree(parser);

			final SingleValueProductArgument singleValueProductArgument = mapper.reader().withAttribute(groupAttribute, gqGroup)
					.readValue(node.get("singleValueProductArgument"), SingleValueProductArgument.class);

			if (node.has("hadamardArgument")) {
				final GqElement c_b = mapper.reader().withAttribute(groupAttribute, gqGroup).readValue(node.get("c_b"), GqElement.class);
				final HadamardArgument hadamardArgument = mapper.reader().withAttribute(groupAttribute, gqGroup)
						.readValue(node.get("hadamardArgument"), HadamardArgument.class);

				return new ProductArgument(c_b, hadamardArgument, singleValueProductArgument);
			} else {
				return new ProductArgument(singleValueProductArgument);
			}
		}
	}
}
