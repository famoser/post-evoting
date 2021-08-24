/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.ws.application.operation;

import java.io.IOException;
import java.io.Reader;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import ch.post.it.evoting.cryptolib.certificates.utils.CryptographicOperationException;
import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.model.confirmation.TraceableConfirmationMessage;
import ch.post.it.evoting.domain.returncodes.CastCodeAndComputeResults;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.SemanticErrorException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.SyntaxErrorException;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdInstance;
import ch.post.it.evoting.votingserver.commons.ui.Constants;
import ch.post.it.evoting.votingserver.commons.util.ValidationUtils;
import ch.post.it.evoting.votingserver.voteverification.service.CastCodeService;

/**
 * Web service to retrieve cast codes.
 */
@Path(CastCodeResource.RESOURCE_PATH)
@Stateless(name = "vv-CastCodeResource")
public class CastCodeResource {

	/**
	 *
	 */

	static final String RESOURCE_PATH = "/castcodes";

	static final String RETRIEVE_CAST_CODES_PATH = "/tenant/{tenantId}/electionevent/{electionEventId}/verificationcard/{verificationCardId}";

	static final String PARAMETER_VALUE_TENANT_ID = "tenantId";

	static final String PARAMETER_VALUE_ELECTION_EVENT_ID = "electionEventId";

	static final String PARAMETER_VALUE_VERIFICATION_CARD_ID = "verificationCardId";

	private static final String RESOURCE = "CAST_CODES";

	private static final String ERROR_CODE_MANDATORY_FIELD = "mandatory.field";

	private static final String VERIFIATION_CARD_ID_IS_NULL = "Verification card id is null";

	private static final String ELECTION_EVENT_ID_IS_NULL = "Election event id is null";

	private static final String TENANT_ID_IS_NULL = "Tenant id is null";

	@Inject
	private TrackIdInstance tackIdInstance;

	@Inject
	private CastCodeService castCodeService;

	/**
	 * Retrieve a cast code taking into account a tenant, election event and verification card for a
	 * given confirmation message.
	 *
	 * @param trackingId                - the track id to be used for logging purposes.
	 * @param tenantId                  - the tenant identifier.
	 * @param electionEventId           - the election event identifier.
	 * @param verificationCardId        - the verification card identifier.
	 * @param confirmationMessageReader - the confirmation message to process.
	 * @param request                   - the http servlet request.
	 * @return The http response of execute the operation. HTTP status code 200 if the request has
	 * succeed.
	 * @throws ApplicationException            if any input parameters is null or empty.
	 * @throws SemanticErrorException          If there are semantic errors in the data included in the body of
	 *                                         the request..
	 * @throws SyntaxErrorException            If the URI is incorrect or if the body of the request has syntax
	 *                                         errors (For instance, a missing field)..
	 * @throws IOException
	 * @throws ResourceNotFoundException
	 * @throws CryptographicOperationException
	 * @throws ClassNotFoundException
	 */
	@Path(RETRIEVE_CAST_CODES_PATH)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response retrieveCastCodes(
			@HeaderParam(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@PathParam(PARAMETER_VALUE_TENANT_ID)
					String tenantId,
			@PathParam(PARAMETER_VALUE_ELECTION_EVENT_ID)
					String electionEventId,
			@PathParam(PARAMETER_VALUE_VERIFICATION_CARD_ID)
					String verificationCardId,
			@NotNull
					Reader confirmationMessageReader,
			@Context
					HttpServletRequest request)
			throws ApplicationException, SyntaxErrorException, SemanticErrorException, IOException, ResourceNotFoundException,
			CryptographicOperationException, ClassNotFoundException {
		// set the track id to be logged
		tackIdInstance.setTrackId(trackingId);

		validateInput(tenantId, electionEventId, verificationCardId);

		// convert json to object
		TraceableConfirmationMessage traceableConfirmationMessage = ObjectMappers
				.fromJson(confirmationMessageReader, TraceableConfirmationMessage.class);

		// validate voter verification
		ValidationUtils.validate(traceableConfirmationMessage);

		CastCodeAndComputeResults castCodeMessage;
		castCodeMessage = castCodeService.retrieveCastCode(tenantId, electionEventId, verificationCardId, traceableConfirmationMessage);

		return Response.ok().entity(castCodeMessage).build();

	}

	private void validateInput(String tenantId, String electionEventId, String verificationCardId) throws ApplicationException {
		if (tenantId == null) {
			throw new ApplicationException(TENANT_ID_IS_NULL, RESOURCE, ERROR_CODE_MANDATORY_FIELD, PARAMETER_VALUE_TENANT_ID);
		}
		if (electionEventId == null) {
			throw new ApplicationException(ELECTION_EVENT_ID_IS_NULL, RESOURCE, ERROR_CODE_MANDATORY_FIELD, PARAMETER_VALUE_ELECTION_EVENT_ID);
		}
		if (verificationCardId == null) {
			throw new ApplicationException(VERIFIATION_CARD_ID_IS_NULL, RESOURCE, ERROR_CODE_MANDATORY_FIELD, PARAMETER_VALUE_VERIFICATION_CARD_ID);
		}
	}
}
