/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content;

import javax.json.JsonObject;

import ch.post.it.evoting.votingserver.commons.util.JsonUtils;

public class ElectionInformationContentDynamic {
	private static final String ELECTION_INFORMATION_PARAMS = "electionInformationParams";
	private final JsonObject value;

	public ElectionInformationContentDynamic(String value) {
		this.value = JsonUtils.getJsonObject(value);
	}

	public int numVotesPerVotingCard() {
		final String attr = "numVotesPerVotingCard";
		JsonObject intermediate = value.getJsonObject(ELECTION_INFORMATION_PARAMS);
		return Integer.parseInt(intermediate.getString(attr));
	}

	public int numVotesPerAuthToken() {
		final String attr = "numVotesPerAuthToken";
		JsonObject intermediate = value.getJsonObject(ELECTION_INFORMATION_PARAMS);
		return Integer.parseInt(intermediate.getString(attr));
	}
}
