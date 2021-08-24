/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox;

import java.nio.charset.StandardCharsets;

import javax.ejb.EJB;
import javax.json.JsonObject;

import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.crypto.KeystoreForObjectRepository;
import ch.post.it.evoting.votingserver.commons.logging.service.I18nLoggerMessages;
import ch.post.it.evoting.votingserver.commons.util.JsonUtils;

/**
 * Ballot box keystore repository implementation.
 */
public class BallotBoxKeystoreRepository implements KeystoreForObjectRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(BallotBoxKeystoreRepository.class);

	private static final I18nLoggerMessages I18N = I18nLoggerMessages.getInstance();

	private static final String KEYSTORE = "keystore";

	@EJB
	private BallotBoxContentRepository ballotBoxContentRepository;

	/**
	 * Find a keystore data string given this tenant identifier and the electionEventIdentifier. *
	 *
	 * @param tenantId        the tenant identifier.
	 * @param electionEventId the election event identifier.
	 * @param ballotBoxId     the ballot box identifier.
	 * @return the keystore data represented as a string.
	 * @throws ResourceNotFoundException if the keystore data can not be found.
	 * @see KeystoreForObjectRepository#getJsonByTenantEEIDObjectId(String, String, String)
	 */
	@Override
	public String getJsonByTenantEEIDObjectId(String tenantId, String electionEventId, String ballotBoxId) throws ResourceNotFoundException {

		LOGGER.info(I18N.getMessage("BallotBoxContentService.getBallotBoxContent.recoveringBallotBoxContent"), tenantId, electionEventId,
				ballotBoxId);

		// recover from db
		BallotBoxContent ballotBoxContent = ballotBoxContentRepository
				.findByTenantIdElectionEventIdBallotBoxId(tenantId, electionEventId, ballotBoxId);

		// convert to json object
		JsonObject object = JsonUtils.getJsonObject(ballotBoxContent.getJson());

		LOGGER.info(I18N.getMessage("BallotBoxContentService.getBallotBoxContent.ballotBoxContentFound"), tenantId, electionEventId, ballotBoxId);

		return new String(Base64.decode(object.getString(KEYSTORE)), StandardCharsets.UTF_8);
	}
}
