/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement.persistence;

import static ch.post.it.evoting.domain.Validations.validateUUID;

import java.io.Serializable;
import java.util.Objects;

public class CcrjReturnCodesKeysEntityPrimaryKey implements Serializable {
	private String nodeId;
	private String electionEventId;
	private String verificationCardSetId;

	public CcrjReturnCodesKeysEntityPrimaryKey() {
		// Needed by the repository.
	}

	public CcrjReturnCodesKeysEntityPrimaryKey(final String nodeId, final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		this.nodeId = nodeId;
		this.electionEventId = electionEventId;
		this.verificationCardSetId = verificationCardSetId;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final CcrjReturnCodesKeysEntityPrimaryKey that = (CcrjReturnCodesKeysEntityPrimaryKey) o;
		return this.nodeId.equals(that.nodeId) && this.electionEventId.equals(that.electionEventId) && this.verificationCardSetId
				.equals(that.verificationCardSetId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.nodeId, this.electionEventId, this.verificationCardSetId);
	}
}
