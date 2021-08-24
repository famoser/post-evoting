/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement.persistence;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

@Entity
@Table(name = "CCR_RETURN_CODES_KEYS")
@IdClass(CcrjReturnCodesKeysEntityPrimaryKey.class)
public class CcrjReturnCodesKeysEntity {

	@Id
	private String nodeId;

	@Id
	private String electionEventId;

	@Id
	private String verificationCardSetId;

	private byte[] ccrjReturnCodesGenerationSecretKey;

	private byte[] ccrjReturnCodesGenerationPublicKey;

	private byte[] ccrjReturnCodesGenerationPublicKeySignature;

	private byte[] ccrjChoiceReturnCodesEncryptionSecretKey;

	private byte[] ccrjChoiceReturnCodesEncryptionPublicKey;

	private byte[] ccrjChoiceReturnCodesEncryptionPublicKeySignature;

	public CcrjReturnCodesKeysEntity() {
		// Needed by the repository.
	}

	public CcrjReturnCodesKeysEntity(final String nodeId, final String electionEventId, final String verificationCardSetId,
			final byte[] ccrjReturnCodesGenerationSecretKey, final byte[] ccrjReturnCodesGenerationPublicKey,
			final byte[] ccrjReturnCodesGenerationPublicKeySignature, final byte[] ccrjChoiceReturnCodesEncryptionSecretKey,
			final byte[] ccrjChoiceReturnCodesEncryptionPublicKey, final byte[] ccrjChoiceReturnCodesEncryptionPublicKeySignature) {
		this.nodeId = nodeId;
		this.electionEventId = electionEventId;
		this.verificationCardSetId = verificationCardSetId;
		this.ccrjReturnCodesGenerationSecretKey = ccrjReturnCodesGenerationSecretKey;
		this.ccrjReturnCodesGenerationPublicKey = ccrjReturnCodesGenerationPublicKey;
		this.ccrjReturnCodesGenerationPublicKeySignature = ccrjReturnCodesGenerationPublicKeySignature;
		this.ccrjChoiceReturnCodesEncryptionSecretKey = ccrjChoiceReturnCodesEncryptionSecretKey;
		this.ccrjChoiceReturnCodesEncryptionPublicKey = ccrjChoiceReturnCodesEncryptionPublicKey;
		this.ccrjChoiceReturnCodesEncryptionPublicKeySignature = ccrjChoiceReturnCodesEncryptionPublicKeySignature;
	}

	public byte[] getCcrjReturnCodesGenerationSecretKey() {
		return this.ccrjReturnCodesGenerationSecretKey;
	}

	public byte[] getCcrjReturnCodesGenerationPublicKey() {
		return this.ccrjReturnCodesGenerationPublicKey;
	}

	public byte[] getCcrjReturnCodesGenerationPublicKeySignature() {
		return this.ccrjReturnCodesGenerationPublicKeySignature;
	}

	public byte[] getCcrjChoiceReturnCodesEncryptionSecretKey() {
		return this.ccrjChoiceReturnCodesEncryptionSecretKey;
	}

	public byte[] getCcrjChoiceReturnCodesEncryptionPublicKey() {
		return this.ccrjChoiceReturnCodesEncryptionPublicKey;
	}

	public byte[] getCcrjChoiceReturnCodesEncryptionPublicKeySignature() {
		return this.ccrjChoiceReturnCodesEncryptionPublicKeySignature;
	}
}
