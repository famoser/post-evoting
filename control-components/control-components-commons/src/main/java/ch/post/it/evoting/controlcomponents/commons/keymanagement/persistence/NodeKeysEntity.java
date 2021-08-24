/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement.persistence;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "CC_NODE_KEYS")
public class NodeKeysEntity {

	@Id
	private String nodeId;

	private byte[] keys;

	public NodeKeysEntity() {
		// Needed by the repository.
	}

	public NodeKeysEntity(final String nodeId, final byte[] keys) {
		this.nodeId = nodeId;
		this.keys = keys;
	}

	public byte[] getKeys() {
		return this.keys;
	}
}
