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
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
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
import ch.post.it.evoting.votingserver.electioninformation.services.domain.common.SignedObject;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionPublicKey;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionPublicKeyRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.remote.EiRemoteCertificateService;

/**
 * Web service for handling electoral data resource.
 */
@Path("/electoraldata")
@Stateless(name = "ei-ElectoralDataResource")
public class ElectoralDataResource {

	public static final String ADMINISTRATION_BOARD_CN_PREFIX = "AdministrationBoard ";

	// The name of the query parameter electoralAuthorityId
	private static final String QUERY_PARAMETER_ELECTORAL_AUTHORITY_ID = "electoralAuthorityId";

	private static final String CONSTANT_ELECTION_PUBLIC_KEY = "electionPublicKey";

	private static final String CONSTANT_SIGNATURE = "signature";

	private static final String QUERY_PARAMETER_ADMIN_BOARD_ID = "adminBoardId";

	// The name of the resource handle by this web service.
	private static final String RESOURCE_NAME = "electoraldata";

	// The name of the query parameter tenantId
	private static final String QUERY_PARAMETER_TENANT_ID = "tenantId";

	// The name of the query parameter electionEventId
	private static final String QUERY_PARAMETER_ELECTION_EVENT_ID = "electionEventId";

	// The name of the query parameter ballotBoxId
	private static final String QUERY_PARAMETER_BALLOT_BOX_ID = "ballotBoxId";
	private static final Logger LOGGER = LoggerFactory.getLogger(ElectoralDataResource.class);
	// An instance of the election public key repository
	@EJB
	private ElectionPublicKeyRepository electionPublicKeyRepository;

	@Inject
	@EiRemoteCertificateService
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
	@Path("tenant/{tenantId}/electionevent/{electionEventId}/electoralauthority/{electoralAuthorityId}/adminboard/{adminBoardId}")
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
					HttpServletRequest request) throws ApplicationException, ResourceNotFoundException, IOException {

		// set the track id to be logged
		trackIdInstance.setTrackId(trackingId);

		LOGGER.info("Saving election public key information with electoralAuthorityId: {}, electionEventId: {}, and tenantId: {}.",
				electoralAuthorityId, electionEventId, tenantId);

		// validate parameters
		validateParameters(tenantId, electionEventId, electoralAuthorityId);

		LOGGER.info("Fetching the administration board certificate");
		String adminBoardCommonName = ADMINISTRATION_BOARD_CN_PREFIX + adminBoardId;

		CertificateEntity adminBoardCertificateEntity = remoteCertificateService.getAdminBoardCertificate(adminBoardCommonName);
		String adminBoardCertPEM = adminBoardCertificateEntity.getCertificateContent();
		Certificate adminBoardCert;
		try {
			adminBoardCert = PemUtils.certificateFromPem(adminBoardCertPEM);
		} catch (GeneralCryptoLibException e) {
			LOGGER.error("An error occurred while fetching the administration board certificate", e);
			return Response.status(Response.Status.PRECONDITION_FAILED).build();
		}
		PublicKey adminBoardPublicKey = adminBoardCert.getPublicKey();

		ElectionPublicKey electionPublicKey = new ElectionPublicKey();
		electionPublicKey.setElectionEventId(electionEventId);
		electionPublicKey.setTenantId(tenantId);
		electionPublicKey.setElectoralAuthorityId(electoralAuthorityId);

		JsonObject inputJsonObject = JsonUtils.getJsonObject(inputJson);
		String electionPublicKeyJSON = inputJsonObject.getJsonObject(CONSTANT_ELECTION_PUBLIC_KEY).toString();
		SignedObject signedObject = ObjectMappers.fromJson(electionPublicKeyJSON, SignedObject.class);
		String signature = signedObject.getSignature();

		ch.post.it.evoting.domain.election.ElectionPublicKey electionPublicKeyObj;
		try {
			LOGGER.info("Verifying election public key signature");
			electionPublicKeyObj = JsonSignatureService
					.verify(adminBoardPublicKey, signature, ch.post.it.evoting.domain.election.ElectionPublicKey.class);
			LOGGER.info("Election public key signature was successfully verified");
		} catch (Exception e) {
			LOGGER.error("Election public key signature could not be verified", e);
			return Response.status(Response.Status.PRECONDITION_FAILED).build();
		}

		try {

			JsonObjectBuilder electionPublicKeyJSONBuilder = JsonUtils
					.jsonObjectToBuilder(JsonUtils.getJsonObject(ObjectMappers.toJson(electionPublicKeyObj)));
			electionPublicKeyJSONBuilder.add(CONSTANT_SIGNATURE, signature);
			String electionPKJSON = electionPublicKeyJSONBuilder.build().toString();
			electionPublicKey.setJson(electionPKJSON);
			electionPublicKeyRepository.save(electionPublicKey);

			LOGGER.info("Election public key information with electoralAuthorityId: {}, electionEventId: {}, and tenantId: {} saved.",
					electoralAuthorityId, electionEventId, tenantId);
		} catch (DuplicateEntryException ex) {
			LOGGER.warn(
					"Duplicate entry tried to be inserted for election public key with electoralAuthorityId: {}, electionEventId: {}, and tenantId: {}.",
					electoralAuthorityId, electionEventId, tenantId, ex);
		}

		return Response.ok().build();
	}

	// Validate parameters.
	private void validateParameters(String tenantId, String electionEventId, String ballotBoxId) throws ApplicationException {
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
