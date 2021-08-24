/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore.PasswordProtection;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.security.auth.DestroyFailedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

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
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;

@Repository
public class KeysRepository {
	private static final Logger LOGGER = LoggerFactory.getLogger(KeysRepository.class);

	private final Codec codec;
	private final Generator generator;
	private final String nodeId;
	private final NodeKeysEntityRepository nodeKeysEntityRepository;
	private final ElectionSigningKeysEntityRepository electionSigningKeysEntityRepository;
	private final CcmjElectionKeysEntityRepository ccmjElectionKeysEntityRepository;
	private final CcrjReturnCodesKeysEntityRepository ccrjReturnCodesKeysEntityRepository;

	private AtomicReference<KeyPair> encryptionKeys;

	public KeysRepository(final Codec codec, final Generator generator, final NodeKeysEntityRepository nodeKeysEntityRepository,
			final ElectionSigningKeysEntityRepository electionSigningKeysEntityRepository,
			final CcmjElectionKeysEntityRepository ccmjElectionKeysEntityRepository,
			final CcrjReturnCodesKeysEntityRepository ccrjReturnCodesKeysEntityRepository,
			@Value("${key.node.id}")
			final String nodeId) {

		this.codec = codec;
		this.generator = generator;
		this.nodeId = nodeId;
		this.nodeKeysEntityRepository = nodeKeysEntityRepository;
		this.electionSigningKeysEntityRepository = electionSigningKeysEntityRepository;
		this.ccmjElectionKeysEntityRepository = ccmjElectionKeysEntityRepository;
		this.ccrjReturnCodesKeysEntityRepository = ccrjReturnCodesKeysEntityRepository;
	}

	public CcrjReturnCodesKeys loadCcrjReturnCodesKeys(final String electionEventId, final String verificationCardSetId)
			throws KeyManagementException {

		final Optional<CcrjReturnCodesKeysEntity> optionalCcrjReturnCodesKeysEntity = ccrjReturnCodesKeysEntityRepository
				.findById(new CcrjReturnCodesKeysEntityPrimaryKey(nodeId, electionEventId, verificationCardSetId));

		if (!optionalCcrjReturnCodesKeysEntity.isPresent()) {
			throw new KeyNotFoundException(
					String.format("CCR_j Return Codes keys not found for node id %s, election event id %s and verification card set id %s.", nodeId,
							electionEventId, verificationCardSetId));
		}

		final CcrjReturnCodesKeysEntity ccrjReturnCodesKeysEntity = optionalCcrjReturnCodesKeysEntity.get();

		final ElGamalPrivateKey ccrjReturnCodesGenerationSecretKey = codec
				.decodeElGamalPrivateKey(ccrjReturnCodesKeysEntity.getCcrjReturnCodesGenerationSecretKey(), encryptionKeys.get().getPrivate());
		final ElGamalPublicKey ccrjReturnCodesGenerationPublicKey = codec
				.decodeElGamalPublicKey(ccrjReturnCodesKeysEntity.getCcrjReturnCodesGenerationPublicKey());
		final byte[] ccrjReturnCodesGenerationPublicKeySignature = ccrjReturnCodesKeysEntity.getCcrjReturnCodesGenerationPublicKeySignature();

		final ElGamalPrivateKey ccrjChoiceReturnCodesEncryptionSecretKey = codec
				.decodeElGamalPrivateKey(ccrjReturnCodesKeysEntity.getCcrjChoiceReturnCodesEncryptionSecretKey(), encryptionKeys.get().getPrivate());
		final ElGamalPublicKey ccrjChoiceReturnCodesEncryptionPublicKey = codec
				.decodeElGamalPublicKey(ccrjReturnCodesKeysEntity.getCcrjChoiceReturnCodesEncryptionPublicKey());
		final byte[] ccrjChoiceReturnCodesEncryptionPublicKeySignature = ccrjReturnCodesKeysEntity
				.getCcrjChoiceReturnCodesEncryptionPublicKeySignature();

		return new CcrjReturnCodesKeys.Builder()
				.setCcrjReturnCodesGenerationKeys(ccrjReturnCodesGenerationSecretKey, ccrjReturnCodesGenerationPublicKey,
						ccrjReturnCodesGenerationPublicKeySignature)
				.setCcrjChoiceReturnCodesEncryptionKeys(ccrjChoiceReturnCodesEncryptionSecretKey, ccrjChoiceReturnCodesEncryptionPublicKey,
						ccrjChoiceReturnCodesEncryptionPublicKeySignature).build();
	}

	public ElectionSigningKeys loadElectionSigningKeys(final String electionEventId) throws KeyManagementException {

		final Optional<ElectionSigningKeysEntity> optionalElectionSigningKeysEntity = electionSigningKeysEntityRepository
				.findById(new ElectionSigningKeysEntityPrimaryKey(nodeId, electionEventId));

		if (!optionalElectionSigningKeysEntity.isPresent()) {
			throw new KeyNotFoundException(
					String.format("Election signing keys not found for node id %s and election event id %s.", nodeId, electionEventId));
		}

		final ElectionSigningKeysEntity electionSigningKeysEntity = optionalElectionSigningKeysEntity.get();
		final PasswordProtection password = codec.decodePassword(electionSigningKeysEntity.getPassword(), encryptionKeys.get().getPrivate());

		try {
			return codec.decodeElectionSigningKeys(electionSigningKeysEntity.getKeys(), password);
		} finally {
			try {
				password.destroy();
			} catch (DestroyFailedException e) {
				LOGGER.warn(String.format("Failed to destroy the password for node id %s and election event id %s.", nodeId, electionEventId), e);
			}
		}
	}

	public CcmjElectionKeys loadCcmjElectionKeys(final String electionEventId) throws KeyManagementException {

		final Optional<CcmjElectionKeysEntity> optionalCcmjElectionKeysEntity = ccmjElectionKeysEntityRepository
				.findById(new CcmjElectionKeysEntityPrimaryKey(nodeId, electionEventId));

		if (!optionalCcmjElectionKeysEntity.isPresent()) {
			throw new KeyNotFoundException(
					String.format("CCM_j Election keys not found for node id %s and election event id %s.", nodeId, electionEventId));
		}

		final CcmjElectionKeysEntity ccmjElectionKeysEntity = optionalCcmjElectionKeysEntity.get();

		final ElGamalPrivateKey ccmjElectionSecretKey = codec
				.decodeElGamalPrivateKey(ccmjElectionKeysEntity.getCcmjElectionSecretKey(), encryptionKeys.get().getPrivate());
		final ElGamalPublicKey ccmjElectionPublicKey = codec.decodeElGamalPublicKey(ccmjElectionKeysEntity.getCcmjElectionPublicKey());
		final byte[] ccmjElectionPublicKeySignature = ccmjElectionKeysEntity.getCcmjElectionPublicKeySignature();

		return new CcmjElectionKeys(ccmjElectionSecretKey, ccmjElectionPublicKey, ccmjElectionPublicKeySignature);
	}

	public NodeKeys loadNodeKeys(final PasswordProtection passwordProtection) throws KeyManagementException {
		final Optional<NodeKeysEntity> optionalNodeKeysEntity = nodeKeysEntityRepository.findById(nodeId);

		if (!optionalNodeKeysEntity.isPresent()) {
			throw new KeyNotFoundException(String.format("Node keys not found for node id %s.", nodeId));
		}

		final byte[] keys = optionalNodeKeysEntity.get().getKeys();

		return codec.decodeNodeKeys(keys, passwordProtection);
	}

	public void saveCcrjReturnCodesKeys(final String electionEventId, final String verificationCardSetId,
			final CcrjReturnCodesKeys ccrjReturnCodesKeys) throws KeyManagementException {

		if (ccrjReturnCodesKeysEntityRepository.existsById(new CcrjReturnCodesKeysEntityPrimaryKey(nodeId, electionEventId, verificationCardSetId))) {
			throw new KeyAlreadyExistsException(
					String.format("CCR_j Return Codes Keys already exist for node id %s, election event id %s and verification card set id %s.",
							nodeId, electionEventId, verificationCardSetId));
		}

		final byte[] ccrjReturnCodesGenerationSecretKey = codec
				.encodeElGamalPrivateKey(ccrjReturnCodesKeys.getCcrjReturnCodesGenerationSecretKey(), encryptionKeys.get().getPublic());
		final byte[] ccrjReturnCodesGenerationPublicKey = codec.encodeElGamalPublicKey(ccrjReturnCodesKeys.getCcrjReturnCodesGenerationPublicKey());
		final byte[] ccrjReturnCodesGenerationPublicKeySignature = ccrjReturnCodesKeys.getCcrjReturnCodesGenerationPublicKeySignature();

		final byte[] ccrjChoiceReturnCodesEncryptionSecretKey = codec
				.encodeElGamalPrivateKey(ccrjReturnCodesKeys.getCcrjChoiceReturnCodesEncryptionSecretKey(), encryptionKeys.get().getPublic());
		final byte[] ccrjChoiceReturnCodesEncryptionPublicKey = codec
				.encodeElGamalPublicKey(ccrjReturnCodesKeys.getCcrjChoiceReturnCodesEncryptionPublicKey());
		final byte[] ccrjChoiceReturnCodesEncryptionPublicKeySignature = ccrjReturnCodesKeys.getCcrjChoiceReturnCodesEncryptionPublicKeySignature();

		ccrjReturnCodesKeysEntityRepository
				.save(new CcrjReturnCodesKeysEntity(nodeId, electionEventId, verificationCardSetId, ccrjReturnCodesGenerationSecretKey,
						ccrjReturnCodesGenerationPublicKey, ccrjReturnCodesGenerationPublicKeySignature, ccrjChoiceReturnCodesEncryptionSecretKey,
						ccrjChoiceReturnCodesEncryptionPublicKey, ccrjChoiceReturnCodesEncryptionPublicKeySignature));
	}

	public void saveElectionSigningKeys(final String electionEventId, final ElectionSigningKeys electionSigningKeys) throws KeyManagementException {

		if (electionSigningKeysEntityRepository.existsById(new ElectionSigningKeysEntityPrimaryKey(nodeId, electionEventId))) {
			throw new KeyAlreadyExistsException(
					String.format("Election signing keys already exist for node id %s and election event id %s.", nodeId, electionEventId));
		}

		final PasswordProtection passwordProtection = generator.generatePassword();
		try {

			final byte[] keys = codec.encodeElectionSigningKeys(electionSigningKeys, passwordProtection);
			final byte[] password = codec.encodePassword(passwordProtection, encryptionKeys.get().getPublic());

			electionSigningKeysEntityRepository.save(new ElectionSigningKeysEntity(nodeId, electionEventId, keys, password));

		} finally {
			try {
				passwordProtection.destroy();
			} catch (DestroyFailedException e) {
				LOGGER.warn(String.format("Failed to destroy the password for node id %s and election event id %s.", nodeId, electionEventId), e);
			}
		}
	}

	public void saveCcmjElectionKeys(final String electionEventId, final CcmjElectionKeys ccmjElectionKeys) throws KeyManagementException {
		if (hasCcmjElectionKeys(electionEventId)) {
			throw new KeyAlreadyExistsException(
					String.format("CCM_j election keys already exist for node id %s and election event id %s.", nodeId, electionEventId));
		}

		final byte[] ccmjElectionSecretKey = codec.encodeElGamalPrivateKey(ccmjElectionKeys.getCcmjElectionSecretKey(), encryptionKeys.get().getPublic());
		final byte[] ccmjElectionPublicKey = codec.encodeElGamalPublicKey(ccmjElectionKeys.getCcmjElectionPublicKey());

		ccmjElectionKeysEntityRepository.save(new CcmjElectionKeysEntity(nodeId, electionEventId, ccmjElectionSecretKey, ccmjElectionPublicKey,
				ccmjElectionKeys.getCcmjElectionPublicKeySignature()));
	}

	public void saveNodeKeys(final NodeKeys nodeKeys, final PasswordProtection passwordProtection) throws KeyManagementException {
		if (nodeKeysEntityRepository.existsById(nodeId)) {
			throw new KeyAlreadyExistsException(String.format("Node keys already exist for node id %s.", nodeId));
		}

		final byte[] keys = codec.encodeNodeKeys(nodeKeys, passwordProtection);
		nodeKeysEntityRepository.save(new NodeKeysEntity(nodeId, keys));
	}

	public void setEncryptionKeys(final PrivateKey privateKey, final PublicKey publicKey) {
		this.encryptionKeys = new AtomicReference<>(new KeyPair(publicKey, privateKey));
	}

	protected boolean hasCcrjReturnCodesKeys(final String electionEventId, final String verificationCardSetId) {
		return ccrjReturnCodesKeysEntityRepository
				.existsById(new CcrjReturnCodesKeysEntityPrimaryKey(nodeId, electionEventId, verificationCardSetId));
	}

	protected boolean hasCcmjElectionKeys(final String electionEventId) {
		return ccmjElectionKeysEntityRepository.existsById(new CcmjElectionKeysEntityPrimaryKey(nodeId, electionEventId));
	}

}
