/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.model.authentication.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;

import ch.post.it.evoting.cryptolib.api.derivation.CryptoAPIDerivedKey;
import ch.post.it.evoting.cryptolib.api.derivation.CryptoAPIPBKDFDeriver;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.primitives.PrimitivesServiceAPI;
import ch.post.it.evoting.cryptolib.api.symmetric.SymmetricServiceAPI;
import ch.post.it.evoting.sdm.config.exceptions.specific.GenerateAuthenticationValuesException;
import ch.post.it.evoting.sdm.config.model.authentication.AuthenticationDerivedElement;
import ch.post.it.evoting.sdm.config.model.authentication.StartVotingKey;

public class AuthenticationKeyCryptoService {

	@Autowired
	PrimitivesServiceAPI primitivesService;

	@Autowired
	SymmetricServiceAPI symmetricService;

	/**
	 * Derives a element from an authentication key for a given salt and election event id
	 *
	 * @param salt            - Constant element used for derivation purposes
	 * @param electionEventId - election event identifier
	 * @param value           - authentication key
	 * @return the derived element as String
	 */
	public AuthenticationDerivedElement deriveElement(final String salt, final String electionEventId, final String value) {

		try {

			CryptoAPIPBKDFDeriver pbkdfDeriver = primitivesService.getPBKDFDeriver();
			String composedSalt = salt.concat(electionEventId);
			final byte[] hashedSalt = primitivesService.getHash(composedSalt.getBytes(StandardCharsets.UTF_8));
			final CryptoAPIDerivedKey cryptoAPIDerivedKey = pbkdfDeriver.deriveKey(value.toCharArray(), hashedSalt);
			final byte[] keyIdBytes = cryptoAPIDerivedKey.getEncoded();
			return AuthenticationDerivedElement.of(cryptoAPIDerivedKey, new String(Hex.encodeHex(keyIdBytes)));
		} catch (GeneralCryptoLibException e) {
			throw new GenerateAuthenticationValuesException(e);
		}
	}

	/**
	 * Encrypt a StartVoting Key for a given password
	 *
	 * @param svk
	 * @param derivedPassword
	 * @return the encrypted start voting key in Base64 format
	 */
	public String encryptSVK(final StartVotingKey svk, final AuthenticationDerivedElement derivedPassword) {

		try {

			final SecretKey secretKeyForEncryption = symmetricService.getSecretKeyForEncryptionFromDerivedKey(derivedPassword.getDerivedKey());
			byte[] encryptedValue = symmetricService.encrypt(secretKeyForEncryption, svk.getValue().getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(encryptedValue);
		} catch (GeneralCryptoLibException e) {
			throw new GenerateAuthenticationValuesException(e);
		}
	}

	/**
	 * Decrypt a StartVoting Key for a given password
	 *
	 * @param encryptedSVK
	 * @param derivedPassword
	 * @return the encrypted start voting key in Base64 format
	 */
	public String decryptSVK(final String encryptedSVK, final AuthenticationDerivedElement derivedPassword) {

		try {

			final SecretKey secretKeyForEncryption = symmetricService.getSecretKeyForEncryptionFromDerivedKey(derivedPassword.getDerivedKey());
			byte[] decryptedValue = symmetricService.decrypt(secretKeyForEncryption, Base64.getDecoder().decode(encryptedSVK));
			return new String(decryptedValue, StandardCharsets.UTF_8);
		} catch (GeneralCryptoLibException e) {
			throw new GenerateAuthenticationValuesException(e);
		}
	}
}
