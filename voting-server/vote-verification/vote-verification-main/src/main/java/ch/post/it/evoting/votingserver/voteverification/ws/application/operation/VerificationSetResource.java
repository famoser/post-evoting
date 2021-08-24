/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.ws.application.operation;

import java.io.IOException;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.beans.verificationset.VerificationSet;
import ch.post.it.evoting.votingserver.commons.beans.verificationset.VerificationSetData;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdInstance;
import ch.post.it.evoting.votingserver.commons.ui.Constants;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetEntity;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetRepository;

/**
 * Service which offers the possibility of getting voter verifications sets from the system.
 */
@Path("/verificationsets")
@Stateless
public class VerificationSetResource {

	private static final String PARAMETER_VALUE_TENANT_ID = "tenantId";

	private static final String PARAMETER_VALUE_VERIFICATION_CARD_SET_ID = "verificationCardSetId";

	private static final String RESOURCE = "VERIFICATIONSET";

	private static final String PARAMETER_VALUE_ELECTION_EVENT_ID = "electionEventId";

	private static final String ERROR_CODE_MANDATORY_FIELD = "mandatory.field";

	private static final String VERIFIATION_CARD_SET_ID_IS_NULL = "Verification card set id is null";

	private static final String ELECTION_EVENT_ID_IS_NULL = "Election event id is null";

	private static final String TENANT_ID_IS_NULL = "Tenant id is null";

	@Inject
	private VerificationSetRepository verificationSetDataRepository;

	@Inject
	private TrackIdInstance tackIdInstance;

	/**
	 * Returns a credential and verification data for a given tenant, election event and verification card.
	 *
	 * @param trackingId            The track id to be used for logging purposes.
	 * @param tenantId              The tenant identifier.
	 * @param electionEventId       The election event identifier.
	 * @param verificationCardSetId The verification card identifier.
	 * @param request               The http servlet request.
	 * @return a credential and verification data.
	 * @throws ResourceNotFoundException If voter information is not found.
	 * @throws IOException               Conversion exception from object to json. Material data. For example, both credential and verification data
	 *                                   are null.
	 * @throws ApplicationException      if one of the input parameters is null.
	 */
	@Path("/tenant/{tenantId}/electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getVoterInformation(
			@HeaderParam(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@PathParam(PARAMETER_VALUE_TENANT_ID)
					String tenantId,
			@PathParam(PARAMETER_VALUE_ELECTION_EVENT_ID)
					String electionEventId,
			@PathParam(PARAMETER_VALUE_VERIFICATION_CARD_SET_ID)
					String verificationCardSetId,
			@Context
					HttpServletRequest request) throws IOException, ResourceNotFoundException, ApplicationException {
		// set the track id to be logged
		tackIdInstance.setTrackId(trackingId);

		validateInput(tenantId, electionEventId, verificationCardSetId);

		VerificationSetEntity verification = verificationSetDataRepository
				.findByTenantIdElectionEventIdVerificationCardSetId(tenantId, electionEventId, verificationCardSetId);

		// convert from string
		VerificationSetData verificationSetData = ObjectMappers.fromJson(verification.getJson(), VerificationSetData.class);

		// build the result
		VerificationSet verificationSet = new VerificationSet();
		verificationSet.setId(verification.getVerificationCardSetId());
		verificationSet.setData(verificationSetData);
		verificationSet.setSignature(verification.getSignature());

		return Response.ok().entity(verificationSet).build();
	}

	private void validateInput(String tenantId, String electionEventId, String verificationCardId) throws ApplicationException {
		if (tenantId == null) {
			throw new ApplicationException(TENANT_ID_IS_NULL, RESOURCE, ERROR_CODE_MANDATORY_FIELD, PARAMETER_VALUE_TENANT_ID);
		}
		if (electionEventId == null) {
			throw new ApplicationException(ELECTION_EVENT_ID_IS_NULL, RESOURCE, ERROR_CODE_MANDATORY_FIELD, PARAMETER_VALUE_ELECTION_EVENT_ID);
		}
		if (verificationCardId == null) {
			throw new ApplicationException(VERIFIATION_CARD_SET_ID_IS_NULL, RESOURCE, ERROR_CODE_MANDATORY_FIELD,
					PARAMETER_VALUE_VERIFICATION_CARD_SET_ID);
		}
	}
}
