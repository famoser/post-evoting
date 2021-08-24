/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election;

/**
 * An object to represent election information. It will be serialized to a json file in order to be used by other modules.
 */
public class ElectionInformationContents {

	private String electionEventId;

	private String electionRootCA;

	private String servicesCA;

	private String credentialsCA;

	private String adminBoard;

	private String authoritiesCA;

	private ElectionInformationParams electionInformationParams;

	public String getElectionEventId() {
		return electionEventId;
	}

	public void setElectionEventId(final String electionEventId) {
		this.electionEventId = electionEventId;
	}

	public String getElectionRootCA() {
		return electionRootCA;
	}

	public void setElectionRootCA(final String electionRootCA) {
		this.electionRootCA = electionRootCA;
	}

	public String getServicesCA() {
		return servicesCA;
	}

	public void setServicesCA(final String servicesCA) {
		this.servicesCA = servicesCA;
	}

	public String getCredentialsCA() {
		return credentialsCA;
	}

	public void setCredentialsCA(final String credentialsCA) {
		this.credentialsCA = credentialsCA;
	}

	public ElectionInformationParams getElectionInformationParams() {
		return electionInformationParams;
	}

	public void setElectionInformationParams(final ElectionInformationParams electionInformationParams) {
		this.electionInformationParams = electionInformationParams;
	}

	public String getAdminBoard() {
		return adminBoard;
	}

	public void setAdminBoard(final String adminBoard) {
		this.adminBoard = adminBoard;
	}

	public String getAuthoritiesCA() {
		return authoritiesCA;
	}

	public void setAuthoritiesCA(final String authoritiesCA) {
		this.authoritiesCA = authoritiesCA;
	}

}
