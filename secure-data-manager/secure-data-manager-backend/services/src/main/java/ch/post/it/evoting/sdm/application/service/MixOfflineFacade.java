/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static ch.post.it.evoting.cryptoprimitives.GroupVector.toGroupVector;
import static ch.post.it.evoting.domain.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientMessage;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.HashService;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.cryptoadapters.CryptoAdapters;
import ch.post.it.evoting.domain.election.Ballot;
import ch.post.it.evoting.domain.election.CombinedCorrectnessInformation;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;
import ch.post.it.evoting.domain.election.model.messaging.PayloadSignatureException;
import ch.post.it.evoting.domain.mixnet.MixnetFinalPayload;
import ch.post.it.evoting.domain.mixnet.MixnetShufflePayload;
import ch.post.it.evoting.sdm.application.exception.CheckedIllegalStateException;
import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.application.service.writers.VotePrimeFactorsFileRepository;
import ch.post.it.evoting.sdm.commons.CryptolibPayloadSignatureService;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.mixing.MixingKeys;
import ch.post.it.evoting.sdm.domain.model.mixing.PrimeFactors;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.infrastructure.mixnetpayload.MixnetFinalPayloadFileRepository;
import ch.post.it.evoting.sdm.infrastructure.mixnetpayload.MixnetShufflePayloadFileRepository;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;

/**
 * Handles the offline mixing steps.
 */
@Service
public class MixOfflineFacade {

	private static final Logger LOGGER = LoggerFactory.getLogger(MixOfflineFacade.class);

	private final BallotBoxService ballotBoxService;
	private final BallotBoxRepository ballotBoxRepository;
	private final MixnetShufflePayloadFileRepository shufflePayloadFileRepository;
	private final MixnetFinalPayloadFileRepository finalPayloadFileRepository;
	private final ConfigurationEntityStatusService configurationEntityStatusService;
	private final FactorizeService factorizeService;
	private final VotePrimeFactorsFileRepository votePrimeFactorsFileRepository;
	private final BallotService ballotService;
	private final MixDecryptService mixDecryptService;
	private final HashService hashService;
	private final CryptolibPayloadSignatureService cryptolibPayloadSignatureService;
	private final AdminBoardService adminBoardService;
	private final MixnetShufflePayloadService mixnetShufflePayloadService;

	@Autowired
	MixOfflineFacade(final BallotBoxService ballotBoxService, final BallotBoxRepository ballotBoxRepository,
			final MixnetShufflePayloadFileRepository shufflePayloadFileRepository, final MixnetFinalPayloadFileRepository finalPayloadFileRepository,
			final ConfigurationEntityStatusService configurationEntityStatusService, final FactorizeService factorizeService,
			final VotePrimeFactorsFileRepository votePrimeFactorsFileRepository, final BallotService ballotService,
			final MixDecryptService mixDecryptService,
			@Qualifier("cryptoPrimitivesHashService")
			final HashService hashService, final CryptolibPayloadSignatureService cryptolibPayloadSignatureService,
			final AdminBoardService adminBoardService, final MixnetShufflePayloadService mixnetShufflePayloadService) {
		this.ballotBoxService = ballotBoxService;
		this.ballotBoxRepository = ballotBoxRepository;
		this.shufflePayloadFileRepository = shufflePayloadFileRepository;
		this.finalPayloadFileRepository = finalPayloadFileRepository;
		this.configurationEntityStatusService = configurationEntityStatusService;
		this.factorizeService = factorizeService;
		this.votePrimeFactorsFileRepository = votePrimeFactorsFileRepository;
		this.ballotService = ballotService;
		this.mixDecryptService = mixDecryptService;
		this.hashService = hashService;
		this.cryptolibPayloadSignatureService = cryptolibPayloadSignatureService;
		this.adminBoardService = adminBoardService;
		this.mixnetShufflePayloadService = mixnetShufflePayloadService;
	}

	/**
	 * Coordinates the offline mixing: mixing, decryption, factorisation and persistence
	 *
	 * @param electionEventId the id of the election event for which we want to mix a ballot box. Not null
	 * @param ballotBoxId     the id of the ballot box to mix. Not null
	 * @param mixingKeys      the mixing keys for decrypting the ballot and signing the result. Not null.
	 * @throws ResourceNotFoundException    if no ballot can be found for this ballot box id
	 * @throws CheckedIllegalStateException if the ballot box has not been downloaded prior
	 */
	public void mixOffline(String electionEventId, String ballotBoxId, MixingKeys mixingKeys)
			throws ResourceNotFoundException, CheckedIllegalStateException {

		checkNotNull(electionEventId);
		checkNotNull(ballotBoxId);
		checkNotNull(mixingKeys);
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		String ballotId = ballotBoxService.getBallotId(ballotBoxId);

		if (!ballotBoxService.isDownloaded(ballotBoxId)) {
			throw new CheckedIllegalStateException(String.format("Ballot box %s has not been downloaded, hence it cannot be mixed.", ballotBoxId));
		}

		final PrivateKey administrationBoardPrivateKey;
		try {
			administrationBoardPrivateKey = PemUtils.privateKeyFromPem(mixingKeys.getAdministrationBoardPrivateKeyPEM());
		} catch (GeneralCryptoLibException e) {
			throw new IllegalArgumentException("Cannot decode a private key from the provided string.", e);
		}

		final ElGamalPrivateKey electoralBoardPrivateKey = decodePrivateKey(mixingKeys.getElectoralBoardPrivateKey());

		final X509Certificate[] signingCertificateChain;
		try {
			signingCertificateChain = adminBoardService.getCertificateChain(mixingKeys.getAdminBoardId());
		} catch (CertificateManagementException e) {
			throw new IllegalArgumentException("Cannot extract certificate chain for the provided admin board Id.", e);
		}

		if (!ballotBoxService.isDownloadedBallotBoxEmpty(electionEventId, ballotId, ballotBoxId) && ballotBoxService
				.hasDownloadedBallotBoxConfirmedVotes(electionEventId, ballotId, ballotBoxId)) {

			LOGGER.info("Mixing and decrypting election {}, ballot {}, ballot box {}", electionEventId, ballotId, ballotBoxId);

			checkArgument(mixnetShufflePayloadService.areOnlinePayloadSignaturesValid(electionEventId, ballotId, ballotBoxId),
					String.format("The signatures verifications failed for election %s, ballot %s and ballot box %s.", electionEventId, ballotId,
							ballotBoxId));

			final MixnetFinalPayload payload = mixDecrypt(electionEventId, ballotId, ballotBoxId, electoralBoardPrivateKey,
					administrationBoardPrivateKey, signingCertificateChain);

			LOGGER.info("Persisting final payload for election {}, ballot {}, ballot box {}.", electionEventId, ballotId, ballotBoxId);
			finalPayloadFileRepository.savePayload(electionEventId, ballotId, ballotBoxId, payload);

			final GroupVector<ElGamalMultiRecipientMessage, GqGroup> decryptedVotes = payload.getVerifiablePlaintextDecryption().getDecryptedVotes();
			final GroupVector<GqElement, GqGroup> encodedVoterSelections = extractEncodedVoterSelection(decryptedVotes);
			final Ballot ballot = ballotService.getBallot(electionEventId, ballotId);
			final List<GqElement> encodedVotingOptions = getVoteOptions(payload.getEncryptionGroup(), ballot);
			final int numberOfSelections = getNumberOfSelections(ballot);

			LOGGER.info("Factorizing voter selections");
			final List<PrimeFactors> voterSelections = factorizeService.factorize(encodedVoterSelections, encodedVotingOptions, numberOfSelections);

			LOGGER.info("Persisting decompressed votes");
			votePrimeFactorsFileRepository
					.saveDecompressedVotes(voterSelections, electionEventId, ballotId, ballotBoxId, administrationBoardPrivateKey);

		} else {
			LOGGER.info("Persisting an empty decompressed votes. There are no votes in the ballot box.");
			votePrimeFactorsFileRepository
					.saveDecompressedVotes(new ArrayList<>(), electionEventId, ballotId, ballotBoxId, administrationBoardPrivateKey);
		}

		configurationEntityStatusService.update(Status.DECRYPTED.name(), ballotBoxId, ballotBoxRepository);
	}

	private GroupVector<GqElement, GqGroup> extractEncodedVoterSelection(GroupVector<ElGamalMultiRecipientMessage, GqGroup> decryptedVotes) {
		return decryptedVotes.stream().map(message -> message.get(0)).collect(toGroupVector());
	}

	/**
	 * Computes the allowed number of selections
	 */
	private int getNumberOfSelections(final Ballot ballot) {
		final CombinedCorrectnessInformation combinedCorrectnessInformation = new CombinedCorrectnessInformation(ballot);
		return combinedCorrectnessInformation.getTotalNumberOfSelections();
	}

	private ImmutableList<GqElement> getVoteOptions(final GqGroup encryptionGroup, final Ballot ballot) {
		final List<BigInteger> encodedVotingOptions = ballot.getEncodedVotingOptions();
		return encodedVotingOptions.stream().map(bigInteger -> GqElement.create(bigInteger, encryptionGroup)).collect(toImmutableList());
	}

	/**
	 * Mixes and decrypts the votes in the specified ballot box.
	 *
	 * @param electionEventId          identifier of the election event.
	 * @param ballotBoxId              identifier of the ballot box to mix and decrypt.
	 * @param electoralBoardPrivateKey the private key used to decrypt the votes.
	 * @return the result of the mixing and decryption as a MixnetPayload object.
	 */
	private MixnetFinalPayload mixDecrypt(String electionEventId, String ballotId, String ballotBoxId, ElGamalPrivateKey electoralBoardPrivateKey,
			PrivateKey administrationBoardSigningKey, X509Certificate[] signingCertificateChain) {

		MixnetShufflePayload lastPayload = shufflePayloadFileRepository.getPayload(electionEventId, ballotId, ballotBoxId, 3);

		GqGroup encryptionGroup = lastPayload.getEncryptionGroup();
		GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> ciphertexts = GroupVector.from(lastPayload.getEncryptedVotes());
		ElGamalMultiRecipientPublicKey remainingElectionPublicKey = lastPayload.getRemainingElectionPublicKey();

		ElGamalMultiRecipientKeyPair electoralBoardKeyPair = CryptoAdapters.toElGamalMultiRecipientKeyPair(electoralBoardPrivateKey);

		Instant start = Instant.now();
		MixDecryptService.Result mixingResult = mixDecryptService
				.mixDecryptOffline(ciphertexts, remainingElectionPublicKey, electoralBoardKeyPair, electionEventId, ballotBoxId);
		LOGGER.info("Mixing duration was {}", Duration.between(start, Instant.now()));

		final MixnetFinalPayload payload = new MixnetFinalPayload(encryptionGroup, mixingResult.getVerifiableShuffle().orElse(null),
				mixingResult.getVerifiablePlaintextDecryption(), remainingElectionPublicKey, null);

		final byte[] payloadHash = hashPayload(payload);
		final CryptolibPayloadSignature signature;
		try {
			signature = cryptolibPayloadSignatureService.sign(payloadHash, administrationBoardSigningKey, signingCertificateChain);
		} catch (PayloadSignatureException e) {
			throw new IllegalArgumentException("Cannot create payload signature", e);
		}
		payload.setSignature(signature);

		return payload;
	}

	/*
	 	Decodes the string representation of the private key into a ElGamalPrivateKey object.
	 */
	private ElGamalPrivateKey decodePrivateKey(String encodedElectoralBoardPrivateKey) {
		final byte[] electoralBoardPrivateKeyJsonBytes = Base64.getDecoder().decode(encodedElectoralBoardPrivateKey);
		String electoralBoardPrivateKeyJson = new String(electoralBoardPrivateKeyJsonBytes, StandardCharsets.UTF_8);
		ElGamalPrivateKey privateKeyCryptoLib;
		try {
			privateKeyCryptoLib = ElGamalPrivateKey.fromJson(electoralBoardPrivateKeyJson);
		} catch (GeneralCryptoLibException e) {
			throw new IllegalArgumentException("Could not retrieve the electoral board secret key from the json format", e);
		}
		return privateKeyCryptoLib;
	}

	private byte[] hashPayload(final MixnetFinalPayload payload) {
		if (payload.getVerifiableShuffle().isPresent()) {
			return hashService.recursiveHash(payload.getEncryptionGroup(), payload.getVerifiableShuffle().orElse(null),
					payload.getVerifiablePlaintextDecryption().getDecryptedVotes(), payload.getVerifiablePlaintextDecryption().getDecryptionProofs(),
					payload.getPreviousRemainingElectionPublicKey());
		} else {
			return hashService.recursiveHash(payload.getEncryptionGroup(), payload.getVerifiablePlaintextDecryption().getDecryptedVotes(),
					payload.getVerifiablePlaintextDecryption().getDecryptionProofs(), payload.getPreviousRemainingElectionPublicKey());
		}
	}
}
