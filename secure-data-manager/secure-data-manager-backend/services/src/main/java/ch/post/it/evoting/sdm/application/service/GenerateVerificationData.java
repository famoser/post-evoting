/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static ch.post.it.evoting.cryptolib.commons.validations.Validate.validateUUID;
import static ch.post.it.evoting.cryptoprimitives.math.BigIntegerOperations.modExponentiate;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.json.JsonObject;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.cms.CMSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.returncode.VoterCodesService;
import ch.post.it.evoting.cryptoprimitives.CryptoPrimitives;
import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientMessage;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.HashService;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.RandomService;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.domain.cryptoadapters.CryptoAdapters;
import ch.post.it.evoting.domain.election.Ballot;
import ch.post.it.evoting.domain.election.CombinedCorrectnessInformation;
import ch.post.it.evoting.domain.election.EncryptionParameters;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;
import ch.post.it.evoting.domain.election.model.messaging.PayloadSignatureException;
import ch.post.it.evoting.domain.returncodes.ReturnCodeGenerationInput;
import ch.post.it.evoting.domain.returncodes.ReturnCodeGenerationRequestPayload;
import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.CryptolibPayloadSignatureService;
import ch.post.it.evoting.sdm.commons.VerificationCardSet;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.status.InvalidStatusTransitionException;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.infrastructure.cc.PayloadStorageException;
import ch.post.it.evoting.sdm.infrastructure.cc.ReturnCodeGenerationRequestPayloadRepository;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;
import ch.post.it.evoting.sdm.utils.EncryptionParametersLoader;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * Service that deals with the pre-computation of voting card sets.
 * <p>
 * It implements the GenVerDat algorithm described in the cryptographic protocol.
 */
@Service
public class GenerateVerificationData extends BaseVotingCardSetService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenerateVerificationData.class);

	private final CryptoPrimitives cryptoPrimitives;
	private final IdleStatusService idleStatusService;
	private final BallotBoxRepository ballotBoxRepository;
	private final ConfigurationEntityStatusService configurationEntityStatusService;
	private final VoterCodesService voterCodesService;
	private final CryptolibPayloadSignatureService payloadSignatureService;
	private final ReturnCodeGenerationRequestPayloadRepository returnCodeGenerationRequestPayloadRepository;
	private final AdminBoardService adminBoardService;
	private final BallotService ballotService;
	private final EncryptionParametersLoader encryptionParametersLoader;
	private final HashService hashService;

	@Value("${tenantID}")
	private String tenantId;

	@Value("${choiceCodeGenerationChunkSize:100}")
	private int chunkSize;

	private ElGamalMultiRecipientPublicKey setupPublicKey;
	private String votingCardSetId;
	private String electionEventId;
	private String administrationBoardPrivateKeyPEM;
	private String adminBoardId;

	@Autowired
	public GenerateVerificationData(final CryptoPrimitives cryptoPrimitives, final IdleStatusService idleStatusService,
			final BallotBoxRepository ballotBoxRepository, final ConfigurationEntityStatusService configurationEntityStatusService,
			final VoterCodesService voterCodesService, final CryptolibPayloadSignatureService payloadSignatureService,
			final ReturnCodeGenerationRequestPayloadRepository returnCodeGenerationRequestPayloadRepository,
			final AdminBoardService adminBoardService, final BallotService ballotService, final EncryptionParametersLoader encryptionParametersLoader,
			@Qualifier("cryptoPrimitivesHashService")
			final HashService hashService) {

		this.cryptoPrimitives = cryptoPrimitives;
		this.idleStatusService = idleStatusService;
		this.ballotBoxRepository = ballotBoxRepository;
		this.configurationEntityStatusService = configurationEntityStatusService;
		this.voterCodesService = voterCodesService;
		this.payloadSignatureService = payloadSignatureService;
		this.returnCodeGenerationRequestPayloadRepository = returnCodeGenerationRequestPayloadRepository;
		this.adminBoardService = adminBoardService;
		this.ballotService = ballotService;
		this.encryptionParametersLoader = encryptionParametersLoader;
		this.hashService = hashService;
	}

	private static String getReferencedBallotBoxId(final JsonObject votingCardSetJson) {
		final JsonObject ballotBoxReferenceJson = votingCardSetJson.getJsonObject(JsonConstants.BALLOT_BOX);
		return ballotBoxReferenceJson.getString(JsonConstants.ID);
	}

	/**
	 * Pre-compute a voting card set.
	 *
	 * @throws InvalidStatusTransitionException If the original status does not allow pre-computing
	 * @throws GeneralCryptoLibException        If an error occurs while processing operations with the cryptolib.
	 * @throws PayloadStorageException          If an error occurs while storing the payload.
	 * @throws PayloadSignatureException        If an error occurs while signing the payload.
	 */
	public void precompute(final String votingCardSetId, final String electionEventId, final String administrationBoardPrivateKeyPEM,
			final String adminBoardId)
			throws ResourceNotFoundException, InvalidStatusTransitionException, GeneralCryptoLibException, PayloadStorageException,
			PayloadSignatureException {

		validateUUID(votingCardSetId);
		validateUUID(electionEventId);

		this.votingCardSetId = votingCardSetId;
		this.electionEventId = electionEventId;
		this.administrationBoardPrivateKeyPEM = administrationBoardPrivateKeyPEM;
		this.adminBoardId = adminBoardId;

		if (!idleStatusService.getIdLock(this.votingCardSetId)) {
			return;
		}

		try {
			performPrecompute();
		} finally {
			idleStatusService.freeIdLock(this.votingCardSetId);
		}
	}

	private void performPrecompute()
			throws ResourceNotFoundException, InvalidStatusTransitionException, PayloadStorageException, PayloadSignatureException {

		LOGGER.info("Starting pre-computation of voting card set {}...", this.votingCardSetId);

		final Status fromStatus = Status.LOCKED;
		final Status toStatus = Status.PRECOMPUTED;

		checkVotingCardSetStatusTransition(this.electionEventId, this.votingCardSetId, fromStatus, toStatus);

		// Get the voting card set.
		final JsonObject votingCardSet = votingCardSetRepository.getVotingCardSetJson(this.electionEventId, this.votingCardSetId);

		// Extract the number of voting cards in the set, which corresponds to the number of eligible voters for that voting card set.
		final int numberOfVotingCardsToGenerate = votingCardSet.getInt(JsonConstants.NUMBER_OF_VOTING_CARDS_TO_GENERATE);

		LOGGER.info("Generating {} voting cards for votingCardSetId {}...", numberOfVotingCardsToGenerate, this.votingCardSetId);

		// Find the matching verification card set identifier.
		final String verificationCardSetId = votingCardSetRepository.getVerificationCardSetId(this.votingCardSetId);
		final String ballotBoxId = getReferencedBallotBoxId(votingCardSet);

		// Get the setup public key.
		setupPublicKey = getSetupPublicKey();
		final GqGroup electionGqGroup = setupPublicKey.getGroup();

		returnCodeGenerationRequestPayloadRepository.remove(this.electionEventId, verificationCardSetId);

		// Retrieve the prime numbers representing the voting options.
		final List<GqElement> encodedVotingOptions = getEncodedVotingOptionsForBallotBox(ballotBoxId, electionGqGroup);

		final VerificationCardSet verificationCardSet = new VerificationCardSet(this.adminBoardId, ballotBoxId, this.electionEventId,
				verificationCardSetId, this.votingCardSetId);

		// Build payloads to request the return code generation (choice return codes and vote cast return code) from the control components.

		// Build full-sized chunks (i.e. with `chunkSize` elements)
		final int fullChunkCount = numberOfVotingCardsToGenerate / chunkSize;
		for (int i = 0; i < fullChunkCount; i++) {
			// Generate verification data for the chunk.
			generateRequestPayload(verificationCardSetId, encodedVotingOptions, verificationCardSet, i, chunkSize);
		}

		// Build an eventual last chunk with the remaining elements.
		final int lastChunkSize = numberOfVotingCardsToGenerate % chunkSize;
		if (lastChunkSize > 0) {
			generateRequestPayload(verificationCardSetId, encodedVotingOptions, verificationCardSet, fullChunkCount, lastChunkSize);
		}

		LOGGER.info("Generation of {} voting cards for votingCardSetId {} successful.", numberOfVotingCardsToGenerate, this.votingCardSetId);

		// Update the voting card set status to 'pre-computed'.
		configurationEntityStatusService.update(toStatus.name(), this.votingCardSetId, votingCardSetRepository);
	}

	/**
	 * Generates the verification data with genVerData and create the ReturnCodeGenerationRequestPayload.
	 */
	private void generateRequestPayload(final String verificationCardSetId, final List<GqElement> encodedVotingOptions,
			final VerificationCardSet verificationCardSet, final int chunkId, final int chunkSize)
			throws PayloadSignatureException, PayloadStorageException {

		// Generate verification data. Since we chunk the payloads, we are not directly working with the number of eligible voters (N_E),
		// but rather with the chunk size as input of the algorithm.
		LOGGER.debug("Generating verification data for verificationCardSet {} and chunk {}...", verificationCardSetId, chunkId);
		final GenVerDatOutput genVerDatOutput = genVerDat(chunkSize, encodedVotingOptions);

		// Persist casting keys and verification card key pairs.
		persistBallotCastingKeys(verificationCardSetId, genVerDatOutput);
		persistVerificationCardKeyPairs(verificationCardSetId, genVerDatOutput);

		LOGGER.debug("Generation of verification data for verificationCardSet {} and chunk {} successful. Creating payload...", verificationCardSetId,
				chunkId);

		// Create the payload.
		final ReturnCodeGenerationRequestPayload payload = createRequestPayload(verificationCardSet, chunkId, genVerDatOutput);

		// Store the payload.
		returnCodeGenerationRequestPayloadRepository.store(payload);
	}

	private ReturnCodeGenerationRequestPayload createRequestPayload(final VerificationCardSet verificationCardSet, final int chunkId,
			final GenVerDatOutput genVerDatOutput) throws PayloadSignatureException {

		// Create payload.
		final List<ReturnCodeGenerationInput> returnCodeGenerationInputList = new ArrayList<>();
		for (int j = 0; j < genVerDatOutput.size(); j++) {
			final String verificationCardId = genVerDatOutput.getVerificationCardIds().get(j);
			final ElGamalMultiRecipientCiphertext confirmationKey = genVerDatOutput.getEncryptedHashedConfirmationKeys().get(j);
			final ElGamalMultiRecipientCiphertext partialChoiceCode = genVerDatOutput.getEncryptedHashedPartialChoiceReturnCodes().get(j);
			final ElGamalMultiRecipientPublicKey verificationCardPublicKey = genVerDatOutput.getVerificationCardKeyPairs().get(j).getPublicKey();

			returnCodeGenerationInputList
					.add(new ReturnCodeGenerationInput(verificationCardId, confirmationKey, partialChoiceCode, verificationCardPublicKey));
		}
		final ReturnCodeGenerationRequestPayload payload = new ReturnCodeGenerationRequestPayload(tenantId, verificationCardSet.getElectionEventId(),
				verificationCardSet.getVerificationCardSetId(), chunkId, genVerDatOutput.getGroup(), returnCodeGenerationInputList,
				new CombinedCorrectnessInformation(getBallot(verificationCardSet.getBallotBoxId(), verificationCardSet.getElectionEventId())));

		// Sign the payload.
		LOGGER.debug("Signing payload for verificationCardSet {} and chunkId {}...", verificationCardSet.getVerificationCardSetId(), chunkId);
		try {
			signPayload(payload);
		} catch (CertificateManagementException | PayloadSignatureException e) {
			throw new PayloadSignatureException(e);
		}

		LOGGER.debug("Payload successfully created and signed for verificationCardSet {} and chunkId {}.",
				verificationCardSet.getVerificationCardSetId(), chunkId);

		return payload;
	}

	/**
	 * Signs a return code generation request payload.
	 *
	 * @param payload the payload to sign
	 * @throws PayloadSignatureException      If an error occurs while getting the admin board's signing key.
	 * @throws CertificateManagementException If an error occurs while getting the admin board's certificate chain.
	 */
	private void signPayload(final ReturnCodeGenerationRequestPayload payload) throws PayloadSignatureException, CertificateManagementException {
		// Get the admin board's signing key.
		final PrivateKey signingKey;
		try {
			signingKey = PemUtils.privateKeyFromPem(administrationBoardPrivateKeyPEM);
		} catch (GeneralCryptoLibException e) {
			throw new PayloadSignatureException(e);
		}

		// Get the admin board's certificate chain.
		final X509Certificate[] certificateChain = adminBoardService.getCertificateChain(adminBoardId);

		// Hash and sign the payload.
		final byte[] payloadHash = hashService.recursiveHash(payload);
		final CryptolibPayloadSignature signature = payloadSignatureService.sign(payloadHash, signingKey, certificateChain);
		payload.setSignature(signature);
	}

	/**
	 * Initialize the control components' computation of the return codes.
	 *
	 * @param eligibleVoters       N<sub>E</sub>, the number of eligible voters. Must be strictly greater than 0.
	 * @param encodedVotingOptions p&#771;, the encoded voting options as prime numbers. Must no contain any null.
	 * @return the generated verification data as a {@link GenVerDatOutput}.
	 * @throws NullPointerException     if {@code encodedVotingOptions} is null.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                      <li>{@code encodedVotingOptions} contains any null</li>
	 *                                      <li>{@code eligibleVoters} is not strictly greater than 0</li>
	 *                                      <li>The number of voting options is greater than the secret key length</li>
	 *                                  </ul>
	 */
	@SuppressWarnings("java:S117")
	private GenVerDatOutput genVerDat(final int eligibleVoters, final List<GqElement> encodedVotingOptions) {
		checkNotNull(encodedVotingOptions);
		checkArgument(eligibleVoters > 0, "The number of eligible voters must be strictly greater than 0.");
		checkArgument(encodedVotingOptions.stream().allMatch(Objects::nonNull), "The encoded voting options must not contain any null elements.");

		final int N_E = eligibleVoters;
		final int l_ID = Constants.BASE16_ID_LENGTH;
		final ImmutableList<GqElement> p_tilde = ImmutableList.copyOf(encodedVotingOptions);
		final ElGamalMultiRecipientPublicKey pk_setup = setupPublicKey;
		final int n = p_tilde.size();
		final int omega = pk_setup.size();
		checkArgument(n <= omega, "The number of voting options must be smaller than or equal to the setup secret key length.");

		final GqGroup gqGroup = pk_setup.getGroup();
		final ZqGroup zqGroup = ZqGroup.sameOrderAs(gqGroup);
		final BigInteger p = gqGroup.getP();
		final RandomService randomService = new RandomService();

		// Output variables.
		final List<String> vc = new ArrayList<>();
		final List<ElGamalMultiRecipientKeyPair> verificationCardKeyPairs = new ArrayList<>();
		final List<String> BCK = new ArrayList<>();
		final List<ElGamalMultiRecipientCiphertext> c_pCC = new ArrayList<>();
		final List<ElGamalMultiRecipientCiphertext> c_ck = new ArrayList<>();

		// Algorithm.
		final ElGamalMultiRecipientPublicKey pk_setup_hat = pk_setup.compress(n);
		for (int i = 0; i < N_E; i++) {
			final String vc_id = cryptoPrimitives.genRandomBase16String(l_ID).toLowerCase();
			final ElGamalMultiRecipientKeyPair verificationCardKeyPair = ElGamalMultiRecipientKeyPair.genKeyPair(gqGroup, 1, randomService);

			// Compute hpCC_id.
			final List<GqElement> hpCC_id_elements = new ArrayList<>();
			final ZqElement k_id = verificationCardKeyPair.getPrivateKey().get(0);
			for (int k = 0; k < n; k++) {
				final GqElement p_k = p_tilde.get(k);
				final GqElement pCC_id_k = p_k.exponentiate(k_id);
				final GqElement hpCC_id_k = pCC_id_k.hashAndSquare(hashService);
				hpCC_id_elements.add(hpCC_id_k);
			}
			final ElGamalMultiRecipientMessage hpCC_id = new ElGamalMultiRecipientMessage(hpCC_id_elements);

			// Compute c_pCC_id.
			final ZqElement hpCC_id_exponent = ZqElement.create(cryptoPrimitives.genRandomInteger(gqGroup.getQ()), zqGroup);
			final ElGamalMultiRecipientCiphertext c_pCC_id = ElGamalMultiRecipientCiphertext.getCiphertext(hpCC_id, hpCC_id_exponent, pk_setup_hat);

			// Generate BCK_id.
			final String BCK_id = voterCodesService.generateBallotCastingKey();

			// Compute c_ck_id.
			final GqElement CK_id = GqElement.create(modExponentiate(new BigInteger(BCK_id, 10), BigInteger.valueOf(2), p), gqGroup)
					.exponentiate(k_id);
			final ElGamalMultiRecipientMessage hCK_id = new ElGamalMultiRecipientMessage(Collections.singletonList(CK_id.hashAndSquare(hashService)));

			final ZqElement hCKExponent = ZqElement.create(cryptoPrimitives.genRandomInteger(gqGroup.getQ()), zqGroup);
			final ElGamalMultiRecipientCiphertext c_ck_id = ElGamalMultiRecipientCiphertext.getCiphertext(hCK_id, hCKExponent, pk_setup_hat);

			// Outputs.
			vc.add(vc_id);
			verificationCardKeyPairs.add(verificationCardKeyPair);
			BCK.add(BCK_id);
			c_pCC.add(c_pCC_id);
			c_ck.add(c_ck_id);
		}

		return new GenVerDatOutput(vc, verificationCardKeyPairs, BCK, GroupVector.from(c_pCC), GroupVector.from(c_ck));
	}

	private void persistVerificationCardKeyPairs(final String verificationCardSetId, final GenVerDatOutput genVerDatOutput) {

		final Path verificationCardSetKeyPairsPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR).resolve(this.electionEventId)
				.resolve(Constants.CONFIG_DIR_NAME_OFFLINE).resolve(Constants.CONFIG_VERIFICATION_CARDS_KEY_PAIR_DIRECTORY)
				.resolve(verificationCardSetId);
		try {
			Files.createDirectories(verificationCardSetKeyPairsPath);
		} catch (IOException e) {
			throw new UncheckedIOException(String.format("Failed to create all directories along the path %s.", verificationCardSetKeyPairsPath), e);
		}

		for (int i = 0; i < genVerDatOutput.size(); i++) {
			final String verificationCardId = genVerDatOutput.getVerificationCardIds().get(i);
			final ElGamalMultiRecipientKeyPair keyPair = genVerDatOutput.getVerificationCardKeyPairs().get(i);
			final ElGamalMultiRecipientPublicKey publicKey = keyPair.getPublicKey();
			final ElGamalMultiRecipientPrivateKey privateKey = keyPair.getPrivateKey();

			final String verificationCardSecretKey;
			final String verificationCardPublicKey;

			// Convert keys to cryptolib.
			final ElGamalPublicKey elGamalPublicKey = CryptoAdapters.convert(publicKey);
			final ElGamalPrivateKey elGamalPrivateKey = CryptoAdapters.convert(privateKey);
			try {
				verificationCardSecretKey = elGamalPrivateKey.toJson();
				verificationCardPublicKey = elGamalPublicKey.toJson();
			} catch (GeneralCryptoLibException e) {
				throw new IllegalArgumentException("Failed to convert keys to json.", e);
			}

			final Path verificationCardKeyPairFilePath = verificationCardSetKeyPairsPath.resolve(verificationCardId + Constants.KEY);

			try {
				FileUtils.writeByteArrayToFile(verificationCardKeyPairFilePath.toFile(),
						(verificationCardSecretKey + System.lineSeparator() + verificationCardPublicKey).getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new IllegalStateException("Error saving verification cards key pairs", e);
			}
		}
	}

	@VisibleForTesting
	void persistBallotCastingKeys(final String verificationCardSetId, final GenVerDatOutput genVerDatOutput) {
		LOGGER.info("Persisting the ballot casting keys for for the electionEventId {} and verificationCardSetId {}.", this.electionEventId,
				verificationCardSetId);

		final Path ballotCastingKeysDirPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR).resolve(this.electionEventId)
				.resolve(Constants.CONFIG_DIR_NAME_OFFLINE).resolve(Constants.CONFIG_BALLOT_CASTING_KEYS_DIRECTORY).resolve(verificationCardSetId);

		final File ballotCastingKeyPairsFile = ballotCastingKeysDirPath.toFile();

		if (!ballotCastingKeyPairsFile.exists() && !ballotCastingKeyPairsFile.mkdirs()) {
			throw new IllegalStateException(String.format(
					"An error occurred while creating the directory for saving the ballot casting keys for electionEventId %s and verificationCardSetId %s",
					this.electionEventId, verificationCardSetId));
		}

		for (int i = 0; i < genVerDatOutput.size(); i++) {
			final String verificationCardId = genVerDatOutput.getVerificationCardIds().get(i);
			final String BCK = genVerDatOutput.getBallotCastingKeys().get(i);
			try {
				Files.write(ballotCastingKeysDirPath.resolve(verificationCardId + Constants.KEY), BCK.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new IllegalStateException(String.format(
						"An error occurred while saving ballot casting key for electionEventId %s, verificationCardSetId %s and verificationCardId %s.",
						this.electionEventId, verificationCardSetId, verificationCardId), e);
			}
		}
	}

	private Ballot getBallot(final String ballotBoxId, final String electionEventId) {
		final String ballotId = getBallotIdForBallotBox(ballotBoxId, electionEventId);
		return ballotService.getBallot(this.electionEventId, ballotId);
	}

	private String getBallotIdForBallotBox(final String ballotBoxId, final String electionEventId) {
		final JsonObject ballotBox = getBallotBoxObject(ballotBoxId, electionEventId);
		final JsonObject ballotObject = ballotBox.getJsonObject(JsonConstants.BALLOT);

		return ballotObject.getString(JsonConstants.ID);
	}

	private JsonObject getBallotBoxObject(final String ballotBoxId, final String electionEventId) {
		final Map<String, Object> attributeValueMap = new HashMap<>();
		attributeValueMap.put(JsonConstants.ELECTION_EVENT_DOT_ID, electionEventId);
		attributeValueMap.put(JsonConstants.ID, ballotBoxId);

		final String ballotBoxAsJson = ballotBoxRepository.find(attributeValueMap);

		return JsonUtils.getJsonObject(ballotBoxAsJson);
	}

	/**
	 * Gets a ballot box's encoded voting options (prime numbers) as {@link GqElement}s.
	 *
	 * @param ballotBoxId the ballot box with the voting options.
	 * @param group       the election event group.
	 * @return a stream of encrypted representations
	 */
	private List<GqElement> getEncodedVotingOptionsForBallotBox(final String ballotBoxId, final GqGroup group) {
		return getBallot(ballotBoxId, this.electionEventId).getEncodedVotingOptions().stream().map(value -> GqElement.create(value, group))
				.collect(Collectors.toList());
	}

	/**
	 * Read the setup secret key from the file system and derive the setup public key from it. To reconstruct the secret key, the encryption
	 * parameters are first read from the file system.
	 *
	 * @return the setup public key.
	 */
	private ElGamalMultiRecipientPublicKey getSetupPublicKey() {
		// Load the election encryption parameters from file system to retrieve the gqGroup.
		final EncryptionParameters encryptionParameters;
		final Path electionEventPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, electionEventId);
		try {
			encryptionParameters = encryptionParametersLoader.load(electionEventPath);
		} catch (CertificateException | CMSException | GeneralCryptoLibException | IOException e) {
			throw new IllegalStateException(String.format("Failed to load the encryption parameters located at: %s", electionEventPath), e);
		}
		final BigInteger p = new BigInteger(encryptionParameters.getP(), 10);
		final BigInteger q = new BigInteger(encryptionParameters.getQ(), 10);
		final BigInteger g = new BigInteger(encryptionParameters.getG(), 10);
		final GqGroup gqGroup = new GqGroup(p, q, g);

		try {
			// Read the setup secret key from file system.
			final ElGamalMultiRecipientPrivateKey setupSecretKey = objectMapper.reader().withAttribute("group", gqGroup).readValue(pathResolver
					.resolve(Constants.CONFIG_FILES_BASE_DIR, electionEventId, Constants.CONFIG_DIR_NAME_OFFLINE,
							Constants.SETUP_SECRET_KEY_FILE_NAME).toFile(), ElGamalMultiRecipientPrivateKey.class);

			// Create key pair from the setup secret key and retrieve corresponding setup public key.
			return ElGamalMultiRecipientKeyPair.from(setupSecretKey, gqGroup.getGenerator()).getPublicKey();
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to deserialize setup secret key.", e);
		}
	}

	/**
	 * Holds the output of the genVerData algorithm.
	 */
	static class GenVerDatOutput {

		private final int size;
		private final GqGroup gqGroup;

		private final List<String> verificationCardIds;
		private final List<ElGamalMultiRecipientKeyPair> verificationCardKeyPairs;
		private final List<String> ballotCastingKeys;
		private final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes;
		private final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeys;

		GenVerDatOutput(final List<String> verificationCardIds, final List<ElGamalMultiRecipientKeyPair> verificationCardKeyPairs,
				final List<String> ballotCastingKeys,
				final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes,
				final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeys) {

			checkNotNull(verificationCardIds);
			checkNotNull(verificationCardKeyPairs);
			checkNotNull(ballotCastingKeys);
			checkNotNull(encryptedHashedPartialChoiceReturnCodes);
			checkNotNull(encryptedHashedConfirmationKeys);

			checkArgument(!verificationCardIds.isEmpty(), "The output must not be empty.");

			this.size = verificationCardIds.size();
			checkArgument(size() == verificationCardKeyPairs.size() && this.size == ballotCastingKeys.size()
							&& this.size == encryptedHashedPartialChoiceReturnCodes.size() && this.size == encryptedHashedConfirmationKeys.size(),
					"All vectors must have the same size.");

			final GqGroup group = verificationCardKeyPairs.get(0).getGroup();
			checkArgument(group.equals(encryptedHashedPartialChoiceReturnCodes.get(0).getGroup()) && group
					.equals(encryptedHashedConfirmationKeys.get(0).getGroup()), "All vectors must belong to the same group.");

			if (new HashSet<>(verificationCardIds).size() != this.size) {
				throw new IllegalStateException("The verificationCardId is duplicated.");
			}

			this.gqGroup = group;
			this.verificationCardIds = verificationCardIds;
			this.verificationCardKeyPairs = verificationCardKeyPairs;
			this.ballotCastingKeys = ballotCastingKeys;
			this.encryptedHashedPartialChoiceReturnCodes = encryptedHashedPartialChoiceReturnCodes;
			this.encryptedHashedConfirmationKeys = encryptedHashedConfirmationKeys;
		}

		public List<String> getVerificationCardIds() {
			return verificationCardIds;
		}

		public List<ElGamalMultiRecipientKeyPair> getVerificationCardKeyPairs() {
			return verificationCardKeyPairs;
		}

		public List<String> getBallotCastingKeys() {
			return ballotCastingKeys;
		}

		public List<ElGamalMultiRecipientCiphertext> getEncryptedHashedPartialChoiceReturnCodes() {
			return encryptedHashedPartialChoiceReturnCodes;
		}

		public List<ElGamalMultiRecipientCiphertext> getEncryptedHashedConfirmationKeys() {
			return encryptedHashedConfirmationKeys;
		}

		private int size() {
			return this.size;
		}

		private GqGroup getGroup() {
			return this.gqGroup;
		}

	}

}

