/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.primitives.PrimitivesServiceAPI;
import ch.post.it.evoting.cryptolib.primitives.service.PollingPrimitivesServiceFactory;

@Service
public class HashService {

	private final PrimitivesServiceAPI primitivesService;

	/**
	 * Constructor.
	 */
	public HashService() {
		primitivesService = new PollingPrimitivesServiceFactory().create();
	}

	/**
	 * Get a B64 encoded hash of the provided file.
	 *
	 * @param filePath the file to hash.
	 * @return the encoded B64 file hash.
	 * @throws HashServiceException if there is an error hashing the data.
	 */
	public String getB64FileHash(Path filePath) throws HashServiceException {
		try (InputStream fileIn = new FileInputStream(filePath.toString())) {
			return Base64.getEncoder().encodeToString(primitivesService.getHash(fileIn));
		} catch (IOException | GeneralCryptoLibException e) {
			throw new HashServiceException("Error computing the hash of " + filePath.toString(), e);
		}
	}

	/**
	 * Get a B64 encoded hash of the provided bytes.
	 *
	 * @param dataToHash the bytes to hash.
	 * @return the hashed bytes B64 encoded.
	 * @throws HashServiceException if there is an error hashing the data.
	 */
	public String getB64Hash(byte[] dataToHash) throws HashServiceException {
		try {
			byte[] hashBytes = primitivesService.getHash(dataToHash);
			return Base64.getEncoder().encodeToString(hashBytes);
		} catch (GeneralCryptoLibException e) {
			throw new HashServiceException("Error computing a hash with provided bytes", e);
		}
	}

	/**
	 * Gets the hash value for member.
	 *
	 * @param member the member
	 * @return the hash value for member
	 * @throws HashServiceException if an empty member is provided
	 */
	public String getHashValueForMember(final String member) throws HashServiceException {

		byte[] memberByteArray = member.getBytes(StandardCharsets.UTF_8);
		return getB64Hash(memberByteArray);
	}
}
