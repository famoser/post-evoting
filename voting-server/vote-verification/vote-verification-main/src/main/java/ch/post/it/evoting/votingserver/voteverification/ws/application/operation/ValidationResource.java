/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.ws.application.operation;

import javax.ejb.EJB;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationResult;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.SemanticErrorException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.SyntaxErrorException;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdInstance;
import ch.post.it.evoting.votingserver.commons.ui.Constants;
import ch.post.it.evoting.votingserver.commons.util.ValidationUtils;
import ch.post.it.evoting.votingserver.voteverification.domain.service.ValidationService;

@Path("/validations")
public class ValidationResource {

	private static final String PARAMETER_VALUE_TENANT_ID = "tenantId";

	private static final String PARAMETER_VALUE_ELECTION_EVENT_ID = "electionEventId";
	private static final Logger LOGGER = LoggerFactory.getLogger(ValidationResource.class);
	private final Gson gson = new Gson();

	@Inject
	private TrackIdInstance trackIdInstance;

	@EJB
	private ValidationService validationService;

	/**
	 * Validates a vote by applying the configured rules for a tenant, election event and verification
	 * card.
	 *
	 * @param electionEventId - election event identifier
	 * @param tenantId        - tenant identifier
	 * @param vote            - the vote to be validated.
	 * @param trackId         - the track id to be used for logging purposes.
	 * @param request         - the http servlet request.
	 * @return if the validation is successfully performed, returns a response with HTTP status code
	 * 200. If the validation fails, returns a response with HTTP status code 422 with a
	 * message "Validation failed!" and the errors with the reason why it failed.
	 * @throws ApplicationException   if there is a problem with the validation of the vote.
	 * @throws SemanticErrorException if there are semantic errors in configuration input.
	 * @throws SyntaxErrorException   if there are syntax errors in configuration input.
	 */
	@POST
	@Consumes(javax.ws.rs.core.MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/tenant/{tenantId}/electionevent/{electionEventId}")
	public Response validateVote(
			@PathParam(PARAMETER_VALUE_TENANT_ID)
					String tenantId,
			@PathParam(PARAMETER_VALUE_ELECTION_EVENT_ID)
					String electionEventId,
			@NotNull
					Vote vote,
			@NotNull
			@HeaderParam(Constants.PARAMETER_X_REQUEST_ID)
					String trackId,
			@Context
					HttpServletRequest request) throws ApplicationException, SyntaxErrorException, SemanticErrorException {
		// set the track id to be logged
		trackIdInstance.setTrackId(trackId);

		// validate input
		ValidationUtils.validate(vote);

		LOGGER.info("Validating the vote for tenant: {}, electionEvent: {} and verificationCard: {}.", vote.getTenantId(), vote.getElectionEventId(),
				vote.getVerificationCardId());

		// validate the vote
		ValidationResult voteValidationResult = validationService.validate(vote);

		LOGGER.info("Result of validating the vote: {} ", voteValidationResult.isResult());

		// convert to string
		String json = gson.toJson(voteValidationResult);

		// return the ballot text json
		return Response.ok().entity(json).build();
	}

}
