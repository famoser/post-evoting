/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Encapsulates the information contained within a ballot.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ballot {

	private final String id;

	private final ElectionEvent electionEvent;

	private final List<Contest> contests;

	@JsonCreator
	public Ballot(
			@JsonProperty("id")
			final String id,
			@JsonProperty("electionEvent")
			final ElectionEvent electionEvent,
			@JsonProperty("contests")
			final List<Contest> contests) {

		this.id = id;
		this.electionEvent = electionEvent;
		this.contests = contests;
	}

	public String getId() {
		return id;
	}

	public ElectionEvent getElectionEvent() {
		return electionEvent;
	}

	public List<Contest> getContests() {
		return contests;
	}

	@JsonIgnore
	public List<BigInteger> getEncodedVotingOptions() {
		return this.getContests().stream().map(Contest::getOptions).flatMap(Collection::stream).map(ElectionOption::getRepresentation)
				.map(representation -> new BigInteger(representation, 10)).collect(Collectors.toList());
	}
}
