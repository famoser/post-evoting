/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.mixnetpayload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import ch.post.it.evoting.cryptoprimitives.SecurityLevel;
import ch.post.it.evoting.cryptoprimitives.SecurityLevelConfig;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;
import ch.post.it.evoting.domain.mixnet.MixnetFinalPayload;
import ch.post.it.evoting.domain.mixnet.ObjectMapperMixnetConfig;
import ch.post.it.evoting.domain.mixnet.SerializationUtils;
import ch.post.it.evoting.domain.mixnet.VerifiablePlaintextDecryption;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.infrastructure.PathResolver;

@DisplayName("Use MixnetFinalPayloadPersistenceService to ")
class MixnetFinalPayloadFileRepositoryTest {

	private static final String FILE_NAME = "mixnetFinalPayload_4";
	private static final String electionEventId = "f28493b098604663b6a6969f53f51b56";
	private static final String ballotId = "f0642fdfe4864f4985ac07c057da54b7";
	private static final String ballotBoxId = "672b1b49ed0341a3baa953d611b90b74";

	private static MixnetFinalPayload expectedMixnetPayload;
	private static MixnetFinalPayloadFileRepository mixnetFinalPayloadFileRepository;
	private static PathResolver payloadResolver;

	@TempDir
	Path tempDir;

	@BeforeAll
	static void setUpAll() {
		// Create ciphertexts list.
		final GqGroup gqGroup = SerializationUtils.getGqGroup();
		final int nbrMessage = 4;

		final VerifiableShuffle verifiableShuffle = SerializationUtils.getVerifiableShuffle(nbrMessage);
		final ElGamalMultiRecipientPublicKey previousRemainingPublicKey = SerializationUtils.getPublicKey();
		final VerifiablePlaintextDecryption verifiablePlaintextDecryption = SerializationUtils.getVerifiablePlaintextDecryption(nbrMessage);

		// Generate random bytes for signature content and create payload signature.
		final byte[] randomBytes = new byte[10];
		new SecureRandom().nextBytes(randomBytes);
		final X509Certificate certificate = SerializationUtils.generateTestCertificate();
		final CryptolibPayloadSignature signature = new CryptolibPayloadSignature(randomBytes, new X509Certificate[] { certificate });

		expectedMixnetPayload = new MixnetFinalPayload(gqGroup, verifiableShuffle, verifiablePlaintextDecryption, previousRemainingPublicKey,
				signature);

		payloadResolver = Mockito.mock(PathResolver.class);
		mixnetFinalPayloadFileRepository = new MixnetFinalPayloadFileRepository(FILE_NAME, ObjectMapperMixnetConfig.getNewInstance(),
				payloadResolver);
	}

	@Test
	@DisplayName("read MixnetFinalPayload file")
	void readMixnetPayload() {
		// Mock payloadResolver path and write payload
		when(payloadResolver.resolveBallotBoxPath(electionEventId, ballotId, ballotBoxId)).thenReturn(tempDir);
		mixnetFinalPayloadFileRepository.savePayload(electionEventId, ballotId, ballotBoxId, expectedMixnetPayload);

		try (MockedStatic<SecurityLevelConfig> mockedSecurityLevel = Mockito.mockStatic(SecurityLevelConfig.class)) {
			mockedSecurityLevel.when(SecurityLevelConfig::getSystemSecurityLevel).thenReturn(SecurityLevel.TESTING_ONLY);
			// Read payload and check
			final MixnetFinalPayload actualMixnetPayload = mixnetFinalPayloadFileRepository.getPayload(electionEventId, ballotId, ballotBoxId);

			assertEquals(expectedMixnetPayload, actualMixnetPayload);
		}
	}

	@Test
	@DisplayName("save MixnetFinalPayload file")
	void saveMixnetPayload() {
		// Mock payloadResolver path
		when(payloadResolver.resolveBallotBoxPath(electionEventId, ballotId, ballotBoxId)).thenReturn(tempDir);
		final Path payloadPath = tempDir.resolve(FILE_NAME + Constants.JSON);

		assertFalse(Files.exists(payloadPath), "The mixnet payload file should not exist at this point");

		// Write payload
		assertNotNull(mixnetFinalPayloadFileRepository.savePayload(electionEventId, ballotId, ballotBoxId, expectedMixnetPayload));

		assertTrue(Files.exists(payloadPath), "The mixnet payload file should exist at this point");
	}

}