/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import static java.text.MessageFormat.format;
import static java.util.Arrays.fill;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponents.commons.keymanagement.exception.InvalidPasswordException;
import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.stores.StoresServiceAPI;
import ch.post.it.evoting.cryptolib.api.stores.bean.KeyStoreType;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;

@Service
public class Codec {
	private static final String NODE_CA_ALIAS = "CA";

	private static final String NODE_ENCRYPTION_ALIAS = "encryption";

	private static final String NODE_LOG_SIGNING_ALIAS = "logSigning";

	private static final String NODE_LOG_ENCRYPTION_ALIAS = "logEncryption";

	private static final String ELECTION_SIGNING_ALIAS = "signing";

	private final StoresServiceAPI storesService;

	private final AsymmetricServiceAPI asymmetricService;

	public Codec(final StoresServiceAPI storesService, final AsymmetricServiceAPI asymmetricService) {
		this.storesService = storesService;
		this.asymmetricService = asymmetricService;
	}

	private static PrivateKeyEntry decodePrivateKeyEntry(KeyStore store, String alias, PasswordProtection protection) throws KeyManagementException {
		try {
			if (!store.entryInstanceOf(alias, PrivateKeyEntry.class)) {
				throw new KeyManagementException(format("Key entry ''{0}'' is missing or invalid.", alias));
			}
			return (PrivateKeyEntry) store.getEntry(alias, protection);
		} catch (UnrecoverableKeyException e) {
			throw new InvalidPasswordException("Invalid key password.", e);
		} catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException e) {
			throw new KeyManagementException("Failed to decode private key entry.", e);
		}
	}

	public ElectionSigningKeys decodeElectionSigningKeys(byte[] bytes, PasswordProtection protection) throws KeyManagementException {
		KeyStore store = decodeKeyStore(bytes, protection);
		PrivateKeyEntry entry = decodePrivateKeyEntry(store, ELECTION_SIGNING_ALIAS, protection);
		return new ElectionSigningKeys(entry.getPrivateKey(), (X509Certificate[]) entry.getCertificateChain());
	}

	public ElGamalPrivateKey decodeElGamalPrivateKey(byte[] bytes, PrivateKey encryptionKey) throws KeyManagementException {

		try {
			byte[] decryptedBytes = asymmetricService.decrypt(encryptionKey, bytes);
			String json = new String(decryptedBytes, StandardCharsets.UTF_8);
			return ElGamalPrivateKey.fromJson(json);
		} catch (GeneralCryptoLibException e) {
			throw new KeyManagementException("Failed to decode ElGamal private key.", e);
		}
	}

	public ElGamalPublicKey decodeElGamalPublicKey(byte[] bytes) throws KeyManagementException {
		String json = new String(bytes, StandardCharsets.UTF_8);
		try {
			return ElGamalPublicKey.fromJson(json);
		} catch (GeneralCryptoLibException e) {
			throw new KeyManagementException("Failed to decode ElGamal public key.", e);
		}
	}

	public NodeKeys decodeNodeKeys(final byte[] bytes, final PasswordProtection password) throws KeyManagementException {
		final KeyStore store = decodeKeyStore(bytes, password);
		return new NodeKeys.Builder().setCAKeys(decodePrivateKeyEntry(store, NODE_CA_ALIAS, password))
				.setEncryptionKeys(decodePrivateKeyEntry(store, NODE_ENCRYPTION_ALIAS, password))
				.setLogSigningKeys(decodePrivateKeyEntry(store, NODE_LOG_SIGNING_ALIAS, password))
				.setLogEncryptionKeys(decodePrivateKeyEntry(store, NODE_LOG_ENCRYPTION_ALIAS, password)).build();
	}

	public PasswordProtection decodePassword(byte[] bytes, PrivateKey encryptionKey) throws KeyManagementException {

		byte[] decryptedBytes;
		try {
			decryptedBytes = asymmetricService.decrypt(encryptionKey, bytes);
		} catch (GeneralCryptoLibException e) {
			throw new KeyManagementException("Failed to decode password.", e);
		}
		ByteBuffer byteBuffer = ByteBuffer.wrap(decryptedBytes);
		try {
			CharBuffer charBuffer = StandardCharsets.UTF_8.decode(byteBuffer);
			try {
				return new PasswordProtection(charBuffer.array());
			} finally {
				fill(charBuffer.array(), '\u0000');
			}
		} finally {
			fill(byteBuffer.array(), (byte) 0);
		}
	}

	public byte[] encodeElectionSigningKeys(final ElectionSigningKeys electionSigningKeys, final PasswordProtection protection)
			throws KeyManagementException {
		try {
			final KeyStore store = storesService.createKeyStore(KeyStoreType.PKCS12);
			store.setKeyEntry(ELECTION_SIGNING_ALIAS, electionSigningKeys.privateKey(), protection.getPassword(),
					electionSigningKeys.certificateChain());
			try (final ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
				store.store(stream, protection.getPassword());
				return stream.toByteArray();
			}
		} catch (IOException | GeneralCryptoLibException | GeneralSecurityException e) {
			throw new KeyManagementException("Failed to encode election signing keys.", e);
		}
	}

	public byte[] encodeElGamalPrivateKey(ElGamalPrivateKey key, PublicKey encryptionKey) throws KeyManagementException {
		try {
			byte[] bytes = key.toJson().getBytes(StandardCharsets.UTF_8);
			return asymmetricService.encrypt(encryptionKey, bytes);
		} catch (GeneralCryptoLibException e) {
			throw new KeyManagementException("Failed to encode ElGamal private key.", e);
		}
	}

	public byte[] encodeElGamalPublicKey(ElGamalPublicKey key) throws KeyManagementException {
		try {
			return key.toJson().getBytes(StandardCharsets.UTF_8);
		} catch (GeneralCryptoLibException e) {
			throw new KeyManagementException("Failed to encode ElGamal public key.", e);
		}
	}

	public byte[] encodeNodeKeys(final NodeKeys nodeKeys, final PasswordProtection protection) throws KeyManagementException {
		try {
			final KeyStore store = storesService.createKeyStore(KeyStoreType.PKCS12);
			store.setKeyEntry(NODE_CA_ALIAS, nodeKeys.caPrivateKey(), protection.getPassword(), nodeKeys.caCertificateChain());
			store.setKeyEntry(NODE_ENCRYPTION_ALIAS, nodeKeys.encryptionPrivateKey(), protection.getPassword(),
					nodeKeys.encryptionCertificateChain());
			store.setKeyEntry(NODE_LOG_SIGNING_ALIAS, nodeKeys.logSigningPrivateKey(), protection.getPassword(),
					nodeKeys.logSigningCertificateChain());
			store.setKeyEntry(NODE_LOG_ENCRYPTION_ALIAS, nodeKeys.logEncryptionPrivateKey(), protection.getPassword(),
					nodeKeys.logEncryptionCertificateChain());
			try (final ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
				store.store(stream, protection.getPassword());
				return stream.toByteArray();
			}
		} catch (IOException | GeneralCryptoLibException | GeneralSecurityException e) {
			throw new KeyManagementException("Failed to encode election signing keys.", e);
		}
	}

	public byte[] encodePassword(PasswordProtection protection, PublicKey encryptionKey) throws KeyManagementException {
		CharBuffer charBuffer = CharBuffer.wrap(protection.getPassword());
		ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
		byte[] bytes = new byte[byteBuffer.limit()];
		byteBuffer.get(bytes);
		try {
			return asymmetricService.encrypt(encryptionKey, bytes);
		} catch (GeneralCryptoLibException e) {
			throw new KeyManagementException("Failed to encode password.", e);
		} finally {
			fill(bytes, (byte) 0);
			fill(byteBuffer.array(), (byte) 0);
			fill(charBuffer.array(), '\u0000');
		}
	}

	private KeyStore decodeKeyStore(byte[] bytes, PasswordProtection protection) throws KeyManagementException {
		try (InputStream stream = new ByteArrayInputStream(bytes)) {
			return storesService.loadKeyStore(KeyStoreType.PKCS12, stream, protection.getPassword());
		} catch (GeneralCryptoLibException e) {
			if (e.getCause() instanceof IOException && e.getCause().getCause() instanceof UnrecoverableKeyException) {
				throw new InvalidPasswordException("Key store password is invalid.", e);
			} else {
				throw new KeyManagementException("Failed to decode the key store.", e);
			}
		} catch (IOException e) {
			throw new KeyManagementException("Failed to decode the key store.", e);
		}
	}
}
