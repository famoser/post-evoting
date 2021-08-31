/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.ws.application.operation;

import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.Certificate;

import javax.ejb.EJB;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.commons.serialization.JsonSignatureService;
import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.BallotBox;
import ch.post.it.evoting.domain.election.BallotBoxContextData;
import ch.post.it.evoting.domain.election.model.certificate.CertificateEntity;
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
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxContent;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxContentRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxData;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxInformation;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxInformationRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.remote.EiRemoteCertificateService;

/**
 * Web service for handling ballot box data information.
 */
@Path("/ballotboxdata")
@Stateless
public class BallotBoxDataResource {

	public static final String ADMINISTRATION_BOARD_CN_PREFIX = "AdministrationBoard ";
	private static final String ADD_BALLOT_BOX_CONTENT_AND_INFORMATION_PATH = "tenant/{tenantId}/electionevent/{electionEventId}/ballotbox/{ballotBoxId}/adminboard/{adminBoardId}";
	private static final String RESOURCE_NAME = "ballotboxdata";

	private static final String QUERY_PARAMETER_TENANT_ID = "tenantId";

	private static final String QUERY_PARAMETER_ELECTION_EVENT_ID = "electionEventId";

	private static final String QUERY_PARAMETER_BALLOT_BOX_ID = "ballotBoxId";

	private static final String QUERY_PARAMETER_ADMIN_BOARD_ID = "adminBoardId";
	private static final Logger LOGGER = LoggerFactory.getLogger(BallotBoxDataResource.class);
	@EJB
	private BallotBoxContentRepository ballotBoxContentRepository;
	@EJB
	private BallotBoxInformationRepository ballotBoxInformationRepository;

	@Inject
	@EiRemoteCertificateService
	private RemoteCertificateService remoteCertificateService;

	@Inject
	private TrackIdInstance trackIdInstance;

	/**
	 * Adds ballot box "content" and "information" to the ballot box
	 *
	 * @param trackingId      - the track id to be used for logging purposes.
	 * @param jsonContent     - the data to be added to a ballot box.
	 * @param tenantId        - the tenant identifier.
	 * @param electionEventId - the election event identifier.
	 * @param ballotBoxId     - the ballot identifier.
	 * @param request         - the http servlet request.
	 * @return http status
	 * @throws ApplicationException      if the input parameters are not valid.
	 * @throws IOException               if there are errors during conversion of ballot to json format.
	 * @throws ResourceNotFoundException if the ballot is not found.
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ADD_BALLOT_BOX_CONTENT_AND_INFORMATION_PATH)
	public Response addBallotBoxContentAndInformation(
			@HeaderParam(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@NotNull
			final BallotBoxData jsonContent,
			@PathParam(QUERY_PARAMETER_TENANT_ID)
			final String tenantId,
			@PathParam(QUERY_PARAMETER_ELECTION_EVENT_ID)
			final String electionEventId,
			@PathParam(QUERY_PARAMETER_BALLOT_BOX_ID)
			final String ballotBoxId,
			@PathParam(QUERY_PARAMETER_ADMIN_BOARD_ID)
			final String adminBoardId,
			@Context
			final HttpServletRequest request) throws ApplicationException, IOException {

		// set the track id to be logged
		trackIdInstance.setTrackId(trackingId);

		// validate parameters
		validateParameters(tenantId, electionEventId, ballotBoxId);

		LOGGER.info("Adding ballot box content for ballotBoxId: {}, electionEventId: {}, and tenantId: {}.", ballotBoxId, electionEventId, tenantId);

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

		final BallotBoxInformation ballotBoxInformation = new BallotBoxInformation();
		ballotBoxInformation.setTenantId(tenantId);
		ballotBoxInformation.setBallotBoxId(ballotBoxId);
		ballotBoxInformation.setElectionEventId(electionEventId);

		final String signedBallotBox = jsonContent.getBallotBox();
		final SignedObject signedBallotBoxObject = ObjectMappers.fromJson(signedBallotBox, SignedObject.class);
		final String signatureBallotBox = signedBallotBoxObject.getSignature();

		BallotBox ballotBox;
		try {
			LOGGER.info("Verifying ballot box signature");
			ballotBox = JsonSignatureService.verify(adminBoardPublicKey, signatureBallotBox, BallotBox.class);
			LOGGER.info("Ballot box signature was successfully verified");
		} catch (final Exception e) {
			LOGGER.error("Ballot box signature could not be verified", e);
			return Response.status(Response.Status.PRECONDITION_FAILED).build();
		}

		try {
			final String ballotBoxJSON = ObjectMappers.toJson(ballotBox);
			ballotBoxInformation.setJson(ballotBoxJSON);
			ballotBoxInformation.setSignature(signatureBallotBox);
			ballotBoxInformationRepository.save(ballotBoxInformation);

			final BallotBoxContent ballotBoxContent = new BallotBoxContent();
			ballotBoxContent.setTenantId(tenantId);
			ballotBoxContent.setElectionEventId(electionEventId);
			ballotBoxContent.setBallotBoxId(ballotBoxId);

			final String signedBallotBoxContents = jsonContent.getBallotBoxContextData();
			final SignedObject signedBallotBoxContentsObject = ObjectMappers.fromJson(signedBallotBoxContents, SignedObject.class);
			final String signatureBallotBoxContents = signedBallotBoxContentsObject.getSignature();

			BallotBoxContextData ballotBoxContents;
			try {
				LOGGER.info("Verifying ballot context data signature");
				ballotBoxContents = JsonSignatureService.verify(adminBoardPublicKey, signatureBallotBoxContents, BallotBoxContextData.class);
				LOGGER.info("Ballot context data signature was successfully verified");
			} catch (final Exception e) {
				LOGGER.error("Ballot context data signature could not be verified", e);
				return Response.status(Response.Status.PRECONDITION_FAILED).build();
			}
			final String ballotBoxContentsJSON = ObjectMappers.toJson(ballotBoxContents);
			ballotBoxContent.setJson(ballotBoxContentsJSON);
			ballotBoxContentRepository.save(ballotBoxContent);

			LOGGER.info("Ballot box content for ballotBoxId: {}, electionEventId: {}, and tenantId: {} added.", ballotBoxId, electionEventId,
					tenantId);
		} catch (final DuplicateEntryException ex) {
			LOGGER.warn("Duplicate entry tried to be inserted for ballot box: {}, electionEventId: {}, and tenantId: {}", ballotBoxId,
					electionEventId, tenantId, ex);
		}

		return Response.ok().build();

	}

	private void validateParameters(final String tenantId, final String electionEventId, final String ballotBoxId) throws ApplicationException {
		if (tenantId == null || tenantId.isEmpty()) {
			throw new ApplicationException(ApplicationExceptionMessages.EXCEPTION_MESSAGE_QUERY_PARAMETER_IS_NULL, RESOURCE_NAME,
					ErrorCodes.MISSING_QUERY_PARAMETER, QUERY_PARAMETER_TENANT_ID);
		}

		if (electionEventId == null || electionEventId.isEmpty()) {
			throw new ApplicationException(ApplicationExceptionMessages.EXCEPTION_MESSAGE_QUERY_PARAMETER_IS_NULL, RESOURCE_NAME,
					ErrorCodes.MISSING_QUERY_PARAMETER, QUERY_PARAMETER_ELECTION_EVENT_ID);
		}

		if (ballotBoxId == null || ballotBoxId.isEmpty()) {
			throw new ApplicationException(ApplicationExceptionMessages.EXCEPTION_MESSAGE_QUERY_PARAMETER_IS_NULL, RESOURCE_NAME,
					ErrorCodes.MISSING_QUERY_PARAMETER, QUERY_PARAMETER_BALLOT_BOX_ID);
		}
	}
}
