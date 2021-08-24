/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.ShuffleArgument;
import ch.post.it.evoting.domain.MapperSetUp;

@DisplayName("A ShuffleArgument")
class ShuffleArgumentMixInTest extends MapperSetUp {

	private static ShuffleArgument shuffleArgument;
	private static GqGroup gqGroup;

	@BeforeAll
	static void setUpAll() {
		gqGroup = SerializationUtils.getGqGroup();
		shuffleArgument = SerializationUtils.createShuffleArgument();
	}

	@Test
	@DisplayName("serialized then deserialized gives original ShuffleArgument")
	void cycle() throws IOException {
		final String serializedShuffleArgument = mapper.writeValueAsString(shuffleArgument);

		final ShuffleArgument deserializedShuffleArgument = mapper.reader().withAttribute("group", gqGroup)
				.readValue(serializedShuffleArgument, ShuffleArgument.class);

		assertEquals(shuffleArgument, deserializedShuffleArgument);
	}

	@Nested
	@DisplayName("with m=1")
	class WithMEqualOne {

		@Test
		@DisplayName("serialized then deserialized gives original ShuffleArgument")
		void cycle() throws IOException {
			final ShuffleArgument simpleShuffleArgument = SerializationUtils.createSimplestShuffleArgument();

			final String serializedShuffleArgument = mapper.writeValueAsString(simpleShuffleArgument);

			final ShuffleArgument deserializedShuffleArgument = mapper.reader().withAttribute("group", gqGroup)
					.readValue(serializedShuffleArgument, ShuffleArgument.class);

			assertEquals(simpleShuffleArgument, deserializedShuffleArgument);
		}

	}

}