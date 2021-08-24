/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service.writers;

import static ch.post.it.evoting.sdm.application.service.writers.VotePrimeFactorsFileRepository.AUDITABLE_VOTES_FILENAME;
import static ch.post.it.evoting.sdm.application.service.writers.VotePrimeFactorsFileRepository.AUDITABLE_VOTES_SIGNATURE_FILENAME;
import static ch.post.it.evoting.sdm.application.service.writers.VotePrimeFactorsFileRepository.DECOMPRESSED_VOTES_FILENAME;
import static ch.post.it.evoting.sdm.application.service.writers.VotePrimeFactorsFileRepository.DECOMPRESSED_VOTES_SIGNATURE_FILENAME;
import static ch.post.it.evoting.sdm.application.service.writers.VotePrimeFactorsFileRepository.SEMICOLON_SEPARATOR;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptoprimitives.hashing.HashService;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.domain.mixnet.ObjectMapperMixnetConfig;
import ch.post.it.evoting.sdm.application.sign.FileSignerService;
import ch.post.it.evoting.sdm.domain.model.mixing.PrimeFactors;
import ch.post.it.evoting.sdm.infrastructure.PathResolver;

@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig(VotePrimeFactorsFileRepositoryTest.PrivateConfiguration.class)
class VotePrimeFactorsFileRepositoryTest {

	private final AsymmetricService asymmetricService = new AsymmetricService();
	private final KeyPair keyPairForSigning = asymmetricService.getKeyPairForSigning();
	private final PrivateKey privateKey = keyPairForSigning.getPrivate();

	private final GqGroup group = GroupTestData.getGroupP59();

	private final String ballotId = "b7e28ca876364dfa9a9315d795f59172";
	private final String ballotBoxId = "089378cdc15c480b85560b7f9adcea64";
	private final String electionEventId = "3a2434c5a1004d71ac53b55d3ccdbfb8";

	private final List<PrimeFactors> selectedEncodedVotingOptions = Arrays.asList(new PrimeFactors(ImmutableList
			.of(GqElement.create(BigInteger.valueOf(35), group), GqElement.create(BigInteger.ONE, group),
					GqElement.create(BigInteger.valueOf(21), group), GqElement.create(BigInteger.valueOf(7), group),
					GqElement.create(BigInteger.valueOf(45), group))), new PrimeFactors(ImmutableList
			.of(GqElement.create(BigInteger.valueOf(48), group), GqElement.create(BigInteger.valueOf(28), group),
					GqElement.create(BigInteger.valueOf(25), group), GqElement.create(BigInteger.valueOf(53), group))));

	private final List<List<String>> errorsList = Arrays
			.asList(Arrays.asList(DateTimeFormatter.ISO_INSTANT.format(Instant.now()), "First error.", electionEventId, "ballotBoxId1"),
					Arrays.asList(DateTimeFormatter.ISO_INSTANT.format(Instant.now()), "Second error.", electionEventId, "ballotBoxId2"),
					Arrays.asList(DateTimeFormatter.ISO_INSTANT.format(Instant.now()), "Third error.", electionEventId, "ballotBoxId3"),
					Arrays.asList(DateTimeFormatter.ISO_INSTANT.format(Instant.now()), "Fourth error.", electionEventId, "ballotBoxId4"),
					Arrays.asList(DateTimeFormatter.ISO_INSTANT.format(Instant.now()), "Fifth error.", electionEventId, "ballotBoxId1"),
					Arrays.asList(DateTimeFormatter.ISO_INSTANT.format(Instant.now()), "Sixth error.", electionEventId, "ballotBoxId2"));

	@Mock
	private PathResolver pathResolverMock;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private FileSignerService fileSignerService;

	private VotePrimeFactorsFileRepository votePrimeFactorsFileRepository;

	private static Stream<Arguments> auditableVotesWithSpecialChars() {
		final String electionEventId = "3a2434c5a1004d71ac53b55d3ccdbfb8";

		return Stream.of(Arguments.of(Collections
						.singletonList(Arrays.asList(DateTimeFormatter.ISO_INSTANT.format(Instant.now()), "", electionEventId, "ballotBoxId3")), 1), Arguments
						.of(Collections.singletonList(
								Arrays.asList(DateTimeFormatter.ISO_INSTANT.format(Instant.now()), "Another \nerror", electionEventId, "ballotBoxId2")), 2),
				Arguments.of(Collections.singletonList(
						Arrays.asList(DateTimeFormatter.ISO_INSTANT.format(Instant.now()), "This \"error\"", electionEventId, "ballotBoxId1")), 1),
				Arguments.of(Collections.singletonList(
						Arrays.asList(DateTimeFormatter.ISO_INSTANT.format(Instant.now()), "A last error;", electionEventId, "ballotBoxId3")), 1));
	}

	@BeforeEach
	void setUp() {
		votePrimeFactorsFileRepository = new VotePrimeFactorsFileRepository(pathResolverMock, fileSignerService, objectMapper);
	}

	@Test
	void persistDecompressedVotesTest(
			@TempDir
			final Path tempDir) throws IOException {

		when(pathResolverMock.resolveBallotBoxPath(any(), any(), any())).thenReturn(tempDir);

		assertDoesNotThrow(() -> votePrimeFactorsFileRepository
				.saveDecompressedVotes(selectedEncodedVotingOptions, electionEventId, ballotId, ballotBoxId, privateKey));

		// decompressed votes must contain actorsList.size() lines.
		assertEquals(selectedEncodedVotingOptions.size(), Files.lines(tempDir.resolve(DECOMPRESSED_VOTES_FILENAME)).count());

		// decompressed votes must contain exactly the given input selectedEncodedVotingOptions.
		assertEquals(selectedEncodedVotingOptions, Files.lines(tempDir.resolve(DECOMPRESSED_VOTES_FILENAME))
				.map(line -> Arrays.stream(line.split(SEMICOLON_SEPARATOR + ""))
						.map(encodedVotingOption -> GqElement.create(BigInteger.valueOf(Long.parseLong(encodedVotingOption)), group))
						.collect(Collectors.collectingAndThen(toImmutableList(), PrimeFactors::new))).collect(Collectors.toList()));

		// decompressed votes metadata must contain one single line.
		assertEquals(1, Files.lines(tempDir.resolve(DECOMPRESSED_VOTES_SIGNATURE_FILENAME)).count());

		// decompressed votes metadata must contain a field signature non-empty.
		try (final InputStream inputStream = Files.newInputStream(tempDir.resolve(DECOMPRESSED_VOTES_SIGNATURE_FILENAME), StandardOpenOption.CREATE);
				final JsonReader jsonReader = Json.createReader(inputStream)) {

			assertFalse(jsonReader.readObject().getJsonString("signature").getString().isEmpty());
		}
	}

	@Test
	void persistDecompressedVotesNullTest() {
		assertAll(() -> assertThrows(NullPointerException.class,
				() -> votePrimeFactorsFileRepository.saveDecompressedVotes(null, electionEventId, ballotId, ballotBoxId, privateKey)),
				() -> assertThrows(NullPointerException.class, () -> votePrimeFactorsFileRepository
						.saveDecompressedVotes(selectedEncodedVotingOptions, electionEventId, null, ballotBoxId, privateKey)),
				() -> assertThrows(NullPointerException.class, () -> votePrimeFactorsFileRepository
						.saveDecompressedVotes(selectedEncodedVotingOptions, electionEventId, ballotId, null, privateKey)),
				() -> assertThrows(NullPointerException.class, () -> votePrimeFactorsFileRepository
						.saveDecompressedVotes(selectedEncodedVotingOptions, null, ballotId, ballotBoxId, privateKey)),
				() -> assertThrows(NullPointerException.class, () -> votePrimeFactorsFileRepository
						.saveDecompressedVotes(selectedEncodedVotingOptions, electionEventId, ballotId, ballotBoxId, null)));
	}

	@Test
	void persistDecompressedVotesInvalidUUIDTest() {
		assertAll(() -> assertThrows(IllegalArgumentException.class, () -> votePrimeFactorsFileRepository
						.saveDecompressedVotes(selectedEncodedVotingOptions, electionEventId, "123", ballotBoxId, privateKey)),
				() -> assertThrows(IllegalArgumentException.class, () -> votePrimeFactorsFileRepository
						.saveDecompressedVotes(selectedEncodedVotingOptions, electionEventId, ballotId, "456", privateKey)),
				() -> assertThrows(IllegalArgumentException.class, () -> votePrimeFactorsFileRepository
						.saveDecompressedVotes(selectedEncodedVotingOptions, "789", ballotId, ballotBoxId, privateKey)));
	}

	@Test
	void persistDecompressedVotesEmptyInputsTest(
			@TempDir
			final Path tempDir) {
		when(pathResolverMock.resolveBallotBoxPath(any(), any(), any())).thenReturn(tempDir);

		final List<PrimeFactors> emptySelectedEncodedVotingOptions = Collections.emptyList();
		assertAll(() -> assertDoesNotThrow(() -> votePrimeFactorsFileRepository
						.saveDecompressedVotes(emptySelectedEncodedVotingOptions, electionEventId, ballotId, ballotBoxId, privateKey)),

				// decompressed votes must be empty.
				() -> assertEquals(0, Files.lines(tempDir.resolve(DECOMPRESSED_VOTES_FILENAME)).count()),

				// decompressed votes metadata must contain one single line.
				() -> assertEquals(1, Files.lines(tempDir.resolve(DECOMPRESSED_VOTES_SIGNATURE_FILENAME)).count()));

		assertAll(() -> assertThrows(IllegalArgumentException.class, () -> votePrimeFactorsFileRepository
						.saveDecompressedVotes(selectedEncodedVotingOptions, electionEventId, "", ballotBoxId, privateKey)),
				() -> assertThrows(IllegalArgumentException.class, () -> votePrimeFactorsFileRepository
						.saveDecompressedVotes(selectedEncodedVotingOptions, electionEventId, ballotId, "", privateKey)),
				() -> assertThrows(IllegalArgumentException.class, () -> votePrimeFactorsFileRepository
						.saveDecompressedVotes(selectedEncodedVotingOptions, "", ballotId, ballotBoxId, privateKey)));
	}

	@Test
	void persistDecompressedVotesExistingFilesTest(
			@TempDir
			final Path tempDir) throws IOException {
		when(pathResolverMock.resolveBallotBoxPath(any(), any(), any())).thenReturn(tempDir);

		assertTrue(tempDir.resolve(DECOMPRESSED_VOTES_FILENAME).toFile().createNewFile());
		assertTrue(tempDir.resolve(DECOMPRESSED_VOTES_SIGNATURE_FILENAME).toFile().createNewFile());

		assertAll(() -> assertDoesNotThrow(() -> votePrimeFactorsFileRepository
						.saveDecompressedVotes(selectedEncodedVotingOptions, electionEventId, ballotId, ballotBoxId, privateKey)),

				// decompressed votes must contain exactly the given input selectedEncodedVotingOptions.
				() -> assertEquals(selectedEncodedVotingOptions, Files.lines(tempDir.resolve(DECOMPRESSED_VOTES_FILENAME))
						.map(line -> Arrays.stream(line.split(SEMICOLON_SEPARATOR + ""))
								.map(encodedVotingOption -> GqElement.create(BigInteger.valueOf(Long.parseLong(encodedVotingOption)), group))
								.collect(Collectors.collectingAndThen(toImmutableList(), PrimeFactors::new))).collect(Collectors.toList())),

				// decompressed votes metadata must contain one single line.
				() -> assertEquals(1, Files.lines(tempDir.resolve(DECOMPRESSED_VOTES_SIGNATURE_FILENAME)).count()));

		// decompressed votes metadata must contain a field signature non-empty.
		try (final InputStream inputStream = Files.newInputStream(tempDir.resolve(DECOMPRESSED_VOTES_SIGNATURE_FILENAME), StandardOpenOption.CREATE);
				final JsonReader jsonReader = Json.createReader(inputStream)) {

			assertFalse(jsonReader.readObject().getJsonString("signature").getString().isEmpty());
		}
	}

	@Test
	void persistAuditableVotesTest(
			@TempDir
			final Path tempDir) throws IOException {
		when(pathResolverMock.resolveBallotBoxPath(any(), any(), any())).thenReturn(tempDir);

		assertDoesNotThrow(() -> votePrimeFactorsFileRepository.saveAuditableVotes(errorsList, electionEventId, ballotId, ballotBoxId, privateKey));

		// auditable votes must contain errors.size() lines.
		assertEquals(errorsList.size(), Files.lines(tempDir.resolve(AUDITABLE_VOTES_FILENAME)).count());

		// auditable votes must contain exactly the given errors.
		assertEquals(errorsList,
				Files.lines(tempDir.resolve(AUDITABLE_VOTES_FILENAME)).map(line -> Arrays.stream(line.split(SEMICOLON_SEPARATOR + ""))
						// Remove quotes char added on persist.
						.map(split -> split.isEmpty() ? split : split.substring(1, split.length() - 1)).collect(Collectors.toList()))
						.collect(Collectors.toList()));

		// auditable votes metadata must contain one single line.
		assertEquals(1, Files.lines(tempDir.resolve(AUDITABLE_VOTES_SIGNATURE_FILENAME)).count());

		// auditable votes metadata must contain a field signature non-empty.
		try (final InputStream inputStream = Files.newInputStream(tempDir.resolve(AUDITABLE_VOTES_SIGNATURE_FILENAME), StandardOpenOption.CREATE);
				final JsonReader jsonReader = Json.createReader(inputStream)) {

			assertFalse(jsonReader.readObject().getJsonString("signature").getString().isEmpty());
		}

	}

	@ParameterizedTest
	@MethodSource("auditableVotesWithSpecialChars")
	void persistAuditableVotesSpecialCharsTest(final List<List<String>> errorsList, final int expectedFileLines,
			@TempDir
			final Path tempDir) throws IOException {
		when(pathResolverMock.resolveBallotBoxPath(any(), any(), any())).thenReturn(tempDir);

		assertDoesNotThrow(() -> votePrimeFactorsFileRepository.saveAuditableVotes(errorsList, electionEventId, ballotId, ballotBoxId, privateKey));

		assertEquals(expectedFileLines, Files.lines(tempDir.resolve(AUDITABLE_VOTES_FILENAME)).count());
	}

	@Test
	void persistAuditableVotesNullTest() {
		assertAll(() -> assertThrows(NullPointerException.class,
				() -> votePrimeFactorsFileRepository.saveAuditableVotes(null, electionEventId, ballotId, ballotBoxId, privateKey)),
				() -> assertThrows(NullPointerException.class,
						() -> votePrimeFactorsFileRepository.saveAuditableVotes(errorsList, electionEventId, null, ballotBoxId, privateKey)),
				() -> assertThrows(NullPointerException.class,
						() -> votePrimeFactorsFileRepository.saveAuditableVotes(errorsList, electionEventId, ballotId, null, privateKey)),
				() -> assertThrows(NullPointerException.class,
						() -> votePrimeFactorsFileRepository.saveAuditableVotes(errorsList, null, ballotId, ballotBoxId, privateKey)),
				() -> assertThrows(NullPointerException.class,
						() -> votePrimeFactorsFileRepository.saveAuditableVotes(errorsList, electionEventId, ballotId, ballotBoxId, null)));
	}

	@Test
	void persistAuditableVotesInvalidUUIDTest() {
		assertAll(() -> assertThrows(IllegalArgumentException.class,
				() -> votePrimeFactorsFileRepository.saveAuditableVotes(errorsList, electionEventId, "123", ballotBoxId, privateKey)),
				() -> assertThrows(IllegalArgumentException.class,
						() -> votePrimeFactorsFileRepository.saveAuditableVotes(errorsList, electionEventId, ballotId, "456", privateKey)),
				() -> assertThrows(IllegalArgumentException.class,
						() -> votePrimeFactorsFileRepository.saveAuditableVotes(errorsList, "789", ballotId, ballotBoxId, privateKey)));
	}

	@Test
	void persistAuditableVotesEmptyInputsTest(
			@TempDir
			final Path tempDir) {
		when(pathResolverMock.resolveBallotBoxPath(any(), any(), any())).thenReturn(tempDir);

		final List<List<String>> emptyErrorsList = Collections.emptyList();
		assertAll(() -> assertDoesNotThrow(
				() -> votePrimeFactorsFileRepository.saveAuditableVotes(emptyErrorsList, electionEventId, ballotId, ballotBoxId, privateKey)),

				// auditable votes must be empty.
				() -> assertEquals(0, Files.lines(tempDir.resolve(AUDITABLE_VOTES_FILENAME)).count()),

				// auditable votes metadata must contain one single line.
				() -> assertEquals(1, Files.lines(tempDir.resolve(AUDITABLE_VOTES_SIGNATURE_FILENAME)).count()));

		assertAll(() -> assertThrows(IllegalArgumentException.class,
				() -> votePrimeFactorsFileRepository.saveAuditableVotes(errorsList, electionEventId, "", ballotBoxId, privateKey)),
				() -> assertThrows(IllegalArgumentException.class,
						() -> votePrimeFactorsFileRepository.saveAuditableVotes(errorsList, electionEventId, ballotId, "", privateKey)),
				() -> assertThrows(IllegalArgumentException.class,
						() -> votePrimeFactorsFileRepository.saveAuditableVotes(errorsList, "", ballotId, ballotBoxId, privateKey)));
	}

	@Test
	void persistAuditableVotesExistingFilesTest(
			@TempDir
			final Path tempDir) throws IOException {
		when(pathResolverMock.resolveBallotBoxPath(any(), any(), any())).thenReturn(tempDir);

		assertTrue(tempDir.resolve(AUDITABLE_VOTES_FILENAME).toFile().createNewFile());
		assertTrue(tempDir.resolve(AUDITABLE_VOTES_SIGNATURE_FILENAME).toFile().createNewFile());

		assertAll(() -> assertDoesNotThrow(
				() -> votePrimeFactorsFileRepository.saveAuditableVotes(errorsList, electionEventId, ballotId, ballotBoxId, privateKey)),

				// auditable votes must contain exactly the given errors.
				() -> assertEquals(errorsList,
						Files.lines(tempDir.resolve(AUDITABLE_VOTES_FILENAME)).map(line -> Arrays.stream(line.split(SEMICOLON_SEPARATOR + ""))
								// Remove quotes char added on persist.
								.map(split -> split.isEmpty() ? split : split.substring(1, split.length() - 1)).collect(Collectors.toList()))
								.collect(Collectors.toList())),

				// auditable votes metadata must contain one single line.
				() -> assertEquals(1, Files.lines(tempDir.resolve(AUDITABLE_VOTES_SIGNATURE_FILENAME)).count()));

		// auditable votes metadata must contain a field signature non-empty.
		try (final InputStream inputStream = Files.newInputStream(tempDir.resolve(AUDITABLE_VOTES_SIGNATURE_FILENAME), StandardOpenOption.CREATE);
				final JsonReader jsonReader = Json.createReader(inputStream)) {

			assertFalse(jsonReader.readObject().getJsonString("signature").getString().isEmpty());
		}
	}

	@Configuration
	static class PrivateConfiguration {

		@Bean
		AsymmetricServiceAPI asymmetricServiceAPI() {
			return new AsymmetricService();
		}

		@Bean
		HashService hashService() {
			return new HashService();
		}

		@Bean
		FileSignerService metadataFileSigner(final AsymmetricServiceAPI asymmetricServiceAPI, final HashService hashService) {
			return new FileSignerService(asymmetricServiceAPI, hashService);
		}

		@Bean
		public ObjectMapper objectMapper() {
			return ObjectMapperMixnetConfig.getNewInstance();
		}
	}
}

