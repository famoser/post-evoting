/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.ws.application.operation;

import java.io.IOException;
import java.io.Reader;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.time.Clock;
import java.time.Instant;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.commons.serialization.JsonSignatureService;
import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.ElectionInformationContents;
import ch.post.it.evoting.domain.election.model.certificate.CertificateEntity;
import ch.post.it.evoting.domain.election.validation.ValidationResult;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationExceptionMessages;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.DuplicateEntryException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RetrofitException;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.service.RemoteCertificateService;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdInstance;
import ch.post.it.evoting.votingserver.commons.ui.Constants;
import ch.post.it.evoting.votingserver.commons.ui.ws.rs.persistence.ErrorCodes;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.common.SignedObject;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionInformationContent;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionInformationContentRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.service.election.ElectionService;
import ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.remote.EiRemoteCertificateService;

/**
 * Web service for handling electoral data resource.
 */
@Path("/electioneventdata")
@Stateless(name = "ei-ElectionEventDataResource")
public class ElectionEventDataResource {

	public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";

	public static final String HEADER_CONTENT_LENGTH = "Content-Length";

	public static final String ATTACHMENT_FILENAME_SIZE = "attachment; filename=%s";

	public static final String ADMINISTRATION_BOARD_CN_PREFIX = "AdministrationBoard ";

	// The name of the resource handle by this web service.
	private static final String RESOURCE_NAME = "electioneventdata";

	// The name of the query parameter tenantId
	private static final String QUERY_PARAMETER_TENANT_ID = "tenantId";

	// The name of the query parameter electionEventId
	private static final String QUERY_PARAMETER_ELECTION_EVENT_ID = "electionEventId";

	private static final String QUERY_PARAMETER_ADMIN_BOARD_ID = "adminBoardId";

	private static final String CASTED_VC_NAME_FORMAT = "%s-usedVotingCards-%s";
	private static final Logger LOGGER = LoggerFactory.getLogger(ElectionEventDataResource.class);
	// An instance of the election information content repository
	@EJB
	private ElectionInformationContentRepository electionInformationContentRepository;

	@Inject
	@EiRemoteCertificateService
	private RemoteCertificateService remoteCertificateService;

	@Inject
	private ElectionService electionService;

	// The track id instance
	@Inject
	private TrackIdInstance trackIdInstance;

	/**
	 * Save the election event data given the tenant and the election event id.
	 *
	 * @param tenantId        - the tenant identifier.
	 * @param electionEventId - the election event identifier.
	 * @param data            - the election event data.
	 * @param request         - the http servlet request.
	 * @return status 200 on success.
	 * @throws ApplicationException if the input parameters are not valid.
	 */
	@POST
	@Path("tenant/{tenantId}/electionevent/{electionEventId}/adminboard/{adminBoardId}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response saveElectionEventData(
			@HeaderParam(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@PathParam(QUERY_PARAMETER_TENANT_ID)
			final String tenantId,
			@PathParam(QUERY_PARAMETER_ELECTION_EVENT_ID)
			final String electionEventId,
			@PathParam(QUERY_PARAMETER_ADMIN_BOARD_ID)
			final String adminBoardId,
			@NotNull
			final Reader data,
			@Context
			final HttpServletRequest request) throws ApplicationException, IOException {

		// set the track id to be logged
		trackIdInstance.setTrackId(trackingId);

		LOGGER.info("Saving election event information for electionEventId: {}, and tenantId: {}.", electionEventId, tenantId);

		validateParameters(tenantId, electionEventId);

		LOGGER.info("Fetching the administration board certificate");
		final String adminBoardCommonName = ADMINISTRATION_BOARD_CN_PREFIX + adminBoardId;

		Certificate adminBoardCert;
		try {
			final CertificateEntity adminBoardCertificateEntity = remoteCertificateService.getAdminBoardCertificate(adminBoardCommonName);
			final String adminBoardCertPEM = adminBoardCertificateEntity.getCertificateContent();
			adminBoardCert = PemUtils.certificateFromPem(adminBoardCertPEM);
		} catch (final GeneralCryptoLibException | RetrofitException e) {
			LOGGER.error("An error occurred while fetching the administration board certificate", e);
			return Response.status(Response.Status.PRECONDITION_FAILED).build();
		}
		final PublicKey adminBoardPublicKey = adminBoardCert.getPublicKey();

		final ElectionInformationContent electionInformationContent = new ElectionInformationContent();
		electionInformationContent.setTenantId(tenantId);
		electionInformationContent.setElectionEventId(electionEventId);

		final SignedObject signedElectionInformationContentObject = ObjectMappers.fromJson(data, SignedObject.class);
		final String signatureElectionInformationContent = signedElectionInformationContentObject.getSignature();

		ElectionInformationContents electionInformationContents;
		try {
			LOGGER.info("Verifying election information configuration signature");
			electionInformationContents = JsonSignatureService
					.verify(adminBoardPublicKey, signatureElectionInformationContent, ElectionInformationContents.class);
			LOGGER.info("Election information configuration signature was successfully verified");
		} catch (final Exception e) {
			LOGGER.error("Election information configuration signature could not be verified", e);
			return Response.status(Response.Status.PRECONDITION_FAILED).build();
		}
		final String electionInformationContentsJSON = ObjectMappers.toJson(electionInformationContents);
		electionInformationContent.setJson(electionInformationContentsJSON);
		try {
			electionInformationContentRepository.save(electionInformationContent);
			LOGGER.info("Election event information with electionEventId: {}, and tenantId: {} saved.", electionEventId, tenantId);
		} catch (final DuplicateEntryException ex) {
			LOGGER.warn("Duplicate entry tried to be inserted for election information content: {}", electionInformationContent.toString(), ex);
		}

		// return on success
		return Response.ok().build();
	}

	/**
	 * Returns the result of validate if the election event data is empty.
	 *
	 * @param tenantId        - the tenant identifier.
	 * @param electionEventId - the election event identifier.
	 * @return Returns the result of the validation.
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("tenant/{tenantId}/electionevent/{electionEventId}/status")
	public Response checkIfElectionEventDataIsEmpty(
			@HeaderParam(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@PathParam(QUERY_PARAMETER_TENANT_ID)
			final String tenantId,
			@PathParam(QUERY_PARAMETER_ELECTION_EVENT_ID)
			final String electionEventId,
			@Context
			final HttpServletRequest request) {

		// set the track id to be logged
		trackIdInstance.setTrackId(trackingId);

		final ValidationResult validationResult = new ValidationResult();
		validationResult.setResult(Boolean.FALSE);
		try {
			electionInformationContentRepository.findByTenantIdElectionEventId(tenantId, electionEventId);
		} catch (final ResourceNotFoundException e) {
			LOGGER.info("Resource not found, so election event data is empty as expected.", e);
			validationResult.setResult(Boolean.TRUE);
		}
		final Gson gson = new Gson();
		// convert to string
		final String json = gson.toJson(validationResult);

		return Response.ok().entity(json).build();
	}

	// Validate parameters.
	private void validateParameters(final String tenantId, final String electionEventId) throws ApplicationException {
		if (tenantId == null || tenantId.isEmpty()) {
			throw new ApplicationException(ApplicationExceptionMessages.EXCEPTION_MESSAGE_QUERY_PARAMETER_IS_NULL, RESOURCE_NAME,
					ErrorCodes.MISSING_QUERY_PARAMETER, QUERY_PARAMETER_TENANT_ID);
		}

		if (electionEventId == null || electionEventId.isEmpty()) {
			throw new ApplicationException(ApplicationExceptionMessages.EXCEPTION_MESSAGE_QUERY_PARAMETER_IS_NULL, RESOURCE_NAME,
					ErrorCodes.MISSING_QUERY_PARAMETER, QUERY_PARAMETER_ELECTION_EVENT_ID);
		}
	}

	@GET
	@Produces({ MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_PLAIN })
	@Path("/secured/tenant/{tenantId}/electionevent/{electionEventId}/cast-voting-cards")
	public Response getCastVotingCardsReport(
			@HeaderParam(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@PathParam(QUERY_PARAMETER_TENANT_ID)
			final String tenantId,
			@PathParam(QUERY_PARAMETER_ELECTION_EVENT_ID)
			final String electionEventId,
			@Context
			final HttpServletRequest request) {

		// set the track id to be logged
		trackIdInstance.setTrackId(trackingId);

		// validate parameters
		try {
			validateParameters(tenantId, electionEventId);
		} catch (ApplicationException e) {
			throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
		}

		String timestamp = Long.toString(Instant.now(Clock.systemUTC()).getEpochSecond());
		String filenamePrefix = String.format(CASTED_VC_NAME_FORMAT, electionEventId, timestamp);
		String filename = filenamePrefix + ".zip";
		StreamingOutput entity = stream -> electionService.writeCastVotes(tenantId, electionEventId, filenamePrefix, stream);
		return Response.ok().entity(entity).header(HEADER_CONTENT_DISPOSITION, String.format(ATTACHMENT_FILENAME_SIZE, filename))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM).build();
	}

	@GET
	@Produces({ MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_PLAIN })
	@Path("/secured/tenant/{tenantId}/electionevent/{electionEventId}/used-voting-cards")
	public Response getVerifiedVotingCardsReport(
			@HeaderParam(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@PathParam(QUERY_PARAMETER_TENANT_ID)
			final String tenantId,
			@PathParam(QUERY_PARAMETER_ELECTION_EVENT_ID)
			final String electionEventId,
			@Context
			final HttpServletRequest request) {

		// set the track id to be logged
		trackIdInstance.setTrackId(trackingId);

		// validate parameters
		try {
			validateParameters(tenantId, electionEventId);
		} catch (ApplicationException e) {
			throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
		}

		String timestamp = Long.toString(Instant.now(Clock.systemUTC()).getEpochSecond());
		String filenamePrefix = String.format(CASTED_VC_NAME_FORMAT, electionEventId, timestamp);
		String filename = filenamePrefix + ".zip";
		StreamingOutput entity = stream -> electionService.writeVerifiedVotes(tenantId, electionEventId, filenamePrefix, stream);
		return Response.ok().entity(entity).header(HEADER_CONTENT_DISPOSITION, String.format(ATTACHMENT_FILENAME_SIZE, filename))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM).build();
	}
}
