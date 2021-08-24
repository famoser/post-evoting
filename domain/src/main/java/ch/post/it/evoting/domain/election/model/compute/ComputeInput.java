/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.model.compute;

import java.util.Map;

public class ComputeInput {

	private String encryptedRepresentations;

	private Map<String, String> verificationCardIdsToBallotCastingKeys;

	public String getEncryptedRepresentations() {
		return encryptedRepresentations;
	}

	public void setEncryptedRepresentations(String encryptedRepresentations) {
		this.encryptedRepresentations = encryptedRepresentations;
	}

	public Map<String, String> getVerificationCardIdsToBallotCastingKeys() {
		return verificationCardIdsToBallotCastingKeys;
	}

	public void setVerificationCardIdsToBallotCastingKeys(Map<String, String> verificationCardIdsToBallotCastingKeys) {
		this.verificationCardIdsToBallotCastingKeys = verificationCardIdsToBallotCastingKeys;
	}
}
