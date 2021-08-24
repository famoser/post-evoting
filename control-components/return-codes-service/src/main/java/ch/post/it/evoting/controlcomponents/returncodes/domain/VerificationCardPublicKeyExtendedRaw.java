/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain;

import static ch.post.it.evoting.domain.Validations.validateUUID;
import static org.msgpack.core.Preconditions.checkNotNull;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "CC_VERIFICATION_CARD_PUBLIC_KEY")
class VerificationCardPublicKeyExtendedRaw {

	private String electionEventId;
	@Id
	private String verificationCardId;
	private String verificationCardSetId;
	private byte[] verificationCardPublicKey;

	public VerificationCardPublicKeyExtendedRaw() {
		// Needed by the repository.
	}

	public VerificationCardPublicKeyExtendedRaw(final String electionEventId, final String verificationCardId, final String verificationCardSetId,
			final byte[] verificationCardPublicKey) {
		validateUUID(electionEventId);
		validateUUID(verificationCardId);
		validateUUID(verificationCardSetId);
		checkNotNull(verificationCardPublicKey);

		this.electionEventId = electionEventId;
		this.verificationCardId = verificationCardId;
		this.verificationCardSetId = verificationCardSetId;
		this.verificationCardPublicKey = verificationCardPublicKey;
	}

	public String getElectionEventId() {
		return this.electionEventId;
	}

	public String getVerificationCardId() {
		return this.verificationCardId;
	}

	public String getVerificationCardSetId() {
		return this.verificationCardSetId;
	}

	public byte[] getVerificationCardPublicKey() {
		return this.verificationCardPublicKey;
	}
}
