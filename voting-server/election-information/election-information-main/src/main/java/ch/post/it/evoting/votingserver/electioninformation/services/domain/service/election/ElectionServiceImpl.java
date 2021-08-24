/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.election;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.io.output.CloseShieldOutputStream;
import org.bouncycastle.cms.CMSSignedDataStreamGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.CryptographicOperationException;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.cms.CMSService;
import ch.post.it.evoting.votingserver.commons.crypto.PrivateKeyForObjectRepository;
import ch.post.it.evoting.votingserver.commons.util.JsonUtils;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxContentRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxInformation;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxInformationRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.castcode.VoteCastCodeRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.validation.ElectionValidationRequest;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.votingcard.VotingCardWriter;

/**
 * Handles operations on election.
 */
@Stateless
public class ElectionServiceImpl implements ElectionService {
	private static final String JSON_PARAMETER_DATE_FROM = "startDate";

	private static final String JSON_PARAMETER_DATE_TO = "endDate";

	private static final String GRACE_PERIOD = "gracePeriod";

	private static final String KEYSTORE_PK_ALIAS = "privatekey";

	private static final String BALLOT_BOX_CERT_FIELD = "ballotBoxCert";
	private static final Logger LOGGER = LoggerFactory.getLogger(ElectionServiceImpl.class);
	@Inject
	private CMSService cmsService;
	@Inject
	private BallotBoxInformationRepository ballotBoxInformationRepository;
	@Inject
	private PrivateKeyForObjectRepository privateKeyRepository;

	@EJB
	private VoteCastCodeRepository voteCastCodeRepository;

	@EJB
	private BallotBoxContentRepository ballotBoxContentRepository;

	@Inject
	private BallotBoxRepository ballotBoxRepository;

	/**
	 * Validates if the election is open
	 *
	 * @param request a request object including the ids and whether the grace period has to be
	 *                applied or not
	 * @return
	 */
	@Override
	public ValidationError validateIfElectionIsOpen(final ElectionValidationRequest request) {
		ValidationError result = new ValidationError();
		BallotBoxInformation info = null;
		try {
			info = ballotBoxInformationRepository
					.findByTenantIdElectionEventIdBallotBoxId(request.getTenantId(), request.getElectionEventId(), request.getBallotBoxId());
		} catch (ResourceNotFoundException e) {
			LOGGER.error("Failed to validate is election is open.", e);
			return result;
		}

		result.setValidationErrorType(ValidationErrorType.SUCCESS);
		JsonObject json = JsonUtils.getJsonObject(info.getJson());
		String dateFromString = json.getString(JSON_PARAMETER_DATE_FROM);
		String dateToString = json.getString(JSON_PARAMETER_DATE_TO);

		ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
		ZonedDateTime dateTo = ZonedDateTime.parse(dateToString, DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC));
		ZonedDateTime dateFrom = ZonedDateTime.parse(dateFromString, DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC));
		DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);
		int gracePeriodInSeconds = Integer.parseInt(json.getString(GRACE_PERIOD));
		boolean isElectionOverDate = request.isValidatedWithGracePeriod() ?
				isElectionOverDateWithGracePeriod(now, dateTo, gracePeriodInSeconds) :
				isElectionOverDate(now, dateTo);
		if (isElectionOverDate) {
			result.setValidationErrorType(ValidationErrorType.ELECTION_OVER_DATE);
			String dateError = request.isValidatedWithGracePeriod() ?
					dateTo.plusSeconds(gracePeriodInSeconds).format(formatter) :
					dateTo.format(formatter);
			String[] errorArgs = { dateError };
			result.setErrorArgs(errorArgs);
		} else if (hasNotElectionStarted(now, dateFrom)) {
			result.setValidationErrorType(ValidationErrorType.ELECTION_NOT_STARTED);
			String[] errorArgs = { dateFrom.format(formatter) };
			result.setErrorArgs(errorArgs);
		}
		return result;
	}

	@Override
	public void writeCastVotes(final String tenantId, final String electionEventId, final String filenamePrefix, final OutputStream stream)
			throws IOException {
		try (ZipOutputStream zip = new ZipOutputStream(new CloseShieldOutputStream(stream), StandardCharsets.UTF_8)) {
			zip.putNextEntry(new ZipEntry(filenamePrefix + ".csv"));
			ByteArrayOutputStream signature = new ByteArrayOutputStream();
			try {
				CMSSignedDataStreamGenerator generator = newCMSSignedDataStreamGenerator(tenantId, electionEventId);
				try (VotingCardWriter writer = new VotingCardWriter(generator.open(signature, false, new CloseShieldOutputStream(zip)))) {
					voteCastCodeRepository.findAndWriteCastVotingCards(tenantId, electionEventId, writer);
				}
			} finally {
				signature.close();
			}
			zip.putNextEntry(new ZipEntry(filenamePrefix + ".p7"));
			zip.write(signature.toByteArray());
		}
	}

	@Override
	public void writeVerifiedVotes(final String tenantId, final String electionEventId, final String filenamePrefix, final OutputStream stream)
			throws IOException {
		try (ZipOutputStream zip = new ZipOutputStream(new CloseShieldOutputStream(stream), StandardCharsets.UTF_8)) {
			zip.putNextEntry(new ZipEntry(filenamePrefix + ".csv"));
			ByteArrayOutputStream signature = new ByteArrayOutputStream();
			try {
				CMSSignedDataStreamGenerator generator = newCMSSignedDataStreamGenerator(tenantId, electionEventId);
				try (VotingCardWriter writer = new VotingCardWriter(generator.open(signature, false, new CloseShieldOutputStream(zip)))) {
					ballotBoxRepository.findAndWriteUsedVotingCards(tenantId, electionEventId, writer);
				}
			} finally {
				signature.close();
			}
			zip.putNextEntry(new ZipEntry(filenamePrefix + ".p7"));
			zip.write(signature.toByteArray());
		}
	}

	private CMSSignedDataStreamGenerator newCMSSignedDataStreamGenerator(final String tenantId, final String electionEventId) throws IOException {
		CMSSignedDataStreamGenerator generator;
		try {
			String ballotBoxId = ballotBoxContentRepository.findFirstBallotBoxForElection(tenantId, electionEventId);
			PrivateKey key = privateKeyRepository.findByTenantEEIDObjectIdAlias(tenantId, electionEventId, ballotBoxId, KEYSTORE_PK_ALIAS);
			BallotBoxInformation info = ballotBoxInformationRepository
					.findByTenantIdElectionEventIdBallotBoxId(tenantId, electionEventId, ballotBoxId);
			JsonObject object = JsonUtils.getJsonObject(info.getJson());
			String pem = object.getString(BALLOT_BOX_CERT_FIELD);
			X509Certificate certificate;
			certificate = (X509Certificate) PemUtils.certificateFromPem(pem);
			generator = cmsService.newCMSSignedDataStreamGenerator(key, certificate);
		} catch (ResourceNotFoundException | CryptographicOperationException | GeneralCryptoLibException | GeneralSecurityException e) {
			throw new IOException("Failed to create PKCS#7 signature generator.", e);
		}
		return generator;
	}

	private boolean isElectionOverDate(final ZonedDateTime now, final ZonedDateTime dateTo) {
		return now.isAfter(dateTo);
	}

	private boolean isElectionOverDateWithGracePeriod(final ZonedDateTime now, final ZonedDateTime dateTo, final int gracePeriodInSeconds) {
		return now.isAfter(dateTo.plusSeconds(gracePeriodInSeconds));
	}

	private boolean hasNotElectionStarted(final ZonedDateTime now, final ZonedDateTime dateFrom) {
		return now.isBefore(dateFrom);
	}
}
