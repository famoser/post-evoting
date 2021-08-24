/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.voters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ch.post.it.evoting.domain.election.Ballot;
import ch.post.it.evoting.domain.election.Contest;
import ch.post.it.evoting.domain.election.ElectionAttributes;
import ch.post.it.evoting.domain.election.ElectionOption;
import ch.post.it.evoting.domain.election.EncryptionParameters;
import ch.post.it.evoting.domain.election.Question;

class VotersGenerationTaskStaticContentProviderTest {

	@Test
	void correctnessAttributesTest() throws IOException {
		EncryptionParameters encryptionParameters = new EncryptionParameters();
		encryptionParameters.setG("2");
		encryptionParameters.setP("23");
		encryptionParameters.setQ("11");
		List<Contest> contests = new ArrayList<>();
		List<ElectionOption> options = new ArrayList<>();
		options.add(new ElectionOption("", "15", "non-corr1"));
		options.add(new ElectionOption("", "17", "non-corr2"));
		options.add(new ElectionOption("", "19", "non-corr3"));
		List<ElectionAttributes> attributes = new ArrayList<>();
		List<String> related = new ArrayList<>();
		related.add("corr");
		List<String> related2 = new ArrayList<>();
		related2.add("corr2");
		List<String> related3 = new ArrayList<>();
		related3.add("corr");
		related3.add("corr2");
		attributes.add(new ElectionAttributes("non-corr1", "al", related, false));
		attributes.add(new ElectionAttributes("non-corr2", "al", related2, false));
		attributes.add(new ElectionAttributes("non-corr3", "al", related3, false));
		attributes.add(new ElectionAttributes("corr", "al", new ArrayList<>(), true));
		attributes.add(new ElectionAttributes("corr2", "al", new ArrayList<>(), true));
		List<Question> questions = new ArrayList<>();
		contests.add(new Contest("", "", "", "", "", "", options, attributes, questions));
		Ballot ballot = new Ballot("", null, contests);
		List<String> choiceCodesKey = Collections.singletonList("choiceCodesKey");
		VotersParametersHolder holder = new VotersParametersHolder(1, "", ballot, "", "", "", "", Files.createTempFile("", "test"), 1, 1, "",
				ZonedDateTime.now(), ZonedDateTime.now(), "", "", choiceCodesKey, "", null);

		VotersGenerationTaskStaticContentProvider provider = new VotersGenerationTaskStaticContentProvider(encryptionParameters, holder);
		Map<BigInteger, List<String>> representationsWithCorrectness = provider.getRepresentationsWithCorrectness();

		assertEquals(1, representationsWithCorrectness.get(BigInteger.valueOf(15)).size());
		assertEquals("corr", representationsWithCorrectness.get(BigInteger.valueOf(15)).get(0));
		assertEquals(1, representationsWithCorrectness.get(BigInteger.valueOf(17)).size());
		assertEquals("corr2", representationsWithCorrectness.get(BigInteger.valueOf(17)).get(0));
		assertEquals(2, representationsWithCorrectness.get(BigInteger.valueOf(19)).size());
		assertTrue(representationsWithCorrectness.get(BigInteger.valueOf(19)).contains("corr2"));
		assertTrue(representationsWithCorrectness.get(BigInteger.valueOf(19)).contains("corr"));
	}

}
