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

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.CryptographicOperationException;
import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.returncodes.ChoiceCodeAndComputeResults;
import ch.post.it.evoting.domain.returncodes.VoteAndComputeResults;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.SemanticErrorException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.SyntaxErrorException;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdInstance;
import ch.post.it.evoting.votingserver.commons.ui.Constants;
import ch.post.it.evoting.votingserver.commons.util.ValidationUtils;
import ch.post.it.evoting.votingserver.voteverification.service.ChoiceCodesService;

/**
 * Web service for creating choice codes.
 */
@Path(ChoiceCodeResource.RESOURCE_PATH)
@Stateless
public class ChoiceCodeResource {

	static final String RESOURCE_PATH = "/choicecodes";

	static final String GENERATE_CHOICE_CODES_PATH = "/tenant/{tenantId}/electionevent/{electionEventId}/verificationcard/{verificationCardId}";

	static final String PARAMETER_VALUE_TENANT_ID = "tenantId";

	static final String PARAMETER_VALUE_ELECTION_EVENT_ID = "electionEventId";

	static final String PARAMETER_VALUE_VERIFICATION_CARD_ID = "verificationCardId";

	private static final String RESOURCE = "CHOICE_CODES";

	private static final String ERROR_CODE_MANDATORY_FIELD = "mandatory.field";

	private static final String VERIFIATION_CARD_ID_IS_NULL = "Verification card id is null";

	private static final String ELECTION_EVENT_ID_IS_NULL = "Election event id is null";

	private static final String TENANT_ID_IS_NULL = "Tenant id is null";

	@Inject
	private TrackIdInstance tackIdInstance;

	@Inject
	private ChoiceCodesService choiceCodeService;

	/**
	 * Generates the choice codes taking into account a tenant, election event and verification card for a given encrypted vote.
	 *
	 * @param trackingId         - the track id to be used for logging purposes.
	 * @param tenantId           - the tenant identifier.
	 * @param electionEventId    - the election event identifier.
	 * @param verificationCardId - the verification card identifier.
	 * @param voteReader         - the encrypted vote.
	 * @param request            - the http servlet request.
	 * @return The http response of execute the operation. HTTP status code 200 if the request has succeed.
	 * @throws ApplicationException            if any input parameters is null or empty.
	 * @throws IOException
	 * @throws SemanticErrorException
	 * @throws SyntaxErrorException
	 * @throws ResourceNotFoundException
	 * @throws CryptographicOperationException
	 * @throws GeneralCryptoLibException
	 */
	@Path(GENERATE_CHOICE_CODES_PATH)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response generateChoiceCodes(
			@HeaderParam(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@PathParam(PARAMETER_VALUE_TENANT_ID)
					String tenantId,
			@PathParam(PARAMETER_VALUE_ELECTION_EVENT_ID)
					String electionEventId,
			@PathParam(PARAMETER_VALUE_VERIFICATION_CARD_ID)
					String verificationCardId,
			@NotNull
					Reader voteReader,
			@Context
					HttpServletRequest request)
			throws ApplicationException, IOException, SyntaxErrorException, SemanticErrorException, ResourceNotFoundException,
			CryptographicOperationException, GeneralCryptoLibException {

		// set the track id to be logged
		tackIdInstance.setTrackId(trackingId);

		validateInput(tenantId, electionEventId, verificationCardId);

		// convert json to object
		VoteAndComputeResults voteAndComputeResults = ObjectMappers.fromJson(voteReader, VoteAndComputeResults.class);

		// validate voter verification
		ValidationUtils.validate(voteAndComputeResults.getVote());

		ChoiceCodeAndComputeResults choiceCodes = choiceCodeService
				.generateChoiceCodes(tenantId, electionEventId, verificationCardId, voteAndComputeResults);

		return Response.ok(choiceCodes).build();
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
