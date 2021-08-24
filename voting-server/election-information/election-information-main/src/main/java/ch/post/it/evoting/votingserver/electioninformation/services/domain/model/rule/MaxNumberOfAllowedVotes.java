/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.rule;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.domain.model.rule.AbstractRule;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBox;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.DynamicElectionInformationContentFactory;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionInformationContent;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionInformationContentDynamic;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionInformationContentRepository;

/**
 * Validates number of allowed votes for voting card id and authentication token.
 */
public class MaxNumberOfAllowedVotes implements AbstractRule<Vote> {

	private static final Logger LOGGER = LoggerFactory.getLogger(MaxNumberOfAllowedVotes.class);
	@Inject
	private ElectionInformationContentRepository electionInformationContentRepository;
	@Inject
	private BallotBoxRepository ballotBoxRepository;
	@Inject
	private DynamicElectionInformationContentFactory dynamicElectionInformationContentFactory;

	/**
	 * @see AbstractRule#execute(Object)
	 */
	@Override
	public ValidationError execute(Vote vote) {
		// validation result. By default is set to false
		ValidationError result = new ValidationError();
		result.setValidationErrorType(ValidationErrorType.FAILED);

		// recover election information content data
		ElectionInformationContent electionInformationContent;
		try {
			electionInformationContent = electionInformationContentRepository
					.findByTenantIdElectionEventId(vote.getTenantId(), vote.getElectionEventId());
		} catch (ResourceNotFoundException e) {
			LOGGER.error("Error recovering data from election information content", e);
			return result;
		}

		ElectionInformationContentDynamic dynamicElectionInformationContent = dynamicElectionInformationContentFactory
				.aNew(electionInformationContent.getJson());
		int numVotesPerVotingCard = dynamicElectionInformationContent.numVotesPerVotingCard();
		int numVotesPerAuthToken = dynamicElectionInformationContent.numVotesPerAuthToken();

		// recover all votes for voting card
		List<BallotBox> storedVotesInBallotBox = ballotBoxRepository
				.findByTenantIdElectionEventIdVotingCardIdBallotBoxIdBallotId(vote.getTenantId(), vote.getElectionEventId(), vote.getVotingCardId(),
						vote.getBallotBoxId(), vote.getBallotId());

		// perform validations
		if (validateMaxNumVotesPerVotingCardId(storedVotesInBallotBox, numVotesPerVotingCard) && validateMaxNumVotesPerAuthToken(vote,
				storedVotesInBallotBox, numVotesPerAuthToken)) {
			result.setValidationErrorType(ValidationErrorType.SUCCESS);
		}

		return result;
	}

	private boolean validateMaxNumVotesPerVotingCardId(List<BallotBox> storedVotesInBallotBox, int maxNumVotesPerVotingCard) {
		// count all votes per voting card from bb
		return storedVotesInBallotBox.size() < maxNumVotesPerVotingCard;
	}

	private boolean validateMaxNumVotesPerAuthToken(Vote vote, List<BallotBox> storedVotesInBallotBox, int maxNumVotesPerAuthToken) {
		// count the occurrences of an auth token for each vote
		int numVotesForGivenAuthToken = 0;
		for (BallotBox ballotBox : storedVotesInBallotBox) {
			try {
				// check if the auth token is in any of the stored votes
				Vote storedVote = ObjectMappers.fromJson(ballotBox.getVote(), Vote.class);
				// compare both auth tokens
				boolean result = compareAuthToken(vote.getAuthenticationToken(), storedVote.getAuthenticationToken().replaceAll("\\\\", ""));
				if (result) {
					numVotesForGivenAuthToken = numVotesForGivenAuthToken + 1;
				}
			} catch (IOException e) {
				LOGGER.error("Vote in json format can not be mapped to an object", e);
				return false;
			}
		}

		return numVotesForGivenAuthToken < maxNumVotesPerAuthToken;
	}

	@SuppressWarnings("unchecked")
	private boolean compareAuthToken(String authToken1, String authToken2) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> a1 = mapper.readValue(authToken1, Map.class);
			Map<String, Object> a2 = mapper.readValue(authToken2, Map.class);
			return a1.equals(a2);
		} catch (IOException e) {
			LOGGER.error("Auth token in json format can not be mapped to be compared", e);
			return false;
		}
	}

	/**
	 * @see AbstractRule#getName()
	 */
	@Override
	public String getName() {
		return RuleNames.VOTE_MAX_NUMBER_OF_ALLOWED_VOTES.getText();
	}

}
