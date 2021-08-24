/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch;

import static ch.post.it.evoting.sdm.config.logevents.ConfigGeneratorLogEvents.GENCREDAT_ERROR_DERIVING_CREDENTIAL_ID;
import static ch.post.it.evoting.sdm.config.logevents.ConfigGeneratorLogEvents.GENSVPK_ERROR_DERIVING_KEYSTORE_SYMMETRIC_ENCRYPTION_KEY;
import static ch.post.it.evoting.sdm.config.logevents.ConfigGeneratorLogEvents.GENSVPK_ERROR_GENERATING_SVK;
import static ch.post.it.evoting.sdm.config.logevents.ConfigGeneratorLogEvents.GENSVPK_ERROR_GENERATING_VCID;
import static ch.post.it.evoting.sdm.config.logevents.ConfigGeneratorLogEvents.GENVCC_ERROR_GENERATING_LONGVOTECASTCODE;
import static ch.post.it.evoting.sdm.config.logevents.ConfigGeneratorLogEvents.GENVCC_ERROR_STORING_VOTECASTCODE;
import static java.util.Arrays.fill;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;

import ch.post.it.evoting.cryptolib.api.derivation.CryptoAPIDerivedKey;
import ch.post.it.evoting.cryptolib.api.derivation.CryptoAPIPBKDFDeriver;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.returncode.CodesMappingTableEntry;
import ch.post.it.evoting.cryptolib.returncode.VoterCodesService;
import ch.post.it.evoting.cryptoprimitives.CryptoPrimitives;
import ch.post.it.evoting.cryptoprimitives.CryptoPrimitivesService;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientMessage;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalService;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.cryptoadapters.CryptoAdapters;
import ch.post.it.evoting.domain.election.Ballot;
import ch.post.it.evoting.domain.election.helpers.ReplacementsHolder;
import ch.post.it.evoting.domain.mixnet.ObjectMapperMixnetConfig;
import ch.post.it.evoting.logging.api.domain.Level;
import ch.post.it.evoting.logging.api.domain.LogContent;
import ch.post.it.evoting.logging.api.factory.LoggingFactory;
import ch.post.it.evoting.logging.api.writer.LoggingWriter;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.commons.domain.VcIdCombinedReturnCodesGenerationValues;
import ch.post.it.evoting.sdm.config.actions.ExtendedAuthenticationService;
import ch.post.it.evoting.sdm.config.commands.voters.JobExecutionObjectContext;
import ch.post.it.evoting.sdm.config.commands.voters.VotersGenerationTaskStaticContentProvider;
import ch.post.it.evoting.sdm.config.commands.voters.VotersParametersHolder;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VerificationCardCodesDataPack;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VerificationCardCredentialDataPack;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VerificationCardSetCredentialDataPack;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VotingCardCredentialDataPack;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.generators.VerificationCardCredentialDataPackGenerator;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.generators.VotingCardCredentialDataPackGenerator;
import ch.post.it.evoting.sdm.config.exceptions.CreateVotingCardSetException;
import ch.post.it.evoting.sdm.config.exceptions.specific.GenerateCredentialIdException;
import ch.post.it.evoting.sdm.config.exceptions.specific.GenerateSVKVotingCardIdPassKeystoreException;
import ch.post.it.evoting.sdm.config.exceptions.specific.GenerateVerificationCardCodesException;
import ch.post.it.evoting.sdm.config.exceptions.specific.GenerateVotingcardIdException;
import ch.post.it.evoting.sdm.config.logevents.ConfigGeneratorLogEvents;
import ch.post.it.evoting.sdm.config.model.authentication.ExtendedAuthInformation;
import ch.post.it.evoting.sdm.config.model.authentication.StartVotingKey;
import ch.post.it.evoting.sdm.config.model.authentication.service.StartVotingKeyService;

/**
 * Generates voting and verification cards.
 */
@Service
class VotingCardGenerator implements ItemProcessor<VcIdCombinedReturnCodesGenerationValues, GeneratedVotingCardOutput> {

	private static final Logger LOGGER = LoggerFactory.getLogger(VotingCardGenerator.class);
	private static final String KEY_ALREADY_EXISTS = "Key %s already exists in codes mapping table";

	private final CryptoPrimitives cryptoPrimitives = CryptoPrimitivesService.get();
	private final ElGamalService cryptoPrimitivesElGamalService = new ElGamalService();
	private final ObjectMapper mapper = ObjectMapperMixnetConfig.getNewInstance();
	private final VotingCardGenerationJobExecutionContext jobExecutionContext;
	private final String verificationCardSetId;
	private final String votingCardSetId;
	private final int numberOfVotingCards;
	private final CryptoAPIPBKDFDeriver pbkdfDeriver;
	private final VotersParametersHolder holder;
	private final VotersGenerationTaskStaticContentProvider staticContentProvider;

	@Autowired
	LoggingFactory loggerFactory;

	private LoggingWriter logWriter;
	@Autowired
	private JobExecutionObjectContext objectCache;
	@Autowired
	private VoterCodesService voterCodesService;
	@Autowired
	private ExtendedAuthenticationService extendedAuthenticationService;
	@Autowired
	private StartVotingKeyService startVotingKeyService;
	@Autowired
	private VotingCardCredentialDataPackGenerator votingCardCredentialDataPackGenerator;
	@Autowired
	@Qualifier("verificationCardCredentialDataPackGeneratorWithJobScope")
	private VerificationCardCredentialDataPackGenerator verificationCardCredentialDataPackGeneratorWithJobScope;
	@Autowired
	private PathResolver pathResolver;

	public VotingCardGenerator(final VotersParametersHolder holder, final VotingCardGenerationJobExecutionContext jobExecutionContext,
			final CryptoAPIPBKDFDeriver pbkdfDeriver, final VotersGenerationTaskStaticContentProvider staticContentProvider) {
		this.holder = holder;
		this.jobExecutionContext = jobExecutionContext;
		this.verificationCardSetId = jobExecutionContext.getVerificationCardSetId();
		this.votingCardSetId = jobExecutionContext.getVotingCardSetId();
		this.numberOfVotingCards = jobExecutionContext.getNumberOfVotingCards();
		this.pbkdfDeriver = pbkdfDeriver;
		this.staticContentProvider = staticContentProvider;
	}

	@VisibleForTesting
	static String retrieveBallotCastingKey(final String verificationCardSetId, final String verificationCardId, final Path basePath) {

		LOGGER.info("Retrieving the ballot casting key for the base path {}, verificationCardSetId {} and verificationCardId {}.", basePath,
				verificationCardSetId, verificationCardId);

		final Path ballotCastingKeyPath = basePath.resolve(Constants.CONFIG_DIR_NAME_OFFLINE).resolve(Constants.CONFIG_BALLOT_CASTING_KEYS_DIRECTORY)
				.resolve(verificationCardSetId).resolve(verificationCardId + Constants.KEY);

		try {
			return new String(Files.readAllBytes(ballotCastingKeyPath), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException(
					String.format("Error retrieving the ballot casting key for the base path %s, verificationCardSetId %s and verificationCardId %s.",
							basePath, verificationCardSetId, verificationCardId), e);
		}

	}

	@PostConstruct
	private void initializeLogger() {
		logWriter = loggerFactory.getLogger(VotingCardGenerator.class);
	}

	@Override
	public GeneratedVotingCardOutput process(final VcIdCombinedReturnCodesGenerationValues vcIdCombinedReturnCodesGenerationValues) throws Exception {

		final String verificationCardId = vcIdCombinedReturnCodesGenerationValues.getVerificationCardId();
		final String jobInstanceId = jobExecutionContext.getJobInstanceId();
		final String electionEventId = jobExecutionContext.getElectionEventId();
		final byte[] saltCredentialId = Base64.getDecoder().decode(jobExecutionContext.getSaltCredentialId());
		final byte[] saltKeystoreSymmetricEncryptionKey = Base64.getDecoder().decode(jobExecutionContext.getSaltKeystoreSymmetricEncryptionKey());

		LOGGER.debug("Generating voting and verification cards for computed values of electionEventId {} and verificationCardId {}.", electionEventId,
				verificationCardId);

		char[] keystoreSymmetricEncryptionKey = null;
		try {

			final String startVotingKey = generateStartVotingKey(electionEventId);
			final String credentialId = generateCredentialId(pbkdfDeriver, electionEventId, saltCredentialId, startVotingKey);

			keystoreSymmetricEncryptionKey = generateKeystoreSymmetricEncryptionKey(pbkdfDeriver, electionEventId, saltKeystoreSymmetricEncryptionKey,
					startVotingKey);

			final VotingCardCredentialDataPack voterCredentialDataPack = generateVotersCredentialDataPack(keystoreSymmetricEncryptionKey,
					credentialId);

			final VerificationCardSetCredentialDataPack verificationCardSetCredentialDataPack = objectCache
					.get(jobInstanceId, VerificationCardSetCredentialDataPack.class);

			final PrivateKey verificationCardSetIssuerPrivateKey = verificationCardSetCredentialDataPack.getVerificationCardSetIssuerKeyPair()
					.getPrivate();

			final VerificationCardCredentialDataPack verificationCardCredentialDataPack = verificationCardCredentialDataPackGeneratorWithJobScope
					.generate(holder.getVerificationCardInputDataPack(), electionEventId, verificationCardId, verificationCardSetId,
							verificationCardSetIssuerPrivateKey, keystoreSymmetricEncryptionKey, holder.getAbsoluteBasePath());

			final GqGroup gqGroup = vcIdCombinedReturnCodesGenerationValues.getEncryptedPreChoiceReturnCodes().getGroup();
			final ElGamalMultiRecipientPrivateKey setupPrivateKey = getSetupPrivateKey(gqGroup);
			final int n = vcIdCombinedReturnCodesGenerationValues.getEncryptedPreChoiceReturnCodes().size();

			final ElGamalMultiRecipientPrivateKey compressedSetupSecretKey = setupPrivateKey.compress(n);

			final List<ZpGroupElement> preChoiceReturnCodes = decryptPreChoiceReturnCodes(
					vcIdCombinedReturnCodesGenerationValues.getEncryptedPreChoiceReturnCodes(), compressedSetupSecretKey);

			final List<BigInteger> encodedVotingOptions = holder.getBallot().getEncodedVotingOptions();

			if (encodedVotingOptions.size() != preChoiceReturnCodes.size()) {
				throw new IllegalStateException(String.format("The encodedVotingOptions size (%s) does not match the preChoiceReturnCodes size (%s)",
						encodedVotingOptions.size(), preChoiceReturnCodes.size()));
			}

			final Map<BigInteger, ZpGroupElement> encodedVotingOptionsToPreChoiceReturnCodes = IntStream.range(0, encodedVotingOptions.size()).boxed()
					.collect(Collectors.toMap(encodedVotingOptions::get, preChoiceReturnCodes::get));

			final String ballotCastingKey = retrieveBallotCastingKey(verificationCardSetId, verificationCardId, holder.getAbsoluteBasePath());

			final ZpGroupElement preVoteCastReturnCode = decryptPreVoteCastReturnCode(
					vcIdCombinedReturnCodesGenerationValues.getEncryptedPreVoteCastReturnCode(), compressedSetupSecretKey);

			final VerificationCardCodesDataPack verificationCardCodesDataPack = createVerificationCardCode(holder.getBallot(), verificationCardId,
					encodedVotingOptionsToPreChoiceReturnCodes, ballotCastingKey, preVoteCastReturnCode);

			final ExtendedAuthInformation extendedAuthInformation = getExtendedAuthInformation(electionEventId, startVotingKey);
			final String ballotId = jobExecutionContext.getBallotId();
			final String ballotBoxId = jobExecutionContext.getBallotBoxId();
			final String votingCardId = generateVotingCardId(electionEventId);

			return GeneratedVotingCardOutput
					.success(votingCardId, votingCardSetId, ballotId, ballotBoxId, credentialId, electionEventId, verificationCardId,
							verificationCardSetId, startVotingKey, voterCredentialDataPack, verificationCardCredentialDataPack,
							verificationCardCodesDataPack, extendedAuthInformation);

		} finally {
			if (keystoreSymmetricEncryptionKey != null) {
				fill(keystoreSymmetricEncryptionKey, ' ');
			}
		}
	}

	private ElGamalMultiRecipientPrivateKey getSetupPrivateKey(final GqGroup gqGroup) {
		final String electionEventId = jobExecutionContext.getElectionEventId();
		try {
			// Read secret key from file system.
			return mapper.reader().withAttribute("group", gqGroup).readValue(pathResolver
					.resolve(Constants.CONFIG_FILES_BASE_DIR, electionEventId, Constants.CONFIG_DIR_NAME_OFFLINE,
							Constants.SETUP_SECRET_KEY_FILE_NAME).toFile(), ElGamalMultiRecipientPrivateKey.class);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to deserialize set up secret key.", e);
		}
	}

	private List<ZpGroupElement> decryptPreChoiceReturnCodes(final ElGamalMultiRecipientCiphertext encryptedPreChoiceReturnCodes,
			final ElGamalMultiRecipientPrivateKey compressedSetupSecretKey) {

		final ElGamalMultiRecipientMessage pC_id = cryptoPrimitivesElGamalService.getMessage(encryptedPreChoiceReturnCodes, compressedSetupSecretKey);

		return pC_id.stream().map(CryptoAdapters::convert).collect(Collectors.toList());
	}

	private ZpGroupElement decryptPreVoteCastReturnCode(final ElGamalMultiRecipientCiphertext encryptedPreVoteCastReturnCode,
			ElGamalMultiRecipientPrivateKey compressedSetupSecretKey) {

		final ElGamalMultiRecipientMessage pVCC_id = cryptoPrimitivesElGamalService
				.getMessage(encryptedPreVoteCastReturnCode, compressedSetupSecretKey);

		return CryptoAdapters.convert(pVCC_id.get(0));
	}

	private ExtendedAuthInformation getExtendedAuthInformation(final String electionEventId, final String startVotingKey) {
		// generates the authentication key and derives the ID
		return extendedAuthenticationService.create(StartVotingKey.ofValue(startVotingKey), electionEventId);
	}

	private VerificationCardCodesDataPack createVerificationCardCode(final Ballot enrichedBallot, final String verificationCardId,
			final Map<BigInteger, ZpGroupElement> ballotVotingOptionToPreChoiceReturnCodes, final String ballotCastingKey,
			final ZpGroupElement preVoteCastReturnCode) {

		// Create the Vote Cast Return Code
		final String voteCastReturnCode = voterCodesService.generateShortVoteCastReturnCode();

		// Extract the list of encoded voting options
		final List<ZpGroupElement> optionRepresentations = staticContentProvider.getOptionRepresentations();
		final Map<BigInteger, List<String>> representationsWithCorrectness = staticContentProvider.getRepresentationsWithCorrectness();

		final Map<String, BigInteger> choiceCodes2BallotVotingOptionOrderedBasedOnInsertion = new LinkedHashMap<>();

		// The TreeMap shuffles the Codes Mapping Table since it sorts the Map by the hash of the long Choice Return Code or the hash of the long
		// Vote Cast Return Code.
		final Map<String, String> shuffledCodesMappingTable = new TreeMap<>();

		for (final Map.Entry<BigInteger, ZpGroupElement> entry : ballotVotingOptionToPreChoiceReturnCodes.entrySet()) {
			final String shortChoiceCode = generateShortChoiceReturnCodeForVO(choiceCodes2BallotVotingOptionOrderedBasedOnInsertion);

			// add to the map [short Choice Return Codes - encoded voting options]
			choiceCodes2BallotVotingOptionOrderedBasedOnInsertion.put(shortChoiceCode, entry.getKey());

			final byte[] shortChoiceCodeAsBytes = shortChoiceCode.getBytes(StandardCharsets.UTF_8);
			final ZpGroupElement preChoiceReturnCode = ballotVotingOptionToPreChoiceReturnCodes.get(entry.getKey());

			// create a long Choice Return Code
			final List<String> attributesWithCorrectness = representationsWithCorrectness.get(entry.getKey());
			final byte[] longChoiceCode = generateLongChoiceReturnCodeForVO(holder.getEeid(), verificationCardId, preChoiceReturnCode,
					attributesWithCorrectness);

			// encrypt the Short Choice Return Code with the long Choice Return Code and add the generated entry to the map
			final CodesMappingTableEntry codesMappingTableEntry;
			try {
				codesMappingTableEntry = voterCodesService.generateCodesMappingTableEntry(shortChoiceCodeAsBytes, longChoiceCode);
			} catch (GeneralCryptoLibException e) {
				throw new CreateVotingCardSetException("An error occurred while generating the mapping table codes.", e);
			}

			// convert key and data to Base64
			final String keyAsBase64 = Base64.getEncoder().encodeToString(codesMappingTableEntry.getKey());
			final String dataAsBase64 = Base64.getEncoder().encodeToString(codesMappingTableEntry.getData());

			putKeyValuePairInMap(enrichedBallot, verificationCardId, shuffledCodesMappingTable, keyAsBase64, dataAsBase64);
		}

		// success - pre-Choice Return Codes successfully generated
		logWriter.log(Level.DEBUG, new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENVCC_SUCCESS_PRE_CHOICECODES_GENERATED)
				.electionEvent(enrichedBallot.getElectionEvent().getId()).user(Constants.ADMIN_ID).objectId(verificationCardSetId)
				.additionalInfo(Constants.VERIFC_ID, verificationCardId).additionalInfo("num_cc", Integer.toString(optionRepresentations.size()))
				.createLogInfo());

		// success - long Choice Return Code successfully generated
		logWriter.log(Level.DEBUG, new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENVCC_SUCCESS_LONGCHOICECODES_GENERATED)
				.electionEvent(enrichedBallot.getElectionEvent().getId()).objectId(verificationCardSetId).user(Constants.ADMIN_ID)
				.additionalInfo(Constants.VERIFC_ID, verificationCardId).additionalInfo("num_cc", Integer.toString(optionRepresentations.size()))
				.createLogInfo());

		final byte[] longVoteCastReturnCode = generateLongVoteCastReturnCode(enrichedBallot, holder.getEeid(), verificationCardId,
				preVoteCastReturnCode, Collections.emptyList());

		final String keyAsBase64;
		final String dataAsBase64;
		try {
			final CodesMappingTableEntry voterCastingCodeMappingEntry = voterCodesService
					.generateCodesMappingTableEntry(voteCastReturnCode.getBytes(StandardCharsets.UTF_8), longVoteCastReturnCode);
			keyAsBase64 = Base64.getEncoder().encodeToString(voterCastingCodeMappingEntry.getKey());
			dataAsBase64 = Base64.getEncoder().encodeToString(voterCastingCodeMappingEntry.getData());

		} catch (GeneralCryptoLibException e) {
			// error storing the Vote Cast Return Code in the Codes Mapping Table
			logWriter.log(Level.ERROR, new LogContent.LogContentBuilder().logEvent(GENVCC_ERROR_STORING_VOTECASTCODE)
					.electionEvent(enrichedBallot.getElectionEvent().getId()).user(Constants.ADMIN_ID).objectId(verificationCardSetId)
					.additionalInfo(Constants.VERIFC_ID, verificationCardId).additionalInfo(Constants.ERR_DESC, e.getMessage()).createLogInfo());
			throw new GenerateVerificationCardCodesException(GENVCC_ERROR_STORING_VOTECASTCODE.getInfo(), e);
		}

		putKeyValuePairInMap(enrichedBallot, verificationCardId, shuffledCodesMappingTable, keyAsBase64, dataAsBase64);

		// Vote Cast Return Code correctly stored
		logWriter.log(Level.DEBUG, new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENVCC_SUCCESS_VOTECASTCODE_STORED)
				.electionEvent(enrichedBallot.getElectionEvent().getId()).user(Constants.ADMIN_ID).objectId(verificationCardSetId)
				.additionalInfo(Constants.VERIFC_ID, verificationCardId).createLogInfo());

		return new VerificationCardCodesDataPack(shuffledCodesMappingTable, ballotCastingKey, voteCastReturnCode,
				choiceCodes2BallotVotingOptionOrderedBasedOnInsertion);
	}

	private void putKeyValuePairInMap(final Ballot enrichedBallot, final String verificationCardId,
			final Map<String, String> shuffledCodesMappingTable, final String keyAsBase64, final String dataAsBase64) {

		if (shuffledCodesMappingTable.containsKey(keyAsBase64)) {
			logWriter.log(Level.ERROR, new LogContent.LogContentBuilder().logEvent(GENVCC_ERROR_STORING_VOTECASTCODE)
					.electionEvent(enrichedBallot.getElectionEvent().getId()).user(Constants.ADMIN_ID).objectId(verificationCardSetId)
					.additionalInfo(Constants.VERIFC_ID, verificationCardId)
					.additionalInfo(Constants.ERR_DESC, String.format(KEY_ALREADY_EXISTS, keyAsBase64)).createLogInfo());
			throw new GenerateVerificationCardCodesException(GENVCC_ERROR_STORING_VOTECASTCODE.getInfo());
		}

		shuffledCodesMappingTable.put(keyAsBase64, dataAsBase64);
	}

	private byte[] generateLongVoteCastReturnCode(final Ballot enrichedBallot, final String electionEventId, final String verificationCardId,
			final ZpGroupElement preVoteCastReturnCode, final List<String> attributesWithCorrectness) {

		final byte[] longVoteCastReturnCode;
		try {
			longVoteCastReturnCode = voterCodesService
					.generateLongReturnCode(electionEventId, verificationCardId, preVoteCastReturnCode, attributesWithCorrectness);
		} catch (Exception e) {
			// error generating the long Vote Cast Return Code
			logWriter.log(Level.ERROR, new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENVCC_ERROR_GENERATING_LONGVOTECASTCODE)
					.electionEvent(enrichedBallot.getElectionEvent().getId()).user(Constants.ADMIN_ID).objectId(verificationCardSetId)
					.additionalInfo(Constants.VERIFC_ID, verificationCardId).additionalInfo(Constants.ERR_DESC, e.getMessage()).createLogInfo());
			throw new GenerateVerificationCardCodesException(GENVCC_ERROR_GENERATING_LONGVOTECASTCODE.getInfo(), e);
		}

		// success - long Vote Cast Return Code successfully generated
		logWriter.log(Level.DEBUG, new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENVCC_SUCCESS_LONGVOTECASTCODE_GENERATED)
				.electionEvent(enrichedBallot.getElectionEvent().getId()).user(Constants.ADMIN_ID).objectId(verificationCardSetId)
				.additionalInfo(Constants.VERIFC_ID, verificationCardId).createLogInfo());

		return longVoteCastReturnCode;
	}

	private byte[] generateLongChoiceReturnCodeForVO(final String electionEventId, final String verificationCardId,
			final ZpGroupElement preChoiceReturnCode, final List<String> attributesWithCorrectness) {
		return voterCodesService.generateLongReturnCode(electionEventId, verificationCardId, preChoiceReturnCode, attributesWithCorrectness);
	}

	private String generateShortChoiceReturnCodeForVO(final Map<String, BigInteger> choiceCodes2BallotVotingOption) {
		String shortChoiceReturnCode;
		do {
			// create a short Choice Return Code that differs from the previous ones.
			shortChoiceReturnCode = voterCodesService.generateShortChoiceReturnCode();
		} while (choiceCodes2BallotVotingOption.containsKey(shortChoiceReturnCode));

		return shortChoiceReturnCode;
	}

	private VotingCardCredentialDataPack generateVotersCredentialDataPack(final char[] keystoreSymmetricEncryptionKey, final String credentialId) {
		final VotingCardCredentialDataPack voterCredentialDataPack;

		// create replacementHolder with eeid and credential ID
		final ReplacementsHolder replacementsHolder = new ReplacementsHolder(holder.getVotingCardCredentialInputDataPack().getEeid(), credentialId);

		try {
			voterCredentialDataPack = votingCardCredentialDataPackGenerator
					.generate(holder.getVotingCardCredentialInputDataPack(), replacementsHolder, keystoreSymmetricEncryptionKey, credentialId,
							holder.getVotingCardSetID(),
							holder.getCreateVotingCardSetCertificateProperties().getCredentialSignCertificateProperties(),
							holder.getCreateVotingCardSetCertificateProperties().getCredentialAuthCertificateProperties(),
							holder.getCredentialCACert(), holder.getElectionCACert());

		} catch (GeneralCryptoLibException e) {
			throw new CreateVotingCardSetException("An error occurred while generating the voters credential data pack: " + e.getMessage(), e);
		}
		return voterCredentialDataPack;
	}

	private String generateVotingCardId(final String electionEventId) {
		final String votingCardId;

		try {
			votingCardId = cryptoPrimitives.genRandomBase16String(Constants.BASE16_ID_LENGTH).toLowerCase();
			logWriter.log(Level.DEBUG,
					new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENSVPK_SUCCESS_VCIDS_GENERATED).user(Constants.ADMIN_ID)
							.electionEvent(electionEventId).objectId(votingCardSetId).additionalInfo("vcid", votingCardId).createLogInfo());
		} catch (Exception e) {
			logWriter.log(Level.ERROR,
					new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENSVPK_ERROR_GENERATING_VCID).user(Constants.ADMIN_ID)
							.electionEvent(electionEventId).objectId(votingCardSetId).additionalInfo(Constants.ERR_DESC, e.getMessage())
							.createLogInfo());
			throw new GenerateVotingcardIdException(GENSVPK_ERROR_GENERATING_VCID.getInfo(), e);
		}

		return votingCardId;
	}

	private String generateCredentialId(final CryptoAPIPBKDFDeriver pbkdfDeriver, final String electionEventId, final byte[] salt,
			final String startVotingKey) {
		final String credentialId;
		try {
			credentialId = String.valueOf(getDerivedBytesInHEX(pbkdfDeriver, salt, startVotingKey));
		} catch (Exception e) {
			logWriter.log(Level.ERROR, new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENCREDAT_ERROR_DERIVING_CREDENTIAL_ID)
					.user(Constants.ADMIN_ID).objectId(votingCardSetId).electionEvent(electionEventId)
					.additionalInfo(Constants.VCS_SIZE, Integer.toString(numberOfVotingCards)).additionalInfo(Constants.ERR_DESC, e.getMessage())
					.createLogInfo());
			throw new GenerateCredentialIdException(GENCREDAT_ERROR_DERIVING_CREDENTIAL_ID.getInfo(), e);
		}

		logWriter.log(Level.DEBUG,
				new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENCREDAT_SUCCESS_CREDENTIAL_ID_DERIVED).user(Constants.ADMIN_ID)
						.electionEvent(electionEventId).objectId(votingCardSetId).additionalInfo("c_id", credentialId).createLogInfo());

		return credentialId;
	}

	private char[] generateKeystoreSymmetricEncryptionKey(final CryptoAPIPBKDFDeriver pbkdfDeriver, final String electionEventId, final byte[] salt,
			final String startVotingKey) {
		final char[] keystoreSymmetricEncryptionKey;

		try {
			keystoreSymmetricEncryptionKey = getDerivedBytesInHEX(pbkdfDeriver, salt, startVotingKey);
		} catch (Exception e) {
			// error deriving the keystore symmetric encryption key KSKey
			logWriter.log(Level.ERROR,
					new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENSVPK_ERROR_DERIVING_KEYSTORE_SYMMETRIC_ENCRYPTION_KEY)
							.user(Constants.ADMIN_ID).objectId(votingCardSetId).electionEvent(electionEventId)
							.additionalInfo(Constants.VCS_SIZE, Integer.toString(numberOfVotingCards))
							.additionalInfo(Constants.ERR_DESC, e.getMessage()).createLogInfo());
			throw new GenerateSVKVotingCardIdPassKeystoreException(GENSVPK_ERROR_DERIVING_KEYSTORE_SYMMETRIC_ENCRYPTION_KEY.getInfo(), e);
		}

		// success - Keystore symmetric encryption key KSKey successfully derived
		logWriter.log(Level.DEBUG,
				new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENSVPK_SUCCESS_KEYSTORE_SYMMETRIC_ENCRYPTION_KEY_DERIVED)
						.electionEvent(electionEventId).objectId(votingCardSetId).user(Constants.ADMIN_ID)
						.additionalInfo(Constants.VCS_SIZE, Integer.toString(numberOfVotingCards)).createLogInfo());

		return keystoreSymmetricEncryptionKey;
	}

	private String generateStartVotingKey(final String electionEventId) {
		final String startVotingKey;

		try {
			startVotingKey = startVotingKeyService.generateStartVotingKey();
		} catch (Exception e) {
			// error when generating the Start Voting Key
			logWriter.log(Level.ERROR,
					new LogContent.LogContentBuilder().logEvent(GENSVPK_ERROR_GENERATING_SVK).electionEvent(electionEventId).user(Constants.ADMIN_ID)
							.objectId(holder.getVotingCardSetID()).additionalInfo(Constants.VCS_SIZE, Integer.toString(numberOfVotingCards))
							.additionalInfo(Constants.ERR_DESC, e.getMessage()).createLogInfo());

			throw new CreateVotingCardSetException(GENSVPK_ERROR_GENERATING_SVK.getInfo() + ":" + e.getMessage(), e);
		}

		// success - Start Voting Key successfully generated
		logWriter.log(Level.INFO,
				new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENSVPK_SUCCESS_SVK_GENERATED).electionEvent(electionEventId)
						.objectId(votingCardSetId).user(Constants.ADMIN_ID).additionalInfo(Constants.VCS_SIZE, Integer.toString(numberOfVotingCards))
						.createLogInfo());

		return startVotingKey;
	}

	private char[] getDerivedBytesInHEX(final CryptoAPIPBKDFDeriver derived, final byte[] salt, final String inputString)
			throws GeneralCryptoLibException {
		final CryptoAPIDerivedKey cryptoAPIDerivedKey = derived.deriveKey(inputString.toCharArray(), salt);
		final byte[] encoded = cryptoAPIDerivedKey.getEncoded();
		return Hex.encodeHex(encoded);
	}

}
