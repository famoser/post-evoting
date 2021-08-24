/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static ch.post.it.evoting.cryptolib.commons.validations.Validate.validateUUID;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.newDirectoryStream;
import static java.nio.file.Files.newOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.function.BooleanSupplier;

import javax.json.JsonObject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.commons.serialization.JsonSignatureService;
import ch.post.it.evoting.domain.election.VerificationCardSetData;
import ch.post.it.evoting.domain.election.VoteVerificationContextData;
import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.domain.common.SignedObject;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.generator.DataGeneratorResponse;
import ch.post.it.evoting.sdm.domain.model.status.InvalidStatusTransitionException;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.domain.model.status.SynchronizeStatus;
import ch.post.it.evoting.sdm.domain.service.BallotBoxDataGeneratorService;
import ch.post.it.evoting.sdm.domain.service.BallotDataGeneratorService;
import ch.post.it.evoting.sdm.domain.service.VotingCardSetDataGeneratorService;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.infrastructure.cc.PayloadStorageException;
import ch.post.it.evoting.sdm.infrastructure.cc.ReturnCodeGenerationRequestPayloadRepository;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;
import ch.post.it.evoting.sdm.utils.ConfigObjectMapper;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * This is an application service that manages voting card sets.
 */
@Service
public class VotingCardSetService extends BaseVotingCardSetService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VotingCardSetService.class);
	private static final String TWO_PARAMETERS_LOGGER_STRING = "{} {}";

	@Autowired
	IdleStatusService idleStatusService;

	@Autowired
	private BallotBoxRepository ballotBoxRepository;

	@Autowired
	private BallotBoxDataGeneratorService ballotBoxDataGeneratorService;

	@Autowired
	private BallotDataGeneratorService ballotDataGeneratorService;

	@Autowired
	private VotingCardSetDataGeneratorService votingCardSetDataGeneratorService;

	@Autowired
	private ConfigurationEntityStatusService configurationEntityStatusService;

	@Autowired
	private SignatureService signatureService;

	@Autowired
	private ExtendedAuthenticationService extendedAuthenticationService;

	@Autowired
	private VotingCardSetChoiceCodesService votingCardSetChoiceCodesService;

	@Autowired
	private ReturnCodeGenerationRequestPayloadRepository returnCodeGenerationRequestPayloadRepository;

	private static boolean isNodeContributions(Path file) {
		String name = file.getFileName().toString();
		return name.startsWith(Constants.CONFIG_FILE_NAME_NODE_CONTRIBUTIONS) && name.endsWith(Constants.JSON);
	}

	private static void signJSONs(final PrivateKey privateKey, final Path voteVerificationPath) throws IOException {

		ConfigObjectMapper mapper = new ConfigObjectMapper();

		Path verificationCardSetPath = voteVerificationPath.resolve(Constants.CONFIG_FILE_NAME_VERIFICATIONSET_DATA).toAbsolutePath();

		Path signedVerificationCardSetPath = voteVerificationPath.resolve(Constants.CONFIG_FILE_NAME_SIGNED_VERIFICATIONSET_DATA).toAbsolutePath();

		VerificationCardSetData verificationCardSetData = mapper
				.fromJSONFileToJava(new File(verificationCardSetPath.toString()), VerificationCardSetData.class);

		LOGGER.info("Signing verification card set data");
		String signedVerificationCardSetData = JsonSignatureService.sign(privateKey, verificationCardSetData);
		SignedObject signedSignedVerificationCardSetDataObject = new SignedObject();
		signedSignedVerificationCardSetDataObject.setSignature(signedVerificationCardSetData);
		mapper.fromJavaToJSONFile(signedSignedVerificationCardSetDataObject, new File(signedVerificationCardSetPath.toString()));

		Path verificationContextPath = voteVerificationPath.resolve(Constants.CONFIG_FILE_NAME_VERIFICATION_CONTEXT_DATA);

		Path signedVerificationContextPath = voteVerificationPath.resolve(Constants.CONFIG_FILE_NAME_SIGNED_VERIFICATION_CONTEXT_DATA);

		VoteVerificationContextData voteVerificationContextData = mapper
				.fromJSONFileToJava(new File(verificationContextPath.toString()), VoteVerificationContextData.class);

		LOGGER.info("Signing vote verification context data");
		String signedVoteVerificationContextData = JsonSignatureService.sign(privateKey, voteVerificationContextData);
		SignedObject signedSignedVoteVerificationContextDataObject = new SignedObject();
		signedSignedVoteVerificationContextDataObject.setSignature(signedVoteVerificationContextData);
		mapper.fromJavaToJSONFile(signedSignedVoteVerificationContextDataObject, new File(signedVerificationContextPath.toString()));
	}

	private static boolean evaluateUploadResult(final BooleanSupplier arg) {
		return arg.getAsBoolean();
	}

	/**
	 * Download the computed values for a votingCardSet
	 *
	 * @throws InvalidStatusTransitionException if the original status does not allow the download
	 */
	public void download(String votingCardSetId, String electionEventId)
			throws ResourceNotFoundException, InvalidStatusTransitionException, IOException {

		if (!idleStatusService.getIdLock(votingCardSetId)) {
			return;
		}

		try {
			Status fromStatus = Status.COMPUTED;
			Status toStatus = Status.VCS_DOWNLOADED;

			checkVotingCardSetStatusTransition(electionEventId, votingCardSetId, fromStatus, toStatus);

			JsonObject votingCardSetJson = votingCardSetRepository.getVotingCardSetJson(electionEventId, votingCardSetId);
			String verificationCardSetId = getVerificationCardSetId(votingCardSetJson);

			deleteNodeContributions(electionEventId, verificationCardSetId);

			int chunkCount;
			try {
				chunkCount = returnCodeGenerationRequestPayloadRepository.getCount(electionEventId, verificationCardSetId);
			} catch (PayloadStorageException e) {
				throw new IllegalStateException("Failed to get the chunk count.", e);
			}

			for (int i = 0; i < chunkCount; i++) {
				try (InputStream contributions = votingCardSetChoiceCodesService.download(electionEventId, verificationCardSetId, i)) {
					writeNodeContributions(electionEventId, verificationCardSetId, i, contributions);
				}
			}

			configurationEntityStatusService.update(toStatus.name(), votingCardSetId, votingCardSetRepository);

		} finally {
			idleStatusService.freeIdLock(votingCardSetId);
		}

	}

	private void deleteNodeContributions(String electionEventId, String verificationCardSetId) throws IOException {
		Path folder = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR).resolve(electionEventId).resolve(Constants.CONFIG_DIR_NAME_ONLINE)
				.resolve(Constants.CONFIG_DIR_NAME_VOTERVERIFICATION).resolve(verificationCardSetId);
		Filter<Path> filter = VotingCardSetService::isNodeContributions;
		try (DirectoryStream<Path> files = newDirectoryStream(folder, filter)) {
			for (Path file : files) {
				delete(file);
			}
		}
	}

	private void writeNodeContributions(String electionEventId, String verificationCardSetId, int chunkId, InputStream contributions)
			throws IOException {
		String fileName = Constants.CONFIG_FILE_NAME_NODE_CONTRIBUTIONS + "." + chunkId + Constants.JSON;
		Path file = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR).resolve(electionEventId).resolve(Constants.CONFIG_DIR_NAME_ONLINE)
				.resolve(Constants.CONFIG_DIR_NAME_VOTERVERIFICATION).resolve(verificationCardSetId).resolve(fileName);

		try (OutputStream stream = newOutputStream(file)) {
			IOUtils.copy(contributions, stream);
		}
	}

	/**
	 * Generates the voting card set data based on the given votingCardSetId. The generation contains 3 steps: generate the ballot box data, generate
	 * the ballot file and finally the voting card set data.
	 *
	 * @param electionEventId The id of the election event.
	 * @param votingCardSetId The id of the voting card set for which the data is generated.
	 * @return a DataGeneratorResponse containing information about the result of the generation.
	 * @throws InvalidStatusTransitionException if the original status does not allow the generation
	 */
	public DataGeneratorResponse generate(final String votingCardSetId, final String electionEventId)
			throws IOException, ResourceNotFoundException, InvalidStatusTransitionException {

		if (!idleStatusService.getIdLock(votingCardSetId)) {
			return new DataGeneratorResponse();
		}

		try {

			Status fromStatus = Status.VCS_DOWNLOADED;
			Status toStatus = Status.GENERATED;
			DataGeneratorResponse result;

			checkVotingCardSetStatusTransition(electionEventId, votingCardSetId, fromStatus, toStatus);

			validateUUID(votingCardSetId);
			validateUUID(electionEventId);

			// generate the needed data (encryption parameters file, ballot box, ballot)
			result = generateDataNeeded(votingCardSetId, electionEventId);
			if (!result.isSuccessful()) {
				return result;
			}

			// generate voting card set data
			result = votingCardSetDataGeneratorService.generate(votingCardSetId, electionEventId);
			return result;

		} finally {
			idleStatusService.freeIdLock(votingCardSetId);
		}
	}

	// Generates the needed data like ballot box and ballot.
	private DataGeneratorResponse generateDataNeeded(final String votingCardSetId, final String electionEventId) throws IOException {
		DataGeneratorResponse result = new DataGeneratorResponse();

		// get voting card set from repository
		String ballotBoxId = votingCardSetRepository.getBallotBoxId(votingCardSetId);
		if (StringUtils.isEmpty(ballotBoxId)) {
			result.setSuccessful(false);
			return result;
		}

		// get ballot box from repository
		String ballotId = ballotBoxRepository.getBallotId(ballotBoxId);
		if (StringUtils.isEmpty(ballotId)) {
			result.setSuccessful(false);
			return result;
		}

		// generate ballot data
		result = ballotDataGeneratorService.generate(ballotId, electionEventId);
		if (!result.isSuccessful()) {
			return result;
		}

		// generate ballot box data if it is not already done
		// The status locked means that it is not generated yet
		String ballotBoxAsJson = ballotBoxRepository.find(ballotBoxId);
		String ballotBoxStatus = JsonUtils.getJsonObject(ballotBoxAsJson).getString(JsonConstants.STATUS);
		if (Status.LOCKED.name().equals(ballotBoxStatus)) {
			result = ballotBoxDataGeneratorService.generate(ballotBoxId, electionEventId);
			if (!result.isSuccessful()) {
				return result;
			}
			configurationEntityStatusService.update(Status.READY.name(), ballotBoxId, ballotBoxRepository);
		}
		return result;
	}

	/**
	 * Change the state of the voting card set from generated to SIGNED for a given election event and voting card set id.
	 *
	 * @param electionEventId the election event id.
	 * @param votingCardSetId the voting card set id.
	 * @param privateKeyPEM
	 * @return true if the status is successfully changed to signed. Otherwise, false.
	 * @throws ResourceNotFoundException if the voting card set is not found.
	 */
	public boolean sign(final String electionEventId, final String votingCardSetId, final String privateKeyPEM)
			throws ResourceNotFoundException, GeneralCryptoLibException, IOException {

		boolean result = false;

		JsonObject votingCardSetJson = votingCardSetRepository.getVotingCardSetJson(electionEventId, votingCardSetId);

		if (votingCardSetJson != null && votingCardSetJson.containsKey(JsonConstants.STATUS)) {
			String status = votingCardSetJson.getString(JsonConstants.STATUS);
			if (Status.GENERATED.name().equals(status)) {

				PrivateKey privateKey = PemUtils.privateKeyFromPem(privateKeyPEM);

				String verificationCardSetId = getVerificationCardSetId(votingCardSetJson);

				LOGGER.info("Signing voting card set {}", votingCardSetId);
				LOGGER.info("Signing voter material configuration");
				signVoterMaterial(electionEventId, votingCardSetId, privateKey);
				LOGGER.info("Signing verification card set {}", verificationCardSetId);
				LOGGER.info("Signing vote verification configuration");
				signVoteVerification(electionEventId, verificationCardSetId, votingCardSetId, privateKey);
				LOGGER.info("Signing the extended authentication");
				signExtendedAuthentication(electionEventId, votingCardSetId, verificationCardSetId, privateKey);
				LOGGER.info("Signing the printing");
				signPrinting(electionEventId, votingCardSetId, privateKey);
				LOGGER.info("Changing the status of the voting card set");
				configurationEntityStatusService
						.updateWithSynchronizedStatus(Status.SIGNED.name(), votingCardSetId, votingCardSetRepository, SynchronizeStatus.PENDING);
				result = true;
			}
		}

		return result;
	}

	private void signVoteVerification(final String electionEventId, final String verificationCardSetId, final String votingCardSetId,
			final PrivateKey privateKey) throws IOException {

		Path voteVerificationPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR).resolve(electionEventId)
				.resolve(Constants.CONFIG_DIR_NAME_ONLINE).resolve(Constants.CONFIG_DIR_NAME_VOTERVERIFICATION).resolve(verificationCardSetId);

		boolean correctSigning = signAllVoteVerificationCSVFiles(privateKey, voteVerificationPath);

		if (!correctSigning) {
			LOGGER.error("An error occurred while signing the verification card set, rolling back to its original state");
			signatureService.deleteSignaturesFromCSVs(voteVerificationPath);

			Path voterMaterialPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR).resolve(electionEventId)
					.resolve(Constants.CONFIG_DIR_NAME_ONLINE).resolve(Constants.CONFIG_DIR_NAME_VOTERMATERIAL).resolve(votingCardSetId);

			signatureService.deleteSignaturesFromCSVs(voterMaterialPath);
		} else {
			signJSONs(privateKey, voteVerificationPath);
		}

	}

	private void signExtendedAuthentication(final String electionEventId, final String votingCardSetId, final String verificationCardSetId,
			final PrivateKey privateKey) throws IOException {

		boolean signatureResult = extendedAuthenticationService.signExtendedAuthentication(electionEventId, votingCardSetId, privateKey);

		if (!signatureResult) {

			LOGGER.error("An error occurred while signing the extended authentication , rolling back to its original state");
			Path voteVerificationPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR).resolve(electionEventId)
					.resolve(Constants.CONFIG_DIR_NAME_ONLINE).resolve(Constants.CONFIG_DIR_NAME_VOTERVERIFICATION).resolve(verificationCardSetId);
			signatureService.deleteSignaturesFromCSVs(voteVerificationPath);

			Path voterMaterialPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR).resolve(electionEventId)
					.resolve(Constants.CONFIG_DIR_NAME_ONLINE).resolve(Constants.CONFIG_DIR_NAME_VOTERMATERIAL).resolve(votingCardSetId);

			signatureService.deleteSignaturesFromCSVs(voterMaterialPath);
		}

	}

	private boolean signAllVoteVerificationCSVFiles(final PrivateKey privateKey, final Path voteVerificationPath) {
		return evaluateUploadResult(() -> {
			long numberFailed;
			try {
				numberFailed = Files.walk(voteVerificationPath, 1).filter(csvPath -> {
					boolean failed = false;
					try {
						String name = csvPath.getFileName().toString();
						if ((name.startsWith(Constants.CONFIG_FILE_NAME_CODES_MAPPING) && name.endsWith(Constants.CSV)) || (
								name.startsWith(Constants.CONFIG_FILE_VERIFICATION_CARD_DATA) && name.endsWith(Constants.CSV)) || (
								name.startsWith(Constants.CONFIG_FILE_NAME_DERIVED_KEYS) && name.endsWith(Constants.CSV))) {

							LOGGER.info(TWO_PARAMETERS_LOGGER_STRING, Constants.SIGNING_FILE, name);
							String signatureB64 = signatureService.signCSV(privateKey, csvPath.toFile());
							LOGGER.info(Constants.SAVING_SIGNATURE);
							signatureService.saveCSVSignature(signatureB64, csvPath);
						}
					} catch (IOException | GeneralCryptoLibException e) {
						LOGGER.warn("Error trying to sign All Vote Verification CSV Files.", e);
						failed = true;
					}
					return failed;
				}).count();
			} catch (IOException e) {
				LOGGER.warn("Error trying to sign All Vote Verification CSV Files.", e);
				return Boolean.FALSE;
			}
			return numberFailed == 0;
		});
	}

	private void signVoterMaterial(final String electionEventId, final String votingCardSetId, final PrivateKey privateKey) throws IOException {

		Path voterMaterialPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR).resolve(electionEventId)
				.resolve(Constants.CONFIG_DIR_NAME_ONLINE).resolve(Constants.CONFIG_DIR_NAME_VOTERMATERIAL).resolve(votingCardSetId);

		boolean correctSigning = signAllVoterMaterialCSVFiles(privateKey, voterMaterialPath);

		if (!correctSigning) {
			LOGGER.error("An error occurred while signing the voting card set, rolling back to its original state");
			signatureService.deleteSignaturesFromCSVs(voterMaterialPath);
		}

	}

	private boolean signAllVoterMaterialCSVFiles(final PrivateKey privateKey, final Path voterMaterialPath) {
		return evaluateUploadResult(() -> {
			long numberFailed;
			try {
				numberFailed = Files.walk(voterMaterialPath, 1).filter(csvPath -> {
					boolean failed = false;
					try {
						String name = csvPath.getFileName().toString();
						if ((name.startsWith(Constants.CONFIG_FILE_NAME_VOTER_INFORMATION) && name.endsWith(Constants.CSV)) || (
								name.startsWith(Constants.CONFIG_FILE_NAME_CREDENTIAL_DATA) && name.endsWith(Constants.CSV))) {

							LOGGER.info(TWO_PARAMETERS_LOGGER_STRING, Constants.SIGNING_FILE, name);
							String signatureB64 = signatureService.signCSV(privateKey, csvPath.toFile());
							LOGGER.info(Constants.SAVING_SIGNATURE);
							signatureService.saveCSVSignature(signatureB64, csvPath);
						}
					} catch (IOException | GeneralCryptoLibException e) {
						LOGGER.warn("Error trying to sign All Vote Material CSV Files.", e);
						failed = true;
					}
					return failed;
				}).count();
			} catch (IOException e) {
				LOGGER.warn("Error trying to sign All Vote Material CSV Files.", e);
				return Boolean.FALSE;
			}
			return numberFailed == 0;
		});
	}

	private void signPrinting(final String electionEventId, final String votingCardSetId, final PrivateKey privateKey) throws IOException {

		Path printingPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR).resolve(electionEventId).resolve(Constants.CONFIG_DIR_NAME_ONLINE)
				.resolve(Constants.CONFIG_DIR_NAME_PRINTING).resolve(votingCardSetId);

		boolean correctSigning = signAllPrintingDataCSVFiles(privateKey, printingPath);

		if (!correctSigning) {
			LOGGER.error("An error occurred while signing the printing data, rolling back to its original state");
			signatureService.deleteSignaturesFromCSVs(printingPath);
		}

	}

	private boolean signAllPrintingDataCSVFiles(final PrivateKey privateKey, final Path voteVerificationPath) {
		return evaluateUploadResult(() -> {
			long numberFailed;
			try {
				numberFailed = Files.walk(voteVerificationPath, 1).filter(csvPath -> {
					boolean failed = false;
					try {
						String name = csvPath.getFileName().toString();
						if ((name.startsWith(Constants.PRINTING_DATA) && name.endsWith(Constants.CSV))) {
							LOGGER.info(TWO_PARAMETERS_LOGGER_STRING, Constants.SIGNING_FILE, name);
							String signatureB64 = signatureService.signCSV(privateKey, csvPath.toFile());
							LOGGER.info(Constants.SAVING_SIGNATURE);
							signatureService.saveCSVSignature(signatureB64, csvPath);
						}
					} catch (IOException | GeneralCryptoLibException e) {
						LOGGER.warn("Error trying to sign All Printing Data CSV Files.", e);
						failed = true;
					}
					return failed;
				}).count();
			} catch (IOException e) {
				LOGGER.warn("Error trying to sign All Printing Data CSV Files.", e);
				return Boolean.FALSE;
			}
			return numberFailed == 0;
		});
	}

	/**
	 * Gets the verification card set identifier corresponding to a voting card set.
	 *
	 * @param votingCardSetJson the voting card set Json object
	 * @return the verification card set identifier
	 */
	protected String getVerificationCardSetId(JsonObject votingCardSetJson) {

		return votingCardSetJson.getString(JsonConstants.VERIFICATION_CARD_SET_ID);
	}
}
