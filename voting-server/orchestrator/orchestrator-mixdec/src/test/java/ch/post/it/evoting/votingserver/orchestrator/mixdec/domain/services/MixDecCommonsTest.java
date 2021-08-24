/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.mixdec.domain.services;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ch.post.it.evoting.domain.returncodes.KeyCreationDTO;

public class MixDecCommonsTest {

	@Test
	public void whenGenerateKeyThenExpectedValue() {

		String ee = "AAAAAAAAAAA";
		String ea = "BBBBBBBBBBB";
		String tracking = "CCCCCCCCCCC";

		KeyCreationDTO keyCreationDTO = new KeyCreationDTO();
		keyCreationDTO.setElectionEventId(ee);
		keyCreationDTO.setResourceId(ea);
		keyCreationDTO.setRequestId(tracking);

		String output = MixDecCommons.getMixDecKeyGenerationKey(keyCreationDTO);
		String expectedOutput = String.format("MIXDEC_KEY_GENERATION.%s.%s.%s", ee, ea, tracking);

		assertEquals(expectedOutput, output);
	}
}
