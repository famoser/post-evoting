/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.commons.domain;

import java.util.Properties;

public class CreateBallotBoxesInput {

	private String ballotID;

	private String electoralAuthorityID;

	private String ballotBoxID;

	private String alias;

	private String outputFolder;

	private String start;

	private String end;

	private Integer validityPeriod;

	private String keyForProtectingKeystorePassword;

	private String test;

	private String gracePeriod;

	private String writeInAlphabet;

	private Properties ballotBoxCertificateProperties;

	public String getBallotID() {
		return ballotID;
	}

	public void setBallotID(final String ballotID) {
		this.ballotID = ballotID;
	}

	public String getElectoralAuthorityID() {
		return electoralAuthorityID;
	}

	public void setElectoralAuthorityID(final String electoralAuthorityID) {
		this.electoralAuthorityID = electoralAuthorityID;
	}

	public String getOutputFolder() {
		return outputFolder;
	}

	public void setOutputFolder(final String outputFolder) {
		this.outputFolder = outputFolder;
	}

	public String getBallotBoxID() {
		return ballotBoxID;
	}

	public void setBallotBoxID(final String ballotBoxID) {
		this.ballotBoxID = ballotBoxID;
	}

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

	public String getKeyForProtectingKeystorePassword() {
		return keyForProtectingKeystorePassword;
	}

	public void setKeyForProtectingKeystorePassword(final String keyForProtectingKeystorePassword) {
		this.keyForProtectingKeystorePassword = keyForProtectingKeystorePassword;
	}

	public String getTest() {
		return test;
	}

	public void setTest(String test) {
		this.test = test;
	}

	public String getGracePeriod() {
		return gracePeriod;
	}

	public void setGracePeriod(String gracePeriod) {
		this.gracePeriod = gracePeriod;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getWriteInAlphabet() {
		return writeInAlphabet;
	}

	public void setWriteInAlphabet(String writeInAlphabet) {
		this.writeInAlphabet = writeInAlphabet;
	}

	public Properties getBallotBoxCertificateProperties() {
		return ballotBoxCertificateProperties;
	}

	public void setBallotBoxCertificateProperties(Properties ballotBoxCertificateProperties) {
		this.ballotBoxCertificateProperties = ballotBoxCertificateProperties;
	}
}
