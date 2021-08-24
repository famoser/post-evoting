/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.common.csv;

import java.io.OutputStream;

import ch.post.it.evoting.votingserver.commons.infrastructure.csv.AbstractCSVWriter;
import ch.post.it.evoting.votingserver.commons.infrastructure.csv.CSVConstants;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.vote.FailedVote;

public class FailedVotesWriter extends AbstractCSVWriter<FailedVote> {

	public FailedVotesWriter(final OutputStream outputStream) {
		super(outputStream, CSVConstants.DEFAULT_CHARSET, CSVConstants.SEMICOLON_SEPARATOR, CSVConstants.NO_QUOTE_CHARACTER,
				CSVConstants.NO_ESCAPE_CHARACTER);
	}

	@Override
	protected String[] extractValues(FailedVote object) {
		return new String[] { object.getVotingCardId(), object.getTimestamp().toString(), object.getValidationError() };
	}
}
