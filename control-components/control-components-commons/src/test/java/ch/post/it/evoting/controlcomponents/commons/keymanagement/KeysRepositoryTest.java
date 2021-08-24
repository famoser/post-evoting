/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.KeyManagementException;
import java.security.KeyStore.PasswordProtection;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.controlcomponents.commons.keymanagement.exception.KeyAlreadyExistsException;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.exception.KeyNotFoundException;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.persistence.CcmjElectionKeysEntity;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.persistence.CcmjElectionKeysEntityPrimaryKey;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.persistence.CcmjElectionKeysEntityRepository;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.persistence.CcrjReturnCodesKeysEntity;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.persistence.CcrjReturnCodesKeysEntityPrimaryKey;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.persistence.CcrjReturnCodesKeysEntityRepository;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.persistence.ElectionSigningKeysEntity;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.persistence.ElectionSigningKeysEntityPrimaryKey;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.persistence.ElectionSigningKeysEntityRepository;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.persistence.NodeKeysEntity;
import ch.post.it.evoting.controlcomponents.commons.keymanagement.persistence.NodeKeysEntityRepository;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;

class KeysRepositoryTest {

	private static final String NODE_ID = "nodeId";
	private static final String ELECTION_EVENT_ID = "0b88257ec32142bb8ee0ed1bb70f362e";
	private static final String VERIFICATION_CARD_SET_ID = "ffbf3c4fd4314309b5988b3df2668a2c";
	private static final String ENCRYPTION_PARAMETERS_JSON = "{\"encryptionParams\":{\"p\":\"AMoeTsDNAOCZ1I+tv3/xjI/HVT8Ab3h8Ifm"
			+ "/d1623VoBQaL8mzFgtUerEiw/FcjyUnjwvu0cc2kPR6uM4eMgK6c7YXE/LSJ2BDgNMVLUgIIC+qVZLiEG/yPZAT"
			+ "+8akHS0Audz7XIxBnd1EzqsnkIFjsgtER3iVWpC7BTxJ1sT19pB+OaA2b"
			+ "+L6nVW0sZNwiHFCvvQ5rLTEc4uDEBcT6aNIXCK4Qkztc1hTfccn3k6158NNGMddvBB5GzqBoQMzjW35mKUBXOufRhQTYVQjNSoapfVfVKDsTkD1BDvdffJC/gP82m"
			+ "+/kud0x0p6RWfmfHgFoNmG6EjJkoMGfGLVJAMVs=\",\"q\":\"ZQ8nYGaAcEzqR9bfv/jGR+Oqn4A3vD4Q/N+7r1turQCg0X5NmLBao9WJFh"
			+ "+K5HkpPHhfdo45tIej1cZw8ZAV052wuJ+WkTsCHAaYqWpAQQF9UqyXEIN"
			+ "/keyAn941IOloBc7n2uRiDO7qJnVZPIQLHZBaIjvEqtSF2CniTrYnr7SD8c0Bs38X1OqtpYybhEOKFfehzWWmI5xcGIC4n00aQuEVwhJna5rCm"
			+ "+45PvJ1rz4aaMY67eCDyNnUDQgZnGtvzMUoCudc+jCgmwqhGalQ1S+q+qUHYnIHqCHe6++SF/Af5tN9/Jc7pjpT0is/M+PALQbMN0JGTJQYM+MWqSAYrQ==\","
			+ "\"g\":\"Aw==\"}}";

	private static ElGamalPrivateKey ELGAMAL_PRIVATE_KEY;
	private static ElGamalPublicKey ELGAMAL_PUBLIC_KEY;

	private final PasswordProtection password = new PasswordProtection("password".toCharArray());
	private final Codec codec = mock(Codec.class);
	private final Generator generator = mock(Generator.class);
	private final PrivateKey encryptionPrivateKey = mock(PrivateKey.class);
	private final PublicKey encryptionPublicKey = mock(PublicKey.class);
	private final NodeKeysEntityRepository nodeKeysEntityRepository = mock(NodeKeysEntityRepository.class);
	private final ElectionSigningKeysEntityRepository electionSigningKeysEntityRepository = mock(ElectionSigningKeysEntityRepository.class);
	private final CcmjElectionKeysEntityRepository ccmjElectionKeysEntityRepository = mock(CcmjElectionKeysEntityRepository.class);
	private final CcrjReturnCodesKeysEntityRepository ccrjReturnCodesKeysEntityRepository = mock(CcrjReturnCodesKeysEntityRepository.class);

	private KeysRepository keysRepository;

	@BeforeAll
	public static void beforeClass() throws GeneralCryptoLibException {
		final ElGamalKeyPair pair = new ElGamalService().generateKeyPair(ElGamalEncryptionParameters.fromJson(ENCRYPTION_PARAMETERS_JSON), 1);
		ELGAMAL_PRIVATE_KEY = pair.getPrivateKeys();
		ELGAMAL_PUBLIC_KEY = pair.getPublicKeys();
	}

	@BeforeEach
	public void setUp() throws KeyManagementException {
		when(generator.generatePassword()).thenReturn(password);

		keysRepository = new KeysRepository(codec, generator, nodeKeysEntityRepository, electionSigningKeysEntityRepository,
				ccmjElectionKeysEntityRepository, ccrjReturnCodesKeysEntityRepository, NODE_ID);
		keysRepository.setEncryptionKeys(encryptionPrivateKey, encryptionPublicKey);
	}

	@Nested
	class NodeKeysTests {

		@Test
		void testSaveNodeKeys() throws KeyManagementException {
			final PrivateKey privateKey = mock(PrivateKey.class);
			final X509Certificate[] certificateChain = { mock(X509Certificate.class) };
			final NodeKeys nodeKeys = new NodeKeys.Builder().setCAKeys(privateKey, certificateChain).setEncryptionKeys(privateKey, certificateChain)
					.setLogSigningKeys(privateKey, certificateChain).setLogEncryptionKeys(privateKey, certificateChain).build();

			final byte[] keysBytes = { 1, 2, 3 };

			when(codec.encodeNodeKeys(nodeKeys, password)).thenReturn(keysBytes);
			when(nodeKeysEntityRepository.existsById(NODE_ID)).thenReturn(false);

			assertDoesNotThrow(() -> keysRepository.saveNodeKeys(nodeKeys, password));
		}

		@Test
		void testSaveNodeKeysAlreadyExist() {
			final PrivateKey privateKey = mock(PrivateKey.class);
			final X509Certificate[] certificateChain = { mock(X509Certificate.class) };
			final NodeKeys nodeKeys = new NodeKeys.Builder().setCAKeys(privateKey, certificateChain).setEncryptionKeys(privateKey, certificateChain)
					.setLogSigningKeys(privateKey, certificateChain).setLogEncryptionKeys(privateKey, certificateChain).build();

			when(nodeKeysEntityRepository.existsById(NODE_ID)).thenReturn(true);

			final KeyAlreadyExistsException keyAlreadyExistsException = assertThrows(KeyAlreadyExistsException.class,
					() -> keysRepository.saveNodeKeys(nodeKeys, password));
			assertEquals(String.format("Node keys already exist for node id %s.", NODE_ID), keyAlreadyExistsException.getMessage());
		}

		@Test
		void testSaveNodeKeysCodecException() throws KeyManagementException {
			final PrivateKey privateKey = mock(PrivateKey.class);
			final X509Certificate[] certificateChain = { mock(X509Certificate.class) };
			final NodeKeys nodeKeys = new NodeKeys.Builder().setCAKeys(privateKey, certificateChain).setEncryptionKeys(privateKey, certificateChain)
					.setLogSigningKeys(privateKey, certificateChain).setLogEncryptionKeys(privateKey, certificateChain).build();

			final String exceptionMessage = "exceptionMessage";

			when(codec.encodeNodeKeys(nodeKeys, password)).thenThrow(new KeyManagementException(exceptionMessage));
			when(nodeKeysEntityRepository.existsById(NODE_ID)).thenReturn(false);

			final KeyManagementException keyManagementException = assertThrows(KeyManagementException.class,
					() -> keysRepository.saveNodeKeys(nodeKeys, password));
			assertEquals(exceptionMessage, keyManagementException.getMessage());
		}

		@Test
		void testLoadNodeKeys() throws KeyManagementException {
			final PrivateKey privateKey = mock(PrivateKey.class);
			final X509Certificate[] certificateChain = { mock(X509Certificate.class) };
			final NodeKeys nodeKeys = new NodeKeys.Builder().setCAKeys(privateKey, certificateChain).setEncryptionKeys(privateKey, certificateChain)
					.setLogSigningKeys(privateKey, certificateChain).setLogEncryptionKeys(privateKey, certificateChain).build();

			final byte[] keysBytes = { 1, 2, 3 };
			when(nodeKeysEntityRepository.findById(NODE_ID)).thenReturn(Optional.of(new NodeKeysEntity(NODE_ID, keysBytes)));
			when(codec.decodeNodeKeys(keysBytes, password)).thenReturn(nodeKeys);

			assertEquals(nodeKeys, keysRepository.loadNodeKeys(password));
		}

		@Test
		void testLoadNodeKeysCodecException() throws KeyManagementException {
			when(nodeKeysEntityRepository.findById(NODE_ID)).thenReturn(Optional.of(new NodeKeysEntity(NODE_ID, new byte[0])));

			final String exceptionMessage = "exceptionMessage";
			when(codec.decodeNodeKeys(any(byte[].class), eq(password))).thenThrow(new KeyManagementException(exceptionMessage));

			final KeyManagementException keyManagementException = assertThrows(KeyManagementException.class,
					() -> keysRepository.loadNodeKeys(password));
			assertEquals(exceptionMessage, keyManagementException.getMessage());
		}

		@Test
		void testLoadNodeKeysNotFound() {
			when(nodeKeysEntityRepository.findById(NODE_ID)).thenReturn(Optional.empty());

			final KeyNotFoundException keyNotFoundException = assertThrows(KeyNotFoundException.class, () -> keysRepository.loadNodeKeys(password));
			assertEquals(String.format("Node keys not found for node id %s.", NODE_ID), keyNotFoundException.getMessage());
		}
	}

	@Nested
	class ElectionSigningKeysTests {
		@Test
		void testLoadElectionSigningKeys() throws KeyManagementException {
			final PrivateKey privateKey = mock(PrivateKey.class);
			final X509Certificate[] certificateChain = { mock(X509Certificate.class) };

			final ElectionSigningKeys electionSigningKeys = new ElectionSigningKeys(privateKey, certificateChain);

			final byte[] keysBytes = { 1, 2, 3 };
			final byte[] passwordBytes = { 4, 5, 6 };

			when(codec.decodePassword(passwordBytes, encryptionPrivateKey)).thenReturn(password);
			when(codec.decodeElectionSigningKeys(keysBytes, password)).thenReturn(electionSigningKeys);

			when(electionSigningKeysEntityRepository.findById(new ElectionSigningKeysEntityPrimaryKey(NODE_ID, ELECTION_EVENT_ID)))
					.thenReturn(Optional.of(new ElectionSigningKeysEntity(NODE_ID, ELECTION_EVENT_ID, keysBytes, passwordBytes)));

			assertEquals(electionSigningKeys, keysRepository.loadElectionSigningKeys(ELECTION_EVENT_ID));
			assertTrue(password.isDestroyed());
		}

		@Test
		void testLoadElectionSigningKeysCodecException() throws KeyManagementException {
			final String exceptionMessage = "exceptionMessage";
			when(codec.decodePassword(any(byte[].class), eq(encryptionPrivateKey))).thenThrow(new KeyManagementException(exceptionMessage));

			when(electionSigningKeysEntityRepository.findById(new ElectionSigningKeysEntityPrimaryKey(NODE_ID, ELECTION_EVENT_ID)))
					.thenReturn(Optional.of(new ElectionSigningKeysEntity(NODE_ID, ELECTION_EVENT_ID, new byte[0], new byte[0])));

			final KeyManagementException keyManagementException = assertThrows(KeyManagementException.class,
					() -> keysRepository.loadElectionSigningKeys(ELECTION_EVENT_ID));

			assertEquals(exceptionMessage, keyManagementException.getMessage());
		}

		@Test
		void testLoadElectionSigningKeysNotFound() {
			when(electionSigningKeysEntityRepository.findById(new ElectionSigningKeysEntityPrimaryKey(NODE_ID, ELECTION_EVENT_ID)))
					.thenReturn(Optional.empty());

			final KeyNotFoundException keyNotFoundException = assertThrows(KeyNotFoundException.class,
					() -> keysRepository.loadElectionSigningKeys(ELECTION_EVENT_ID));

			assertEquals(String.format("Election signing keys not found for node id %s and election event id %s.", NODE_ID, ELECTION_EVENT_ID),
					keyNotFoundException.getMessage());
		}

		@Test
		void testSaveElectionSigningKeys() throws KeyManagementException {
			final PrivateKey privateKey = mock(PrivateKey.class);
			final X509Certificate[] certificateChain = { mock(X509Certificate.class) };
			final ElectionSigningKeys electionSigningKeys = new ElectionSigningKeys(privateKey, certificateChain);

			final byte[] passwordBytes = { 1 };
			final byte[] keysBytes = { 2 };
			when(codec.encodePassword(password, encryptionPublicKey)).thenReturn(passwordBytes);
			when(codec.encodeElectionSigningKeys(electionSigningKeys, password)).thenReturn(keysBytes);

			when(electionSigningKeysEntityRepository.existsById(new ElectionSigningKeysEntityPrimaryKey(NODE_ID, ELECTION_EVENT_ID)))
					.thenReturn(false);

			assertDoesNotThrow(() -> keysRepository.saveElectionSigningKeys(ELECTION_EVENT_ID, electionSigningKeys));
			assertTrue(password.isDestroyed());
		}

		@Test
		void testSaveElectionSigningKeysAlreadyExists() {
			final PrivateKey privateKey = mock(PrivateKey.class);
			final X509Certificate[] certificateChain = { mock(X509Certificate.class) };
			final ElectionSigningKeys electionSigningKeys = new ElectionSigningKeys(privateKey, certificateChain);

			when(electionSigningKeysEntityRepository.existsById(new ElectionSigningKeysEntityPrimaryKey(NODE_ID, ELECTION_EVENT_ID)))
					.thenReturn(true);

			final KeyAlreadyExistsException keyAlreadyExistsException = assertThrows(KeyAlreadyExistsException.class,
					() -> keysRepository.saveElectionSigningKeys(ELECTION_EVENT_ID, electionSigningKeys));

			assertEquals(String.format("Election signing keys already exist for node id %s and election event id %s.", NODE_ID, ELECTION_EVENT_ID),
					keyAlreadyExistsException.getMessage());
		}

		@Test
		void testSaveElectionSigningKeysCodecException() throws KeyManagementException {
			final PrivateKey privateKey = mock(PrivateKey.class);
			final X509Certificate[] certificateChain = { mock(X509Certificate.class) };
			final ElectionSigningKeys electionSigningKeys = new ElectionSigningKeys(privateKey, certificateChain);

			final byte[] passwordBytes = { 1 };
			when(codec.encodePassword(password, encryptionPublicKey)).thenReturn(passwordBytes);

			final String exceptionMessage = "exceptionMessage";
			when(codec.encodeElectionSigningKeys(electionSigningKeys, password)).thenThrow(new KeyManagementException(exceptionMessage));

			when(electionSigningKeysEntityRepository.existsById(new ElectionSigningKeysEntityPrimaryKey(NODE_ID, ELECTION_EVENT_ID)))
					.thenReturn(false);

			final KeyManagementException keyManagementException = assertThrows(KeyManagementException.class,
					() -> keysRepository.saveElectionSigningKeys(ELECTION_EVENT_ID, electionSigningKeys));

			assertEquals(exceptionMessage, keyManagementException.getMessage());
			assertTrue(password.isDestroyed());
		}

		@Test
		void testSaveElectionSigningKeysDuplicates() {
			final PrivateKey privateKey = mock(PrivateKey.class);
			final X509Certificate[] certificateChain = { mock(X509Certificate.class) };
			final ElectionSigningKeys electionSigningKeys = new ElectionSigningKeys(privateKey, certificateChain);

			when(electionSigningKeysEntityRepository.existsById(new ElectionSigningKeysEntityPrimaryKey(NODE_ID, ELECTION_EVENT_ID)))
					.thenReturn(true);

			final KeyAlreadyExistsException keyAlreadyExistsException = assertThrows(KeyAlreadyExistsException.class,
					() -> keysRepository.saveElectionSigningKeys(ELECTION_EVENT_ID, electionSigningKeys));
			assertEquals(String.format("Election signing keys already exist for node id %s and election event id %s.", NODE_ID, ELECTION_EVENT_ID),
					keyAlreadyExistsException.getMessage());
		}

		@Test
		void testSaveElectionSigningKeysGeneratorException() throws KeyManagementException {
			final PrivateKey privateKey = mock(PrivateKey.class);
			final X509Certificate[] certificateChain = { mock(X509Certificate.class) };
			final ElectionSigningKeys electionSigningKeys = new ElectionSigningKeys(privateKey, certificateChain);

			final String exceptionMessage = "exceptionMessage";
			when(generator.generatePassword()).thenThrow(new KeyManagementException(exceptionMessage));

			final KeyManagementException keyManagementException = assertThrows(KeyManagementException.class,
					() -> keysRepository.saveElectionSigningKeys(ELECTION_EVENT_ID, electionSigningKeys));
			assertEquals(exceptionMessage, keyManagementException.getMessage());
		}
	}

	@Nested
	class CcmjElectionKeysTests {

		@Test
		void testLoadCcmjElectionKeys() throws KeyManagementException {
			final byte[] ccmjElectionSecretKey = { 1 };
			final byte[] ccmjElectionPublicKey = { 2 };
			final byte[] ccmjElectionPublicKeySignature = { 1, 2, 3 };

			when(codec.decodeElGamalPrivateKey(ccmjElectionSecretKey, encryptionPrivateKey)).thenReturn(ELGAMAL_PRIVATE_KEY);
			when(codec.decodeElGamalPublicKey(ccmjElectionPublicKey)).thenReturn(ELGAMAL_PUBLIC_KEY);

			when(ccmjElectionKeysEntityRepository.findById(new CcmjElectionKeysEntityPrimaryKey(NODE_ID, ELECTION_EVENT_ID))).thenReturn(Optional.of(
					new CcmjElectionKeysEntity(NODE_ID, ELECTION_EVENT_ID, ccmjElectionSecretKey, ccmjElectionPublicKey,
							ccmjElectionPublicKeySignature)));

			final CcmjElectionKeys ccmjElectionKeys = keysRepository.loadCcmjElectionKeys(ELECTION_EVENT_ID);

			assertEquals(ELGAMAL_PRIVATE_KEY, ccmjElectionKeys.getCcmjElectionSecretKey());
			assertEquals(ELGAMAL_PUBLIC_KEY, ccmjElectionKeys.getCcmjElectionPublicKey());
			assertArrayEquals(ccmjElectionPublicKeySignature, ccmjElectionKeys.getCcmjElectionPublicKeySignature());
		}

		@Test
		void testLoadCcmjElectionKeysCodecException() throws KeyManagementException {
			final byte[] ccmjElectionSecretKey = new byte[0];
			final byte[] ccmjElectionPublicKey = new byte[0];
			final byte[] ccmjElectionPublicKeySignature = new byte[0];

			final String exceptionMessage = "exceptionMessage";
			when(codec.decodeElGamalPrivateKey(any(byte[].class), eq(encryptionPrivateKey))).thenThrow(new KeyManagementException(exceptionMessage));

			when(ccmjElectionKeysEntityRepository.findById(new CcmjElectionKeysEntityPrimaryKey(NODE_ID, ELECTION_EVENT_ID))).thenReturn(Optional.of(
					new CcmjElectionKeysEntity(NODE_ID, ELECTION_EVENT_ID, ccmjElectionSecretKey, ccmjElectionPublicKey,
							ccmjElectionPublicKeySignature)));

			final KeyManagementException keyManagementException = assertThrows(KeyManagementException.class,
					() -> keysRepository.loadCcmjElectionKeys(ELECTION_EVENT_ID));
			assertEquals(exceptionMessage, keyManagementException.getMessage());
		}

		@Test
		void testLoadCcmjElectionKeysNotFound() {
			when(ccmjElectionKeysEntityRepository.findById(new CcmjElectionKeysEntityPrimaryKey(NODE_ID, ELECTION_EVENT_ID)))
					.thenReturn(Optional.empty());

			final KeyNotFoundException keyNotFoundException = assertThrows(KeyNotFoundException.class,
					() -> keysRepository.loadCcmjElectionKeys(ELECTION_EVENT_ID));
			assertEquals(String.format("CCM_j Election keys not found for node id %s and election event id %s.", NODE_ID, ELECTION_EVENT_ID),
					keyNotFoundException.getMessage());
		}

		@Test
		void testSaveCcmjElectionKeys() throws KeyManagementException {
			final CcmjElectionKeys ccmjElectionKeys = new CcmjElectionKeys(ELGAMAL_PRIVATE_KEY, ELGAMAL_PUBLIC_KEY, new byte[] { 1, 2, 3 });
			final byte[] privateKeyBytes = { 1 };
			final byte[] publicKeyBytes = { 2 };

			when(codec.encodeElGamalPrivateKey(ELGAMAL_PRIVATE_KEY, encryptionPublicKey)).thenReturn(privateKeyBytes, privateKeyBytes);
			when(codec.encodeElGamalPublicKey(ELGAMAL_PUBLIC_KEY)).thenReturn(publicKeyBytes, publicKeyBytes);

			when(ccmjElectionKeysEntityRepository.existsById(new CcmjElectionKeysEntityPrimaryKey(NODE_ID, ELECTION_EVENT_ID))).thenReturn(false);

			assertDoesNotThrow(() -> keysRepository.saveCcmjElectionKeys(ELECTION_EVENT_ID, ccmjElectionKeys));
		}

		@Test
		void testSaveCcmjElectionKeysAlreadyExist() {
			final CcmjElectionKeys ccmjElectionKeys = new CcmjElectionKeys(ELGAMAL_PRIVATE_KEY, ELGAMAL_PUBLIC_KEY, new byte[] { 1, 2, 3 });

			when(ccmjElectionKeysEntityRepository.existsById(new CcmjElectionKeysEntityPrimaryKey(NODE_ID, ELECTION_EVENT_ID))).thenReturn(true);

			final KeyAlreadyExistsException keyAlreadyExistsException = assertThrows(KeyAlreadyExistsException.class,
					() -> keysRepository.saveCcmjElectionKeys(ELECTION_EVENT_ID, ccmjElectionKeys));

			assertEquals(String.format("CCM_j election keys already exist for node id %s and election event id %s.", NODE_ID, ELECTION_EVENT_ID),
					keyAlreadyExistsException.getMessage());
		}

		@Test
		void testSaveCcmjElectionKeysCodecException() throws KeyManagementException {
			final CcmjElectionKeys ccmjElectionKeys = new CcmjElectionKeys(ELGAMAL_PRIVATE_KEY, ELGAMAL_PUBLIC_KEY, new byte[] { 1, 2, 3 });

			final String exceptionMessage = "exceptionMessage";
			when(codec.encodeElGamalPrivateKey(ELGAMAL_PRIVATE_KEY, encryptionPublicKey)).thenThrow(new KeyManagementException(exceptionMessage));

			when(ccmjElectionKeysEntityRepository.existsById(new CcmjElectionKeysEntityPrimaryKey(NODE_ID, ELECTION_EVENT_ID))).thenReturn(false);

			final KeyManagementException keyManagementException = assertThrows(KeyManagementException.class,
					() -> keysRepository.saveCcmjElectionKeys(ELECTION_EVENT_ID, ccmjElectionKeys));
			assertEquals(exceptionMessage, keyManagementException.getMessage());
		}
	}

	@Nested
	class CcrjReturnCodesKeysTests {
		@Test
		void testLoadCcrjReturnCodesKeys() throws KeyManagementException {
			final byte[] ccrjReturnCodesGenerationSecretKey = { 1 };
			final byte[] ccrjReturnCodesGenerationPublicKey = { 2 };
			final byte[] ccrjReturnCodesGenerationPublicKeySignature = { 1, 2, 3 };

			final byte[] ccrjChoiceReturnCodesEncryptionSecretKey = { 3 };
			final byte[] ccrjChoiceReturnCodesEncryptionPublicKey = { 4 };
			final byte[] ccrjChoiceReturnCodesEncryptionPublicKeySignature = { 4, 5, 6 };

			when(codec.decodeElGamalPrivateKey(ccrjReturnCodesGenerationSecretKey, encryptionPrivateKey)).thenReturn(ELGAMAL_PRIVATE_KEY);
			when(codec.decodeElGamalPublicKey(ccrjReturnCodesGenerationPublicKey)).thenReturn(ELGAMAL_PUBLIC_KEY);
			when(codec.decodeElGamalPrivateKey(ccrjChoiceReturnCodesEncryptionSecretKey, encryptionPrivateKey)).thenReturn(ELGAMAL_PRIVATE_KEY);
			when(codec.decodeElGamalPublicKey(ccrjChoiceReturnCodesEncryptionPublicKey)).thenReturn(ELGAMAL_PUBLIC_KEY);

			when(ccrjReturnCodesKeysEntityRepository
					.findById(new CcrjReturnCodesKeysEntityPrimaryKey(NODE_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID))).thenReturn(Optional.of(
					new CcrjReturnCodesKeysEntity(NODE_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, ccrjReturnCodesGenerationSecretKey,
							ccrjReturnCodesGenerationPublicKey, ccrjReturnCodesGenerationPublicKeySignature, ccrjChoiceReturnCodesEncryptionSecretKey,
							ccrjChoiceReturnCodesEncryptionPublicKey, ccrjChoiceReturnCodesEncryptionPublicKeySignature)));

			final CcrjReturnCodesKeys ccrjReturnCodesKeys = keysRepository.loadCcrjReturnCodesKeys(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID);

			assertEquals(ELGAMAL_PRIVATE_KEY, ccrjReturnCodesKeys.getCcrjReturnCodesGenerationSecretKey());
			assertEquals(ELGAMAL_PUBLIC_KEY, ccrjReturnCodesKeys.getCcrjReturnCodesGenerationPublicKey());
			assertArrayEquals(ccrjReturnCodesGenerationPublicKeySignature, ccrjReturnCodesKeys.getCcrjReturnCodesGenerationPublicKeySignature());

			assertEquals(ELGAMAL_PRIVATE_KEY, ccrjReturnCodesKeys.getCcrjChoiceReturnCodesEncryptionSecretKey());
			assertEquals(ELGAMAL_PUBLIC_KEY, ccrjReturnCodesKeys.getCcrjChoiceReturnCodesEncryptionPublicKey());
			assertArrayEquals(ccrjChoiceReturnCodesEncryptionPublicKeySignature,
					ccrjReturnCodesKeys.getCcrjChoiceReturnCodesEncryptionPublicKeySignature());
		}

		@Test
		void testLoadCcrjReturnCodesKeysCodecException() throws KeyManagementException {
			final String exceptionMessage = "exceptionMessage";
			when(codec.decodeElGamalPrivateKey(any(byte[].class), eq(encryptionPrivateKey))).thenThrow(new KeyManagementException(exceptionMessage));

			when(ccrjReturnCodesKeysEntityRepository
					.findById(new CcrjReturnCodesKeysEntityPrimaryKey(NODE_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID))).thenReturn(Optional.of(
					new CcrjReturnCodesKeysEntity(NODE_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, new byte[0], new byte[0], new byte[0],
							new byte[0], new byte[0], new byte[0])));

			final KeyManagementException keyManagementException = assertThrows(KeyManagementException.class,
					() -> keysRepository.loadCcrjReturnCodesKeys(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID));
			assertEquals(exceptionMessage, keyManagementException.getMessage());
		}

		@Test
		void testLoadCcrjReturnCodesKeysNotFound() {
			when(ccrjReturnCodesKeysEntityRepository
					.findById(new CcrjReturnCodesKeysEntityPrimaryKey(NODE_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID)))
					.thenReturn(Optional.empty());

			final KeyNotFoundException keyNotFoundException = assertThrows(KeyNotFoundException.class,
					() -> keysRepository.loadCcrjReturnCodesKeys(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID));

			assertEquals(
					String.format("CCR_j Return Codes keys not found for node id %s, election event id %s and verification card set id %s.", NODE_ID,
							ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID), keyNotFoundException.getMessage());
		}

		@Test
		void testSaveCcrjReturnCodesKeys() throws KeyManagementException {
			final CcrjReturnCodesKeys ccrjReturnCodesKeys = new CcrjReturnCodesKeys.Builder()
					.setCcrjReturnCodesGenerationKeys(ELGAMAL_PRIVATE_KEY, ELGAMAL_PUBLIC_KEY, new byte[] { 1, 2, 3 })
					.setCcrjChoiceReturnCodesEncryptionKeys(ELGAMAL_PRIVATE_KEY, ELGAMAL_PUBLIC_KEY, new byte[] { 4, 5, 6 }).build();
			final byte[] ccrjReturnCodesGenerationSecretKey = { 1 };
			final byte[] ccrjReturnCodesGenerationPublicKey = { 2 };
			final byte[] ccrjChoiceReturnCodesEncryptionSecretKey = { 3 };
			final byte[] ccrjChoiceReturnCodesEncryptionPublicKey = { 4 };

			when(codec.encodeElGamalPrivateKey(ELGAMAL_PRIVATE_KEY, encryptionPublicKey))
					.thenReturn(ccrjReturnCodesGenerationSecretKey, ccrjChoiceReturnCodesEncryptionSecretKey);
			when(codec.encodeElGamalPublicKey(ELGAMAL_PUBLIC_KEY))
					.thenReturn(ccrjReturnCodesGenerationPublicKey, ccrjChoiceReturnCodesEncryptionPublicKey);

			when(ccrjReturnCodesKeysEntityRepository
					.existsById(new CcrjReturnCodesKeysEntityPrimaryKey(NODE_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID))).thenReturn(false);

			assertDoesNotThrow(() -> keysRepository.saveCcrjReturnCodesKeys(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, ccrjReturnCodesKeys));
		}

		@Test
		void testSaveCcrjReturnCodesKeysAlreadyExist() {
			final CcrjReturnCodesKeys ccrjReturnCodesKeys = new CcrjReturnCodesKeys.Builder()
					.setCcrjReturnCodesGenerationKeys(ELGAMAL_PRIVATE_KEY, ELGAMAL_PUBLIC_KEY, new byte[] { 1, 2, 3 })
					.setCcrjChoiceReturnCodesEncryptionKeys(ELGAMAL_PRIVATE_KEY, ELGAMAL_PUBLIC_KEY, new byte[] { 4, 5, 6 }).build();

			when(ccrjReturnCodesKeysEntityRepository
					.existsById(new CcrjReturnCodesKeysEntityPrimaryKey(NODE_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID))).thenReturn(true);

			final KeyAlreadyExistsException keyAlreadyExistsException = assertThrows(KeyAlreadyExistsException.class,
					() -> keysRepository.saveCcrjReturnCodesKeys(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, ccrjReturnCodesKeys));
			assertEquals(String.format("CCR_j Return Codes Keys already exist for node id %s, election event id %s and verification card set id %s.",
					NODE_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID), keyAlreadyExistsException.getMessage());
		}

		@Test
		void testSaveCcrjReturnCodesKeysCodecException() throws KeyManagementException {
			final CcrjReturnCodesKeys ccrjReturnCodesKeys = new CcrjReturnCodesKeys.Builder()
					.setCcrjReturnCodesGenerationKeys(ELGAMAL_PRIVATE_KEY, ELGAMAL_PUBLIC_KEY, new byte[] { 1, 2, 3 })
					.setCcrjChoiceReturnCodesEncryptionKeys(ELGAMAL_PRIVATE_KEY, ELGAMAL_PUBLIC_KEY, new byte[] { 4, 5, 6 }).build();

			final String exceptionMessage = "exceptionMessage";
			when(codec.encodeElGamalPrivateKey(ELGAMAL_PRIVATE_KEY, encryptionPublicKey)).thenThrow(new KeyManagementException(exceptionMessage));

			final KeyManagementException keyManagementException = assertThrows(KeyManagementException.class,
					() -> keysRepository.saveCcrjReturnCodesKeys(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, ccrjReturnCodesKeys));
			assertEquals(exceptionMessage, keyManagementException.getMessage());
		}

	}

}
