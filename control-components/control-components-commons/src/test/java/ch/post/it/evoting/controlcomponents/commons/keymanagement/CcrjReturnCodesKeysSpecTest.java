/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;

/**
 * Tests of {@link CcrjReturnCodesKeysSpec}.
 */
class CcrjReturnCodesKeysSpecTest {
	@Test
	void testBuilder() throws GeneralCryptoLibException {
		CcrjReturnCodesKeysSpec.Builder builder = new CcrjReturnCodesKeysSpec.Builder();
		builder.setElectionEventId("electionEventId");
		builder.setVerificationCardSetId("verificationCardSetId");
		builder.setCcrjReturnCodesGenerationKeyLength(1);
		builder.setCcrjChoiceReturnCodesEncryptionKeyLength(2);
		ElGamalEncryptionParameters parameters = new ElGamalEncryptionParameters(BigInteger.valueOf(7), BigInteger.valueOf(3), BigInteger.valueOf(2));
		builder.setParameters(parameters);

		CcrjReturnCodesKeysSpec spec = builder.build();

		assertAll(() -> assertEquals("electionEventId", spec.getElectionEventId()),
				() -> assertEquals("verificationCardSetId", spec.getVerificationCardSetId()),
				() -> assertEquals(1, spec.getCcrjReturnCodesGenerationKeyLength()),
				() -> assertEquals(2, spec.getCcrjChoiceReturnCodesEncryptionKeyLength()), () -> assertEquals(parameters, spec.getParameters()));
	}
}
