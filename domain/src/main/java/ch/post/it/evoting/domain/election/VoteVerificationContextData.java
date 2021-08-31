/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election;

public class VoteVerificationContextData {

	private String electoralAuthorityId;

	private String electionEventId;

	private String verificationCardSetId;

	private EncryptionParameters encryptionParameters;

	private String nonCombinedChoiceCodesEncryptionPublicKeys;

	public String getElectoralAuthorityId() {
		return electoralAuthorityId;
	}

	public void setElectoralAuthorityId(String electoralAuthorityId) {
		this.electoralAuthorityId = electoralAuthorityId;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public void setElectionEventId(final String electionEventId) {
		this.electionEventId = electionEventId;
	}

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	public void setVerificationCardSetId(final String verificationCardSetId) {
		this.verificationCardSetId = verificationCardSetId;
	}

	public EncryptionParameters getEncryptionParameters() {
		return encryptionParameters;
	}

	public void setEncryptionParameters(final EncryptionParameters encryptionParameters) {
		this.encryptionParameters = encryptionParameters;
	}

	public String getNonCombinedChoiceCodesEncryptionPublicKeys() {
		return nonCombinedChoiceCodesEncryptionPublicKeys;
	}

	public void setNonCombinedChoiceCodesEncryptionPublicKeys(String nonCombinedChoiceCodesEncryptionPublicKeys) {
		this.nonCombinedChoiceCodesEncryptionPublicKeys = nonCombinedChoiceCodesEncryptionPublicKeys;
	}
}
