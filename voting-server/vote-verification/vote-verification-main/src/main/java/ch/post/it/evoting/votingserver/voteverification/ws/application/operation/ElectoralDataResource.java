/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.ws.application.operation;

import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.Certificate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.commons.serialization.JsonSignatureService;
import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.model.certificate.CertificateEntity;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationExceptionMessages;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.DuplicateEntryException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.service.RemoteCertificateService;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdInstance;
import ch.post.it.evoting.votingserver.commons.ui.Constants;
import ch.post.it.evoting.votingserver.commons.ui.ws.rs.persistence.ErrorCodes;
import ch.post.it.evoting.votingserver.commons.util.JsonUtils;
import ch.post.it.evoting.votingserver.voteverification.domain.common.SignedObject;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.ElectionPublicKey;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.ElectionPublicKeyRepository;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.VerificationContentRepository;
import ch.post.it.evoting.votingserver.voteverification.infrastructure.remote.VvRemoteCertificateService;

/**
 * Web service for handling electoral data resource.
 */
@Path(ElectoralDataResource.RESOURCE_PATH)
@Stateless(name = "vv-ElectoralDataResource")
public class ElectoralDataResource {

	public static final String ADMINISTRATION_BOARD_CN_PREFIX = "AdministrationBoard ";
	static final String RESOURCE_PATH = "/electoraldata";
	static final String SAVE_ELECTORAL_DATA_PATH = "/tenant/{tenantId}/electionevent/{electionEventId}/electoralauthority/{electoralAuthorityId}/adminboard/{adminBoardId}";
	// The name of the query parameter tenantId
	static final String QUERY_PARAMETER_TENANT_ID = "tenantId";
	// The name of the query parameter electionEventId
	static final String QUERY_PARAMETER_ELECTION_EVENT_ID = "electionEventId";
	// The name of the query parameter electoralAuthorityId
	static final String QUERY_PARAMETER_ELECTORAL_AUTHORITY_ID = "electoralAuthorityId";
	static final String QUERY_PARAMETER_ADMIN_BOARD_ID = "adminBoardId";
	// The name of the resource handle by this web service.
	private static final String RESOURCE_NAME = "electoraldata";
	private static final String CONSTANT_ELECTION_PUBLIC_KEY = "electionPublicKey";
	private static final Logger LOGGER = LoggerFactory.getLogger(ElectoralDataResource.class);
	// An instance of the verification content repository
	@EJB
	private VerificationContentRepository verificationContentRepository;
	// An instance of the election public key repository
	@EJB
	private ElectionPublicKeyRepository electionPublicKeyRepository;

	@Inject
	@VvRemoteCertificateService
	private RemoteCertificateService remoteCertificateService;

	// The track id instance
	@Inject
	private TrackIdInstance trackIdInstance;

	/**
	 * Save electoral data given the tenant, the election event id and the electoral authority identifier.
	 *
	 * @param tenantId             - the tenant identifier.
	 * @param electionEventId      - the election event identifier.
	 * @param electoralAuthorityId - the electoral authority identifier.
	 * @param request              - the http servlet request.
	 * @return Returns status 200 on success.
	 * @throws ApplicationException if the input parameters are not valid.
	 */
	@POST
	@Path(SAVE_ELECTORAL_DATA_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response saveElectoralData(
			@HeaderParam(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@PathParam(QUERY_PARAMETER_TENANT_ID)
					String tenantId,
			@PathParam(QUERY_PARAMETER_ELECTION_EVENT_ID)
					String electionEventId,
			@PathParam(QUERY_PARAMETER_ELECTORAL_AUTHORITY_ID)
					String electoralAuthorityId,
			@PathParam(QUERY_PARAMETER_ADMIN_BOARD_ID)
					String adminBoardId,
			@NotNull
					String inputJson,
			@Context
					HttpServletRequest request) throws ApplicationException, ResourceNotFoundException {

		trackIdInstance.setTrackId(trackingId);

		if (inputJson == null || inputJson.isEmpty()) {
			LOGGER.error("Missing request body");
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		LOGGER.info("Saving election public key information with electoralAuthorityId: {}, electionEventId: {}, and tenantId: {}.",
				electoralAuthorityId, electionEventId, tenantId);

		// validate parameters
		validateParameters(tenantId, electionEventId, electoralAuthorityId);

		LOGGER.info("Fetching the administration board certificate");
		String adminBoardCommonName = ADMINISTRATION_BOARD_CN_PREFIX + adminBoardId;

		CertificateEntity adminBoardCertificateEntity = remoteCertificateService.getAdminBoardCertificate(adminBoardCommonName);
		if (adminBoardCertificateEntity == null) {
			LOGGER.error("Could not find Admin Board certificate '{}'", adminBoardCommonName);
			return Response.status(Response.Status.PRECONDITION_FAILED).build();
		}

		String adminBoardCertPEM = adminBoardCertificateEntity.getCertificateContent();
		PublicKey adminBoardPublicKey;
		try {
			Certificate adminBoardCert = PemUtils.certificateFromPem(adminBoardCertPEM);
			adminBoardPublicKey = adminBoardCert.getPublicKey();
		} catch (GeneralCryptoLibException e) {
			LOGGER.error("An error occurred while loading the administration board certificate", e);
			return Response.status(Response.Status.PRECONDITION_FAILED).build();
		}

		ElectionPublicKey electionPublicKey = new ElectionPublicKey();
		electionPublicKey.setElectionEventId(electionEventId);
		electionPublicKey.setTenantId(tenantId);
		electionPublicKey.setElectoralAuthorityId(electoralAuthorityId);

		String signature;
		try {
			JsonObject inputJsonObject = JsonUtils.getJsonObject(inputJson);
			String signedElectionPublicKeyJSON = inputJsonObject.getJsonObject(CONSTANT_ELECTION_PUBLIC_KEY).toString();
			SignedObject signedObject = ObjectMappers.fromJson(signedElectionPublicKeyJSON, SignedObject.class);
			signature = signedObject.getSignature();
		} catch (IOException e) {
			LOGGER.error("Error reading election public key information", e);
			return Response.status(Response.Status.PRECONDITION_FAILED).build();
		}

		try {
			LOGGER.info("Verifying election public key signature");
			JsonSignatureService.verify(adminBoardPublicKey, signature, ch.post.it.evoting.domain.election.ElectionPublicKey.class);
			LOGGER.info("Election public key signature was successfully verified");
		} catch (Exception e) {
			LOGGER.error("Election public key signature could not be verified", e);
			return Response.status(Response.Status.PRECONDITION_FAILED).build();
		}

		try {
			electionPublicKey.setJwt(signature);
			electionPublicKeyRepository.save(electionPublicKey);

			LOGGER.info("Election Public Key information with electoralAuthorityId: {}, electionEventId: {}, and tenantId: {} saved.",
					electoralAuthorityId, electionEventId, tenantId);

		} catch (DuplicateEntryException ex) {
			LOGGER.warn(
					"Duplicate entry tried to be inserted for election public key with electoralAuthorityId: {}, electionEventId: {}, and tenantId: {}.",
					electoralAuthorityId, electionEventId, tenantId, ex);
		}
		return Response.ok().build();
	}

	// Validate parameters.
	private void validateParameters(String tenantId, String electionEventId, String electoralAuthorityId) throws ApplicationException {
		if (tenantId == null || tenantId.isEmpty()) {
			throw new ApplicationException(ApplicationExceptionMessages.EXCEPTION_MESSAGE_QUERY_PARAMETER_IS_NULL, RESOURCE_NAME,
					ErrorCodes.MISSING_QUERY_PARAMETER, QUERY_PARAMETER_TENANT_ID);
		}

		if (electionEventId == null || electionEventId.isEmpty()) {
			throw new ApplicationException(ApplicationExceptionMessages.EXCEPTION_MESSAGE_QUERY_PARAMETER_IS_NULL, RESOURCE_NAME,
					ErrorCodes.MISSING_QUERY_PARAMETER, QUERY_PARAMETER_ELECTION_EVENT_ID);
		}

		if (electoralAuthorityId == null || electoralAuthorityId.isEmpty()) {
			throw new ApplicationException(ApplicationExceptionMessages.EXCEPTION_MESSAGE_QUERY_PARAMETER_IS_NULL, RESOURCE_NAME,
					ErrorCodes.MISSING_QUERY_PARAMETER, QUERY_PARAMETER_ELECTORAL_AUTHORITY_ID);
		}
	}
}
