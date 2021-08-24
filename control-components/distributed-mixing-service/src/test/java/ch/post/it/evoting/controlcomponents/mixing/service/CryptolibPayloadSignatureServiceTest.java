/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.controlcomponents.mixing.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyManagementException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.controlcomponents.commons.keymanagement.KeysManager;
import ch.post.it.evoting.controlcomponents.commons.payloadsignature.CryptolibPayloadSignatureService;
import ch.post.it.evoting.controlcomponents.mixing.KeyManagerMockConfig;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;
import ch.post.it.evoting.domain.election.model.messaging.PayloadSignatureException;

class CryptolibPayloadSignatureServiceTest {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final AsymmetricService ASYMMETRIC_SERVICE = new AsymmetricService();
	private static KeysManager keysManager;
	private static CryptolibPayloadSignatureService signatureService;

	private byte[] payloadHash;
	private PrivateKey signingKey;
	private X509Certificate[] certificateChain;
	private CryptolibPayloadSignature signature;

	@BeforeAll
	static void setupAll() throws GeneralCryptoLibException, KeyManagementException {
		keysManager = new KeyManagerMockConfig().keyManager();
		signatureService = new CryptolibPayloadSignatureService(ASYMMETRIC_SERVICE);
	}

	@BeforeEach
	void setup() throws PayloadSignatureException, KeyManagementException {
		payloadHash = new byte[100];
		SECURE_RANDOM.nextBytes(payloadHash);
		signingKey = keysManager.getElectionSigningPrivateKey("0b88257ec32142bb8ee0ed1bb70f362e");
		certificateChain = new X509Certificate[1];
		certificateChain[0] = keysManager.getPlatformCACertificate();
		signature = signatureService.sign(payloadHash, signingKey, certificateChain);
	}

	@Test
	@DisplayName("Sign with null arguments throws a NullPointerException")
	void signWithNullArguments() {
		assertThrows(NullPointerException.class, () -> signatureService.sign(null, signingKey, certificateChain));
		assertThrows(NullPointerException.class, () -> signatureService.sign(payloadHash, null, certificateChain));
		assertThrows(NullPointerException.class, () -> signatureService.sign(payloadHash, signingKey, null));
	}

	@Test
	@DisplayName("Sign with valid inputs does not throw")
	void signWithValidInput() {
		CryptolibPayloadSignature signature = assertDoesNotThrow(() -> signatureService.sign(payloadHash, signingKey, certificateChain));
		assertArrayEquals(certificateChain, signature.getCertificateChain());
	}

	@Test
	@DisplayName("Verify with null arguments throws a NullPointerException")
	void verifyWithNullArguments() {
		assertThrows(NullPointerException.class, () -> signatureService.verify(null, certificateChain[0], payloadHash));
		assertThrows(NullPointerException.class, () -> signatureService.verify(signature, null, payloadHash));
		assertThrows(NullPointerException.class, () -> signatureService.verify(signature, certificateChain[0], null));
	}

	@Test
	@DisplayName("Verify with valid arguments does not throw")
	void verifyWithValidArguments() {
		final Boolean valid = assertDoesNotThrow(() -> signatureService.verify(signature, certificateChain[0], payloadHash));
		assertTrue(valid);
	}

	@Test
	@DisplayName("Verify with different payload hash returns false")
	void verifyWithInvalidPayloadHash() {
		payloadHash = new byte[99];
		SECURE_RANDOM.nextBytes(payloadHash);
		final Boolean valid = assertDoesNotThrow(() -> signatureService.verify(signature, certificateChain[0], payloadHash));
		assertFalse(valid);
	}

	@Test
	@DisplayName("Verify with different certificate returns false")
	void verifyWithInvalidCertificate() throws GeneralCryptoLibException, KeyManagementException {
		certificateChain[0] = new KeyManagerMockConfig().keyManager().getPlatformCACertificate();
		final Boolean valid = assertDoesNotThrow(() -> signatureService.verify(signature, certificateChain[0], payloadHash));
		assertFalse(valid);
	}

	@Test
	@DisplayName("Verify with different signature returns false")
	void verifyWithInvalidSignature() throws PayloadSignatureException {
		byte[] differentPayloadHash = new byte[99];
		SECURE_RANDOM.nextBytes(differentPayloadHash);
		signature = signatureService.sign(differentPayloadHash, signingKey, certificateChain);
		final Boolean valid = assertDoesNotThrow(() -> signatureService.verify(signature, certificateChain[0], payloadHash));
		assertFalse(valid);
	}
}
