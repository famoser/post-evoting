/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newDirectoryStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.cms.CMSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.stores.StoresServiceAPI;
import ch.post.it.evoting.cryptolib.api.stores.bean.KeyStoreType;
import ch.post.it.evoting.cryptolib.cmssigner.CMSSigner;
import ch.post.it.evoting.cryptolib.elgamal.bean.VerifiableElGamalEncryptionParameters;
import ch.post.it.evoting.domain.election.model.ballotbox.BallotBoxIdImpl;
import ch.post.it.evoting.sdm.application.exception.ConsistencyCheckException;
import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.EntityRepository;
import ch.post.it.evoting.sdm.domain.model.administrationauthority.AdministrationAuthorityRepository;
import ch.post.it.evoting.sdm.domain.model.ballot.BallotRepository;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.ballottext.BallotTextRepository;
import ch.post.it.evoting.sdm.domain.model.electionevent.ElectionEventRepository;
import ch.post.it.evoting.sdm.domain.model.electoralauthority.ElectoralAuthorityRepository;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.domain.model.votingcardset.VotingCardSetRepository;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.utils.JsonUtils;

@Service
public class ExportImportService {
	public static final int MAX_DEPTH = 1;

	public static final String SDM = "sdm_";

	public static final String MANY_CHARS_REGEX = "(.*)";

	public static final String BALLOT_BOX_ID_PLACEHOLDER = "%s";

	public static final String PAYLOAD_REGEX_FORMAT =
			new BallotBoxIdImpl(MANY_CHARS_REGEX, MANY_CHARS_REGEX, BALLOT_BOX_ID_PLACEHOLDER) + MANY_CHARS_REGEX;

	private static final Logger LOGGER = LoggerFactory.getLogger(ExportImportService.class);

	private static final CopyOption[] COPY_OPTIONS = { StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES };

	@Autowired
	private ElectionEventRepository electionEventRepository;

	@Autowired
	private BallotBoxRepository ballotBoxRepository;

	@Autowired
	private BallotRepository ballotRepository;

	@Autowired
	private BallotTextRepository ballotTextRepository;

	@Autowired
	private VotingCardSetRepository votingCardSetRepository;

	@Autowired
	private ElectoralAuthorityRepository electoralAuthorityRepository;

	@Autowired
	private AdministrationAuthorityRepository administrationAuthorityRepository;

	@Autowired
	private PathResolver pathResolver;

	@Autowired
	@Qualifier(value = "absolutePath")
	private PathResolver absolutePathResolver;

	@Autowired
	private HashService hashService;

	@Autowired
	private StoresServiceAPI storesService;

	@Autowired
	private ConsistencyCheckService consistencyCheckService;

	@Autowired
	private SignaturesVerifierService signaturesVerifierService;

	@Value("${tenantID}")
	private String tenantId;

	private static boolean isNodeContributions(Path file) {
		String name = file.getFileName().toString();
		return name.startsWith(Constants.CONFIG_FILE_NAME_NODE_CONTRIBUTIONS) && name.endsWith(Constants.JSON);
	}

	private static boolean isChoiceCodeGenerationPayload(Path file) {
		String name = file.getFileName().toString();
		return name.startsWith(Constants.CONFIG_FILE_NAME_PREFIX_CHOICE_CODE_GENERATION_REQUEST_PAYLOAD) && name
				.endsWith(Constants.CONFIG_FILE_NAME_SUFFIX_CHOICE_CODE_GENERATION_REQUEST_PAYLOAD);
	}

	public void dumpDatabase(final String eeid) throws ResourceNotFoundException {
		JsonObjectBuilder dumpJsonBuilder = Json.createObjectBuilder();

		String adminBoards = administrationAuthorityRepository.list();
		dumpJsonBuilder.add(JsonConstants.ADMINISTRATION_AUTHORITIES, JsonUtils.getJsonObject(adminBoards).getJsonArray(JsonConstants.RESULT));

		String electionEvent = electionEventRepository.find(eeid);

		if (StringUtils.isEmpty(electionEvent) || JsonConstants.EMPTY_OBJECT.equals(electionEvent)) {
			throw new ResourceNotFoundException("Election Event not found");
		}

		JsonArray electionEvents = Json.createArrayBuilder().add(JsonUtils.getJsonObject(electionEvent)).build();
		dumpJsonBuilder.add(JsonConstants.ELECTION_EVENTS, electionEvents);

		String ballots = ballotRepository.listByElectionEvent(eeid);
		JsonArray ballotsArray = JsonUtils.getJsonObject(ballots).getJsonArray(JsonConstants.RESULT);
		dumpJsonBuilder.add(JsonConstants.BALLOTS, ballotsArray);

		JsonArrayBuilder ballotBoxTextsArrayBuilder = Json.createArrayBuilder();
		for (JsonValue ballotValue : ballotsArray) {
			JsonObject ballotObject = (JsonObject) ballotValue;
			String id = ballotObject.getString(JsonConstants.ID);
			String ballotTexts = ballotTextRepository.list(Collections.singletonMap(JsonConstants.BALLOT_ID, id));
			JsonArray ballotTextsForBallot = JsonUtils.getJsonObject(ballotTexts).getJsonArray(JsonConstants.RESULT);
			for (JsonValue ballotText : ballotTextsForBallot) {
				ballotBoxTextsArrayBuilder.add(ballotText);
			}
		}
		dumpJsonBuilder.add(JsonConstants.TEXTS, ballotBoxTextsArrayBuilder.build());

		String ballotBoxes = ballotBoxRepository.listByElectionEvent(eeid);
		dumpJsonBuilder.add(JsonConstants.BALLOT_BOXES, JsonUtils.getJsonObject(ballotBoxes).getJsonArray(JsonConstants.RESULT));

		String votingCardSets = votingCardSetRepository.listByElectionEvent(eeid);
		dumpJsonBuilder.add(JsonConstants.VOTING_CARD_SETS, JsonUtils.getJsonObject(votingCardSets).getJsonArray(JsonConstants.RESULT));

		String electoralAuthorities = electoralAuthorityRepository.listByElectionEvent(eeid);
		dumpJsonBuilder.add(JsonConstants.ELECTORAL_AUTHORITIES, JsonUtils.getJsonObject(electoralAuthorities).getJsonArray(JsonConstants.RESULT));

		Path dumpPath = getPathOfDumpDatabase();

		try {
			Files.write(dumpPath, dumpJsonBuilder.build().toString().getBytes(StandardCharsets.UTF_8));

			LOGGER.info("Database export to dump file has been completed successfully: {}", dumpPath);
		} catch (IOException e) {
			LOGGER.error("An error occurred writing DB dump to: {}", dumpPath, e);
		}

	}

	public void importDatabase() throws IOException, CertificateException, ConsistencyCheckException, GeneralCryptoLibException, CMSException {

		Path dumpPath = getPathOfDumpDatabase();
		if (!Files.exists(dumpPath)) {
			LOGGER.warn("There is no dump database to import");
			return;
		}

		String dump;
		try {
			dump = new String(Files.readAllBytes(dumpPath), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IOException("Error reading import file ", e);
		}

		JsonObject dumpJson = JsonUtils.getJsonObject(dump);
		JsonArray adminBoards = dumpJson.getJsonArray(JsonConstants.ADMINISTRATION_AUTHORITIES);
		for (JsonValue adminBoard : adminBoards) {
			saveOrUpdate(adminBoard, administrationAuthorityRepository);
		}

		JsonArray electionEvents = dumpJson.getJsonArray(JsonConstants.ELECTION_EVENTS);
		for (JsonValue electionEvent : electionEvents) {
			checkSignaturesAndConsistency(electionEvent);
			saveOrUpdate(electionEvent, electionEventRepository);
		}

		JsonArray ballots = dumpJson.getJsonArray(JsonConstants.BALLOTS);
		for (JsonValue ballot : ballots) {
			saveOrUpdate(ballot, ballotRepository);
		}

		JsonArray ballotTexts = dumpJson.getJsonArray(JsonConstants.TEXTS);
		for (JsonValue ballotText : ballotTexts) {
			saveOrUpdate(ballotText, ballotTextRepository);
		}

		JsonArray ballotBoxes = dumpJson.getJsonArray(JsonConstants.BALLOT_BOXES);
		for (JsonValue ballotBox : ballotBoxes) {
			saveOrUpdate(ballotBox, ballotBoxRepository);
		}

		JsonArray votingCardSets = dumpJson.getJsonArray(JsonConstants.VOTING_CARD_SETS);
		for (JsonValue votingCardSet : votingCardSets) {
			saveOrUpdate(votingCardSet, votingCardSetRepository);
		}

		JsonArray electoralAuthorities = dumpJson.getJsonArray(JsonConstants.ELECTORAL_AUTHORITIES);
		for (JsonValue electoralAuthority : electoralAuthorities) {
			saveOrUpdate(electoralAuthority, electoralAuthorityRepository);
		}

	}

	private void saveOrUpdate(final JsonValue entity, final EntityRepository repository) {
		JsonObject entityObject = (JsonObject) entity;
		String id = entityObject.getString(JsonConstants.ID);
		String foundEntityString = repository.find(id);
		JsonObject foundEntityObject = JsonUtils.getJsonObject(foundEntityString);

		if (foundEntityObject.isEmpty()) {

			repository.save(entity.toString());
		} else if (!foundEntityObject.containsKey(JsonConstants.STATUS) || !entityObject.containsKey(JsonConstants.STATUS)) {

			repository.update(entity.toString());
		} else {

			try {

				String entityStatus = entityObject.getString(JsonConstants.STATUS);
				Status entityStatusEnumValue = Enum.valueOf(Status.class, entityStatus);
				String foundEntityStatus = foundEntityObject.getString(JsonConstants.STATUS);
				Status foundEntityStatusEnumValue = Enum.valueOf(Status.class, foundEntityStatus);

				if (foundEntityStatusEnumValue.isBefore(entityStatusEnumValue)) {
					repository.delete(id);
					repository.save(entity.toString());
				} else {
					LOGGER.warn("Entity {} can't be updated", id);
				}
			} catch (IllegalArgumentException e) {
				LOGGER.error("Not supported entity status found. You might need a new version of this tool that supports such type.", e);
			}
		}
	}

	/**
	 * This method verifies the signature of the prime numbers and encryption parameters files and then check the consistency of the encryption
	 * parameters. The consistency check of the prime numbers is done a the configuration level in offline SDM where primes will be trusted.
	 *
	 * @param entity
	 * @throws CertificateException
	 * @throws GeneralCryptoLibException
	 * @throws CMSException
	 * @throws IOException
	 * @throws ConsistencyCheckException
	 */
	private void checkSignaturesAndConsistency(JsonValue entity)
			throws CertificateException, GeneralCryptoLibException, CMSException, IOException, ConsistencyCheckException {
		JsonObject eventObject = (JsonObject) entity;
		String eeid = eventObject.getString(JsonConstants.ID);

		// Get encryption params
		JsonObject encryptionParameters = eventObject.getJsonObject(JsonConstants.SETTINGS).getJsonObject(JsonConstants.ENCRYPTION_PARAMETERS);

		// Verify jwt with that trusted chain and check consistency between jwt and encryption params
		VerifiableElGamalEncryptionParameters verifiedParams = signaturesVerifierService.verifyEncryptionParams(eeid);

		if (!consistencyCheckService.encryptionParamsConsistent(encryptionParameters, verifiedParams.getGroup())) {
			throw new ConsistencyCheckException("Encryption parameters consistency check between election event data and signed jwt failed.");
		}
	}

	/**
	 * Export Election Event Data only, without voting cards nor customer specific data
	 *
	 * @param usbDrive usb drive
	 * @param eeId     election event id
	 * @param eeAlias  election event alias
	 * @throws IOException
	 */
	public void exportElectionEventWithoutElectionInformation(String usbDrive, String eeId, String eeAlias) throws IOException {

		Path sdmFolder = pathResolver.resolve(Constants.SDM_DIR_NAME);
		Path usbSdmFolder = absolutePathResolver.resolve(usbDrive, SDM + eeAlias);

		Path configFolder = sdmFolder.resolve(Constants.CONFIG_DIR_NAME);
		Path toConfigFolder = usbSdmFolder.resolve(Constants.CONFIG_DIR_NAME);

		Set<String> files = new HashSet<>();
		files.add(Constants.SDM_CONFIG_DIR_NAME);
		files.add(Constants.CONFIG_FILE_NAME_ELECTIONS_CONFIG_JSON); // Copy
		files.add(Constants.CONFIG_FILE_NAME_ELECTIONS_CONFIG_JSON_SIGN);// elections_config.json
		// and signature

		files.add(eeId);
		files.add(Constants.CONFIG_DIR_NAME);
		files.add(Constants.CONFIG_FILE_NAME_ENCRYPTION_PARAMETERS_JSON);// Copy
		// encryptionParameters.json
		files.add(Constants.CONFIG_DIR_NAME_OFFLINE);
		files.add(Constants.CONFIG_FILE_NAME_AUTHORITIESCA_PEM);// Copy
		// authoritiesca.pem
		// file
		files.add(Constants.DBDUMP_FILE_NAME);// Copy db_dump.json file
		files.add(Constants.DBDUMP_SIGNATURE_FILE_NAME);// Copy db_dump.json file signature
		files.add(Constants.CONFIG_FILE_NAME_PLATFORM_ROOT_CA); // platformRootCA.pem file
		files.add(String.format(Constants.CONFIG_FILE_NAME_TENANT_CA_PATTERN, tenantId)); // tenantCA-<tenant
		// ID>-CA.pem
		// file
		Filter<Path> filter = file -> files.contains(file.getFileName().toString());
		copyFolder(sdmFolder, usbSdmFolder, filter, false);

		// Copy all csr folder
		filter = file -> true;
		Path csrFolder = configFolder.resolve(Constants.CSR_FOLDER);
		Path csrUSBFolder = toConfigFolder.resolve(Constants.CSR_FOLDER);
		copyFolder(csrFolder, csrUSBFolder, filter, false);

		// Copy ONLINE folder without voterMaterial, voteVerification ,
		// electionInformation and printing
		Set<String> exceptions = new HashSet<>();
		exceptions.add(Constants.CONFIG_DIR_NAME_VOTERMATERIAL);
		exceptions.add(Constants.CONFIG_DIR_NAME_VOTERVERIFICATION);
		exceptions.add(Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION);
		exceptions.add(Constants.CONFIG_DIR_NAME_PRINTING);
		filter = file -> !exceptions.contains(file.getFileName().toString());

		Path onlineFolder = configFolder.resolve(eeId).resolve(Constants.CONFIG_DIR_NAME_ONLINE);
		Path onlineUSBFolder = toConfigFolder.resolve(eeId).resolve(Constants.CONFIG_DIR_NAME_ONLINE);
		copyFolder(onlineFolder, onlineUSBFolder, filter, false);

		Set<String> requiredFiles = new HashSet<>();
		requiredFiles.add(Constants.CONFIG_FILE_NAME_VOTER_INFORMATION + Constants.CSV);
		requiredFiles.add(Constants.CONFIG_FILE_NAME_VOTER_INFORMATION + Constants.CSV + Constants.SIGN);
		requiredFiles.add(Constants.CONFIG_FILE_NAME_VERIFICATIONSET_DATA);
		requiredFiles.add(Constants.CONFIG_FILE_NAME_VERIFICATIONSET_DATA + Constants.SIGN);
		filter = file -> Files.isDirectory(file) || requiredFiles.contains(file.getFileName().toString());

		// Copy voterInformation.csv
		Path voterMaterialFolder = configFolder.resolve(eeId).resolve(Constants.CONFIG_DIR_NAME_ONLINE)
				.resolve(Constants.CONFIG_DIR_NAME_VOTERMATERIAL);
		Path voterMaterialUsbFolder = toConfigFolder.resolve(eeId).resolve(Constants.CONFIG_DIR_NAME_ONLINE)
				.resolve(Constants.CONFIG_DIR_NAME_VOTERMATERIAL);
		copyFolder(voterMaterialFolder, voterMaterialUsbFolder, filter, false);

		// Copy verificationCardSetData.json
		Path voteVerificationFolder = configFolder.resolve(eeId).resolve(Constants.CONFIG_DIR_NAME_ONLINE)
				.resolve(Constants.CONFIG_DIR_NAME_VOTERVERIFICATION);
		Path voteVerificationUsbFolder = toConfigFolder.resolve(eeId).resolve(Constants.CONFIG_DIR_NAME_ONLINE)
				.resolve(Constants.CONFIG_DIR_NAME_VOTERVERIFICATION);
		copyFolder(voteVerificationFolder, voteVerificationUsbFolder, filter, false);

	}

	/**
	 * Exports the files of the election information context for a given election event
	 *
	 * @param usbDrive
	 * @param eeId
	 * @param eeAlias
	 * @throws IOException
	 */
	public void exportElectionEventElectionInformation(String usbDrive, String eeId, String eeAlias) throws IOException {
		Path sdmFolder = pathResolver.resolve(Constants.SDM_DIR_NAME);
		Path usbSdmFolder = absolutePathResolver.resolve(usbDrive, SDM + eeAlias);

		Path configFolder = sdmFolder.resolve(Constants.CONFIG_DIR_NAME);
		Path toConfigFolder = usbSdmFolder.resolve(Constants.CONFIG_DIR_NAME);
		Path electionInformationFolder = configFolder.resolve(eeId).resolve(Constants.CONFIG_DIR_NAME_ONLINE)
				.resolve(Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION);
		Path electionInformationUsbFolder = toConfigFolder.resolve(eeId).resolve(Constants.CONFIG_DIR_NAME_ONLINE)
				.resolve(Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION);
		if (Files.exists(electionInformationFolder)) {
			copyFolder(electionInformationFolder, electionInformationUsbFolder, getElectionInformationFilter(electionInformationFolder), true);
		} else {
			LOGGER.warn("No files have been found in the election information folder");
		}
	}

	/**
	 * Export Voting Cards only
	 *
	 * @param usbDrive usb drive
	 * @param eeId     election event id
	 * @param eeAlias  election event alias
	 * @throws IOException
	 */
	public void exportVotingCards(String usbDrive, String eeId, String eeAlias) throws IOException {

		Path sdmOnlineFolder = pathResolver.resolve(Constants.SDM_DIR_NAME, Constants.CONFIG_DIR_NAME, eeId, Constants.CONFIG_DIR_NAME_ONLINE);
		Path usbOnlineFolder = absolutePathResolver
				.resolve(usbDrive, SDM + eeAlias, Constants.CONFIG_DIR_NAME, eeId, Constants.CONFIG_DIR_NAME_ONLINE);

		// Copy voterMaterial folder
		Set<String> exceptions = new HashSet<>();
		exceptions.add(Constants.CONFIG_FILE_NAME_VOTER_INFORMATION + Constants.CSV);
		exceptions.add(Constants.CONFIG_FILE_NAME_VOTER_INFORMATION + Constants.CSV + Constants.SIGN);
		exceptions.add(Constants.CONFIG_FILE_NAME_VERIFICATIONSET_DATA);
		exceptions.add(Constants.CONFIG_FILE_NAME_VERIFICATIONSET_DATA + Constants.SIGN);
		Filter<Path> filter = file -> !exceptions.contains(file.getFileName().toString());
		Path voterMaterialFolder = sdmOnlineFolder.resolve(Constants.CONFIG_DIR_NAME_VOTERMATERIAL);
		Path usbVoterMaterialFolder = usbOnlineFolder.resolve(Constants.CONFIG_DIR_NAME_VOTERMATERIAL);
		copyFolder(voterMaterialFolder, usbVoterMaterialFolder, filter, false);

		// Copy voteVerification folder
		Path voteVerification = sdmOnlineFolder.resolve(Constants.CONFIG_DIR_NAME_VOTERVERIFICATION);
		Path usbVoteVerificationFolder = usbOnlineFolder.resolve(Constants.CONFIG_DIR_NAME_VOTERVERIFICATION);
		copyFolder(voteVerification, usbVoteVerificationFolder, filter, false);
	}

	/**
	 * Export customer specific data only
	 *
	 * @param usbDrive usb drive
	 * @param eeId     election event id
	 * @param eeAlias  election event alias
	 * @throws IOException
	 */
	public void exportCustomerSpecificData(String usbDrive, String eeId, String eeAlias) throws IOException {

		Filter<Path> filter = file -> true;
		Path customerFolder = pathResolver.resolve(Constants.SDM_DIR_NAME, Constants.CONFIG_DIR_NAME, eeId, Constants.CONFIG_DIR_NAME_CUSTOMER);
		Path usbCustomerFolder = absolutePathResolver
				.resolve(usbDrive, SDM + eeAlias, Constants.CONFIG_DIR_NAME, eeId, Constants.CONFIG_DIR_NAME_CUSTOMER);

		copyFolder(customerFolder, usbCustomerFolder, filter, false);
	}

	/**
	 * Import all files from selected exported election event to user/sdm
	 *
	 * @param usbElectionPath path to selected exported election event
	 * @throws IOException
	 */
	public void importData(String usbElectionPath) throws IOException {
		Filter<Path> filter = file -> true;
		Path usbFolder = absolutePathResolver.resolve(usbElectionPath);
		Path sdmFolder = pathResolver.resolve(Constants.SDM_DIR_NAME);

		copyFolder(usbFolder, sdmFolder, filter, false);
	}

	private void copyFolder(Path source, Path dest, Filter<Path> filter, boolean isExportingElectionInformationFolders) throws IOException {
		if (!Files.exists(source)) {
			return;
		}

		if (Files.isDirectory(source)) {
			if (!Files.exists(dest)) {
				Files.createDirectories(dest);
				LOGGER.info("Directory created from {} to {}", source, dest);
			}
			Filter<Path> electionInformationFilter = filter;
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(source, electionInformationFilter)) {
				for (Path file : stream) {
					if (isExportingElectionInformationFolders) {
						electionInformationFilter = getElectionInformationFilter(file);
					}
					copyFolder(file, dest.resolve(file.getFileName()), electionInformationFilter, isExportingElectionInformationFolders);
				}
			}
		} else {
			try {

				copy(source, dest, COPY_OPTIONS);

				LOGGER.info("File copied from {} to {}", source, dest);
			} catch (IOException e) {
				LOGGER.error("Error copying files from {} to {}", source, dest, e);
			}
		}
	}

	private Filter<Path> getElectionInformationFilter(Path source) throws IOException {
		/*
		 * The decompressed votes will be present only when the ballot box is decrypted. Once that stage
		 * is reached, the decrypted contents must not be exported.
		 */
		/*
		 * This includes legacy tally scenario.
		 */
		if (isFilePresent("decompressedVotes.csv", source)) {
			return file -> false;
		}
		/*
		 * The downloaded ballot box containing the raw ballot box information will be present with the
		 * rest of the audit files (cleansing outputs, previous to last control components mixing
		 * payloads) when the control component payload containing the mixed ballot box is downloaded.
		 */
		/*
		 * This includes legacy offline cleansing and mixing scenarios.
		 */
		if (isFilePresent("downloadedBallotBox.csv", source)) {
			String ballotBoxId = source.getFileName().toString();
			// tenantId-(.*)-ballotBoxId(.*)
			String payloadRegex = String.format(PAYLOAD_REGEX_FORMAT, ballotBoxId);
			String regex = "^ballotBox(.*)|^downloadedBallotBox(.*)|^failedVotes(.*)|^successfulVotes(.*)|^" + payloadRegex;
			return file -> file.getFileName().toString().matches(regex);
		}

		/*
		 * The default scenario is the pre-electoral scenario, so we want to export all the
		 * configuration data.
		 */
		String regex = "^ballotBox(.*)|^ballot.json$|^electionInformationContents.json(.*)";
		return file -> Files.isDirectory(file) || file.getFileName().toString().matches(regex);
	}

	/**
	 * @param fileName
	 * @param source
	 * @return
	 * @throws IOException
	 */
	public boolean isFilePresent(String fileName, Path source) throws IOException {
		try (Stream<Path> walk = Files.walk(source, MAX_DEPTH)) {
			return walk.anyMatch(path -> path.getFileName().toString().equals(fileName));
		}
	}

	/**
	 * Exports computed choice codes for given USB drive, election event and election event alias.
	 *
	 * @param usbDrive        the USB drive
	 * @param electionEventId the election event identifier
	 * @param eeAlias         the election event alias
	 * @throws IOException I/O error occurred.
	 */
	public void exportComputedChoiceCodes(String usbDrive, String electionEventId, String eeAlias) throws IOException {
		Path sourceFolder = pathResolver.resolve(Constants.SDM_DIR_NAME, Constants.CONFIG_DIR_NAME, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE,
				Constants.CONFIG_DIR_NAME_VOTERVERIFICATION);
		Path destinationFolder = absolutePathResolver
				.resolve(usbDrive, SDM + eeAlias, Constants.CONFIG_DIR_NAME, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE,
						Constants.CONFIG_DIR_NAME_VOTERVERIFICATION);

		String json = votingCardSetRepository.listByElectionEvent(electionEventId);
		JsonObject object = JsonUtils.getJsonObject(json);
		JsonArray array = object.getJsonArray(JsonConstants.RESULT);
		for (JsonValue value : array) {
			JsonObject votingCardSet = (JsonObject) value;
			JsonString attribute = votingCardSet.getJsonString(JsonConstants.STATUS);
			Status status = Status.valueOf(attribute.getString());
			if (status == Status.COMPUTED || status.isBefore(Status.COMPUTED)) {
				// computation results have not been downloaded yet.
				continue;
			}
			attribute = votingCardSet.getJsonString(JsonConstants.VERIFICATION_CARD_SET_ID);
			String verificationCardSetId = attribute.getString();
			Path verificationCardSetFolder = sourceFolder.resolve(verificationCardSetId);
			Filter<Path> filter = ExportImportService::isNodeContributions;
			try (DirectoryStream<Path> files = newDirectoryStream(verificationCardSetFolder, filter)) {
				for (Path source : files) {
					Path path = sourceFolder.relativize(source);
					Path destination = destinationFolder.resolve(path);
					createDirectories(destination.getParent());
					copy(source, destination, COPY_OPTIONS);
				}
			}
		}
	}

	/**
	 * Exports pre-computed choice codes for given USB drive, election event and election event alias.
	 *
	 * @param usbDrive        the USB drive
	 * @param electionEventId the election event identifier
	 * @param eeAlias         the election event alias
	 * @throws IOException I/O error occurred.
	 */
	public void exportPreComputedChoiceCodes(String usbDrive, String electionEventId, String eeAlias) throws IOException {
		Path sourceFolder = pathResolver.resolve(Constants.SDM_DIR_NAME, Constants.CONFIG_DIR_NAME, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE,
				Constants.CONFIG_DIR_NAME_VOTERVERIFICATION);
		Path destinationFolder = absolutePathResolver
				.resolve(usbDrive, SDM + eeAlias, Constants.CONFIG_DIR_NAME, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE,
						Constants.CONFIG_DIR_NAME_VOTERVERIFICATION);

		String json = votingCardSetRepository.listByElectionEvent(electionEventId);
		JsonObject object = JsonUtils.getJsonObject(json);
		JsonArray array = object.getJsonArray(JsonConstants.RESULT);
		for (JsonValue value : array) {
			JsonObject votingCardSet = (JsonObject) value;
			JsonString attribute = votingCardSet.getJsonString(JsonConstants.STATUS);
			Status status = Status.valueOf(attribute.getString());
			if (status.isBefore(Status.PRECOMPUTED)) {
				// pre-computation has not been done yet.
				continue;
			}
			attribute = votingCardSet.getJsonString(JsonConstants.VERIFICATION_CARD_SET_ID);
			String verificationCardSetId = attribute.getString();
			Path verificationCardSetFolder = sourceFolder.resolve(verificationCardSetId);
			Filter<Path> filter = ExportImportService::isChoiceCodeGenerationPayload;
			try (DirectoryStream<Path> files = newDirectoryStream(verificationCardSetFolder, filter)) {
				for (Path source : files) {
					Path path = sourceFolder.relativize(source);
					Path destination = destinationFolder.resolve(path);
					createDirectories(destination.getParent());
					copy(source, destination, COPY_OPTIONS);
				}
			}
		}
	}

	/**
	 * Exports ballot boxes for given USB drive, election event and election event alias.
	 *
	 * @param usbDrive        the USB drive
	 * @param electionEventId the election event identifier
	 * @param eeAlias         the election event alias
	 * @throws IOException I/O error occurred.
	 */
	public void exportBallotBoxes(String usbDrive, String electionEventId, String eeAlias) throws IOException {
		Path sourceFolder = pathResolver.resolve(Constants.SDM_DIR_NAME, Constants.CONFIG_DIR_NAME, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE,
				Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION, Constants.CONFIG_DIR_NAME_BALLOTS);
		Path destinationFolder = absolutePathResolver
				.resolve(usbDrive, SDM + eeAlias, Constants.CONFIG_DIR_NAME, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE,
						Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION, Constants.CONFIG_DIR_NAME_BALLOTS);
		copyFolder(sourceFolder, destinationFolder, p -> true, false);
	}

	/**
	 * Verifies the signature of both db dump and elections config files.
	 *
	 * @throws IOException
	 * @throws CMSException
	 * @throws CertificateException
	 * @throws GeneralCryptoLibException
	 */
	public void verifySignaturesOnImport() throws IOException, CMSException, CertificateException, GeneralCryptoLibException {
		signaturesVerifierService.verifyPkcs7(getPathOfDumpDatabase(), getPathOfDumpDatabaseSignature());
		signaturesVerifierService.verifyPkcs7(getPathOfElectionsConfig(), getPathOfElectionsConfigSignature());
	}

	/**
	 * Signs database dump and elections config files under the CMS PKCS7 standard.
	 *
	 * @param password
	 * @throws IOException  I/O error occurred
	 * @throws CMSException Signing file in P7 format error
	 */
	public void signDumpDatabaseAndElectionsConfig(final char[] password)
			throws IOException, CMSException, GeneralCryptoLibException, KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
		KeyStore keyStore = getOnlineKeyStore(password);
		Enumeration<String> keyAliases = keyStore.aliases();
		String keyAlias = keyAliases.nextElement();
		if (keyAliases.hasMoreElements()) {
			throw new IllegalArgumentException("There should be exactly one private key in the keystore");
		}
		PrivateKey signingKey = (PrivateKey) keyStore.getKey(keyAlias, password);
		List<Certificate> chainAsList = new ArrayList<>(Arrays.asList(keyStore.getCertificateChain(keyAlias)));
		Certificate signerCert = chainAsList.remove(0);

		// Sign Database dump
		Path dumpDatabasePath = getPathOfDumpDatabase();
		signFile(dumpDatabasePath, signingKey, chainAsList, signerCert);

		// Sign elections_config.json file
		Path electionsConfig = getPathOfElectionsConfig();
		signFile(electionsConfig, signingKey, chainAsList, signerCert);
	}

	private KeyStore getOnlineKeyStore(char[] password) throws IOException, GeneralCryptoLibException {
		Path keyStoreOnlinePath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, Constants.INTEGRATION_KEYSTORE_ONLINE_FILE);
		try (InputStream in = new FileInputStream(keyStoreOnlinePath.toFile())) {
			return storesService.loadKeyStore(KeyStoreType.PKCS12, in, password);
		}
	}

	private void signFile(Path filePath, PrivateKey signingKey, List<Certificate> chain, Certificate signerCert) throws IOException, CMSException {

		Path signedFilePath = filePath.resolveSibling(filePath.getFileName() + CMSSigner.SIGNATURE_FILE_EXTENSION);

		CMSSigner.sign(filePath.toFile(), signedFilePath.toFile(), signerCert, chain, signingKey);
	}

	private Path getPathOfDumpDatabase() {
		return pathResolver.resolve(Constants.SDM_DIR_NAME, Constants.DBDUMP_FILE_NAME);
	}

	private Path getPathOfDumpDatabaseSignature() {
		return pathResolver.resolve(Constants.SDM_DIR_NAME, Constants.DBDUMP_SIGNATURE_FILE_NAME);
	}

	private Path getPathOfElectionsConfig() {
		return pathResolver.resolve(Constants.SDM_DIR_NAME, Constants.SDM_CONFIG_DIR_NAME, Constants.CONFIG_FILE_NAME_ELECTIONS_CONFIG_JSON);
	}

	private Path getPathOfElectionsConfigSignature() {
		return pathResolver.resolve(Constants.SDM_DIR_NAME, Constants.SDM_CONFIG_DIR_NAME, Constants.CONFIG_FILE_NAME_ELECTIONS_CONFIG_JSON_SIGN);
	}
}
