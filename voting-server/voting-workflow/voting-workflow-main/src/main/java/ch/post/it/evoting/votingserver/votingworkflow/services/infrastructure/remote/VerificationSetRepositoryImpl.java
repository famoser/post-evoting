/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.votingworkflow.services.infrastructure.remote;

import java.io.IOException;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.beans.verificationset.VerificationSet;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RetrofitConsumer;
import ch.post.it.evoting.votingserver.commons.logging.service.I18nLoggerMessages;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdInstance;
import ch.post.it.evoting.votingserver.commons.util.PropertiesFileReader;
import ch.post.it.evoting.votingserver.votingworkflow.services.domain.model.verificationset.VerificationSetRepository;

/**
 * Implementation of the VerificationRepository using a REST client.
 */
@Stateless(name = "vw-VerificationSetRepositoryImpl")
public class VerificationSetRepositoryImpl implements VerificationSetRepository {

	/**
	 * The properties file reader.
	 */
	private static final PropertiesFileReader PROPERTIES = PropertiesFileReader.getInstance();

	/**
	 * The path to the resource verification.
	 */
	private static final String VERIFICATION_SET_PATH = PROPERTIES.getPropertyValue("VERIFICATION_SET_PATH");

	// Instance of the logger
	private static final Logger LOGGER = LoggerFactory.getLogger(VerificationSetRepositoryImpl.class);

	// Instance of the I18N logger messages
	private static final I18nLoggerMessages I18N = I18nLoggerMessages.getInstance();
	private final VerificationClient verificationClient;
	@Inject
	private TrackIdInstance trackId;

	@Inject
	VerificationSetRepositoryImpl(final VerificationClient verificationClient) {
		this.verificationClient = verificationClient;
	}

	/**
	 * Searches the associated verification data for the given parameters using a Rest client.
	 *
	 * @param tenantId              - the identifier of the tenant.
	 * @param electionEventId       - the identifier of the election event.
	 * @param verificationCardSetId - the identifier of the verification card set.
	 * @return a Verification object if found.
	 * @throws ResourceNotFoundException if the resource is not found.
	 * @throws IOException               when converting to json fails.
	 */
	@Override
	public VerificationSet findByTenantElectionEventVerificationCardSetId(String tenantId, String electionEventId, String verificationCardSetId)
			throws ResourceNotFoundException, IOException {
		LOGGER.info(I18N.getMessage("VerificationSetRepositoryImpl.findByTenantElectionEventVotingCardSet"), tenantId, electionEventId,
				verificationCardSetId);

		try {
			VerificationSet verificationSet = RetrofitConsumer.processResponse(verificationClient
					.findVerificationSetByTenantElectionEventVerificationCardSetId(trackId.getTrackId(), VERIFICATION_SET_PATH, tenantId,
							electionEventId, verificationCardSetId));
			LOGGER.info(I18N.getMessage("VerificationSetRepositoryImpl.findByTenantElectionEventVotingCardSet.found"), tenantId, electionEventId,
					verificationCardSetId);
			return verificationSet;
		} catch (ResourceNotFoundException e) {
			LOGGER.error(I18N.getMessage("VerificationSetRepositoryImpl.findByTenantElectionEventVotingCardSet.notFound"), tenantId, electionEventId,
					verificationCardSetId);
			throw e;
		}

	}

}
