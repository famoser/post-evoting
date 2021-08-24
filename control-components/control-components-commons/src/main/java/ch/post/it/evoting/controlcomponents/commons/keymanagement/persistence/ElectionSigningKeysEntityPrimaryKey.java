/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement.persistence;

import static ch.post.it.evoting.domain.Validations.validateUUID;

import java.io.Serializable;
import java.util.Objects;

public class ElectionSigningKeysEntityPrimaryKey implements Serializable {
	private String nodeId;
	private String electionEventId;

	public ElectionSigningKeysEntityPrimaryKey() {
		// Needed by the repository.
	}

	public ElectionSigningKeysEntityPrimaryKey(final String nodeId, final String electionEventId) {
		validateUUID(electionEventId);

		this.nodeId = nodeId;
		this.electionEventId = electionEventId;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final ElectionSigningKeysEntityPrimaryKey that = (ElectionSigningKeysEntityPrimaryKey) o;
		return this.nodeId.equals(that.nodeId) && this.electionEventId.equals(that.electionEventId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.nodeId, this.electionEventId);
	}
}
