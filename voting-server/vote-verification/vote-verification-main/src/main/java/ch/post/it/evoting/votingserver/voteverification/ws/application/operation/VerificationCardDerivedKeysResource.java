/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.ws.application.operation;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.newBufferedReader;
import static java.text.MessageFormat.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.security.PublicKey;
import java.security.cert.Certificate;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.MappingIterator;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.model.certificate.CertificateEntity;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationExceptionMessages;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.DuplicateEntryException;
import ch.post.it.evoting.votingserver.commons.infrastructure.config.InfrastructureConfig;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RetrofitException;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.service.RemoteCertificateService;
import ch.post.it.evoting.votingserver.commons.infrastructure.transaction.TransactionController;
import ch.post.it.evoting.votingserver.commons.infrastructure.transaction.TransactionalAction;
import ch.post.it.evoting.votingserver.commons.infrastructure.transaction.TransactionalActionException;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdInstance;
import ch.post.it.evoting.votingserver.commons.ui.Constants;
import ch.post.it.evoting.votingserver.commons.ui.ws.rs.persistence.ErrorCodes;
import ch.post.it.evoting.votingserver.commons.verify.CSVVerifier;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.VerificationDerivedKeys;
import ch.post.it.evoting.votingserver.voteverification.infrastructure.remote.VvRemoteCertificateService;
import ch.post.it.evoting.votingserver.voteverification.service.VerificationDerivedKeysService;

@Path(VerificationCardDerivedKeysResource.RESOURCE_PATH)
public class VerificationCardDerivedKeysResource {

	public static final String ADMINISTRATION_BOARD_CN_PREFIX = "AdministrationBoard ";
	static final String RESOURCE_PATH = "/derivedkeys";
	// The name of the resource handle by this web service.
	static final String RESOURCE_NAME = "verificationcardderivedkeys";

	// The name of the query parameter tenantId
	static final String QUERY_PARAMETER_TENANT_ID = "tenantId";

	// The name of the query parameter electionEventId
	static final String QUERY_PARAMETER_ELECTION_EVENT_ID = "electionEventId";

	// The name of the query parameter verificationCardSetId
	static final String QUERY_PARAMETER_VERIFICATION_CARD_SET_ID = "verificationCardSetId";

	static final String QUERY_PARAMETER_ADMIN_BOARD_ID = "adminBoardId";

	static final String SAVE_VERIFICATION_DERIVED_KEYS_PATH = "tenant/{tenantId}/electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}/adminboard/{adminBoardId}";

	private static final int BATCH_SIZE = Integer.parseInt(InfrastructureConfig.getEnvWithDefaultOption("synchronization.batch.size", "1000"));
	private static final char SEMICOLON_SEPARATOR = ';';
	private static final Logger LOGGER = LoggerFactory.getLogger(VerificationCardDataResource.class);
	@EJB(beanName = "VoteVerificationTransactionController")
	private TransactionController controller;
	@Inject
	@VvRemoteCertificateService
	private RemoteCertificateService remoteCertificateService;
	@Inject
	private VerificationDerivedKeysService verificationDerivedKeysService;
	@Inject
	private TrackIdInstance trackIdInstance;

	private static void validateParameter(final String value, final String parameter) throws ApplicationException {
		if (value == null || value.isEmpty()) {
			throw new ApplicationException(ApplicationExceptionMessages.EXCEPTION_MESSAGE_QUERY_PARAMETER_IS_NULL, RESOURCE_NAME,
					ErrorCodes.MISSING_QUERY_PARAMETER, parameter);
		}
	}

	private static void validateParameters(final String tenantId, final String electionEventId, final String verificationCardSetId,
			final String adminBoardId) throws ApplicationException {
		validateParameter(tenantId, QUERY_PARAMETER_TENANT_ID);
		validateParameter(electionEventId, QUERY_PARAMETER_ELECTION_EVENT_ID);
		validateParameter(verificationCardSetId, QUERY_PARAMETER_VERIFICATION_CARD_SET_ID);
		validateParameter(adminBoardId, QUERY_PARAMETER_ADMIN_BOARD_ID);
	}

	/**
	 * Saves verification derived keys signed by the admin board (to be used later in proof
	 * verification phase)
	 *
	 * @param trackingId
	 * @param tenantId
	 * @param electionEventId
	 * @param verificationCardSetId
	 * @param adminBoardId
	 * @param data                  csv content with verificationCardId and derivedkeys in each line + signature
	 * @param request
	 * @return ok if signature valid and saved, precondition failed if signature is invalid
	 * @throws ApplicationException
	 * @throws IOException
	 */
	@POST
	@Path(SAVE_VERIFICATION_DERIVED_KEYS_PATH)
	@Consumes("text/csv")
	public Response saveVerificationDerivedKeys(
			@HeaderParam(Constants.PARAMETER_X_REQUEST_ID)
					String trackingId,
			@PathParam(QUERY_PARAMETER_TENANT_ID)
			final String tenantId,
			@PathParam(QUERY_PARAMETER_ELECTION_EVENT_ID)
			final String electionEventId,
			@PathParam(QUERY_PARAMETER_VERIFICATION_CARD_SET_ID)
			final String verificationCardSetId,
			@PathParam(QUERY_PARAMETER_ADMIN_BOARD_ID)
			final String adminBoardId,
			@NotNull
			final InputStream data,
			@Context
			final HttpServletRequest request) throws ApplicationException, IOException {

		trackIdInstance.setTrackId(trackingId);

		Response.Status status;
		LOGGER.info("Saving verification card derived keys for verification card set id: {}, electionEventId: {}, and tenantId: {}.",
				verificationCardSetId, electionEventId, tenantId);
		validateParameters(tenantId, electionEventId, verificationCardSetId, adminBoardId);

		java.nio.file.Path file = createTemporaryFile(data);
		try {
			if (verifyAndRemoveSignature(adminBoardId, file)) {
				saveVerificationCardDerivedKeys(tenantId, electionEventId, file);
				status = Status.OK;
				LOGGER.info("Verification card derived keys for verification card set id: {}, electionEventId: {}, and tenantId: {} saved.",
						verificationCardSetId, electionEventId, tenantId);
			} else {
				status = Status.PRECONDITION_FAILED;
			}
		} finally {
			deleteTemporaryFile(file);
		}
		return Response.status(status).build();
	}

	private java.nio.file.Path createTemporaryFile(final InputStream data) throws IOException {
		java.nio.file.Path file = createTempFile("VerificationCardDerivedKeys", ".csv");
		try {
			copy(data, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			deleteTemporaryFile(file);
			throw e;
		}
		return file;
	}

	private void deleteTemporaryFile(final java.nio.file.Path file) {
		try {
			delete(file);
		} catch (IOException e) {
			LOGGER.warn(format("Failed to delete temporary file ''{0}''.", file), e);
		}
	}

	private PublicKey getAdminBoardPublicKey(final String adminBoardId) throws GeneralCryptoLibException, RetrofitException {
		LOGGER.info("Fetching the administration board certificate");
		String name = ADMINISTRATION_BOARD_CN_PREFIX + adminBoardId;
		CertificateEntity entity = remoteCertificateService.getAdminBoardCertificate(name);
		String content = entity.getCertificateContent();
		Certificate certificate = PemUtils.certificateFromPem(content);
		return certificate.getPublicKey();
	}

	private void saveVerificationCardDerivedKeys(final String tenantId, final String electionEventId, final java.nio.file.Path file)
			throws IOException {
		try (Reader reader = newBufferedReader(file, StandardCharsets.UTF_8);
				MappingIterator<VerificationDerivedKeys> iterator = ObjectMappers
						.readCsv(reader, VerificationDerivedKeys.class, SEMICOLON_SEPARATOR, "verificationCardId", "ccodeDerivedKeyCommitment",
								"bckDerivedExpCommitment")) {
			TransactionalAction<Void> action = context -> saveVerificationCardDerivedKeysBatch(tenantId, electionEventId, iterator, BATCH_SIZE);
			while (iterator.hasNext()) {
				controller.doInNewTransaction(action);
			}
		} catch (TransactionalActionException e) {
			throw new EJBException(e);
		}
	}

	private Void saveVerificationCardDerivedKeysBatch(final String tenantId, final String electionEventId,
			final MappingIterator<VerificationDerivedKeys> iterator, final int batchSize) throws TransactionalActionException {
		for (int i = 0; i < batchSize && iterator.hasNext(); i++) {
			VerificationDerivedKeys verificationCardIdAndDerivedKeys = iterator.next();
			verificationCardIdAndDerivedKeys.setTenantId(tenantId);
			verificationCardIdAndDerivedKeys.setElectionEventId(electionEventId);
			try {
				verificationDerivedKeysService.save(verificationCardIdAndDerivedKeys);
			} catch (DuplicateEntryException e) {
				throw new TransactionalActionException(e);
			}
		}
		return null;
	}

	private boolean verifyAndRemoveSignature(final String adminBoardId, final java.nio.file.Path file) throws IOException {
		boolean valid = false;
		CSVVerifier verifier = new CSVVerifier();
		try {
			PublicKey key = getAdminBoardPublicKey(adminBoardId);
			valid = verifier.verify(key, file);
		} catch (GeneralCryptoLibException | RetrofitException e) {
			LOGGER.error("Verification card derived keys signature could not be verified", e);
		}
		return valid;
	}

}
