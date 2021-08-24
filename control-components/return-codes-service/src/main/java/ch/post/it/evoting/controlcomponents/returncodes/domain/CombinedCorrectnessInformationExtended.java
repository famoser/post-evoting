/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain;

import static ch.post.it.evoting.domain.Validations.validateUUID;
import static org.msgpack.core.Preconditions.checkNotNull;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import ch.post.it.evoting.controlcomponents.returncodes.domain.converter.CombinedCorrectnessInformationConverter;
import ch.post.it.evoting.controlcomponents.returncodes.domain.primarykey.CombinedCorrectnessInformationExtendedPrimaryKey;
import ch.post.it.evoting.domain.election.CombinedCorrectnessInformation;

@Entity
@Table(name = "CC_COMBINED_CORRECTNESS_INFORMATION")
@IdClass(CombinedCorrectnessInformationExtendedPrimaryKey.class)
public class CombinedCorrectnessInformationExtended {

	@Id
	private String electionEventId;

	@Id
	private String verificationCardSetId;

	@Convert(converter = CombinedCorrectnessInformationConverter.class)
	private CombinedCorrectnessInformation combinedCorrectnessInformation;

	public CombinedCorrectnessInformationExtended() {
		// Needed by the repository.
	}

	public CombinedCorrectnessInformationExtended(final String electionEventId, final String verificationCardSetId,
			final CombinedCorrectnessInformation combinedCorrectnessInformation) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkNotNull(combinedCorrectnessInformation);

		this.electionEventId = electionEventId;
		this.verificationCardSetId = verificationCardSetId;
		this.combinedCorrectnessInformation = combinedCorrectnessInformation;
	}

	public CombinedCorrectnessInformation getCombinedCorrectnessInformation() {
		return this.combinedCorrectnessInformation;
	}

}
