/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service;

import ch.post.it.evoting.sdm.domain.model.generator.DataGeneratorResponse;

/**
 * This interface defines the API for a service which accesses a generator of ballot data.
 */
public interface BallotDataGeneratorService {

	/**
	 * This method generates all the data for a ballot.
	 *
	 * @param id              The identifier of the ballot set for which to generate the data.
	 * @param electionEventId The identifier of the election event to whom this ballot set belongs.
	 * @return a bean containing information about the result of the generation.
	 */
	DataGeneratorResponse generate(String id, String electionEventId);

	/**
	 * Removes all the ballot.json files inside
	 * sdm/config/{electionEventId}/ONLINE/electionInformation/ballots/ This method is used to later
	 * re-generate those files, calling the generate method for each ballot
	 *
	 * @param electionEventId The election event of the ballots to be removed
	 */
	void cleanAll(String electionEventId);
}
