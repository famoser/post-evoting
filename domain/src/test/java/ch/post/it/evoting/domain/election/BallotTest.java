/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class BallotTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private File jsonFile;

	@BeforeEach
	public void setup() {
		jsonFile = new File(getClass().getClassLoader().getResource("l_c_ballot.json").getFile());
	}

	@Test
	void beCreatedFromAJsonContainingMoreFieldsThanExpected() throws IOException {

		final Ballot ballot = objectMapper.readerFor(Ballot.class).readValue(jsonFile);

		assertNotNull(ballot);
	}

	@Test
	void containDefinedClauses() throws IOException {

		final Ballot ballot = objectMapper.readerFor(Ballot.class).readValue(jsonFile);

		assertNotNull(ballot.getContests().get(0).getAttributes());
	}
}
