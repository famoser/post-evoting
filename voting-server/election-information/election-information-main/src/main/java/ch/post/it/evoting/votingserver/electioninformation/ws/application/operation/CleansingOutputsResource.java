/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.ws.application.operation;

import static org.apache.commons.lang3.StringUtils.isEmpty;

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
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationExceptionMessages;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdInstance;
import ch.post.it.evoting.votingserver.commons.ui.Constants;
import ch.post.it.evoting.votingserver.commons.ui.ws.rs.persistence.ErrorCodes;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.validation.ElectionValidationRequest;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.service.ballotbox.BallotBoxService;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.service.election.ElectionService;
import ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.persistence.CleansingOutputsService;

@Path(CleansingOutputsResource.RESOURCE_NAME)
@Stateless
public class CleansingOutputsResource {

	static final String RESOURCE_NAME = "cleansingoutputs";

	static final String SUCCESSFUL_VOTES_PATH = "secured/tenant/{tenantId}/electionevent/{electionEventId}/ballotbox/{ballotBoxId}/successfulvotes";

	static final String FAILED_VOTES_PATH = "secured/tenant/{tenantId}/electionevent/{electionEventId}/ballotbox/{ballotBoxId}/failedvotes";

	private static final String QUERY_PARAMETER_TENANT_ID = "tenantId";

	private static final String QUERY_PARAMETER_ELECTION_EVENT_ID = "electionEventId";

	private static final String QUERY_PARAMETER_BALLOT_BOX_ID = "ballotBoxId";
	private static final Logger LOGGER = LoggerFactory.getLogger(CleansingOutputsResource.class);
	@Inject
	private TrackIdInstance trackIdInstance;
	@Inject
	private BallotBoxService ballotBoxService;
	@Inject
	private ElectionService electionService;

	@Inject
	private CleansingOutputsService cleansingOutputsService;

	/**
	 * Return a stream of successful votes with its signature in case that the election is closed and the ballot box is not a test ballot box.
	 *
	 * @param tenantId        - the tenant identifier.
	 * @param electionEventId - the election event identifier.
	 * @param ballotBoxId     - the ballot box identifier.
	 * @return Returns the successful votes stream for the tenantId, electionEventId and ballotBoxId.
	 * @throws ResourceNotFoundException if the related ballot box is not found.
	 * @throws ApplicationException      if the input parameters are not valid.
	 */
	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Path(SUCCESSFUL_VOTES_PATH)
	public Response getSuccessfulVotes(
			@HeaderParam(Constants.PARAMETER_X_REQUEST_ID)
			final String trackingId,
			@PathParam(QUERY_PARAMETER_TENANT_ID)
			final String tenantId,
			@PathParam(QUERY_PARAMETER_ELECTION_EVENT_ID)
			final String electionEventId,
			@PathParam(QUERY_PARAMETER_BALLOT_BOX_ID)
			final String ballotBoxId,
			@Context
			final HttpServletRequest request) throws ApplicationException, ResourceNotFoundException {

		trackIdInstance.setTrackId(trackingId);

		validateParameters(tenantId, electionEventId, ballotBoxId);

		boolean isTestBallotBox = ballotBoxService.checkIfTest(tenantId, electionEventId, ballotBoxId);

		if (!isTestBallotBox && electionIsOpen(tenantId, electionEventId, ballotBoxId)) {
			return Response.status(Response.Status.PRECONDITION_FAILED).build();
		}

		StreamingOutput entity = stream -> cleansingOutputsService.writeSuccessfulVotes(stream, tenantId, electionEventId, ballotBoxId);

		return Response.ok().entity(entity).header("Content-Disposition", "attachment; filename=successfulVotes.csv;").build();
	}

	/**
	 * Return an stream of failed votes with its signature in case that election is closed.
	 *
	 * @param tenantId        - the tenant identifier.
	 * @param electionEventId - the election event identifier.
	 * @param ballotBoxId     - the ballot box identifier.
	 * @return Returns the successful votes stream for the tenantId, electionEventId and ballotBoxId.
	 * @throws ResourceNotFoundException if the related ballot box is not found.
	 * @throws ApplicationException      if the input parameters are not valid.
	 */
	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Path(FAILED_VOTES_PATH)
	public Response getFailedVotes(
			@HeaderParam(Constants.PARAMETER_X_REQUEST_ID)
			final String trackingId,
			@PathParam(QUERY_PARAMETER_TENANT_ID)
			final String tenantId,
			@PathParam(QUERY_PARAMETER_ELECTION_EVENT_ID)
			final String electionEventId,
			@PathParam(QUERY_PARAMETER_BALLOT_BOX_ID)
			final String ballotBoxId,
			@Context
			final HttpServletRequest request) throws ApplicationException, ResourceNotFoundException {

		trackIdInstance.setTrackId(trackingId);

		validateParameters(tenantId, electionEventId, ballotBoxId);

		boolean isTestBallotBox = ballotBoxService.checkIfTest(tenantId, electionEventId, ballotBoxId);

		if (!isTestBallotBox && electionIsOpen(tenantId, electionEventId, ballotBoxId)) {
			return Response.status(Response.Status.PRECONDITION_FAILED).build();
		}

		StreamingOutput entity = stream -> cleansingOutputsService.writeFailedVotes(stream, tenantId, electionEventId, ballotBoxId);

		return Response.ok().entity(entity).header("Content-Disposition", "attachment; filename=failedVotes.csv;").build();
	}

	private void validateParameters(final String tenantId, final String electionEventId, final String ballotBoxId) throws ApplicationException {
		if (isEmpty(tenantId)) {
			throw new ApplicationException(ApplicationExceptionMessages.EXCEPTION_MESSAGE_QUERY_PARAMETER_IS_NULL, RESOURCE_NAME,
					ErrorCodes.MISSING_QUERY_PARAMETER, QUERY_PARAMETER_TENANT_ID);
		}
		if (isEmpty(electionEventId)) {
			throw new ApplicationException(ApplicationExceptionMessages.EXCEPTION_MESSAGE_QUERY_PARAMETER_IS_NULL, RESOURCE_NAME,
					ErrorCodes.MISSING_QUERY_PARAMETER, QUERY_PARAMETER_ELECTION_EVENT_ID);
		}
		if (isEmpty(ballotBoxId)) {
			throw new ApplicationException(ApplicationExceptionMessages.EXCEPTION_MESSAGE_QUERY_PARAMETER_IS_NULL, RESOURCE_NAME,
					ErrorCodes.MISSING_QUERY_PARAMETER, QUERY_PARAMETER_BALLOT_BOX_ID);
		}
	}

	private boolean electionIsOpen(String tenantId, String electionEventId, String ballotBoxId) {
		ElectionValidationRequest electionValidationRequest = ElectionValidationRequest.create(tenantId, electionEventId, ballotBoxId, true);
		final ValidationError validationResult = electionService.validateIfElectionIsOpen(electionValidationRequest);
		if (!ValidationErrorType.ELECTION_OVER_DATE.equals(validationResult.getValidationErrorType())) {
			LOGGER.error("Ballot box {} not closed therefore cleansing outputs not available ", ballotBoxId);
			return true;
		}
		return false;
	}
}
