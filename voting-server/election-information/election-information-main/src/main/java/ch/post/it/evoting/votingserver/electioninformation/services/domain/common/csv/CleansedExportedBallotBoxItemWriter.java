/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.common.csv;

import java.io.OutputStream;

import ch.post.it.evoting.votingserver.commons.infrastructure.csv.AbstractCSVWriter;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.CleansedExportedBallotBoxItem;

/**
 * Writer in csv for every item to be downloaded of a ballot box
 */
public class CleansedExportedBallotBoxItemWriter extends AbstractCSVWriter<CleansedExportedBallotBoxItem> {

	public CleansedExportedBallotBoxItemWriter(final OutputStream outputStream) {
		super(outputStream);
	}

	/**
	 * Extract the values of a ballot box item into an array of Strings
	 *
	 * @param cleansedExportedBallotBoxItem - ballot box item from which the info is extracted
	 * @return
	 */
	@Override
	public String[] extractValues(final CleansedExportedBallotBoxItem cleansedExportedBallotBoxItem) {

		return new String[] { cleansedExportedBallotBoxItem.getVote() };

	}

}
