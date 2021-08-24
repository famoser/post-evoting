/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.operation;

public class OperationsData {

	private String path;
	private String privateKeyInBase64;
	private char[] password;
	private boolean electionEventData;
	private boolean votingCardsData;
	private boolean customerData;
	private boolean computedChoiceCodes;
	private boolean preComputedChoiceCodes;
	private boolean ballotBoxes;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getPrivateKeyInBase64() {
		return privateKeyInBase64;
	}

	public void setPrivateKeyInBase64(String privateKeyInBase64) {
		this.privateKeyInBase64 = privateKeyInBase64;
	}

	public char[] getPassword() {
		return password;
	}

	public void setPassword(char[] password) {
		this.password = password;
	}

	public boolean isElectionEventData() {
		return electionEventData;
	}

	public void setElectionEventData(boolean electionEventData) {
		this.electionEventData = electionEventData;
	}

	public boolean isVotingCardsData() {
		return votingCardsData;
	}

	public void setVotingCardsData(boolean votingCardsData) {
		this.votingCardsData = votingCardsData;
	}

	public boolean isCustomerData() {
		return customerData;
	}

	public void setCustomerData(boolean customerData) {
		this.customerData = customerData;
	}

	public boolean isComputedChoiceCodes() {
		return computedChoiceCodes;
	}

	public void setComputedChoiceCodes(boolean computedChoiceCodes) {
		this.computedChoiceCodes = computedChoiceCodes;
	}

	public boolean isPreComputedChoiceCodes() {
		return preComputedChoiceCodes;
	}

	public void setPreComputedChoiceCodes(boolean preComputedChoiceCodes) {
		this.preComputedChoiceCodes = preComputedChoiceCodes;
	}

	public boolean isBallotBoxes() {
		return ballotBoxes;
	}

	public void setBallotBoxes(boolean ballotBoxes) {
		this.ballotBoxes = ballotBoxes;
	}
}
