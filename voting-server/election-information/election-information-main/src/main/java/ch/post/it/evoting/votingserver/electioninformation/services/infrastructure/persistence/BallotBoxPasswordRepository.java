/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.persistence;

import java.security.PrivateKey;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.beans.utils.PasswordEncrypter;
import ch.post.it.evoting.votingserver.commons.cache.Cache;
import ch.post.it.evoting.votingserver.commons.crypto.PasswordForObjectRepository;
import ch.post.it.evoting.votingserver.commons.logging.service.I18nLoggerMessages;
import ch.post.it.evoting.votingserver.commons.util.JsonUtils;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxContent;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxContentRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.tenant.EiTenantSystemKeys;

/**
 * Recovers the private key for a ballot box keystore.
 */
public class BallotBoxPasswordRepository implements PasswordForObjectRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(BallotBoxPasswordRepository.class);

	private static final I18nLoggerMessages I18N = I18nLoggerMessages.getInstance();

	private static final String KEYSTORE_PW = "passwordKeystore";
	private static final String KEY_SEPERATOR = "-";

	@Inject
	private BallotBoxContentRepository ballotBoxContentRepository;
	@EJB
	private EiTenantSystemKeys eiTenantSystemKeys;
	@Inject
	private AsymmetricServiceAPI asymmetricService;
	@EJB(beanName = "BallotBoxSignerKeystorePasswordCache")
	private Cache<String, String> passwordCache;

	@Override
	public String getByTenantEEIDObjectId(final String tenantId, final String electionEventId, final String ballotBoxId)
			throws ResourceNotFoundException {
		LOGGER.info(I18N.getMessage("BallotBoxPasswordRepository.getByTenantEEIDObjectIdAlias.recoveringKeystorePassword"), tenantId, electionEventId,
				ballotBoxId);

		String cacheKey = constructCacheKey(tenantId, electionEventId, ballotBoxId);

		String cachedPassword = passwordCache.get(cacheKey);
		if ((cachedPassword != null) && (!"".equals(cachedPassword))) {
			return cachedPassword;
		}

		// recover from db
		BallotBoxContent ballotBoxContent = ballotBoxContentRepository
				.findByTenantIdElectionEventIdBallotBoxId(tenantId, electionEventId, ballotBoxId);

		// convert to json object
		JsonObject object = JsonUtils.getJsonObject(ballotBoxContent.getJson());

		LOGGER.info(I18N.getMessage("BallotBoxPasswordRepository.getByTenantEEIDObjectIdAlias.keystorePasswordFound"), tenantId, electionEventId,
				ballotBoxId);

		String passwordFromDB = object.getString(KEYSTORE_PW);

		String decryptedString;
		try {
			decryptedString = decryptedPassword(tenantId, passwordFromDB);
		} catch (GeneralCryptoLibException e) {
			throw new ResourceNotFoundException(
					"Error while trying to recover tenant system password to decrypt the ballot box password to decrypt the ballot box signer keystore password",
					e);
		}

		passwordCache.put(cacheKey, decryptedString);

		return decryptedString;
	}

	private String constructCacheKey(final String tenantId, final String electionEventId, final String ballotBoxId) {

		return tenantId + KEY_SEPERATOR + electionEventId + KEY_SEPERATOR + ballotBoxId;
	}

	private String decryptedPassword(final String tenantId, final String passwordFromDB) throws GeneralCryptoLibException {

		PasswordEncrypter passwordEncrypter = new PasswordEncrypter(asymmetricService);

		PrivateKey privateKey = eiTenantSystemKeys.getEncryptionPrivateKey(tenantId);

		return passwordEncrypter.decryptPassword(passwordFromDB, privateKey);
	}
}
