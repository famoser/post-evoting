/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement.persistence;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

@Entity
@Table(name = "CCM_ELECTION_KEY")
@IdClass(CcmjElectionKeysEntityPrimaryKey.class)
public class CcmjElectionKeysEntity {

	@Id
	private String nodeId;

	@Id
	private String electionEventId;

	private byte[] ccmjElectionSecretKey;

	private byte[] ccmjElectionPublicKey;

	private byte[] ccmjElectionPublicKeySignature;

	public CcmjElectionKeysEntity() {
		// Needed by the repository.
	}

	public CcmjElectionKeysEntity(final String nodeId, final String electionEventId, final byte[] ccmjElectionSecretKey,
			final byte[] ccmjElectionPublicKey, final byte[] ccmjElectionPublicKeySignature) {
		this.nodeId = nodeId;
		this.electionEventId = electionEventId;
		this.ccmjElectionSecretKey = ccmjElectionSecretKey;
		this.ccmjElectionPublicKey = ccmjElectionPublicKey;
		this.ccmjElectionPublicKeySignature = ccmjElectionPublicKeySignature;
	}

	public byte[] getCcmjElectionSecretKey() {
		return this.ccmjElectionSecretKey;
	}

	public byte[] getCcmjElectionPublicKey() {
		return this.ccmjElectionPublicKey;
	}

	public byte[] getCcmjElectionPublicKeySignature() {
		return this.ccmjElectionPublicKeySignature;
	}

}
