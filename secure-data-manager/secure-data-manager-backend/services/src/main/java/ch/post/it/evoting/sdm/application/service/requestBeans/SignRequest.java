/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service.requestBeans;

import java.util.List;

public class SignRequest {
	private String electionEventId;

	private String votingCardSetId;

	private String privateKeyPEM;

	private List<String> certificatesChain;

	public String getElectionEventId() {
		return electionEventId;
	}

	public void setElectionEventId(String electionEventId) {
		this.electionEventId = electionEventId;
	}

	public String getVotingCardSetId() {
		return votingCardSetId;
	}

	public void setVotingCardSetId(String votingCardSetId) {
		this.votingCardSetId = votingCardSetId;
	}

	public String getPrivateKeyPEM() {
		return privateKeyPEM;
	}

	public void setPrivateKeyPEM(String privateKeyPEM) {
		this.privateKeyPEM = privateKeyPEM;
	}

	public List<String> getCertificatesChain() {
		return certificatesChain;
	}

	public void setCertificatesChain(List<String> certificatesChain) {
		this.certificatesChain = certificatesChain;
	}
}
