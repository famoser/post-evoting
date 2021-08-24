/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election;

public class VerificationCardSetData {

	private String verificationCardSetId;

	private String choicesCodesEncryptionPublicKey;

	private String verificationCardSetIssuerCert;

	private String electionEventId;

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	public void setVerificationCardSetId(final String verificationCardSetId) {
		this.verificationCardSetId = verificationCardSetId;
	}

	public String getChoicesCodesEncryptionPublicKey() {
		return choicesCodesEncryptionPublicKey;
	}

	public void setChoicesCodesEncryptionPublicKey(final String choicesCodesEncryptionPublicKey) {
		this.choicesCodesEncryptionPublicKey = choicesCodesEncryptionPublicKey;
	}

	public String getVerificationCardSetIssuerCert() {
		return verificationCardSetIssuerCert;
	}

	public void setVerificationCardSetIssuerCert(final String verificationCardSetIssuerCert) {
		this.verificationCardSetIssuerCert = verificationCardSetIssuerCert;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public void setElectionEventId(final String electionEventId) {
		this.electionEventId = electionEventId;
	}

}
