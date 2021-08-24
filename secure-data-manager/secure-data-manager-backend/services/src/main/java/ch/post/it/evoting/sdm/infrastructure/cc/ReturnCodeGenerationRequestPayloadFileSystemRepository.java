/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.cc;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newDirectoryStream;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.domain.returncodes.ReturnCodeGenerationRequestPayload;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;

/**
 * A return code generation request (for both choice return codes and vote cast return codes) payload repository which stores payloads in the file
 * system.
 */
@Repository
public class ReturnCodeGenerationRequestPayloadFileSystemRepository implements ReturnCodeGenerationRequestPayloadRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReturnCodeGenerationRequestPayloadFileSystemRepository.class);

	private final PathResolver pathResolver;

	private final ObjectMapper objectMapper;

	@Autowired
	public ReturnCodeGenerationRequestPayloadFileSystemRepository(final PathResolver pathResolver, final ObjectMapper objectMapper) {
		this.pathResolver = pathResolver;
		this.objectMapper = objectMapper;
	}

	/**
	 * Obtains the path where the pre-computed data is stored.
	 *
	 * @param electionEventId       the election event the payload belongs to
	 * @param verificationCardSetId the verification card set the payload was generated for
	 * @param chunkId               the chunk identifier
	 */
	public static Path getStoragePath(PathResolver pathResolver, String electionEventId, String verificationCardSetId, int chunkId) {
		String fileName = Constants.CONFIG_FILE_NAME_PREFIX_CHOICE_CODE_GENERATION_REQUEST_PAYLOAD + chunkId
				+ Constants.CONFIG_FILE_NAME_SUFFIX_CHOICE_CODE_GENERATION_REQUEST_PAYLOAD;
		return getVerificationCardSetFolder(pathResolver, electionEventId, verificationCardSetId).resolve(fileName);
	}

	private static Path getVerificationCardSetFolder(PathResolver pathResolver, String electionEventId, String verificationCardSetId) {
		return pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR).resolve(electionEventId).resolve(Constants.CONFIG_DIR_NAME_ONLINE)
				.resolve(Constants.CONFIG_DIR_NAME_VOTERVERIFICATION).resolve(verificationCardSetId);
	}

	private static boolean isPayloadFile(Path file) {
		String name = file.getFileName().toString();
		return name.startsWith(Constants.CONFIG_FILE_NAME_PREFIX_CHOICE_CODE_GENERATION_REQUEST_PAYLOAD) && name
				.endsWith(Constants.CONFIG_FILE_NAME_SUFFIX_CHOICE_CODE_GENERATION_REQUEST_PAYLOAD);
	}

	/**
	 * Persists a choice code generation request payload.
	 *
	 * @param payload the payload to store
	 * @throws PayloadStorageException
	 */
	public void store(ReturnCodeGenerationRequestPayload payload) throws PayloadStorageException {
		String electionEventId = payload.getElectionEventId();
		String verificationCardSetId = payload.getVerificationCardSetId();
		int chunkId = payload.getChunkId();

		LOGGER.info("Storing choice code generation request payload {}-{}-{}...", electionEventId, verificationCardSetId, chunkId);

		Path file = getStoragePath(pathResolver, electionEventId, verificationCardSetId, chunkId);
		try {
			if (!exists(file.getParent())) {
				createDirectories(file.getParent());
			}
			try (OutputStream stream = newOutputStream(file)) {
				objectMapper.writeValue(stream, payload);
			}
		} catch (IOException e) {
			throw new PayloadStorageException(e);
		}

		LOGGER.info("Choice code generation request payload {}-{}-{} is now stored in {}", electionEventId, verificationCardSetId, chunkId,
				file.toAbsolutePath());
	}

	/**
	 * Retrieves a persisted choice code generation request payload.
	 *
	 * @param electionEventId       the election event identifier
	 * @param verificationCardSetId the verification card set identifier
	 * @return the requested payload
	 * @throws PayloadStorageException
	 */
	public ReturnCodeGenerationRequestPayload retrieve(String electionEventId, String verificationCardSetId, int chunkId)
			throws PayloadStorageException {
		Path file = getStoragePath(pathResolver, electionEventId, verificationCardSetId, chunkId);

		LOGGER.info("Retrieving choice code generation request payload {}-{}-{} from {}...", electionEventId, verificationCardSetId, chunkId,
				file.toAbsolutePath());

		ReturnCodeGenerationRequestPayload payload;
		try (InputStream stream = newInputStream(file)) {
			payload = objectMapper.readValue(stream, ReturnCodeGenerationRequestPayload.class);
		} catch (IOException e) {
			throw new PayloadStorageException(e);
		}

		LOGGER.info("Choice code generation request payload {}-{}-{} retrieved.", electionEventId, verificationCardSetId, chunkId);

		return payload;
	}

	@Override
	public void remove(String electionEventId, String verificationCardSetId) throws PayloadStorageException {
		try (DirectoryStream<Path> files = getPayloadFiles(electionEventId, verificationCardSetId)) {
			for (Path file : files) {
				deleteIfExists(file);
			}
		} catch (NoSuchFileException e) {
			LOGGER.debug("The verification card set folder does not exist.", e);
			// nothing to do, the verification card set folder does not exist.
		} catch (IOException e) {
			throw new PayloadStorageException(e);
		}
	}

	@Override
	public int getCount(String electionEventId, String verificationCardSetId) throws PayloadStorageException {
		int count = 0;
		try (DirectoryStream<Path> files = getPayloadFiles(electionEventId, verificationCardSetId)) {
			for (
					@SuppressWarnings("unused")
							Path file : files) {
				count++;
			}
		} catch (IOException e) {
			throw new PayloadStorageException(e);
		}
		return count;
	}

	private DirectoryStream<Path> getPayloadFiles(String electionEventId, String verificationCardSetId) throws IOException {
		Path folder = getVerificationCardSetFolder(pathResolver, electionEventId, verificationCardSetId);
		Filter<? super Path> filter = ReturnCodeGenerationRequestPayloadFileSystemRepository::isPayloadFile;
		return newDirectoryStream(folder, filter);
	}
}
