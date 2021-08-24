/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.ballotbox;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.io.output.CloseShieldOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.CryptographicOperationException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.HashService;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.election.model.ballotbox.BallotBoxId;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;
import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.mixnet.MixnetInitialPayload;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.CleansedBallotBoxRepositoryException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.CleansedBallotBoxServiceException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.DuplicateEntryException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.crypto.PrivateKeyForObjectRepository;
import ch.post.it.evoting.votingserver.commons.signature.SignatureFactory;
import ch.post.it.evoting.votingserver.commons.util.JsonUtils;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.common.csv.CleansedExportedBallotBoxItemWriter;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxInformationRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.CleansedBallotBox;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.CleansedBallotBoxRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.CleansedExportedBallotBoxItem;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionPublicKey;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionPublicKeyRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.tenant.EiTenantSystemKeys;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.vote.SuccessfulVote;
import ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.persistence.CleansedBallotBoxAccess;
import ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.persistence.SuccessfulVotesAccess;

public class CleansedBallotBoxServiceImpl implements CleansedBallotBoxService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CleansedBallotBoxServiceImpl.class);

	private static final int PAGE_SIZE = 500;
	private static final String KEYSTORE_ALIAS = "privatekey";
	private static final byte[] LINE_SEPARATOR = "\n".getBytes(StandardCharsets.UTF_8);
	private static final String TENANT_ID = "100";
	private static final String ENCRYPTION_PARAMETERS_JSON_FIELD = "encryptionParameters";
	private static final String ELECTORAL_AUTHORITY_ID_JSON_FIELD = "electoralAuthorityId";
	private static final String P_JSON_FIELD = "p";
	private static final String Q_JSON_FIELD = "q";
	private static final String G_JSON_FIELD = "g";

	@Inject
	private SignatureFactory signatureFactory;

	@Inject
	private CleansedBallotBoxRepository cleansedBallotBoxRepository;

	@Inject
	private BallotBoxInformationRepository ballotBoxInformationRepository;

	@Inject
	private CleansedBallotBoxAccess cleansedBallotBoxAccess;

	@Inject
	private SuccessfulVotesAccess successfulVotesAccess;

	@Inject
	private PrivateKeyForObjectRepository privateKeyRepository;

	@EJB
	private EiTenantSystemKeys eiTenantSystemKeys;

	@EJB
	private ElectionPublicKeyRepository electionPublicKeyRepository;

	@Inject
	private AsymmetricServiceAPI asymmetricService;

	@Inject
	private HashService hashService;

	@Override
	public boolean isBallotBoxEmpty(final String electionEventId, final String ballotBoxId) {
		checkNotNull(electionEventId);
		checkNotNull(ballotBoxId);

		return !cleansedBallotBoxRepository.exists(electionEventId, ballotBoxId);
	}

	@Override
	public MixnetInitialPayload getMixnetInitialPayload(final BallotBoxId ballotBoxId)
			throws ResourceNotFoundException, CleansedBallotBoxServiceException {

		checkNotNull(ballotBoxId);

		// Find out how many vote sets fit the ballot box.
		final int voteCount;
		try {
			voteCount = cleansedBallotBoxRepository.count(ballotBoxId);
		} catch (CleansedBallotBoxRepositoryException e) {
			throw new CleansedBallotBoxServiceException(String.format("Failed to count votes for ballot box %s.", ballotBoxId), e);
		}

		// Get the encryption parameters from the ballot box information.
		final JsonObject ballotBoxInformation = getBallotBoxInformationJson(ballotBoxId);
		final JsonObject encryptionParametersJson = ballotBoxInformation.getJsonObject(ENCRYPTION_PARAMETERS_JSON_FIELD);

		final BigInteger p = new BigInteger(encryptionParametersJson.getString(P_JSON_FIELD));
		final BigInteger q = new BigInteger(encryptionParametersJson.getString(Q_JSON_FIELD));
		final BigInteger g = new BigInteger(encryptionParametersJson.getString(G_JSON_FIELD));
		final GqGroup encryptionParameters = new GqGroup(p, q, g);

		// Convert the EncryptedVotes to ElGamalMultiRecipientCiphertext.
		final List<ElGamalMultiRecipientCiphertext> encryptedVotes = cleansedBallotBoxRepository.getVoteSet(ballotBoxId, 0, voteCount)
				.map(vote -> ElGamalMultiRecipientCiphertext.create(GqElement.create(vote.getGamma(), encryptionParameters),
						vote.getPhis().stream().map(bi -> GqElement.create(bi, encryptionParameters)).collect(Collectors.toList())))
				.collect(Collectors.toList());

		// Get the election public key.
		final ElGamalMultiRecipientPublicKey electionPublicKey;
		try {
			// Get the electoral authority identifier.
			final String electoralAuthorityId = ballotBoxInformation.getString(ELECTORAL_AUTHORITY_ID_JSON_FIELD);

			// Get the vote encryption key, which at this stage is the electoral authority public key.
			final ElGamalPublicKey voteEncryptionKey = getVoteEncryptionKey(TENANT_ID, ballotBoxId.getElectionEventId(), electoralAuthorityId);
			final List<ZpGroupElement> keys = voteEncryptionKey.getKeys();

			// Convert cryptolib public key to crypto-primitives public key.
			electionPublicKey = keys.stream().map(k -> GqElement.create(k.getValue(), encryptionParameters))
					.collect(Collectors.collectingAndThen(Collectors.toList(), ElGamalMultiRecipientPublicKey::new));
		} catch (GeneralCryptoLibException | IOException e) {
			throw new CleansedBallotBoxServiceException("Failed to retrieve election public key.");
		}

		// Get the certificate chain for the election information public key.
		LOGGER.info("Finding the validation key certificate chain for ballot box {}...", ballotBoxId);
		final X509Certificate[] fullCertificateChain = eiTenantSystemKeys.getSigningCertificateChain(TENANT_ID);
		if (null == fullCertificateChain) {
			throw new CleansedBallotBoxServiceException("No certificate chain was found for tenant " + TENANT_ID);
		}
		final X509Certificate[] certificateChain = new X509Certificate[fullCertificateChain.length - 1];
		System.arraycopy(fullCertificateChain, 0, certificateChain, 0, fullCertificateChain.length - 1);
		LOGGER.info("Obtained the validation key certificate for tenant {} with {} elements", TENANT_ID, certificateChain.length);

		// Create the initial payload to send.
		final MixnetInitialPayload mixnetInitialPayload = new MixnetInitialPayload(encryptionParameters, encryptedVotes, electionPublicKey);

		// Hash the payload.
		final byte[] payloadHash = hashService
				.recursiveHash(mixnetInitialPayload.getEncryptionGroup(), HashableList.from(mixnetInitialPayload.getEncryptedVotes()),
						mixnetInitialPayload.getElectionPublicKey());

		// Get the election information system key to sign the payload.
		final PrivateKey signingKey = eiTenantSystemKeys.getSigningPrivateKey(TENANT_ID);
		LOGGER.info("Obtained the signing key for tenant {}, signing the initial payload...", TENANT_ID);

		// Sign the payload hash.
		byte[] signature;
		try {
			signature = asymmetricService.sign(signingKey, payloadHash);
		} catch (GeneralCryptoLibException e) {
			throw new CleansedBallotBoxServiceException("Failed to sign the initial payload.", e);
		}
		final CryptolibPayloadSignature payloadSignature = new CryptolibPayloadSignature(signature, certificateChain);
		mixnetInitialPayload.setSignature(payloadSignature);
		LOGGER.info("Initial payload signed successfully.");

		return mixnetInitialPayload;
	}

	/**
	 * Retrieves a ballot box's information
	 *
	 * @param ballotBoxId the ballot box identifier
	 * @return a ballot box's information as a JSON object
	 * @throws ResourceNotFoundException if the ballot box is not found
	 */
	private JsonObject getBallotBoxInformationJson(BallotBoxId ballotBoxId) throws ResourceNotFoundException {
		return JsonUtils.getJsonObject(ballotBoxInformationRepository
				.findByTenantIdElectionEventIdBallotBoxId(ballotBoxId.getTenantId(), ballotBoxId.getElectionEventId(), ballotBoxId.getId())
				.getJson());
	}

	private Signature getSignature(final String tenantId, final String electionEventId, final String ballotBoxId) throws IOException {
		Signature signature = signatureFactory.newSignature();
		try {
			PrivateKey privateKey = privateKeyRepository.findByTenantEEIDObjectIdAlias(tenantId, electionEventId, ballotBoxId, KEYSTORE_ALIAS);
			signature.initSign(privateKey);
		} catch (InvalidKeyException | ResourceNotFoundException | CryptographicOperationException e) {
			throw new IOException("Failed to get signature.", e);
		}
		return signature;
	}

	private void writeCleansedBallotBoxItems(final OutputStream stream, final String tenantId, final String electionEventId, final String ballotBoxId)
			throws IOException {
		try (CleansedExportedBallotBoxItemWriter writer = new CleansedExportedBallotBoxItemWriter(new CloseShieldOutputStream(stream))) {
			int first = 1;
			int last = PAGE_SIZE;
			List<CleansedExportedBallotBoxItem> page = cleansedBallotBoxRepository
					.getEncryptedVotesByTenantIdElectionEventIdBallotBoxId(tenantId, electionEventId, ballotBoxId, first, last);
			while (!page.isEmpty()) {
				for (final CleansedExportedBallotBoxItem item : page) {
					writer.write(item);
				}
				first += PAGE_SIZE;
				last += PAGE_SIZE;
				page = cleansedBallotBoxRepository
						.getEncryptedVotesByTenantIdElectionEventIdBallotBoxId(tenantId, electionEventId, ballotBoxId, first, last);
			}
		}
	}

	private void writeSignature(final OutputStream stream, final Signature signature) throws IOException {
		byte[] base64Signature;
		try {
			base64Signature = Base64.getEncoder().encode(signature.sign());
		} catch (SignatureException e) {
			throw new IOException("Failed to sign encrypted ballot box.", e);
		}
		stream.write(LINE_SEPARATOR);
		stream.write(base64Signature);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void storeCleansedVote(Vote vote) throws DuplicateEntryException {
		CleansedBallotBox cleansedBallotBox = new CleansedBallotBox();
		cleansedBallotBox.setTenantId(vote.getTenantId());
		cleansedBallotBox.setElectionEventId(vote.getElectionEventId());
		cleansedBallotBox.setVotingCardId(vote.getVotingCardId());
		cleansedBallotBox.setBallotId(vote.getBallotId());
		cleansedBallotBox.setBallotBoxId(vote.getBallotBoxId());
		String encryptedVote = vote.getEncryptedOptions();

		cleansedBallotBox.setEncryptedVote(encryptedVote);
		cleansedBallotBoxAccess.save(cleansedBallotBox);
	}

	/**
	 * Store an entry that represents a successful vote. A successful vote entry is composed by the concatenation of the voting card ID and a
	 * timestamp.
	 *
	 * @param votingCardId the voting card ID
	 * @throws DuplicateEntryException if the successful vote already exists in the DB
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void storeSuccessfulVote(String tenantId, String electionEventId, String ballotBoxId, String votingCardId) throws DuplicateEntryException {
		ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
		SuccessfulVote successfulVote = new SuccessfulVote();
		successfulVote.setTimestamp(now);
		successfulVote.setTenantId(tenantId);
		successfulVote.setElectionEventId(electionEventId);
		successfulVote.setBallotBoxId(ballotBoxId);
		successfulVote.setVotingCardId(votingCardId);
		successfulVotesAccess.save(successfulVote);
	}

	/**
	 * Gets the electoral authority public key.
	 *
	 * @param tenantId             the identifier of the tenant that ows the electoral authority key
	 * @param electionEventId      the identifier of the election event linked to the electoral authority
	 * @param electoralAuthorityId the electoral authority identifier
	 * @return the electoral authority public key
	 * @throws ResourceNotFoundException if the electoral authority key is not found
	 * @throws GeneralCryptoLibException if there was a problem de-serialising the electoral authority key
	 * @throws IOException
	 */
	private ElGamalPublicKey getVoteEncryptionKey(String tenantId, String electionEventId, String electoralAuthorityId)
			throws ResourceNotFoundException, GeneralCryptoLibException, IOException {
		ElectionPublicKey entity = electionPublicKeyRepository
				.findByTenantIdElectionEventIdElectoralAuthorityId(tenantId, electionEventId, electoralAuthorityId);

		String elGamalPublicKeyJson = new String(Base64.getDecoder().decode(new ObjectMapper().readTree(entity.getJson()).get("publicKey").asText()),
				StandardCharsets.UTF_8);

		return ElGamalPublicKey.fromJson(elGamalPublicKeyJson);
	}
}
