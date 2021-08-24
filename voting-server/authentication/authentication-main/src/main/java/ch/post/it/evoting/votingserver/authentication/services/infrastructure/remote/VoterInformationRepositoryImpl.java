/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.authentication.services.infrastructure.remote;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.votingserver.authentication.services.domain.model.information.VoterInformation;
import ch.post.it.evoting.votingserver.authentication.services.domain.model.information.VoterInformationRepository;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RetrofitConsumer;
import ch.post.it.evoting.votingserver.commons.logging.service.I18nLoggerMessages;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdInstance;
import ch.post.it.evoting.votingserver.commons.util.PropertiesFileReader;

/**
 * Implementation of the voter informationRepository through a web service client.
 */
@Stateless(name = "au-VoterInformationRepositoryImpl")
public class VoterInformationRepositoryImpl implements VoterInformationRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(VoterInformationRepositoryImpl.class);

	private static final I18nLoggerMessages I18N = I18nLoggerMessages.getInstance();

	// The name of the property path informations.
	private static final String PROPERTY_PATH_INFORMATIONS = "path.informations";

	// The value of the property path informations.
	private static final String PATH_INFORMATIONS = PropertiesFileReader.getInstance().getPropertyValue(PROPERTY_PATH_INFORMATIONS);

	@Inject
	private TrackIdInstance trackId;

	private VoterMaterialClient voterMaterialClient;

	@Inject
	VoterInformationRepositoryImpl(final VoterMaterialClient voterMaterialClient) {
		this.voterMaterialClient = voterMaterialClient;
	}

	/**
	 * Returns a voter information for a given tenant, election event and credential. The
	 * implementation is a REST Client invoking to an get operation of a REST web service to obtain
	 * the voter information for the given parameters. If the web service returns a HTTP NOT FOUND
	 * status (404), then the method throws a ResourceNotFoundException exception.
	 *
	 * @param tenantId        - the tenant identifier.
	 * @param electionEventId - the election event identifier.
	 * @param credentialId    - the credential identifier.
	 * @return The voter information.
	 * @throws ResourceNotFoundException if no voter information is found.
	 */
	@Override
	public VoterInformation findByTenantIdElectionEventIdCredentialId(String tenantId, String electionEventId, String credentialId)
			throws ResourceNotFoundException {
		LOGGER.info(I18N.getMessage("VoterInformationRepositoryImpl.findByTenantIdElectionEventIdVotingCardId.findingVoterInformation"), tenantId,
				electionEventId, credentialId);

		try {
			VoterInformation voterInformation = RetrofitConsumer.processResponse(
					voterMaterialClient.getVoterInformation(trackId.getTrackId(), PATH_INFORMATIONS, tenantId, electionEventId, credentialId));
			LOGGER.info(I18N.getMessage("VoterInformationRepositoryImpl.findByTenantIdElectionEventIdVotingCardId.voterInformationFound"), tenantId,
					electionEventId, credentialId);
			return voterInformation;
		} catch (ResourceNotFoundException e) {
			LOGGER.error(I18N.getMessage("VoterInformationRepositoryImpl.findByTenantIdElectionEventIdVotingCardId.voterInformationNotFound"),
					tenantId, electionEventId, credentialId);
			throw e;
		}
	}
}
