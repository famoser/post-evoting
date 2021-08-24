/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service.writers;

import static ch.post.it.evoting.cryptolib.commons.validations.Validate.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.PrivateKey;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import ch.post.it.evoting.cryptolib.api.exceptions.CryptoLibException;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.sdm.application.service.AliasSignatureConstants;
import ch.post.it.evoting.sdm.application.sign.FileSignature;
import ch.post.it.evoting.sdm.application.sign.FileSignerService;
import ch.post.it.evoting.sdm.application.sign.SignerMetadata;
import ch.post.it.evoting.sdm.domain.model.mixing.PrimeFactors;
import ch.post.it.evoting.sdm.infrastructure.PathResolver;

/**
 * Writer to persist prime factors as files. Moreover, this class signs the produced files and persists the signature in a separate file with the same
 * filename and a suffix ".metadata".
 */
@Repository
public class VotePrimeFactorsFileRepository {

	@VisibleForTesting
	static final String AUDITABLE_VOTES_FILENAME = "auditableVotes.csv";

	@VisibleForTesting
	static final String DECOMPRESSED_VOTES_FILENAME = "decompressedVotes.csv";

	@VisibleForTesting
	static final char SEMICOLON_SEPARATOR = ';';

	private static final String METADATA_FILE_EXTENSION = ".metadata";

	@VisibleForTesting
	static final String AUDITABLE_VOTES_SIGNATURE_FILENAME = AUDITABLE_VOTES_FILENAME + METADATA_FILE_EXTENSION;

	@VisibleForTesting
	static final String DECOMPRESSED_VOTES_SIGNATURE_FILENAME = DECOMPRESSED_VOTES_FILENAME + METADATA_FILE_EXTENSION;

	private static final String MIXING_SERVICE_NAME = "mixing";
	private static final CsvMapper CSV_MAPPER = new CsvMapper();
	private static final CsvSchema CSV_SCHEMA_WITH_QUOTE_CHAR = CSV_MAPPER.schemaFor(ImmutableList.class).withColumnSeparator(SEMICOLON_SEPARATOR);
	private static final CsvSchema CSV_SCHEMA_WITHOUT_QUOTE_CHAR = CSV_MAPPER.schemaFor(ImmutableList.class).withColumnSeparator(SEMICOLON_SEPARATOR)
			.withoutQuoteChar();
	private static final ObjectWriter CSV_OBJECT_WRITER_WITH_QUOTE_CHAR = CSV_MAPPER.writer(CSV_SCHEMA_WITH_QUOTE_CHAR);
	private static final ObjectWriter CSV_OBJECT_WRITER_WITHOUT_QUOTE_CHAR = CSV_MAPPER.writer(CSV_SCHEMA_WITHOUT_QUOTE_CHAR);

	private final PathResolver pathResolver;
	private final FileSignerService fileSignerService;
	private final ObjectMapper objectMapper;

	@Autowired
	public VotePrimeFactorsFileRepository(final PathResolver pathResolver, final FileSignerService fileSignerService,
			final ObjectMapper objectMapper) {
		this.pathResolver = pathResolver;
		this.fileSignerService = fileSignerService;
		this.objectMapper = objectMapper;
	}

	/**
	 * Persists the decompressed votes to {@value DECOMPRESSED_VOTES_FILENAME} and its signature to {@value DECOMPRESSED_VOTES_SIGNATURE_FILENAME} in
	 * the corresponding ballot box directory {@link PathResolver#resolveBallotBoxPath}.
	 * <p>
	 * The content of {@value DECOMPRESSED_VOTES_FILENAME} is printed separated with a semicolon (";").
	 * <p>
	 * If the files do not exist, they will be created and filled with the expected content. If the files already exist, their content will be
	 * overridden by this call.
	 *
	 * @param selectedEncodedVotingOptions the list containing the selections of all votes in the ballot box. One vote contains a list of GqElements:
	 *                                     the prime numbers representing each selected voting option. Must be non-null.
	 * @param electionEventId              the election event id. Must be non-null and a valid UUID.
	 * @param ballotId                     the ballot id. Must be non-null and a valid UUID.
	 * @param ballotBoxId                  the ballot box id. Must be non-null and a valid UUID.
	 * @param signingKey                   the signing key. Must be non-null.
	 * @throws IllegalArgumentException if the ballot id, the ballot box id or the election event id is not a valid UUID.
	 */
	public void saveDecompressedVotes(final List<PrimeFactors> selectedEncodedVotingOptions, final String electionEventId, final String ballotId,
			final String ballotBoxId, final PrivateKey signingKey) {
		checkNotNull(selectedEncodedVotingOptions);

		final ImmutableList<ImmutableList<GqElement>> immutableSelectedEncodedVotingOptions = selectedEncodedVotingOptions.stream()
				.map(PrimeFactors::getFactors).map(ImmutableList::copyOf).collect(ImmutableList.toImmutableList());

		checkNotNull(ballotId);
		validateUUID(ballotId);

		checkNotNull(ballotBoxId);
		validateUUID(ballotBoxId);

		checkNotNull(electionEventId);
		validateUUID(electionEventId);

		checkNotNull(signingKey);

		final Path ballotBoxPath = pathResolver.resolveBallotBoxPath(electionEventId, ballotId, ballotBoxId);
		final Path decompressedVotesFilePath = ballotBoxPath.resolve(DECOMPRESSED_VOTES_FILENAME);

		saveContent(decompressedVotesFilePath.toFile(), immutableSelectedEncodedVotingOptions.stream()
				.map(list -> list.stream().map(factor -> factor.getValue().toString()).collect(ImmutableList.toImmutableList()))
				.collect(ImmutableList.toImmutableList()), false);

		// Sign file and save a metadata file
		SignerMetadata signer = new SignerMetadata();
		try {
			signer.sign(signingKey, decompressedVotesFilePath);
		} catch (IOException e) {
			throw new UncheckedIOException("Couldn't read the decompressed votes file.", e);
		} catch (GeneralCryptoLibException e) {
			throw new CryptoLibException("Couldn't create the signature for the decompressed votes metadata file.", e);
		}
	}

	/**
	 * Persists the auditable votes to {@value AUDITABLE_VOTES_FILENAME} and its signature to {@value AUDITABLE_VOTES_SIGNATURE_FILENAME} in the
	 * corresponding ballot box directory {@link PathResolver#resolveBallotBoxPath}.
	 * <p>
	 * The content of {@value AUDITABLE_VOTES_FILENAME} is printed separated with a semicolon (";") and wrapped in double-quotes.
	 * <p>
	 * If the files do not exist, they will be created and filled with the expected content. If the files already exist, their content will be
	 * overridden by this call.
	 *
	 * @param errorsContent   the list of errors encountered during the decoding of the plaintext votes. Must be non-null.
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @param ballotId        the ballot id. Must be non-null and a valid UUID.
	 * @param ballotBoxId     the ballot box id. Must be non-null and a valid UUID.
	 * @param signingKey      the signing key. Must be non-null.
	 * @throws IllegalArgumentException if the ballot id, the ballot box id or the election event id is not a valid UUID.
	 */
	public void saveAuditableVotes(final List<List<String>> errorsContent, final String electionEventId, final String ballotId,
			final String ballotBoxId, final PrivateKey signingKey) {
		checkNotNull(errorsContent);

		final ImmutableList<ImmutableList<String>> immutableErrorsContent = errorsContent.stream().map(ImmutableList::copyOf)
				.collect(ImmutableList.toImmutableList());

		checkNotNull(ballotId);
		validateUUID(ballotId);

		checkNotNull(ballotBoxId);
		validateUUID(ballotBoxId);

		checkNotNull(electionEventId);
		validateUUID(electionEventId);

		checkNotNull(signingKey);

		final Path ballotBoxPath = pathResolver.resolveBallotBoxPath(electionEventId, ballotId, ballotBoxId);
		final Path auditableVotesFilePath = ballotBoxPath.resolve(AUDITABLE_VOTES_FILENAME);

		saveContent(auditableVotesFilePath.toFile(), immutableErrorsContent, true);

		final FileSignature fileSignature = sign(auditableVotesFilePath, signingKey, electionEventId, ballotBoxId);
		final Path signaturePath = ballotBoxPath.resolve(AUDITABLE_VOTES_SIGNATURE_FILENAME);

		saveSignature(signaturePath, fileSignature);
	}

	/**
	 * Persists the given {@code signatureMetadata} in the file corresponding to the given {@code path}.
	 * <p>
	 * If the file does not exist, it will be created and filled with the given {@code signatureMetadata}. If the file already exists, its content
	 * will be overridden by this one.
	 *
	 * @param path          the path to the file where to persist the signature. Must be non-null.
	 * @param fileSignature the signature to persist. Must be non-null.
	 */
	private void saveSignature(final Path path, final FileSignature fileSignature) {
		checkNotNull(path);
		checkNotNull(fileSignature);

		try {
			final byte[] signatureBytes = objectMapper.writeValueAsBytes(fileSignature);
			Files.write(path, signatureBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
		} catch (IOException e) {
			throw new UncheckedIOException("Couldn't persist the signature", e);
		}
	}

	/**
	 * Persists the given content to the given file.
	 * <p>
	 * The outer list is the lines to be printed and the inner list is each line content. This content is printed separated with a semicolon (";") and
	 * wrapped in double-quotes depending on the value of {@code withQuoteChar}.
	 * <p>
	 * If the file does not exist, it will be created and filled with the given content. If the file already exists, its content will be overridden by
	 * this one.
	 *
	 * @param file          the file to which the content must be written. Must be non-null.
	 * @param content       the content to be written. Must be non-null.
	 * @param withQuoteChar a boolean to indicate whether the content must be wrapped in quotes or not.
	 */
	private void saveContent(final File file, final ImmutableList<ImmutableList<String>> content, boolean withQuoteChar) {
		checkNotNull(content);
		checkNotNull(file);

		final ObjectWriter objectWriter = withQuoteChar ? CSV_OBJECT_WRITER_WITH_QUOTE_CHAR : CSV_OBJECT_WRITER_WITHOUT_QUOTE_CHAR;
		try {
			objectWriter.writeValue(file, content);
		} catch (IOException e) {
			throw new UncheckedIOException(String.format("Couldn't create, write or close file %s", file), e);
		}
	}

	private FileSignature sign(final Path path, final PrivateKey privateKey, final String electionEventId, final String ballotBoxId) {
		final Map<String, String> signedFields = createSignedFields(electionEventId, ballotBoxId);

		try {
			return fileSignerService.createSignature(privateKey, path, signedFields);
		} catch (IOException e) {
			throw new UncheckedIOException("Couldn't create the signature", e);
		} catch (GeneralCryptoLibException e) {
			throw new CryptoLibException("Couldn't create the signature", e);
		}
	}

	private Map<String, String> createSignedFields(final String electionEventId, final String ballotBoxId) {

		final Map<String, String> signedFields = new LinkedHashMap<>();
		signedFields.put(AliasSignatureConstants.ELECTION_EVENT_ID, electionEventId);
		signedFields.put(AliasSignatureConstants.BALLOT_BOX_ID, ballotBoxId);
		signedFields.put(AliasSignatureConstants.TIMESTAMP, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
		signedFields.put(AliasSignatureConstants.COMPONENT, MIXING_SERVICE_NAME);

		return signedFields;
	}

}
