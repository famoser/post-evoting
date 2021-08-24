/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptoprimitives.hashing.HashService;
import ch.post.it.evoting.domain.mixnet.MixnetShufflePayload;
import ch.post.it.evoting.domain.mixnet.ObjectMapperMixnetConfig;
import ch.post.it.evoting.domain.mixnet.exceptions.FailedValidationException;
import ch.post.it.evoting.sdm.commons.CryptolibPayloadSignatureService;
import ch.post.it.evoting.sdm.infrastructure.mixnetpayload.MixnetShufflePayloadFileRepository;

@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig(MixnetShufflePayloadServiceTest.PrivateConfiguration.class)
@DisplayName("A MixnetShufflePayloadService")
class MixnetShufflePayloadServiceTest {

	@Autowired
	private MixnetShufflePayloadService MixnetShufflePayloadService;

	@Autowired
	private MixnetShufflePayloadFileRepository mixnetShufflePayloadFileRepository;

	@Autowired
	private PlatformRootCAService platformRootCAService;

	@Configuration
	static class PrivateConfiguration {

		@Bean
		AsymmetricServiceAPI asymmetricServiceAPI() {
			return new AsymmetricService();
		}

		@Bean
		PlatformRootCAService platformRootCAService() {
			return mock(PlatformRootCAService.class);
		}

		@Bean
		MixnetShufflePayloadFileRepository mixnetShufflePayloadFileRepository() {
			return mock(MixnetShufflePayloadFileRepository.class);
		}

		@Bean
		CryptolibPayloadSignatureService cryptolibPayloadSignatureService(final AsymmetricServiceAPI asymmetricService) {
			return new CryptolibPayloadSignatureService(asymmetricService);
		}

		@Bean
		HashService hashService() {
			return new HashService();
		}

		@Bean
		MixnetShufflePayloadService MixnetShufflePayloadService(final CryptolibPayloadSignatureService cryptolibPayloadSignatureService,
				final MixnetShufflePayloadFileRepository mixnetShufflePayloadFileRepository, final HashService hashService,
				final PlatformRootCAService platformRootCAService) {
			return new MixnetShufflePayloadService(cryptolibPayloadSignatureService, mixnetShufflePayloadFileRepository, hashService,
					platformRootCAService);
		}

	}

	@Nested
	@DisplayName("calling areOnlinePayloadSignaturesValid")
	class AreOnlinePayloadSignaturesValid {

		private final String electionEventId = "e9b5d2ece77c4732ba2b00feafac2fd2";
		private final String ballotId = "6dff70421c3c4388bb6de76f9cd2e8df";
		private final String ballotBoxId = "4e9e694a322545d793b2e51aca63778b";

		@Test
		@DisplayName("with a null input throws a NullPointerException.")
		void nullInputTest() {
			assertAll(() -> assertThrows(NullPointerException.class,
					() -> MixnetShufflePayloadService.areOnlinePayloadSignaturesValid(null, ballotId, ballotBoxId)),
					() -> assertThrows(NullPointerException.class,
							() -> MixnetShufflePayloadService.areOnlinePayloadSignaturesValid(electionEventId, null, ballotBoxId)),
					() -> assertThrows(NullPointerException.class,
							() -> MixnetShufflePayloadService.areOnlinePayloadSignaturesValid(electionEventId, ballotId, null)));
		}

		@Test
		@DisplayName("with a non-UUID input throws a FailedValidationException.")
		void nonUUIDInputTest() {
			assertAll(() -> assertThrows(FailedValidationException.class,
					() -> MixnetShufflePayloadService.areOnlinePayloadSignaturesValid("electionEventId", ballotId, ballotBoxId)),
					() -> assertThrows(FailedValidationException.class,
							() -> MixnetShufflePayloadService.areOnlinePayloadSignaturesValid(electionEventId, "ballotId", ballotBoxId)),
					() -> assertThrows(FailedValidationException.class,
							() -> MixnetShufflePayloadService.areOnlinePayloadSignaturesValid(electionEventId, ballotId, "ballotBoxId")));
		}

		@Test
		@DisplayName("with valid inputs behaves as expected.")
		void happyPathTest() throws URISyntaxException, IOException, CertificateManagementException, GeneralCryptoLibException {

			final ObjectMapper objectMapper = ObjectMapperMixnetConfig.getNewInstance();

			when(mixnetShufflePayloadFileRepository.getPayload(electionEventId, ballotId, ballotBoxId, 1))
					.thenReturn(objectMapper.readValue(loadMixnetShufflePayloadPath(1).toFile(), MixnetShufflePayload.class));
			when(mixnetShufflePayloadFileRepository.getPayload(electionEventId, ballotId, ballotBoxId, 2))
					.thenReturn(objectMapper.readValue(loadMixnetShufflePayloadPath(2).toFile(), MixnetShufflePayload.class));
			when(mixnetShufflePayloadFileRepository.getPayload(electionEventId, ballotId, ballotBoxId, 3))
					.thenReturn(objectMapper.readValue(loadMixnetShufflePayloadPath(3).toFile(), MixnetShufflePayload.class));

			when(platformRootCAService.load()).thenReturn(
					(X509Certificate) PemUtils.certificateFromPem(new String(Files.readAllBytes(loadPlatformRootCAPath()), StandardCharsets.UTF_8)));

			assertTrue(MixnetShufflePayloadService.areOnlinePayloadSignaturesValid(electionEventId, ballotId, ballotBoxId));
		}

		private Path loadMixnetShufflePayloadPath(final int controlComponentNodeId) throws URISyntaxException {
			final String path = String
					.format("/MixnetShufflePayloadServiceTest/e9b5d2ece77c4732ba2b00feafac2fd2/ONLINE/electionInformation/ballots/6dff70421c3c4388bb6de76f9cd2e8df/ballotBoxes/4e9e694a322545d793b2e51aca63778b/mixnetShufflePayload_%s.json",
							controlComponentNodeId);

			return Paths.get(MixnetShufflePayloadServiceTest.class.getResource(path).toURI());
		}

		private Path loadPlatformRootCAPath() throws URISyntaxException {
			return Paths.get(MixnetShufflePayloadServiceTest.class.getResource("/MixnetShufflePayloadServiceTest/platformRootCA.pem").toURI());
		}
	}
}
