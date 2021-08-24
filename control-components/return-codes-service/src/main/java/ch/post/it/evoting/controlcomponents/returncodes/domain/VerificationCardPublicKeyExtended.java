/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain;

import static ch.post.it.evoting.domain.Validations.validateUUID;
import static org.msgpack.core.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;

public class VerificationCardPublicKeyExtended {

	private final String electionEventId;
	private final String verificationCardId;
	private final String verificationCardSetId;
	private final ElGamalMultiRecipientPublicKey verificationCardPublicKey;

	public VerificationCardPublicKeyExtended(final String electionEventId, final String verificationCardId, final String verificationCardSetId,
			final ElGamalMultiRecipientPublicKey verificationCardPublicKey) {
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

	public ElGamalMultiRecipientPublicKey getVerificationCardPublicKey() {
		return this.verificationCardPublicKey;
	}

}
