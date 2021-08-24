/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service;

import java.io.IOException;

import ch.post.it.evoting.sdm.domain.model.generator.DataGeneratorResponse;

/**
 * This interface defines the API for a service which accesses a generator of voting card set data.
 */
public interface VotingCardSetDataGeneratorService {

	/**
	 * This method generates all the data for a voting card set.
	 *
	 * @param id              The identifier of the voting card set for which to generate the data.
	 * @param electionEventId The identifier of the election event to whom this voting card set
	 *                        belongs.
	 * @return a bean containing information about the result of the generation.
	 */
	DataGeneratorResponse generate(String id, String electionEventId) throws IOException;

}
