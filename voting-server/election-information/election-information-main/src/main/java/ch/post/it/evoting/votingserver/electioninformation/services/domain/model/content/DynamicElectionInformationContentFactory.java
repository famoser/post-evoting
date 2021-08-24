/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content;

public class DynamicElectionInformationContentFactory {
	public ElectionInformationContentDynamic aNew(String json) {
		return new ElectionInformationContentDynamic(json);
	}
}
