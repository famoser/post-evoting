/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.commons.domain;

import java.util.List;

public class CreateVotingCardSetInput {

	private String start;
	private String end;
	private Integer validityPeriod;
	private int numberVotingCards;
	private String ballotID;
	private String ballotBoxID;
	private String votingCardSetID;
	private String verificationCardSetID;
	private String votingCardSetAlias;
	private String electoralAuthorityID;
	private String basePath;
	private int numCredentialsPerFile;
	private int numProcessors;
	private String eeID;
	private String ballotPath;
	private String keyForProtectingKeystorePassword;
	private List<String> choiceCodesEncryptionKey;
	private String platformRootCACertificate;
	private CreateVotingCardSetCertificatePropertiesContainer createVotingCardSetCertificateProperties;

	public String getStart() {
		return start;
	}

	public void setStart(final String start) {
		this.start = start;
	}

	public String getEnd() {
		return end;
	}

	public void setEnd(final String end) {
		this.end = end;
	}

	public Integer getValidityPeriod() {
		return validityPeriod;
	}

	public void setValidityPeriod(final Integer validityPeriod) {
		this.validityPeriod = validityPeriod;
	}

	public String getBallotPath() {
		return ballotPath;
	}

	public void setBallotPath(final String ballotPath) {
		this.ballotPath = ballotPath;
	}

	public int getNumberVotingCards() {
		return numberVotingCards;
	}

	public void setNumberVotingCards(final int numberVotingCards) {
		this.numberVotingCards = numberVotingCards;
	}

	public String getBallotID() {
		return ballotID;
	}

	public void setBallotID(final String ballotID) {
		this.ballotID = ballotID;
	}

	public String getBallotBoxID() {
		return ballotBoxID;
	}

	public void setBallotBoxID(final String ballotBoxID) {
		this.ballotBoxID = ballotBoxID;
	}

	public String getVotingCardSetID() {
		return votingCardSetID;
	}

	public void setVotingCardSetID(final String votingCardSetID) {
		this.votingCardSetID = votingCardSetID;
	}

	public String getVerificationCardSetID() {
		return verificationCardSetID;
	}

	public void setVerificationCardSetID(String verificationCardSetID) {
		this.verificationCardSetID = verificationCardSetID;
	}

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public int getNumCredentialsPerFile() {
		return numCredentialsPerFile;
	}

	public void setNumCredentialsPerFile(final int numCredentialsPerFile) {
		this.numCredentialsPerFile = numCredentialsPerFile;
	}

	public int getNumProcessors() {
		return numProcessors;
	}

	public void setNumProcessors(final int numProcessors) {
		this.numProcessors = numProcessors;
	}

	public String getEeID() {
		return eeID;
	}

	public void setEeID(final String eeID) {
		this.eeID = eeID;
	}

	public String getElectoralAuthorityID() {
		return electoralAuthorityID;
	}

	public void setElectoralAuthorityID(final String electoralAuthorityID) {
		this.electoralAuthorityID = electoralAuthorityID;
	}

	public String getKeyForProtectingKeystorePassword() {
		return keyForProtectingKeystorePassword;
	}

	public void setKeyForProtectingKeystorePassword(final String keyForProtectingKeystorePassword) {
		this.keyForProtectingKeystorePassword = keyForProtectingKeystorePassword;
	}

	public String getVotingCardSetAlias() {
		return votingCardSetAlias;
	}

	public void setVotingCardSetAlias(String votingCardSetAlias) {
		this.votingCardSetAlias = votingCardSetAlias;
	}

	public List<String> getChoiceCodesEncryptionKey() {
		return choiceCodesEncryptionKey;
	}

	public void setChoiceCodesEncryptionKey(List<String> choiceCodesEncryptionKey) {
		this.choiceCodesEncryptionKey = choiceCodesEncryptionKey;
	}

	public String getPlatformRootCACertificate() {
		return platformRootCACertificate;
	}

	public void setPlatformRootCACertificate(String platformRootCACertificate) {
		this.platformRootCACertificate = platformRootCACertificate;
	}

	public CreateVotingCardSetCertificatePropertiesContainer getCreateVotingCardSetCertificateProperties() {
		return createVotingCardSetCertificateProperties;
	}

	public void setCreateVotingCardSetCertificateProperties(
			CreateVotingCardSetCertificatePropertiesContainer createVotingCardSetCertificateProperties) {
		this.createVotingCardSetCertificateProperties = createVotingCardSetCertificateProperties;
	}
}
