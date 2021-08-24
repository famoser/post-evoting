/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.patch;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.domain.election.ElectionPublicKey;
import ch.post.it.evoting.domain.election.ElectoralAuthorityPublicKey;
import ch.post.it.evoting.sdm.application.exception.CreateEBKeysSerializerException;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.utils.ConfigObjectMapper;

/**
 * This is now the official implementation of the CreateEBKeysSerializer. It simply stores the EB key in a json file representing the electoral
 * authority public information
 */
public class CreateEBKeysSerializer {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateEBKeysSerializer.class);

	private final ConfigObjectMapper configObjectMapper;

	/**
	 * @param configObjectMapper
	 */
	public CreateEBKeysSerializer(final ConfigObjectMapper configObjectMapper) {
		super();
		this.configObjectMapper = configObjectMapper;
	}

	/**
	 * Serialize the public part of the key stored in the shares as a file in JSON format.
	 *
	 * @param outputFolder                the top level output folder of the serialized electoral board public key.
	 * @param electoralAuthorityId        the id of the serialized electoral authority keys
	 * @param electionPublicKey           the combined electoral authority public key.
	 * @param electoralAuthorityPublicKey the public part of the shares key
	 * @return True if both the Electoral Authority and the shares public key were successfully serialized, false otherwise.
	 */
	public boolean serializeElectionPublicKeys(Path outputFolder, String electoralAuthorityId, ElGamalPublicKey electionPublicKey,
			ElGamalPublicKey electoralAuthorityPublicKey) {

		String electionPublicKeyB64;
		String electoralAuthorityPublicKeyB64;
		try {
			electionPublicKeyB64 = Base64.getEncoder().encodeToString(electionPublicKey.toJson().getBytes(StandardCharsets.UTF_8));
			electoralAuthorityPublicKeyB64 = Base64.getEncoder()
					.encodeToString(electoralAuthorityPublicKey.toJson().getBytes(StandardCharsets.UTF_8));
		} catch (GeneralCryptoLibException e) {
			throw new CreateEBKeysSerializerException("An error occurred while attempting to serialize an ElGamal public key: " + e.getMessage(), e);
		}

		LOGGER.info("Persisting the electoral authority " + electoralAuthorityId + "...");

		ElectionPublicKey electionPK = new ElectionPublicKey();
		electionPK.setId(electoralAuthorityId);
		electionPK.setPublicKey(electionPublicKeyB64);

		ElectoralAuthorityPublicKey electoralAuthorityPublicKeyDTO = new ElectoralAuthorityPublicKey();
		electoralAuthorityPublicKeyDTO.setElectoralAuthorityId(electoralAuthorityId);
		electoralAuthorityPublicKeyDTO.setPublicKey(electoralAuthorityPublicKeyB64);

		final Path electoralAuthorityFolder = outputFolder.resolve(Constants.CONFIG_DIR_NAME_ONLINE)
				.resolve(Constants.CONFIG_DIR_NAME_ELECTORAL_AUTHORITY).resolve(electoralAuthorityId);

		Path electionPublicKeyJsonPath = electoralAuthorityFolder.resolve(Constants.CONFIG_FILE_NAME_ELECTION_PUBLIC_KEY_JSON);
		Path electoralAuthorityKeyJsonPath = electoralAuthorityFolder.resolve(Constants.CONFIG_FILE_NAME_ELECTORAL_AUTHORITY_PUBLIC_KEY_JSON);

		try {
			Files.createDirectories(electoralAuthorityFolder);
			configObjectMapper.fromJavaToJSONFile(electionPK, electionPublicKeyJsonPath.toFile());
			configObjectMapper.fromJavaToJSONFile(electoralAuthorityPublicKeyDTO, electoralAuthorityKeyJsonPath.toFile());
		} catch (IOException e) {
			throw new UncheckedIOException("An error occurred while serializing the public keys for the Electoral Authority " + electoralAuthorityId,
					e);
		}

		return Files.exists(electionPublicKeyJsonPath) && Files.exists(electoralAuthorityKeyJsonPath);
	}

}
