/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement.persistence;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

@Entity
@Table(name = "CC_ELECTION_SIGNING_KEYS")
@IdClass(ElectionSigningKeysEntityPrimaryKey.class)
public class ElectionSigningKeysEntity {

	@Id
	private String nodeId;

	@Id
	private String electionEventId;

	private byte[] keys;

	private byte[] password;

	public ElectionSigningKeysEntity() {
		// Needed by the repository.
	}

	public ElectionSigningKeysEntity(final String nodeId, final String electionEventId, final byte[] keys, final byte[] password) {
		this.nodeId = nodeId;
		this.electionEventId = electionEventId;
		this.keys = keys;
		this.password = password;
	}

	public byte[] getPassword() {
		return this.password;
	}

	public byte[] getKeys() {
		return this.keys;
	}
}
