/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.electoralauthority;

import javax.json.JsonObject;

/**
 * Interface defining a set of methods for upload electoral authority information.
 */
public interface ElectoralAuthorityUploadRepository {

	/**
	 * Uploads the authentication context data.
	 *
	 * @param electionEventId - the election event identifier.
	 * @param adminBoardId
	 * @param json            - the json object containing the authentication context data. @return True if the
	 *                        information is successfully uploaded. Otherwise, false.
	 */
	boolean uploadAuthenticationContextData(String electionEventId, final String adminBoardId, JsonObject json);

	/**
	 * Uploads the election information context data.
	 *
	 * @param electionEventId - the election event identifier.
	 * @param adminBoardId
	 * @param json            - the json object containing the election information context data. @return True if
	 *                        the information is successfully uploaded. Otherwise, false.
	 */
	boolean uploadElectionInformationContextData(String electionEventId, final String adminBoardId, JsonObject json);

	/**
	 * Uploads the electoral data in verification context.
	 *
	 * @param electionEventId      - the election event identifier.
	 * @param electoralAuthorityId - the electoral authority identifier.
	 * @param adminBoardId
	 * @param json                 - the json object containing the electoral authority. @return True if the
	 *                             information is successfully uploaded. Otherwise, false.
	 */
	boolean uploadElectoralDataInVerificationContext(String electionEventId, String electoralAuthorityId, final String adminBoardId, JsonObject json);

	/**
	 * Uploads the electoral data in verification context.
	 *
	 * @param electionEventId      - the election event identifier.
	 * @param electoralAuthorityId - the electoral authority identifier.
	 * @param adminBoardId
	 * @param json                 - the json object containing the electoral authority. @return True if the
	 *                             information is successfully uploaded. Otherwise, false.
	 */
	boolean uploadElectoralDataInElectionInformationContext(String electionEventId, String electoralAuthorityId, final String adminBoardId,
			JsonObject json);

	/**
	 * Checks if there is information for the election Event in the EI context
	 *
	 * @param electionEvent - identifier of the electionevent
	 * @return
	 */
	boolean checkEmptyElectionEventDataInEI(String electionEvent);

	/**
	 * Checks if there is information for the election Event in the EI context
	 *
	 * @param electionEvent - identifier of the electionevent
	 * @return
	 */
	boolean checkEmptyElectionEventDataInAU(String electionEvent);

}
