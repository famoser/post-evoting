/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.votingcardset;

import java.util.List;

import javax.json.JsonObject;

import ch.post.it.evoting.sdm.application.exception.DatabaseException;
import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.domain.model.EntityRepository;

/**
 * Interface providing operations with voting card set.
 */
public interface VotingCardSetRepository extends EntityRepository {

	/**
	 * Returns the ballot box id from the voting card set identified by the given id.
	 *
	 * @param votingCardSetId identifies the voting card set where to search.
	 * @return the ballot box identifier.
	 */
	String getBallotBoxId(String votingCardSetId);

	/**
	 * Update the related ballot for the given voting cards Ids
	 *
	 * @param votingCardIds for which to update the related ballot.
	 */
	void updateRelatedBallot(List<String> votingCardIds);

	/**
	 * Update the related verification card set id for the given voting cards id.
	 *
	 * @param votingCardIds         for which to update the verification card set id.
	 * @param verificationCardSetId for the corresponding verification card set to be updated.
	 */
	void updateRelatedVerificationCardSet(String votingCardIds, String verificationCardSetId);

	/**
	 * Lists authorities matching which belong to the specified election event.
	 *
	 * @param electionEventId the election event identifier
	 * @return the authorities in JSON format
	 * @throws DatabaseException failed to list the authorities
	 */
	String listByElectionEvent(String electionEventId);

	/**
	 * Returns the verification card set id related to the given voting card set id
	 *
	 * @param votingCardSetId identifies the voting card set where to search.
	 * @return the verification card set id
	 */
	String getVerificationCardSetId(String votingCardSetId);

	/**
	 * Returns the specified voting card set in JSON form
	 *
	 * @param votingCardSetId identifies the voting card set where to search.
	 * @return the verification card set id
	 */
	JsonObject getVotingCardSetJson(final String electionEventId, final String votingCardSetId) throws ResourceNotFoundException;

	/**
	 * Returns the voting card set alias related to the given voting card set id.
	 *
	 * @param votingCardSetId identifies the voting card set where to search.
	 * @return the voting card set alias
	 */
	String getVotingCardSetAlias(final String votingCardSetId);
}
