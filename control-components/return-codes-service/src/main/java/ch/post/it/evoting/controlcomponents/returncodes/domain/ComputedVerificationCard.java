/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain;

import static ch.post.it.evoting.domain.Validations.validateUUID;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import ch.post.it.evoting.controlcomponents.returncodes.domain.converter.BooleanConverter;
import ch.post.it.evoting.controlcomponents.returncodes.domain.primarykey.ComputedVerificationCardPrimaryKey;

@Entity
@Table(name = "COMPUTED_VERIFICATION_CARDS")
@IdClass(ComputedVerificationCardPrimaryKey.class)
public class ComputedVerificationCard {

	@Id
	private String electionEventId;

	@Id
	private String verificationCardId;

	private Integer confirmationAttempts = 0;

	@Convert(converter = BooleanConverter.class)
	private boolean exponentiationComputed = false;

	public ComputedVerificationCard() {
		// Needed by the repository.
	}

	public ComputedVerificationCard(final String electionEventId, final String verificationCardId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardId);

		this.electionEventId = electionEventId;
		this.verificationCardId = verificationCardId;
	}

	public Integer getConfirmationAttempts() {
		return this.confirmationAttempts;
	}

	public void setConfirmationAttempts(final int confirmationAttempts) {
		this.confirmationAttempts = confirmationAttempts;
	}

	public boolean isExponentiationComputed() {
		return this.exponentiationComputed;
	}

	public void setExponentiationComputed(final boolean exponentiationComputed) {
		this.exponentiationComputed = exponentiationComputed;
	}

	@Override
	public String toString() {
		return String
				.format("ComputedVerificationCard{electionEventId='%s', verificationCardId='%s', confirmationAttempts=%s, isExponentiationComputed=%s}'",
						this.electionEventId, this.verificationCardId, this.confirmationAttempts, this.exponentiationComputed);
	}
}
