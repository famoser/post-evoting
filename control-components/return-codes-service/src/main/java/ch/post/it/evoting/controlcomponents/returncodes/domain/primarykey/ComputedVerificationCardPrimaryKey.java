/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain.primarykey;

import static ch.post.it.evoting.domain.Validations.validateUUID;

import java.io.Serializable;
import java.util.Objects;

public class ComputedVerificationCardPrimaryKey implements Serializable {
	private String electionEventId;
	private String verificationCardId;

	public ComputedVerificationCardPrimaryKey() {
		// Needed by the repository.
	}

	public ComputedVerificationCardPrimaryKey(final String electionEventId, final String verificationCardId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardId);

		this.electionEventId = electionEventId;
		this.verificationCardId = verificationCardId;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final ComputedVerificationCardPrimaryKey that = (ComputedVerificationCardPrimaryKey) o;
		return this.electionEventId.equals(that.electionEventId) && this.verificationCardId.equals(that.verificationCardId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.electionEventId, this.verificationCardId);
	}
}
