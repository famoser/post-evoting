/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain.primarykey;

import static ch.post.it.evoting.domain.Validations.validateUUID;

import java.io.Serializable;
import java.util.Objects;

public class CombinedCorrectnessInformationExtendedPrimaryKey implements Serializable {
	private String electionEventId;
	private String verificationCardSetId;

	public CombinedCorrectnessInformationExtendedPrimaryKey() {
		// Needed by the repository.
	}

	public CombinedCorrectnessInformationExtendedPrimaryKey(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

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

		final CombinedCorrectnessInformationExtendedPrimaryKey that = (CombinedCorrectnessInformationExtendedPrimaryKey) o;
		return this.electionEventId.equals(that.electionEventId) && this.verificationCardSetId.equals(that.verificationCardSetId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.electionEventId, this.verificationCardSetId);
	}
}
