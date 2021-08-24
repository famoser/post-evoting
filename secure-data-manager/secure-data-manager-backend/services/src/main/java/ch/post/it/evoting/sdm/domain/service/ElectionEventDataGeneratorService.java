/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service;

import java.io.IOException;

import ch.post.it.evoting.sdm.domain.model.generator.DataGeneratorResponse;

/**
 * This interface defines the API for a service which accesses a generator of election event data.
 */
public interface ElectionEventDataGeneratorService {

	/**
	 * This method generates all the data for an election event.
	 *
	 * @param electionEventId The identifier of the election event for whom to generate the data.
	 * @return a bean containing information about the result of the generation.
	 */
	DataGeneratorResponse generate(String electionEventId) throws IOException;
}
